package net.mezzdev.config.gui;

import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.textures.ConfigButtonIcon;
import net.mezzdev.config.gui.textures.ConfigScalableDrawable;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.model.ConfigNavItem;
import net.mezzdev.config.gui.model.ConfigScreenModel;
import net.mezzdev.config.gui.popup.ConfigPopupSelector;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Draws the config screen frame, navigation, value rows, info panel, popups, and tooltips.
 */
final class ConfigScreenView {
	private static final int VALUE_SELECTOR_Z_OFFSET = 350;
	private static final int INFO_PADDING = 5;
	private static final int INFO_BACKGROUND_COLOR = 0xE0101218;
	private static final int INFO_BORDER_COLOR = 0x70FFFFFF;
	private static final int INFO_TITLE_COLOR = 0xFFF3F6FF;
	private static final int INFO_TEXT_COLOR = 0xFFC9D3E2;
	private static final int TITLE_TEXT_COLOR = 0xFF404040;
	private static final int RESIZE_GRIP_SIZE = 11;
	private static final int RESIZE_GRIP_LINE_GAP = 3;
	private static final int RESIZE_EDGE_HIGHLIGHT_SIZE = 2;
	private static final int RESIZE_GRIP_COLOR = 0x45FFFFFF;
	private static final int RESIZE_HANDLE_HOVER_COLOR = 0x80FFFFFF;
	private static final int VALUE_AREA_BACKGROUND_COLOR = 0x82000000;
	private static final int INSET_BORDER_DARK_COLOR = 0xB0000000;
	private static final int INSET_BORDER_LIGHT_COLOR = 0x35FFFFFF;

	private final Component title;
	private final EditBox searchBox;
	private final ConfigScreenModel model;
	private final ConfigScreenLayout layout;
	private final ConfigScreenController controller;
	private final ConfigTextures textures;
	private final ConfigScalableDrawable background;
	private final ConfigScalableDrawable scrollbarMarker;
	private final ConfigScalableDrawable scrollbarBackground;

	ConfigScreenView(
		Component title,
		EditBox searchBox,
		ConfigScreenModel model,
		ConfigScreenLayout layout,
		ConfigScreenController controller,
		ConfigTextures textures
	) {
		this.title = title;
		this.searchBox = searchBox;
		this.model = model;
		this.layout = layout;
		this.controller = controller;
		this.textures = textures;
		this.background = textures.getConfigScreenBackground();
		this.scrollbarMarker = textures.getScrollbarMarker();
		this.scrollbarBackground = textures.getScrollbarBackground();
	}

	void render(
		GuiGraphics guiGraphics,
		int mouseX,
		int mouseY,
		float partialTick,
		@Nullable ConfigPopupSelector valueSelector
	) {
		Font font = Minecraft.getInstance().font;
		ImmutableRect2i area = layout.getArea();
		ConfigScreenLayout.ResizeHandle resizeHandle = layout.getActiveResizeHandle(mouseX, mouseY);
		ImmutableRect2i titleArea = layout.getTitleTextArea();
		ImmutableRect2i navArea = layout.getNavArea();
		ImmutableRect2i contentArea = layout.getContentArea();
		ImmutableRect2i valueSelectorClipArea = getValueSelectorClipArea(contentArea);
		ImmutableRect2i searchBackgroundArea = layout.getSearchBackgroundArea();
		ImmutableRect2i screenListButtonArea = layout.getScreenListButtonArea();
		ImmutableRect2i applyPendingChangesButtonArea = layout.getApplyPendingChangesButtonArea();
		ImmutableRect2i undoChangesButtonArea = layout.getUndoChangesButtonArea();
		@Nullable
		ConfigInfo hoveredValueSelectorInfo = getValueSelectorInfo(valueSelector, valueSelectorClipArea, mouseX, mouseY);
		@Nullable
		ConfigInfo activeValueSelectorInfo = getActiveValueSelectorInfo(valueSelector);
		@Nullable
		ConfigInfo tooltipInfo = getTooltipInfo(
			valueSelector,
			valueSelectorClipArea,
			contentArea,
			screenListButtonArea,
			applyPendingChangesButtonArea,
			undoChangesButtonArea,
			mouseX,
			mouseY
		);
		@Nullable
		ConfigInfo hoveredControlInfo = getControlInfo(
			searchBackgroundArea,
			resizeHandle,
			mouseX,
			mouseY
		);

		guiGraphics.pose().pushPose();
		background.draw(guiGraphics, area);
		drawResizeHandles(guiGraphics, area, resizeHandle);
		drawTitle(guiGraphics, font, titleArea, title);
		drawActionButtons(guiGraphics, screenListButtonArea, applyPendingChangesButtonArea, undoChangesButtonArea, mouseX, mouseY);
		drawNavBackground(guiGraphics, navArea);
		@Nullable
		ConfigNavItem hoveredNavItem = drawNavItems(guiGraphics, navArea, mouseX, mouseY);
		drawInsetBorder(guiGraphics, navArea);
		drawNavScrollBar(guiGraphics);
		drawSearch(guiGraphics, textures, searchBackgroundArea, mouseX, mouseY, partialTick);
		drawValueAreaBackground(guiGraphics, contentArea);
		@Nullable
		ConfigInfo hoveredEntryInfo = drawEntries(guiGraphics, contentArea, mouseX, mouseY, valueSelector == null);
		drawInsetBorder(guiGraphics, contentArea);
		drawInfoPanel(guiGraphics, font, getInfo(hoveredValueSelectorInfo, hoveredControlInfo, hoveredNavItem, hoveredEntryInfo, activeValueSelectorInfo));
		guiGraphics.pose().popPose();

		drawContentScrollBar(guiGraphics);
		drawValueSelector(guiGraphics, valueSelector, valueSelectorClipArea, mouseX, mouseY);
		drawTooltip(guiGraphics, mouseX, mouseY, tooltipInfo);
	}

	static ImmutableRect2i getValueSelectorClipArea(ImmutableRect2i contentArea) {
		return contentArea.insetBy(1);
	}

	@Nullable
	private static ConfigInfo getActiveValueSelectorInfo(@Nullable ConfigPopupSelector valueSelector) {
		if (valueSelector == null) {
			return null;
		}
		return valueSelector.getInfo();
	}

	private static void drawTitle(GuiGraphics guiGraphics, Font font, ImmutableRect2i titleArea, Component title) {
		ConfigEntryWidget.drawFittedText(
			guiGraphics,
			font,
			title,
			titleArea,
			TITLE_TEXT_COLOR,
			true
		);
	}

	private void drawActionButtons(
		GuiGraphics guiGraphics,
		ImmutableRect2i screenListButtonArea,
		ImmutableRect2i applyPendingChangesButtonArea,
		ImmutableRect2i undoChangesButtonArea,
		int mouseX,
		int mouseY
	) {
		drawActionButton(
			guiGraphics,
			screenListButtonArea,
			ConfigButtonIcon.SCREEN_LIST,
			!screenListButtonArea.isEmpty(),
			screenListButtonArea.contains(mouseX, mouseY)
		);
		drawActionButton(
			guiGraphics,
			undoChangesButtonArea,
			ConfigButtonIcon.X,
			controller.hasUndoableChanges(),
			undoChangesButtonArea.contains(mouseX, mouseY)
		);
		drawActionButton(
			guiGraphics,
			applyPendingChangesButtonArea,
			ConfigButtonIcon.CHECK,
			controller.hasPendingChanges(),
			applyPendingChangesButtonArea.contains(mouseX, mouseY)
		);
	}

	private void drawActionButton(
		GuiGraphics guiGraphics,
		ImmutableRect2i area,
		ConfigButtonIcon icon,
		boolean active,
		boolean hovered
	) {
		if (area.isEmpty()) {
			return;
		}
		ConfigEntryWidget.drawButtonBackground(guiGraphics, textures, area, active, active && hovered);
		icon.draw(guiGraphics, area, active);
	}

	private static void drawResizeHandles(GuiGraphics guiGraphics, ImmutableRect2i area, ConfigScreenLayout.ResizeHandle resizeHandle) {
		if (area.isEmpty()) {
			return;
		}
		drawResizeGrip(guiGraphics, area, resizeHandle == ConfigScreenLayout.ResizeHandle.BOTTOM_RIGHT);
		if (resizeHandle == ConfigScreenLayout.ResizeHandle.NONE) {
			return;
		}
		drawResizeEdgeHighlight(guiGraphics, area, resizeHandle);
	}

	private static void drawResizeGrip(GuiGraphics guiGraphics, ImmutableRect2i area, boolean hovered) {
		int color = getResizeGripColor(hovered);
		int right = area.getX() + area.getWidth() - 4;
		int bottom = area.getY() + area.getHeight() - 4;
		for (int i = 0; i < 3; i++) {
			int lineLength = RESIZE_GRIP_SIZE - i * RESIZE_GRIP_LINE_GAP;
			int lineRight = right - i * RESIZE_GRIP_LINE_GAP;
			drawResizeGripLine(guiGraphics, lineRight - lineLength, bottom, lineRight, bottom - lineLength, color);
		}
	}

	private static int getResizeGripColor(boolean hovered) {
		if (hovered) {
			return RESIZE_HANDLE_HOVER_COLOR;
		}
		return RESIZE_GRIP_COLOR;
	}

	private static void drawResizeGripLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
		int length = Math.min(x2 - x1, y1 - y2);
		for (int i = 0; i <= length; i++) {
			guiGraphics.fill(x1 + i, y1 - i, x1 + i + 1, y1 - i + 1, color);
		}
	}

	private static void drawResizeEdgeHighlight(
		GuiGraphics guiGraphics,
		ImmutableRect2i area,
		ConfigScreenLayout.ResizeHandle resizeHandle
	) {
		int x = area.getX();
		int y = area.getY();
		int right = x + area.getWidth();
		int bottom = y + area.getHeight();
		if (resizeHandle.left()) {
			guiGraphics.fill(x, y, x + RESIZE_EDGE_HIGHLIGHT_SIZE, bottom, RESIZE_HANDLE_HOVER_COLOR);
		}
		if (resizeHandle.right()) {
			guiGraphics.fill(right - RESIZE_EDGE_HIGHLIGHT_SIZE, y, right, bottom, RESIZE_HANDLE_HOVER_COLOR);
		}
		if (resizeHandle.top()) {
			guiGraphics.fill(x, y, right, y + RESIZE_EDGE_HIGHLIGHT_SIZE, RESIZE_HANDLE_HOVER_COLOR);
		}
		if (resizeHandle.bottom()) {
			guiGraphics.fill(x, bottom - RESIZE_EDGE_HIGHLIGHT_SIZE, right, bottom, RESIZE_HANDLE_HOVER_COLOR);
		}
	}

	private static void drawNavBackground(GuiGraphics guiGraphics, ImmutableRect2i navArea) {
		guiGraphics.fill(
			navArea.getX(),
			navArea.getY(),
			navArea.getX() + navArea.getWidth(),
			navArea.getY() + navArea.getHeight(),
			0x18000000
		);
	}

	@Nullable
	private ConfigNavItem drawNavItems(GuiGraphics guiGraphics, ImmutableRect2i navArea, int mouseX, int mouseY) {
		@Nullable
		ConfigNavItem hoveredNavItem = null;
		guiGraphics.enableScissor(
			navArea.getX(),
			navArea.getY(),
			navArea.getX() + navArea.getWidth(),
			navArea.getY() + navArea.getHeight()
		);
		List<ConfigNavItem> navItems = model.getNavItems();
		for (int i = 0; i < navItems.size(); i++) {
			ConfigNavItem navItem = navItems.get(i);
			navItem.draw(guiGraphics, mouseX, mouseY, !model.isSearching() && i == model.getActiveCategoryIndex());
			if (navArea.contains(mouseX, mouseY) && navItem.isMouseOver(mouseX, mouseY)) {
				hoveredNavItem = navItem;
			}
		}
		guiGraphics.disableScissor();
		return hoveredNavItem;
	}

	private void drawSearch(
		GuiGraphics guiGraphics,
		ConfigTextures textures,
		ImmutableRect2i searchBackgroundArea,
		int mouseX,
		int mouseY,
		float partialTick
	) {
		textures.getSearchBackground()
			.draw(guiGraphics, searchBackgroundArea);
		searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private static void drawValueAreaBackground(GuiGraphics guiGraphics, ImmutableRect2i contentArea) {
		guiGraphics.fill(
			contentArea.getX(),
			contentArea.getY(),
			contentArea.getX() + contentArea.getWidth(),
			contentArea.getY() + contentArea.getHeight(),
			VALUE_AREA_BACKGROUND_COLOR
		);
	}

	private static void drawInsetBorder(GuiGraphics guiGraphics, ImmutableRect2i area) {
		if (area.isEmpty()) {
			return;
		}
		int x = area.getX();
		int y = area.getY();
		int right = x + area.getWidth();
		int bottom = y + area.getHeight();

		guiGraphics.fill(x, y, right, y + 1, INSET_BORDER_DARK_COLOR);
		guiGraphics.fill(x, y, x + 1, bottom, INSET_BORDER_DARK_COLOR);
		guiGraphics.fill(
			x,
			bottom - 1,
			right,
			bottom,
			INSET_BORDER_LIGHT_COLOR
		);
		guiGraphics.fill(
			right - 1,
			y,
			right,
			bottom,
			INSET_BORDER_LIGHT_COLOR
		);
	}

	@Nullable
	private ConfigInfo drawEntries(
		GuiGraphics guiGraphics,
		ImmutableRect2i contentArea,
		int mouseX,
		int mouseY,
		boolean allowEntryHover
	) {
		@Nullable
		ConfigInfo hoveredEntryInfo = null;
		guiGraphics.enableScissor(
			contentArea.getX(),
			contentArea.getY(),
			contentArea.getX() + contentArea.getWidth(),
			contentArea.getY() + contentArea.getHeight()
		);
		int rowIndex = 0;
		boolean allowHoverAtMouse = isEntryHoverAllowed(contentArea, mouseX, mouseY, allowEntryHover);
		for (ConfigEntryWidget<?> entryWidget : controller.getVisibleEntryWidgets()) {
			if (entryWidget.getArea().equals(ImmutableRect2i.EMPTY)) {
				continue;
			}
			entryWidget.draw(guiGraphics, mouseX, mouseY, allowHoverAtMouse, rowIndex);
			rowIndex++;
			if (allowHoverAtMouse && entryWidget.isMouseOver(mouseX, mouseY)) {
				hoveredEntryInfo = entryWidget.getInfo(mouseX, mouseY);
			}
		}
		guiGraphics.disableScissor();
		return hoveredEntryInfo;
	}

	static boolean isEntryHoverAllowed(ImmutableRect2i contentArea, int mouseX, int mouseY, boolean allowEntryHover) {
		return allowEntryHover && contentArea.contains(mouseX, mouseY);
	}

	@Nullable
	private ConfigInfo getValueSelectorInfo(
		@Nullable ConfigPopupSelector valueSelector,
		ImmutableRect2i valueSelectorClipArea,
		int mouseX,
		int mouseY
	) {
		if (valueSelector != null && valueSelectorClipArea.contains(mouseX, mouseY) && valueSelector.isMouseOver(mouseX, mouseY)) {
			return valueSelector.getInfo();
		}
		return null;
	}

	@Nullable
	private ConfigInfo getTooltipInfo(
		@Nullable ConfigPopupSelector valueSelector,
		ImmutableRect2i valueSelectorClipArea,
		ImmutableRect2i contentArea,
		ImmutableRect2i screenListButtonArea,
		ImmutableRect2i applyPendingChangesButtonArea,
		ImmutableRect2i undoChangesButtonArea,
		int mouseX,
		int mouseY
	) {
		@Nullable
		ConfigInfo actionButtonTooltipInfo = getActionButtonTooltipInfo(
			screenListButtonArea,
			applyPendingChangesButtonArea,
			undoChangesButtonArea,
			mouseX,
			mouseY
		);
		if (actionButtonTooltipInfo != null) {
			return actionButtonTooltipInfo;
		}
		if (valueSelector != null && valueSelectorClipArea.contains(mouseX, mouseY)) {
			@Nullable
			ConfigInfo valueSelectorTooltipInfo = valueSelector.getTooltipInfo(mouseX, mouseY);
			if (valueSelectorTooltipInfo != null) {
				return valueSelectorTooltipInfo;
			}
			if (valueSelector.isMouseOver(mouseX, mouseY)) {
				return null;
			}
		}
		if (!contentArea.contains(mouseX, mouseY)) {
			return null;
		}
		for (ConfigEntryWidget<?> entryWidget : controller.getVisibleEntryWidgets()) {
			if (entryWidget.getArea().equals(ImmutableRect2i.EMPTY) || !entryWidget.isMouseOver(mouseX, mouseY)) {
				continue;
			}
			@Nullable
			ConfigInfo info = entryWidget.getTooltipInfo(mouseX, mouseY);
			if (info != null) {
				return info;
			}
		}
		return null;
	}

	@Nullable
	private ConfigInfo getActionButtonTooltipInfo(
		ImmutableRect2i screenListButtonArea,
		ImmutableRect2i applyPendingChangesButtonArea,
		ImmutableRect2i undoChangesButtonArea,
		int mouseX,
		int mouseY
	) {
		if (screenListButtonArea.contains(mouseX, mouseY)) {
			return getScreenListTooltipInfo();
		}
		if (undoChangesButtonArea.contains(mouseX, mouseY)) {
			return getUndoChangesTooltipInfo();
		}
		if (applyPendingChangesButtonArea.contains(mouseX, mouseY)) {
			return getApplyPendingChangesTooltipInfo();
		}
		return null;
	}

	private void drawNavScrollBar(GuiGraphics guiGraphics) {
		drawScrollBar(guiGraphics, layout.getNavScrollBarArea(), layout.getNavScrollMarkerArea());
	}

	private void drawContentScrollBar(GuiGraphics guiGraphics) {
		drawScrollBar(guiGraphics, layout.getScrollBarArea(), layout.getScrollMarkerArea());
	}

	private void drawScrollBar(GuiGraphics guiGraphics, ImmutableRect2i scrollBarArea, ImmutableRect2i scrollMarkerArea) {
		if (!scrollMarkerArea.isEmpty()) {
			scrollbarBackground.draw(guiGraphics, scrollBarArea);
			scrollbarMarker.draw(guiGraphics, scrollMarkerArea);
		}
	}

	@Nullable
	private ConfigInfo getInfo(
		@Nullable ConfigInfo hoveredValueSelectorInfo,
		@Nullable ConfigInfo hoveredControlInfo,
		@Nullable ConfigNavItem hoveredNavItem,
		@Nullable ConfigInfo hoveredEntryInfo,
		@Nullable ConfigInfo activeValueSelectorInfo
	) {
		if (hoveredValueSelectorInfo != null) {
			return hoveredValueSelectorInfo;
		}
		if (hoveredControlInfo != null) {
			return hoveredControlInfo;
		}
		if (hoveredEntryInfo != null) {
			return hoveredEntryInfo;
		}
		if (hoveredNavItem != null) {
			return hoveredNavItem.getInfo();
		}
		if (activeValueSelectorInfo != null) {
			return activeValueSelectorInfo;
		}
		if (model.isSearching()) {
			return getSearchInfo();
		}
		if (model.hasActiveCategory()) {
			return model.getActiveCategoryWidget().getInfo();
		}
		return null;
	}

	@Nullable
	private ConfigInfo getControlInfo(
		ImmutableRect2i searchBackgroundArea,
		ConfigScreenLayout.ResizeHandle resizeHandle,
		int mouseX,
		int mouseY
	) {
		if (resizeHandle != ConfigScreenLayout.ResizeHandle.NONE) {
			return getResizeInfo();
		}
		if (searchBackgroundArea.contains(mouseX, mouseY)) {
			return getSearchInfo();
		}
		return null;
	}

	private static ConfigInfo getSearchInfo() {
		return new ConfigInfo(
			Component.translatable("mezz_config.config.screen.search.info.title"),
			Component.translatable("mezz_config.config.screen.search.info")
		);
	}

	private static ConfigInfo getScreenListTooltipInfo() {
		return new ConfigInfo(
			Component.translatable("mezz_config.config.screen.allMods.title"),
			Component.translatable("mezz_config.config.screen.allMods.info")
		);
	}

	private ConfigInfo getUndoChangesTooltipInfo() {
		String infoKey = getUndoChangesInfoKey();
		return new ConfigInfo(
			Component.translatable("mezz_config.config.screen.undo.title"),
			Component.translatable(infoKey)
		);
	}

	private String getUndoChangesInfoKey() {
		if (controller.hasUndoableChanges()) {
			return "mezz_config.config.screen.undo.info";
		}
		return "mezz_config.config.screen.undo.disabled.info";
	}

	private ConfigInfo getApplyPendingChangesTooltipInfo() {
		String infoKey = getApplyPendingChangesInfoKey();
		return new ConfigInfo(
			Component.translatable("mezz_config.config.screen.applyPending.title"),
			Component.translatable(infoKey)
		);
	}

	private String getApplyPendingChangesInfoKey() {
		if (controller.hasPendingChanges()) {
			return "mezz_config.config.screen.applyPending.info";
		}
		return "mezz_config.config.screen.applyPending.disabled.info";
	}

	private static ConfigInfo getResizeInfo() {
		return new ConfigInfo(
			Component.translatable("mezz_config.config.screen.resize.title"),
			Component.translatable("mezz_config.config.screen.resize.info")
		);
	}

	private void drawInfoPanel(GuiGraphics guiGraphics, Font font, @Nullable ConfigInfo info) {
		ImmutableRect2i infoArea = layout.getInfoArea();
		guiGraphics.fill(
			infoArea.getX(),
			infoArea.getY(),
			infoArea.getX() + infoArea.getWidth(),
			infoArea.getY() + infoArea.getHeight(),
			INFO_BACKGROUND_COLOR
		);
		guiGraphics.fill(
			infoArea.getX(),
			infoArea.getY(),
			infoArea.getX() + infoArea.getWidth(),
			infoArea.getY() + 1,
			INFO_BORDER_COLOR
		);
		if (info == null) {
			return;
		}

		int textX = infoArea.getX() + INFO_PADDING;
		int textY = infoArea.getY() + INFO_PADDING;
		int maxTextY = infoArea.getY() + infoArea.getHeight() - INFO_PADDING;
		int textWidth = infoArea.getWidth() - INFO_PADDING * 2;

		guiGraphics.enableScissor(
			infoArea.getX(),
			infoArea.getY(),
			infoArea.getX() + infoArea.getWidth(),
			infoArea.getY() + infoArea.getHeight()
		);

		List<FormattedCharSequence> titleLines = font.split(info.title(), textWidth);
		if (!titleLines.isEmpty()) {
			guiGraphics.drawString(font, titleLines.getFirst(), textX, textY, INFO_TITLE_COLOR, false);
			textY += font.lineHeight + 2;
		}

		for (Component line : info.lines()) {
			for (FormattedCharSequence wrappedLine : font.split(line, textWidth)) {
				if (textY + font.lineHeight > maxTextY) {
					guiGraphics.disableScissor();
					return;
				}
				guiGraphics.drawString(font, wrappedLine, textX, textY, INFO_TEXT_COLOR, false);
				textY += font.lineHeight;
			}
		}

		guiGraphics.disableScissor();
	}

	private static void drawValueSelector(
		GuiGraphics guiGraphics,
		@Nullable ConfigPopupSelector valueSelector,
		ImmutableRect2i valueSelectorClipArea,
		int mouseX,
		int mouseY
	) {
		if (valueSelector == null || valueSelectorClipArea.isEmpty()) {
			return;
		}
		guiGraphics.enableScissor(
			valueSelectorClipArea.getX(),
			valueSelectorClipArea.getY(),
			valueSelectorClipArea.getX() + valueSelectorClipArea.getWidth(),
			valueSelectorClipArea.getY() + valueSelectorClipArea.getHeight()
		);
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(0, 0, VALUE_SELECTOR_Z_OFFSET);
		valueSelector.draw(guiGraphics, mouseX, mouseY);
		guiGraphics.pose().popPose();
		guiGraphics.disableScissor();
	}

	private static void drawTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, @Nullable ConfigInfo info) {
		if (info == null) {
			return;
		}
		ConfigTooltip tooltip = new ConfigTooltip();
		tooltip.add(withDefaultColor(info.title(), ChatFormatting.WHITE));
		for (Component line : info.lines()) {
			tooltip.add(withDefaultColor(line, ChatFormatting.GRAY));
		}
		tooltip.draw(guiGraphics, mouseX, mouseY);
	}

	private static MutableComponent withDefaultColor(Component component, ChatFormatting color) {
		MutableComponent result = component.copy();
		if (result.getStyle().getColor() == null) {
			result.withStyle(color);
		}
		return result;
	}
}
