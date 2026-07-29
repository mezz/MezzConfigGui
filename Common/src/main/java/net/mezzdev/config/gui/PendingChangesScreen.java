package net.mezzdev.config.gui;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.mezzdev.config.api.value.ConfigValueUpdateType;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.model.PendingConfigChange;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Confirmation screen shown when closing the config screen with unapplied changes.
 */
final class PendingChangesScreen extends Screen {
	private static final int BUTTON_WIDTH = 100;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_GAP = 4;
	private static final int BACK_BUTTON_TOP_MARGIN = 8;
	private static final int BACKGROUND_COLOR = 0xC0101010;
	private static final int PANEL_BACKGROUND_COLOR = 0xF0191D26;
	private static final int PANEL_BORDER_COLOR = 0x90FFFFFF;
	private static final int ROW_BACKGROUND_COLOR = 0xFF252B36;
	private static final int ROW_ALTERNATE_BACKGROUND_COLOR = 0xFF2B3342;
	private static final int ROW_HOVER_COLOR = 0xFF364456;
	private static final int TITLE_COLOR = 0xFFFFFFFF;
	private static final int MESSAGE_TEXT_COLOR = 0xFFE0E0E0;
	private static final int ROW_TEXT_COLOR = 0xFFE8EEF7;
	private static final int SCROLLBAR_TRACK_COLOR = 0xFF07080C;
	private static final int SCROLLBAR_MARKER_COLOR = 0xFF9AA8BC;
	private static final int CHANGE_TEXT_COLOR = 0xFFC9D3E2;
	private static final int SCREEN_PADDING = 24;
	private static final int PANEL_PADDING = 6;
	private static final int ROW_PADDING = 5;
	private static final int ROW_GAP = 2;
	private static final int SCROLLBAR_WIDTH = 5;
	private static final int SCROLL_AMOUNT = 24;
	private static final int CONTENT_MAX_WIDTH = 420;
	private static final int CONTENT_MIN_WIDTH = 160;
	private static final int TITLE_MESSAGE_GAP = 10;
	private static final int MESSAGE_PANEL_GAP = 10;
	private static final int PANEL_BUTTON_GAP = 12;
	private static final int MIN_PANEL_HEIGHT = 24;

	private final BooleanConsumer callback;
	private final Runnable backAction;
	private final ConfigValueUpdateType updateType;
	private final List<PendingConfigChange> pendingChanges;
	private int scrollOffset;
	private ImmutableRect2i changeListArea = ImmutableRect2i.EMPTY;
	private int changeListContentHeight;

	public PendingChangesScreen(
		BooleanConsumer callback,
		Runnable backAction,
		ConfigValueUpdateType updateType,
		List<PendingConfigChange> pendingChanges
	) {
		super(Component.translatable("mezz_config.config.screen.pendingChanges.title"));
		this.callback = callback;
		this.backAction = backAction;
		this.updateType = updateType;
		this.pendingChanges = List.copyOf(pendingChanges);
	}

	@Override
	protected void init() {
		super.init();
		ScreenLayout screenLayout = getScreenLayout();
		int actionButtonY = screenLayout.actionButtonY();
		int totalWidth = BUTTON_WIDTH * 2 + BUTTON_GAP;
		int x = (width - totalWidth) / 2;
		addRenderableWidget(Button.builder(
			Component.translatable("mezz_config.config.screen.apply"),
			button -> callback.accept(true)
		)
			.bounds(x, actionButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)
			.tooltip(Tooltip.create(getApplyInfo(updateType)))
			.build());
		addRenderableWidget(Button.builder(
			Component.translatable("mezz_config.config.screen.discard"),
			button -> callback.accept(false)
		)
			.bounds(x + BUTTON_WIDTH + BUTTON_GAP, actionButtonY, BUTTON_WIDTH, BUTTON_HEIGHT)
			.tooltip(Tooltip.create(Component.translatable("mezz_config.config.screen.discard.info")))
			.build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> backAction.run())
			.bounds((width - BUTTON_WIDTH) / 2, actionButtonY + BUTTON_HEIGHT + BACK_BUTTON_TOP_MARGIN, BUTTON_WIDTH, BUTTON_HEIGHT)
			.tooltip(Tooltip.create(Component.translatable("mezz_config.config.screen.back.info")))
			.build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(guiGraphics, mouseX, mouseY, partialTick);
		@Nullable PendingConfigChange hoveredChange = drawContent(guiGraphics, mouseX, mouseY);
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		drawTooltip(guiGraphics, mouseX, mouseY, hoveredChange);
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		guiGraphics.fill(0, 0, width, height, BACKGROUND_COLOR);
	}

	@Nullable
	private PendingConfigChange drawContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		ScreenLayout screenLayout = getScreenLayout();
		Component message = getPendingChangesMessage(updateType);
		List<FormattedCharSequence> messageLines = font.split(message, screenLayout.contentWidth());
		int messageY = screenLayout.messageY();

		guiGraphics.drawCenteredString(font, title, width / 2, screenLayout.titleY(), TITLE_COLOR);
		for (FormattedCharSequence line : messageLines) {
			guiGraphics.drawString(font, line, screenLayout.contentX(), messageY, MESSAGE_TEXT_COLOR);
			messageY += font.lineHeight;
		}

		this.changeListArea = new ImmutableRect2i(
			screenLayout.contentX(),
			screenLayout.panelY(),
			screenLayout.contentWidth(),
			screenLayout.panelHeight()
		);
		return drawChangeList(guiGraphics, changeListArea, mouseX, mouseY);
	}

	@Nullable
	private PendingConfigChange drawChangeList(GuiGraphics guiGraphics, ImmutableRect2i area, int mouseX, int mouseY) {
		guiGraphics.fill(area.getX(), area.getY(), area.getX() + area.getWidth(), area.getY() + area.getHeight(), PANEL_BACKGROUND_COLOR);
		drawBorder(guiGraphics, area, PANEL_BORDER_COLOR);

		ImmutableRect2i innerArea = area.insetBy(PANEL_PADDING);
		int contentWidth = innerArea.getWidth();
		this.changeListContentHeight = calculateChangeListHeight(contentWidth);
		if (changeListContentHeight > innerArea.getHeight()) {
			contentWidth -= SCROLLBAR_WIDTH + PANEL_PADDING;
			this.changeListContentHeight = calculateChangeListHeight(contentWidth);
		}

		clampScrollOffset();

		@Nullable PendingConfigChange hoveredChange = null;
		guiGraphics.enableScissor(
			innerArea.getX(),
			innerArea.getY(),
			innerArea.getX() + innerArea.getWidth(),
			innerArea.getY() + innerArea.getHeight()
		);
		int y = innerArea.getY() - scrollOffset;
		for (int i = 0; i < pendingChanges.size(); i++) {
			PendingConfigChange change = pendingChanges.get(i);
			int rowHeight = getChangeRowHeight(change, contentWidth);
			ImmutableRect2i rowArea = new ImmutableRect2i(innerArea.getX(), y, contentWidth, rowHeight);
			boolean rowVisible = y + rowHeight >= innerArea.getY() && y <= innerArea.getY() + innerArea.getHeight();
			boolean rowHovered = rowVisible && rowArea.contains(mouseX, mouseY);
			if (rowVisible) {
				drawChangeRow(guiGraphics, change, i, innerArea.getX(), y, contentWidth, rowHeight, rowHovered);
			}
			if (rowHovered) {
				hoveredChange = change;
			}
			y += rowHeight + ROW_GAP;
		}
		guiGraphics.disableScissor();
		drawScrollbar(guiGraphics, innerArea);
		return hoveredChange;
	}

	private void drawChangeRow(
		GuiGraphics guiGraphics,
		PendingConfigChange change,
		int index,
		int x,
		int y,
		int width,
		int height,
		boolean hovered
	) {
		int color = hovered ? ROW_HOVER_COLOR : index % 2 == 0 ? ROW_BACKGROUND_COLOR : ROW_ALTERNATE_BACKGROUND_COLOR;
		guiGraphics.fill(x, y, x + width, y + height, color);

		int textX = x + ROW_PADDING;
		int textY = y + ROW_PADDING;
		int textWidth = width - ROW_PADDING * 2;
		for (FormattedCharSequence line : font.split(change.name(), textWidth)) {
			guiGraphics.drawString(font, line, textX, textY, ROW_TEXT_COLOR);
			textY += font.lineHeight;
		}

		for (FormattedCharSequence line : font.split(change.valueChange(), textWidth)) {
			guiGraphics.drawString(font, line, textX, textY, CHANGE_TEXT_COLOR);
			textY += font.lineHeight;
		}
	}

	private int calculateChangeListHeight(int width) {
		int height = 0;
		for (PendingConfigChange change : pendingChanges) {
			height += getChangeRowHeight(change, width) + ROW_GAP;
		}
		if (!pendingChanges.isEmpty()) {
			height -= ROW_GAP;
		}
		return height;
	}

	private int getChangeRowHeight(PendingConfigChange change, int width) {
		int textWidth = width - ROW_PADDING * 2;
		int nameLines = font.split(change.name(), textWidth).size();
		int valueLines = font.split(change.valueChange(), textWidth).size();
		return ROW_PADDING * 2 + (nameLines + valueLines) * font.lineHeight;
	}

	private void drawScrollbar(GuiGraphics guiGraphics, ImmutableRect2i innerArea) {
		if (!hasScrollbar()) {
			return;
		}
		int x = innerArea.getX() + innerArea.getWidth() - SCROLLBAR_WIDTH;
		int y = innerArea.getY();
		int height = innerArea.getHeight();
		guiGraphics.fill(x, y, x + SCROLLBAR_WIDTH, y + height, SCROLLBAR_TRACK_COLOR);

		int markerHeight = Math.max(12, height * height / changeListContentHeight);
		int maxScroll = getMaxScroll();
		int markerY = y;
		if (maxScroll > 0) {
			markerY += (height - markerHeight) * scrollOffset / maxScroll;
		}
		guiGraphics.fill(x, markerY, x + SCROLLBAR_WIDTH, markerY + markerHeight, SCROLLBAR_MARKER_COLOR);
	}

	private boolean hasScrollbar() {
		return changeListContentHeight > getChangeListInnerHeight();
	}

	private int getMaxScroll() {
		return Math.max(0, changeListContentHeight - getChangeListInnerHeight());
	}

	private int getChangeListInnerHeight() {
		if (changeListArea.isEmpty()) {
			return 0;
		}
		return Math.max(0, changeListArea.getHeight() - PANEL_PADDING * 2);
	}

	private static void drawTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, @Nullable PendingConfigChange hoveredChange) {
		if (hoveredChange == null) {
			return;
		}

		ConfigInfo info = hoveredChange.tooltipInfo();
		ConfigTooltip tooltip = new ConfigTooltip();
		tooltip.add(info.title().copy().withStyle(ChatFormatting.WHITE));
		for (Component line : info.lines()) {
			tooltip.add(line.copy().withStyle(ChatFormatting.GRAY));
		}
		tooltip.draw(guiGraphics, mouseX, mouseY);
	}

	private void clampScrollOffset() {
		scrollOffset = Math.clamp(scrollOffset, 0, getMaxScroll());
	}

	private static void drawBorder(GuiGraphics guiGraphics, ImmutableRect2i area, int color) {
		guiGraphics.fill(area.getX(), area.getY(), area.getX() + area.getWidth(), area.getY() + 1, color);
		guiGraphics.fill(area.getX(), area.getY(), area.getX() + 1, area.getY() + area.getHeight(), color);
		guiGraphics.fill(area.getX(), area.getY() + area.getHeight() - 1, area.getX() + area.getWidth(), area.getY() + area.getHeight(), color);
		guiGraphics.fill(area.getX() + area.getWidth() - 1, area.getY(), area.getX() + area.getWidth(), area.getY() + area.getHeight(), color);
	}

	private ScreenLayout getScreenLayout() {
		int contentWidth = Math.min(CONTENT_MAX_WIDTH, Math.max(CONTENT_MIN_WIDTH, width - SCREEN_PADDING * 2));
		int contentX = (width - contentWidth) / 2;
		Component message = getPendingChangesMessage(updateType);
		List<FormattedCharSequence> messageLines = font.split(message, contentWidth);
		int messageHeight = messageLines.size() * font.lineHeight;
		int headerHeight = font.lineHeight + TITLE_MESSAGE_GAP + messageHeight + MESSAGE_PANEL_GAP;
		int buttonBlockHeight = BUTTON_HEIGHT * 2 + BACK_BUTTON_TOP_MARGIN;
		int maxPanelHeight = Math.max(
			MIN_PANEL_HEIGHT,
			height - SCREEN_PADDING * 2 - headerHeight - PANEL_BUTTON_GAP - buttonBlockHeight
		);
		int panelHeight = getPreferredPanelHeight(contentWidth, maxPanelHeight);
		int totalHeight = headerHeight + panelHeight + PANEL_BUTTON_GAP + buttonBlockHeight;
		int titleY = Math.max(SCREEN_PADDING, (height - totalHeight) / 2);
		int messageY = titleY + font.lineHeight + TITLE_MESSAGE_GAP;
		int panelY = messageY + messageHeight + MESSAGE_PANEL_GAP;
		int actionButtonY = panelY + panelHeight + PANEL_BUTTON_GAP;
		return new ScreenLayout(contentX, contentWidth, titleY, messageY, panelY, panelHeight, actionButtonY);
	}

	private int getPreferredPanelHeight(int contentWidth, int maxPanelHeight) {
		int innerWidth = contentWidth - PANEL_PADDING * 2;
		int contentHeight = calculateChangeListHeight(innerWidth);
		int preferredPanelHeight = contentHeight + PANEL_PADDING * 2;
		if (preferredPanelHeight <= maxPanelHeight) {
			return Math.clamp(preferredPanelHeight, MIN_PANEL_HEIGHT, maxPanelHeight);
		}

		int scrolledInnerWidth = Math.max(1, innerWidth - SCROLLBAR_WIDTH - PANEL_PADDING);
		contentHeight = calculateChangeListHeight(scrolledInnerWidth);
		preferredPanelHeight = contentHeight + PANEL_PADDING * 2;
		return Math.clamp(preferredPanelHeight, MIN_PANEL_HEIGHT, maxPanelHeight);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (changeListArea.contains(mouseX, mouseY) && getMaxScroll() > 0) {
			scrollOffset -= (int) Math.round(scrollY * SCROLL_AMOUNT);
			clampScrollOffset();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			backAction.run();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		backAction.run();
	}

	private static Component getPendingChangesMessage(ConfigValueUpdateType updateType) {
		if (updateType == ConfigValueUpdateType.RESTART) {
			return Component.translatable("mezz_config.config.screen.pendingChanges.restart.message");
		}
		return Component.translatable("mezz_config.config.screen.pendingChanges.message");
	}

	private static Component getApplyInfo(ConfigValueUpdateType updateType) {
		if (updateType == ConfigValueUpdateType.RESTART) {
			return Component.translatable("mezz_config.config.screen.apply.restart.info");
		}
		return Component.translatable("mezz_config.config.screen.apply.info");
	}

	private record ScreenLayout(
		int contentX,
		int contentWidth,
		int titleY,
		int messageY,
		int panelY,
		int panelHeight,
		int actionButtonY
	) {
	}
}
