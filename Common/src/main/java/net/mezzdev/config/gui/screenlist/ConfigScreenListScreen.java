package net.mezzdev.config.gui.screenlist;

import com.mojang.blaze3d.platform.NativeImage;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.input.InputType;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.textures.ConfigScalableDrawable;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ConfigLocale;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Screen for choosing one of the config screens discovered for the current mod loader.
 */
public final class ConfigScreenListScreen extends Screen {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Component TITLE = Component.translatable("mezz_config.config.screen.list.title");
	private static final Component SEARCH = Component.translatable("mezz_config.config.screen.list.search");
	private static final int TITLE_TEXT_COLOR = 0xFF404040;
	private static final int SEARCH_TEXT_COLOR = 0xFFE8EEF7;
	private static final int SEARCH_HINT_COLOR = 0xFF8F98A6;
	private static final int LIST_BACKGROUND_COLOR = 0x82000000;
	private static final int INSET_BORDER_DARK_COLOR = 0xB0000000;
	private static final int INSET_BORDER_LIGHT_COLOR = 0x35FFFFFF;
	private static final int ROW_BACKGROUND_COLOR = 0x33000000;
	private static final int ROW_HOVER_COLOR = 0x22FFFFFF;
	private static final int ROW_DIVIDER_COLOR = 0x2AFFFFFF;
	private static final int ICON_BORDER_COLOR = 0x40FFFFFF;
	private static final int EMPTY_TEXT_COLOR = 0xFFA0A0A0;
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
	private static final int ITEM_ICON_SIZE = 16;
	private static final int SCROLLBAR_WIDTH = 12;
	private static final int SCROLLBAR_GAP = 2;
	private static final int SCROLL_MARKER_TRACK_INSET = 1;
	private static final int MIN_SCROLL_MARKER_HEIGHT = 10;
	private static final double SCROLL_LERP = 0.35;
	private static final String MINECRAFT_MOD_ID = "minecraft";
	private static final ItemStack MINECRAFT_ICON = new ItemStack(Blocks.GRASS_BLOCK);

	private final List<Entry> entries;
	private final EditBox searchBox;
	private final ConfigTextures textures;
	private final ConfigScalableDrawable background;
	private final ConfigScalableDrawable scrollbarBackground;
	private final ConfigScalableDrawable scrollbarMarker;

	@Nullable
	private final Screen parent;

	private List<Entry> visibleEntries;
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
	private Entry pressedEntry;

	private ConfigScreenListScreen(
		@Nullable Screen parent,
		List<Entry> entries
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
		this.searchBox = new EditBox(font, 0, 0, 0, SEARCH_HEIGHT, SEARCH);
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
		return new ConfigScreenListScreen(parent, createEntries(factoryEntries, metadataProvider));
	}

	private static List<Entry> createEntries(
		List<ConfigScreenFactoryEntry> factoryEntries,
		ConfigScreenOwnerMetadataProvider metadataProvider
	) {
		return factoryEntries.stream()
			.map(factoryEntry -> createEntry(factoryEntry, metadataProvider))
			.sorted(Comparator.comparing(Entry::sortName).thenComparing(Entry::modId))
			.toList();
	}

	private static Entry createEntry(
		ConfigScreenFactoryEntry factoryEntry,
		ConfigScreenOwnerMetadataProvider metadataProvider
	) {
		String modId = factoryEntry.modId();
		ConfigScreenOwnerMetadata metadata = getMetadata(modId, metadataProvider);
		return new Entry(
			modId,
			factoryEntry.title(),
			factoryEntry.factory(),
			new EntryIcon(modId, metadata.iconPath())
		);
	}

	private static ConfigScreenOwnerMetadata getMetadata(
		String modId,
		ConfigScreenOwnerMetadataProvider metadataProvider
	) {
		try {
			ConfigScreenOwnerMetadata metadata = metadataProvider.getMetadata(modId);
			if (metadata != null) {
				return metadata;
			}
		} catch (RuntimeException | LinkageError e) {
			LOGGER.warn("Failed to load config screen list metadata for mod id: {}", modId, e);
		}
		return new ConfigScreenOwnerMetadata();
	}

	private void updateSearchTextColor(String searchText) {
		if (searchText.isEmpty()) {
			searchBox.setTextColor(SEARCH_HINT_COLOR);
			return;
		}
		searchBox.setTextColor(SEARCH_TEXT_COLOR);
	}

	private void updateVisibleEntries() {
		String searchText = ConfigLocale.toLowercase(searchBox.getValue());
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
		updateLayout();
		addWidget(searchBox);
	}

	private void updateLayout() {
		area = getScreenArea(width, height);
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

	private static ImmutableRect2i getScreenArea(int screenWidth, int screenHeight) {
		ConfigGuiOptions.GuiSize guiSize = ConfigGuiOptions.getGuiSize();
		int guiWidth = guiSize.getWidth(screenWidth);
		int guiHeight = guiSize.getHeight(screenHeight);
		return new ImmutableRect2i(
			(screenWidth - guiWidth) / 2,
			(screenHeight - guiHeight) / 2,
			guiWidth,
			guiHeight
		);
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
			minecraft.setScreen(parent);
		}
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (searchBox.isFocused() && searchBox.charTyped(codePoint, modifiers)) {
			return true;
		}
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		UserInput input = UserInput.fromVanilla(keyCode, scanCode, modifiers, InputType.IMMEDIATE);
		if (searchBox.isFocused()) {
			if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
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
		if (button == 1 && searchBox.isMouseOver(mouseX, mouseY)) {
			if (!searchBox.getValue().isEmpty()) {
				searchBox.setValue("");
			}
			searchBox.setFocused(true);
			return true;
		}
		if (button == 0 && startScrollDrag(mouseX, mouseY)) {
			pressedEntry = null;
			return true;
		}
		if (button == 0 && listArea.contains(mouseX, mouseY)) {
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
		if (button == 0 && draggingScroll) {
			draggingScroll = false;
			return true;
		}
		Entry pressedEntry = this.pressedEntry;
		this.pressedEntry = null;
		if (button == 0 && pressedEntry != null && listArea.contains(mouseX, mouseY)) {
			Optional<Entry> clickedEntry = getEntryAt(mouseX, mouseY);
			if (clickedEntry.isPresent() && clickedEntry.get() == pressedEntry) {
				openEntry(pressedEntry);
				return true;
			}
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private Optional<Entry> getEntryAt(double mouseX, double mouseY) {
		int y = listArea.getY() - (int) currentScrollY;
		for (Entry entry : visibleEntries) {
			ImmutableRect2i rowArea = new ImmutableRect2i(listArea.getX(), y, listArea.getWidth(), ROW_HEIGHT);
			if (rowArea.contains(mouseX, mouseY)) {
				return Optional.of(entry);
			}
			y += ROW_HEIGHT + ROW_GAP;
		}
		return Optional.empty();
	}

	private void openEntry(Entry entry) {
		if (minecraft != null) {
			minecraft.setScreen(entry.factory().create(this));
		}
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (button == 0 && draggingScroll) {
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
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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

	private void draw(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		Font font = Minecraft.getInstance().font;
		background.draw(guiGraphics, area);
		drawTitle(guiGraphics, font);
		drawSearch(guiGraphics, mouseX, mouseY, partialTick);
		drawList(guiGraphics, font, mouseX, mouseY);
		drawScrollBar(guiGraphics);
	}

	private void drawTitle(GuiGraphics guiGraphics, Font font) {
		ConfigEntryWidget.drawFittedText(guiGraphics, font, TITLE, titleArea, TITLE_TEXT_COLOR, true);
	}

	private void drawSearch(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		textures.getSearchBackground()
			.draw(guiGraphics, searchArea);
		searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
	}

	private void drawList(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY) {
		guiGraphics.fill(
			listArea.getX(),
			listArea.getY(),
			listArea.getX() + listArea.getWidth(),
			listArea.getY() + listArea.getHeight(),
			LIST_BACKGROUND_COLOR
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
		for (Entry entry : visibleEntries) {
			ImmutableRect2i rowArea = new ImmutableRect2i(listArea.getX(), y, listArea.getWidth(), ROW_HEIGHT);
			if (rowArea.intersects(listArea)) {
				drawEntry(guiGraphics, font, rowArea, entry, rowArea.contains(mouseX, mouseY) && listArea.contains(mouseX, mouseY));
			}
			y += ROW_HEIGHT + ROW_GAP;
		}
		guiGraphics.disableScissor();
	}

	private void drawEmptyListMessage(GuiGraphics guiGraphics, Font font) {
		Component message = Component.translatable("mezz_config.config.screen.list.empty");
		ConfigEntryWidget.drawFittedText(guiGraphics, font, message, listArea.insetBy(ROW_PADDING), EMPTY_TEXT_COLOR, true);
	}

	private void drawEntry(
		GuiGraphics guiGraphics,
		Font font,
		ImmutableRect2i rowArea,
		Entry entry,
		boolean hovered
	) {
		guiGraphics.fill(rowArea.getX(), rowArea.getY(), rowArea.getX() + rowArea.getWidth(), rowArea.getY() + rowArea.getHeight(), ROW_BACKGROUND_COLOR);
		if (hovered) {
			guiGraphics.fill(rowArea.getX(), rowArea.getY(), rowArea.getX() + rowArea.getWidth(), rowArea.getY() + rowArea.getHeight(), ROW_HOVER_COLOR);
		}
		guiGraphics.fill(rowArea.getX(), rowArea.getY() + rowArea.getHeight() - 1, rowArea.getX() + rowArea.getWidth(), rowArea.getY() + rowArea.getHeight(), ROW_DIVIDER_COLOR);

		ImmutableRect2i iconArea = new ImmutableRect2i(
			rowArea.getX() + ROW_PADDING,
			rowArea.getY() + (rowArea.getHeight() - ICON_SIZE) / 2,
			ICON_SIZE,
			ICON_SIZE
		);
		entry.icon().draw(guiGraphics, font, iconArea, entry.modId(), entry.title());

		int textX = iconArea.getX() + iconArea.getWidth() + ROW_PADDING;
		ImmutableRect2i titleTextArea = new ImmutableRect2i(
			textX,
			rowArea.getY() + (rowArea.getHeight() - font.lineHeight) / 2,
			Math.max(0, rowArea.getX() + rowArea.getWidth() - textX - ROW_PADDING),
			font.lineHeight
		);

		ConfigEntryWidget.drawFittedText(guiGraphics, font, entry.title(), titleTextArea, ConfigEntryWidget.TEXT_COLOR, false);
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
		guiGraphics.fill(x, bottom - 1, right, bottom, INSET_BORDER_LIGHT_COLOR);
		guiGraphics.fill(right - 1, y, right, bottom, INSET_BORDER_LIGHT_COLOR);
	}

	private void drawScrollBar(GuiGraphics guiGraphics) {
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

	private static final class EntryIcon {
		private final String modId;
		private final Optional<Path> iconPath;
		@Nullable
		private LoadedIcon loadedIcon;
		private boolean loadAttempted;

		private EntryIcon(String modId, Optional<Path> iconPath) {
			this.modId = modId;
			this.iconPath = iconPath;
		}

		public void draw(
			GuiGraphics guiGraphics,
			Font font,
			ImmutableRect2i iconArea,
			String modId,
			Component displayName
		) {
			Optional<LoadedIcon> icon = getLoadedIcon();
			if (icon.isPresent()) {
				icon.get().draw(guiGraphics, iconArea);
				return;
			}
			drawPlaceholder(guiGraphics, font, iconArea, modId, displayName);
		}

		private Optional<LoadedIcon> getLoadedIcon() {
			if (loadedIcon != null) {
				return Optional.of(loadedIcon);
			}
			if (!loadAttempted) {
				loadAttempted = true;
				loadedIcon = loadIcon().orElse(null);
			}
			return Optional.ofNullable(loadedIcon);
		}

		private Optional<LoadedIcon> loadIcon() {
			if (iconPath.isEmpty()) {
				return Optional.empty();
			}
			Path path = iconPath.get();
			try (InputStream inputStream = Files.newInputStream(path)) {
				NativeImage image = NativeImage.read(inputStream);
				int imageWidth = image.getWidth();
				int imageHeight = image.getHeight();
				DynamicTexture texture = new DynamicTexture(image);
				ResourceLocation location = Minecraft.getInstance()
					.getTextureManager()
					.register("mezz_config_gui_mod_icon", texture);
				return Optional.of(new LoadedIcon(location, imageWidth, imageHeight));
			} catch (IOException | RuntimeException e) {
				LOGGER.debug("Failed to load config screen list icon for mod id: {}, path: {}", modId, path, e);
				return Optional.empty();
			}
		}

		private static void drawPlaceholder(
			GuiGraphics guiGraphics,
			Font font,
			ImmutableRect2i iconArea,
			String modId,
			Component displayName
		) {
			if (MINECRAFT_MOD_ID.equals(modId)) {
				drawMinecraftIcon(guiGraphics, iconArea);
				return;
			}
			int color = getPlaceholderColor(modId);
			guiGraphics.fill(iconArea.getX(), iconArea.getY(), iconArea.getX() + iconArea.getWidth(), iconArea.getY() + iconArea.getHeight(), color);
			drawInsetBorder(guiGraphics, iconArea);
			String initial = getInitial(displayName, modId);
			guiGraphics.drawCenteredString(
				font,
				initial,
				iconArea.getX() + iconArea.getWidth() / 2,
				iconArea.getY() + (iconArea.getHeight() - font.lineHeight) / 2,
				ConfigEntryWidget.TEXT_COLOR
			);
		}

		private static void drawMinecraftIcon(GuiGraphics guiGraphics, ImmutableRect2i iconArea) {
			guiGraphics.fill(
				iconArea.getX() - 1,
				iconArea.getY() - 1,
				iconArea.getX() + iconArea.getWidth() + 1,
				iconArea.getY() + iconArea.getHeight() + 1,
				ICON_BORDER_COLOR
			);
			guiGraphics.pose().pushPose();
			float scale = (float) iconArea.getWidth() / ITEM_ICON_SIZE;
			guiGraphics.pose().translate(iconArea.getX(), iconArea.getY(), 0);
			guiGraphics.pose().scale(scale, scale, 1.0F);
			guiGraphics.renderFakeItem(MINECRAFT_ICON, 0, 0);
			guiGraphics.pose().popPose();
		}

		private static int getPlaceholderColor(String modId) {
			int hash = modId.hashCode();
			int red = 0x40 + (hash & 0x3F);
			int green = 0x40 + ((hash >> 8) & 0x3F);
			int blue = 0x40 + ((hash >> 16) & 0x3F);
			return 0xFF000000 | red << 16 | green << 8 | blue;
		}

		private static String getInitial(Component displayName, String modId) {
			String name = displayName.getString();
			if (name.isBlank()) {
				name = modId;
			}
			if (name.isBlank()) {
				return "?";
			}
			int codePoint = name.codePointAt(0);
			return new String(Character.toChars(Character.toUpperCase(codePoint)));
		}
	}

	private record LoadedIcon(
		ResourceLocation location,
		int width,
		int height
	) {
		private LoadedIcon {
			Objects.requireNonNull(location, "location");
		}

		public void draw(GuiGraphics guiGraphics, ImmutableRect2i iconArea) {
			guiGraphics.fill(
				iconArea.getX() - 1,
				iconArea.getY() - 1,
				iconArea.getX() + iconArea.getWidth() + 1,
				iconArea.getY() + iconArea.getHeight() + 1,
				ICON_BORDER_COLOR
			);
			ImmutableRect2i fittedIconArea = getFittedIconArea(iconArea);
			guiGraphics.blit(
				location,
				fittedIconArea.getX(),
				fittedIconArea.getY(),
				fittedIconArea.getWidth(),
				fittedIconArea.getHeight(),
				0.0F,
				0.0F,
				width,
				height,
				width,
				height
			);
		}

		private ImmutableRect2i getFittedIconArea(ImmutableRect2i iconArea) {
			int fittedWidth = iconArea.getWidth();
			int fittedHeight = Math.max(1, fittedWidth * height / width);
			if (fittedHeight > iconArea.getHeight()) {
				fittedHeight = iconArea.getHeight();
				fittedWidth = Math.max(1, fittedHeight * width / height);
			}
			return new ImmutableRect2i(
				iconArea.getX() + (iconArea.getWidth() - fittedWidth) / 2,
				iconArea.getY() + (iconArea.getHeight() - fittedHeight) / 2,
				fittedWidth,
				fittedHeight
			);
		}
	}

	private record Entry(
		String modId,
		Component title,
		net.mezzdev.config.gui.api.IConfigScreenFactory factory,
		EntryIcon icon
	) {
		private Entry {
			Objects.requireNonNull(modId, "modId");
			Objects.requireNonNull(title, "title");
			Objects.requireNonNull(factory, "factory");
			Objects.requireNonNull(icon, "icon");
		}

		private String sortName() {
			return ConfigLocale.toLowercase(title.getString());
		}

		private boolean matches(String searchText) {
			return ConfigLocale.toLowercase(title.getString()).contains(searchText);
		}
	}
}
