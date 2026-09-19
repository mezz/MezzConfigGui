package net.mezzdev.config.gui.screenlist;

import net.mezzdev.config.gui.ConfigInputUtil;

import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.ConfigScreenResizer;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.input.InputType;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.MezzConfigScreen;
import net.mezzdev.config.gui.textures.ConfigScalableDrawable;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ConfigLocale;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.mezzdev.config.gui.api.LegacyGuiGraphics;
import net.mezzdev.config.gui.LegacyEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Screen for choosing one of the config screens discovered for the current mod loader.
 */
public final class ConfigScreenListScreen extends MezzConfigScreen {
	private static final Component TITLE = Component.translatable("mezz_config.config.screen.list.title");
	private static final Component SEARCH = Component.translatable("mezz_config.config.screen.list.search");
	private static final int BORDER_PADDING = 6;
	private static final int SECTION_GAP = BORDER_PADDING / 2;
	private static final int TITLE_HEIGHT = 18;
	private static final int SEARCH_HEIGHT = 18;
	private static final int SEARCH_TEXT_LEFT_PADDING = 5;
	private static final int SEARCH_TEXT_RIGHT_PADDING = 4;
	private static final int SEARCH_TEXT_HEIGHT = 8;
	private static final int ROW_HEIGHT = 42;
	private static final int ROW_GAP = 2;
	private static final int ROW_PADDING = 5;
	private static final int ICON_SIZE = 32;
	private static final int SCROLLBAR_WIDTH = 12;
	private static final int SCROLLBAR_GAP = 2;
	private static final int SCROLL_MARKER_TRACK_INSET = 1;
	private static final int MIN_SCROLL_MARKER_HEIGHT = 10;
	private static final double SCROLL_LERP = 0.35;

	private final List<ConfigScreenListEntry> entries;
	private final LegacyEditBox searchBox;
	private final ConfigTextures textures;
	private final ConfigScalableDrawable background;
	private final ConfigScalableDrawable scrollbarBackground;
	private final ConfigScalableDrawable scrollbarMarker;
	private final ConfigScreenResizer resizer = new ConfigScreenResizer();

	@Nullable
	private final Screen parent;

	private List<ConfigScreenListEntry> visibleEntries;
	private ImmutableRect2i area = ImmutableRect2i.EMPTY;
	private ImmutableRect2i titleArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i searchArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i listArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i scrollBarArea = ImmutableRect2i.EMPTY;
	private int totalListHeight;
	private double targetScrollY;
	private double currentScrollY;
	private boolean draggingScroll;
	private double scrollDragOffsetY;
	@Nullable
	private ConfigScreenListEntry pressedEntry;

	private ConfigScreenListScreen(
		@Nullable Screen parent,
		List<ConfigScreenListEntry> entries
	) {
		super(TITLE);
		this.parent = parent;
		this.entries = List.copyOf(entries);
		this.visibleEntries = this.entries;
		this.textures = ConfigTextures.get();
		this.background = textures.getConfigScreenBackground();
		this.scrollbarBackground = textures.getScrollbarBackground();
		this.scrollbarMarker = textures.getScrollbarMarker();

		Font font = Minecraft.getInstance().font;
		this.searchBox = new LegacyEditBox(font, 0, 0, 0, SEARCH_HEIGHT, SEARCH);
		this.searchBox.setMaxLength(64);
		this.searchBox.setBordered(false);
		this.searchBox.setHint(SEARCH);
		updateSearchTextColor("");
		this.searchBox.setResponder(searchText -> {
			updateSearchTextColor(searchText);
			updateVisibleEntries();
		});
	}

	public static Screen create(
		@Nullable Screen parent,
		List<ConfigScreenFactoryEntry> factoryEntries,
		ConfigScreenOwnerMetadataProvider metadataProvider
	) {
		return create(parent, ConfigScreenListEntry.create(factoryEntries, metadataProvider));
	}

	public static Screen create(@Nullable Screen parent, List<ConfigScreenListEntry> entries) {
		return new ConfigScreenListScreen(parent, entries);
	}

	private void updateSearchTextColor(String searchText) {
		if (searchText.isEmpty()) {
			searchBox.setTextColor(ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_SEARCH_HINT));
			return;
		}
		searchBox.setTextColor(ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_SEARCH_TEXT));
	}

	private void updateVisibleEntries() {
		String searchText = ConfigLocale.toLowercase(searchBox.getValue());
		List<ConfigScreenListEntry> entries = ConfigScreenListEntry.applyPreferences(this.entries);
		if (searchText.isEmpty()) {
			visibleEntries = entries;
		} else {
			visibleEntries = entries.stream()
				.filter(entry -> entry.matches(searchText))
				.toList();
		}
		targetScrollY = 0;
		currentScrollY = 0;
		updateLayout();
	}

	@Override
	protected void init() {
		super.init();
		updateVisibleEntries();
		addWidget(searchBox);
	}

	private void updateLayout() {
		area = resizer.updateScreenBounds(width, height);
		ImmutableRect2i innerArea = area.insetBy(BORDER_PADDING);
		titleArea = innerArea.keepTop(TITLE_HEIGHT);
		searchArea = innerArea
			.cropTop(TITLE_HEIGHT + SECTION_GAP)
			.keepTop(SEARCH_HEIGHT);
		ImmutableRect2i listWithScrollArea = innerArea
			.cropTop(TITLE_HEIGHT + SECTION_GAP + SEARCH_HEIGHT + SECTION_GAP);

		totalListHeight = getTotalListHeight();
		if (totalListHeight > listWithScrollArea.getHeight()) {
			listArea = listWithScrollArea.cropRight(SCROLLBAR_WIDTH + SCROLLBAR_GAP);
			scrollBarArea = listWithScrollArea.keepRight(SCROLLBAR_WIDTH);
		} else {
			listArea = listWithScrollArea;
			scrollBarArea = ImmutableRect2i.EMPTY;
		}

		searchBox.setX(searchArea.getX() + SEARCH_TEXT_LEFT_PADDING);
		searchBox.setY(searchArea.getY() + (searchArea.getHeight() - SEARCH_TEXT_HEIGHT) / 2);
		searchBox.setWidth(searchArea.getWidth() - SEARCH_TEXT_LEFT_PADDING - SEARCH_TEXT_RIGHT_PADDING);
		searchBox.setHeight(SEARCH_HEIGHT);
		clampScroll();
	}

	@Override
	@Nullable
	public Rect2i getScreenArea() {
		if (area.isEmpty()) {
			return null;
		}
		return new Rect2i(area.getX(), area.getY(), area.getWidth(), area.getHeight());
	}

	private int getTotalListHeight() {
		if (visibleEntries.isEmpty()) {
			return 0;
		}
		return visibleEntries.size() * ROW_HEIGHT + (visibleEntries.size() - 1) * ROW_GAP;
	}

	private void clampScroll() {
		int maxScroll = getMaxScroll();
		targetScrollY = Mth.clamp(targetScrollY, 0, maxScroll);
		currentScrollY = Mth.clamp(currentScrollY, 0, maxScroll);
	}

	private int getMaxScroll() {
		return Math.max(0, totalListHeight - listArea.getHeight());
	}

	@Override
	public void onClose() {
		if (minecraft != null) {
			net.mezzdev.config.gui.ConfigClientUtil.setScreen(parent);
		}
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (searchBox.isFocused() && ConfigInputUtil.charTyped(searchBox, codePoint, modifiers)) {
			return true;
		}
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		UserInput input = UserInput.fromVanilla(keyCode, scanCode, modifiers, InputType.IMMEDIATE);
		if (searchBox.isFocused()) {
			if (ConfigInputUtil.keyPressed(searchBox, keyCode, scanCode, modifiers)) {
				return true;
			}
			if (input.is(Minecraft.getInstance().options.keyInventory)) {
				return true;
			}
		}
		if (input.is(Minecraft.getInstance().options.keyInventory)) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && resizer.startResizeDrag(mouseX, mouseY)) {
			pressedEntry = null;
			return true;
		}
		if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT && searchBox.isMouseOver(mouseX, mouseY)) {
			if (!searchBox.getValue().isEmpty()) {
				searchBox.setValue("");
			}
			searchBox.setFocused(true);
			return true;
		}
		if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && startScrollDrag(mouseX, mouseY)) {
			pressedEntry = null;
			return true;
		}
		if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && listArea.contains(mouseX, mouseY)) {
			pressedEntry = getEntryAt(mouseX, mouseY).orElse(null);
			if (pressedEntry != null) {
				return true;
			}
		}
		if (searchBox.isFocused() && !searchBox.isMouseOver(mouseX, mouseY)) {
			searchBox.setFocused(false);
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && resizer.isResizing()) {
			resizer.finishResizeDrag()
				.ifPresent(resizedArea -> ConfigGuiOptions.setWindowSize(resizedArea.getWidth(), resizedArea.getHeight()));
			return true;
		}
		if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && draggingScroll) {
			draggingScroll = false;
			return true;
		}
		ConfigScreenListEntry pressedEntry = this.pressedEntry;
		this.pressedEntry = null;
		if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && pressedEntry != null && listArea.contains(mouseX, mouseY)) {
			Optional<ConfigScreenListEntry> clickedEntry = getEntryAt(mouseX, mouseY);
			if (clickedEntry.isPresent() && clickedEntry.get() == pressedEntry) {
				openEntry(pressedEntry);
				return true;
			}
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private Optional<ConfigScreenListEntry> getEntryAt(double mouseX, double mouseY) {
		int y = listArea.getY() - (int) currentScrollY;
		for (ConfigScreenListEntry entry : visibleEntries) {
			ImmutableRect2i rowArea = new ImmutableRect2i(listArea.getX(), y, listArea.getWidth(), ROW_HEIGHT);
			if (rowArea.contains(mouseX, mouseY)) {
				return Optional.of(entry);
			}
			y += ROW_HEIGHT + ROW_GAP;
		}
		return Optional.empty();
	}

	private void openEntry(ConfigScreenListEntry entry) {
		if (minecraft != null) {
			net.mezzdev.config.gui.ConfigClientUtil.setScreen(entry.factory().create(this));
		}
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && resizer.isResizing()) {
			if (resizer.dragResize(mouseX, mouseY, width, height)) {
				updateLayout();
			}
			return true;
		}
		if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && draggingScroll) {
			setScrollFromMarkerY(mouseY - scrollDragOffsetY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (listArea.contains(mouseX, mouseY)) {
			targetScrollY = Mth.clamp(targetScrollY - scrollY * ConfigGuiOptions.getScrollSpeed(), 0, getMaxScroll());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private boolean startScrollDrag(double mouseX, double mouseY) {
		ImmutableRect2i markerArea = getScrollMarkerArea();
		if (markerArea.isEmpty() || !scrollBarArea.contains(mouseX, mouseY)) {
			return false;
		}
		if (markerArea.contains(mouseX, mouseY)) {
			scrollDragOffsetY = mouseY - markerArea.getY();
		} else {
			scrollDragOffsetY = markerArea.getHeight() / 2.0;
			setScrollFromMarkerY(mouseY - scrollDragOffsetY);
		}
		draggingScroll = true;
		return true;
	}

	private void setScrollFromMarkerY(double markerY) {
		int maxScroll = getMaxScroll();
		int markerHeight = getScrollMarkerHeight();
		ImmutableRect2i trackArea = getScrollMarkerTrackArea();
		int trackSpace = trackArea.getHeight() - markerHeight;
		if (maxScroll <= 0 || trackSpace <= 0) {
			targetScrollY = 0;
			currentScrollY = 0;
			return;
		}
		double position = Mth.clamp(markerY - trackArea.getY(), 0.0, trackSpace);
		targetScrollY = position * maxScroll / trackSpace;
		currentScrollY = targetScrollY;
	}

	@Override
	public void render(LegacyGuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		updateSearchTextColor(searchBox.getValue());
		renderTransparentBackground(guiGraphics);
		stepScroll();
		draw(guiGraphics, mouseX, mouseY, partialTick);
	}

	private void stepScroll() {
		if (!ConfigGuiOptions.smoothScrolling()) {
			currentScrollY = targetScrollY;
			return;
		}
		if (Math.abs(targetScrollY - currentScrollY) > 0.5) {
			currentScrollY += (targetScrollY - currentScrollY) * SCROLL_LERP;
		} else {
			currentScrollY = targetScrollY;
		}
	}

	private void draw(LegacyGuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		Font font = Minecraft.getInstance().font;
		background.draw(guiGraphics, area);
		ConfigScreenResizer.drawResizeHandles(guiGraphics, area, resizer.getActiveResizeHandle(mouseX, mouseY));
		drawTitle(guiGraphics, font);
		drawSearch(guiGraphics, mouseX, mouseY, partialTick);
		drawList(guiGraphics, font, mouseX, mouseY);
		drawScrollBar(guiGraphics);
	}

	private void drawTitle(LegacyGuiGraphics guiGraphics, Font font) {
		ConfigEntryWidget.drawFittedText(
			guiGraphics,
			font,
			TITLE,
			titleArea,
			ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_TITLE_TEXT),
			true
		);
	}

	private void drawSearch(LegacyGuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		textures.getSearchBackground()
			.draw(guiGraphics, searchArea);
		net.mezzdev.config.gui.ConfigRenderUtil.renderEditBox(searchBox, guiGraphics, mouseX, mouseY, partialTick);
	}

	private void drawList(LegacyGuiGraphics guiGraphics, Font font, int mouseX, int mouseY) {
		guiGraphics.fill(
			listArea.getX(),
			listArea.getY(),
			listArea.getX() + listArea.getWidth(),
			listArea.getY() + listArea.getHeight(),
			ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_BACKGROUND)
		);
		drawInsetBorder(guiGraphics, listArea);
		if (visibleEntries.isEmpty()) {
			drawEmptyListMessage(guiGraphics, font);
			return;
		}

		guiGraphics.enableScissor(
			listArea.getX(),
			listArea.getY(),
			listArea.getX() + listArea.getWidth(),
			listArea.getY() + listArea.getHeight()
		);
		int y = listArea.getY() - (int) currentScrollY;
		for (ConfigScreenListEntry entry : visibleEntries) {
			ImmutableRect2i rowArea = new ImmutableRect2i(listArea.getX(), y, listArea.getWidth(), ROW_HEIGHT);
			if (rowArea.intersects(listArea)) {
				drawEntry(guiGraphics, font, rowArea, entry, rowArea.contains(mouseX, mouseY) && listArea.contains(mouseX, mouseY));
			}
			y += ROW_HEIGHT + ROW_GAP;
		}
		guiGraphics.disableScissor();
	}

	private void drawEmptyListMessage(LegacyGuiGraphics guiGraphics, Font font) {
		Component message = Component.translatable("mezz_config.config.screen.list.empty");
		ConfigEntryWidget.drawFittedText(
			guiGraphics,
			font,
			message,
			listArea.insetBy(ROW_PADDING),
			ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_EMPTY_TEXT),
			true
		);
	}

	private void drawEntry(
		LegacyGuiGraphics guiGraphics,
		Font font,
		ImmutableRect2i rowArea,
		ConfigScreenListEntry entry,
		boolean hovered
	) {
		guiGraphics.fill(
			rowArea.getX(),
			rowArea.getY(),
			rowArea.getX() + rowArea.getWidth(),
			rowArea.getY() + rowArea.getHeight(),
			ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_ROW_BACKGROUND)
		);
		if (hovered) {
			guiGraphics.fill(
				rowArea.getX(),
				rowArea.getY(),
				rowArea.getX() + rowArea.getWidth(),
				rowArea.getY() + rowArea.getHeight(),
				ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_ROW_HOVER)
			);
		}
		guiGraphics.fill(
			rowArea.getX(),
			rowArea.getY() + rowArea.getHeight() - 1,
			rowArea.getX() + rowArea.getWidth(),
			rowArea.getY() + rowArea.getHeight(),
			ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_ROW_DIVIDER)
		);

		ImmutableRect2i iconArea = new ImmutableRect2i(
			rowArea.getX() + ROW_PADDING,
			rowArea.getY() + (rowArea.getHeight() - ICON_SIZE) / 2,
			ICON_SIZE,
			ICON_SIZE
		);
		entry.icon().draw(guiGraphics, font, iconArea);

		int textX = iconArea.getX() + iconArea.getWidth() + ROW_PADDING;
		ImmutableRect2i titleTextArea = new ImmutableRect2i(
			textX,
			rowArea.getY() + (rowArea.getHeight() - font.lineHeight) / 2,
			Math.max(0, rowArea.getX() + rowArea.getWidth() - textX - ROW_PADDING),
			font.lineHeight
		);

		ConfigEntryWidget.drawFittedText(guiGraphics, font, entry.title(), titleTextArea, ConfigEntryWidget.getConfiguredTextColor(), false);
	}

	private static void drawInsetBorder(LegacyGuiGraphics guiGraphics, ImmutableRect2i area) {
		if (area.isEmpty()) {
			return;
		}
		int x = area.getX();
		int y = area.getY();
		int right = x + area.getWidth();
		int bottom = y + area.getHeight();
		guiGraphics.fill(x, y, right, y + 1, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_INSET_BORDER_DARK));
		guiGraphics.fill(x, y, x + 1, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_INSET_BORDER_DARK));
		guiGraphics.fill(x, bottom - 1, right, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_INSET_BORDER_LIGHT));
		guiGraphics.fill(right - 1, y, right, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_INSET_BORDER_LIGHT));
	}

	private void drawScrollBar(LegacyGuiGraphics guiGraphics) {
		ImmutableRect2i markerArea = getScrollMarkerArea();
		if (!markerArea.isEmpty()) {
			scrollbarBackground.draw(guiGraphics, scrollBarArea);
			scrollbarMarker.draw(guiGraphics, markerArea);
		}
	}

	private ImmutableRect2i getScrollMarkerArea() {
		int maxScroll = getMaxScroll();
		if (maxScroll <= 0 || scrollBarArea.isEmpty()) {
			return ImmutableRect2i.EMPTY;
		}
		ImmutableRect2i trackArea = getScrollMarkerTrackArea();
		int markerHeight = getScrollMarkerHeight();
		int trackSpace = trackArea.getHeight() - markerHeight;
		int markerY = trackArea.getY() + (int) (trackSpace * currentScrollY / maxScroll);
		markerY = Mth.clamp(markerY, trackArea.getY(), trackArea.getY() + trackSpace);
		return new ImmutableRect2i(trackArea.getX(), markerY, trackArea.getWidth(), markerHeight);
	}

	private ImmutableRect2i getScrollMarkerTrackArea() {
		return scrollBarArea.insetBy(SCROLL_MARKER_TRACK_INSET);
	}

	private int getScrollMarkerHeight() {
		ImmutableRect2i trackArea = getScrollMarkerTrackArea();
		if (totalListHeight <= 0) {
			return trackArea.getHeight();
		}
		int markerHeight = Math.max(MIN_SCROLL_MARKER_HEIGHT, trackArea.getHeight() * listArea.getHeight() / totalListHeight);
		return Math.min(trackArea.getHeight(), markerHeight);
	}

}
