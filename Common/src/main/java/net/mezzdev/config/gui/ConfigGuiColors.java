package net.mezzdev.config.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Resource-pack-configurable colors used by the config GUI.
 */
public final class ConfigGuiColors {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final ResourceLocation COLORS_RESOURCE = ResourceLocation.fromNamespaceAndPath("mezz_config", "gui/colors.json");
	private static final long MAX_COLOR = 0xFFFFFFFFL;

	private static volatile Map<GuiColor, Integer> colors = createDefaultColors();

	private ConfigGuiColors() {

	}

	public static ResourceManagerReloadListener createReloadListener() {
		return ConfigGuiColors::onResourceManagerReload;
	}

	public static void onResourceManagerReload(ResourceManager resourceManager) {
		Map<GuiColor, Integer> loadedColors = createDefaultColors();
		for (Resource resource : resourceManager.getResourceStack(COLORS_RESOURCE)) {
			try (Reader reader = resource.openAsReader()) {
				JsonElement jsonElement = JsonParser.parseReader(reader);
				loadColors(jsonElement, loadedColors);
			} catch (IOException | RuntimeException e) {
				LOGGER.error("Failed to load MezzConfig GUI colors from resource: {}", COLORS_RESOURCE, e);
			}
		}
		colors = Map.copyOf(loadedColors);
	}

	public static int getColor(GuiColor color) {
		return colors.getOrDefault(color, color.defaultColor);
	}

	private static Map<GuiColor, Integer> createDefaultColors() {
		Map<GuiColor, Integer> defaults = new EnumMap<>(GuiColor.class);
		for (GuiColor color : GuiColor.values()) {
			defaults.put(color, color.defaultColor);
		}
		return defaults;
	}

	static void loadColors(JsonElement jsonElement, Map<GuiColor, Integer> loadedColors) {
		if (!jsonElement.isJsonObject()) {
			LOGGER.error("MezzConfig GUI colors resource must be a JSON object: {}", COLORS_RESOURCE);
			return;
		}
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		for (GuiColor color : GuiColor.values()) {
			JsonElement colorJson = jsonObject.get(color.key);
			if (colorJson != null) {
				OptionalInt colorValue = parseColor(colorJson);
				if (colorValue.isPresent()) {
					loadedColors.put(color, colorValue.getAsInt());
				} else {
					LOGGER.error("Invalid MezzConfig GUI color '{}' in resource '{}': {}", color.key, COLORS_RESOURCE, colorJson);
				}
			}
		}
	}

	static OptionalInt parseColor(JsonElement colorJson) {
		if (!colorJson.isJsonPrimitive()) {
			return OptionalInt.empty();
		}
		JsonPrimitive primitive = colorJson.getAsJsonPrimitive();
		if (!primitive.isString()) {
			return OptionalInt.empty();
		}
		return parseColorString(primitive.getAsString());
	}

	static OptionalInt parseColorString(String string) {
		string = string.trim();
		if (!string.toLowerCase(Locale.ROOT).startsWith("0x")) {
			return OptionalInt.empty();
		}
		string = string.substring(2);
		if (string.length() == 6) {
			string = "FF" + string;
		}
		if (string.length() != 8) {
			return OptionalInt.empty();
		}
		try {
			long value = Long.parseUnsignedLong(string, 16);
			if (value <= MAX_COLOR) {
				return OptionalInt.of((int) value);
			}
		} catch (NumberFormatException ignored) {

		}
		return OptionalInt.empty();
	}

	public enum GuiColor {
		CONFIG_SCREEN_SEARCH_TEXT("configScreenSearchText", 0xFFE8EEF7),
		CONFIG_SCREEN_SEARCH_HINT("configScreenSearchHint", 0xFF8F98A6),
		CONFIG_SCREEN_INFO_BACKGROUND("configScreenInfoBackground", 0xE0101218),
		CONFIG_SCREEN_INFO_BORDER("configScreenInfoBorder", 0x70FFFFFF),
		CONFIG_SCREEN_INFO_TITLE_TEXT("configScreenInfoTitleText", 0xFFF3F6FF),
		CONFIG_SCREEN_INFO_TEXT("configScreenInfoText", 0xFFC9D3E2),
		CONFIG_SCREEN_TITLE_TEXT("configScreenTitleText", 0xFF404040),
		CONFIG_SCREEN_RESIZE_GRIP("configScreenResizeGrip", 0x45FFFFFF),
		CONFIG_SCREEN_RESIZE_HANDLE_HOVER("configScreenResizeHandleHover", 0x80FFFFFF),
		CONFIG_SCREEN_NAVIGATION_BACKGROUND("configScreenNavigationBackground", 0x18000000),
		CONFIG_SCREEN_VALUE_AREA_BACKGROUND("configScreenValueAreaBackground", 0x82000000),
		CONFIG_SCREEN_INSET_BORDER_DARK("configScreenInsetBorderDark", 0xB0000000),
		CONFIG_SCREEN_INSET_BORDER_LIGHT("configScreenInsetBorderLight", 0x35FFFFFF),
		CONFIG_SCREEN_TOOLTIP_TITLE_TEXT("configScreenTooltipTitleText", 0xFFFFFFFF),
		CONFIG_SCREEN_TOOLTIP_TEXT("configScreenTooltipText", 0xFFAAAAAA),

		CONFIG_ENTRY_TEXT("configEntryText", 0xFFFFFFFF),
		CONFIG_ENTRY_SECONDARY_TEXT("configEntrySecondaryText", 0xFFE0E0E0),
		CONFIG_ENTRY_HOVER_TEXT("configEntryHoverText", 0xFFFFFFFF),
		CONFIG_ENTRY_DISABLED_TEXT("configEntryDisabledText", 0xFFA0A0A0),
		CONFIG_ENTRY_ROW_STRIPE_LIGHT("configEntryRowStripeLight", 0x08FFFFFF),
		CONFIG_ENTRY_ROW_STRIPE_DARK("configEntryRowStripeDark", 0x08000000),
		CONFIG_ENTRY_ROW_HOVER("configEntryRowHover", 0x18FFFFFF),
		CONFIG_ENTRY_PENDING_BACKGROUND("configEntryPendingBackground", 0x302F5F8E),
		CONFIG_ENTRY_PENDING_ACCENT("configEntryPendingAccent", 0xFF5E9AD6),
		CONFIG_ENTRY_READ_ONLY_OVERLAY("configEntryReadOnlyOverlay", 0x50000000),
		CONFIG_ENTRY_BUTTON_UNDERLAY("configEntryButtonUnderlay", 0xFF111216),
		CONFIG_ENTRY_INVALID_TEXT("configEntryInvalidText", 0xFFFF7070),

		LIST_ORDERED_GROUP_BACKGROUND("listOrderedGroupBackground", 0x22000000),
		LIST_ORDERED_GROUP_BORDER_DARK("listOrderedGroupBorderDark", 0x90000000),
		LIST_ORDERED_GROUP_BORDER_LIGHT("listOrderedGroupBorderLight", 0x24FFFFFF),
		LIST_ORDERED_ROW_BACKGROUND("listOrderedRowBackground", 0x1E000000),
		LIST_ROW_HOVER("listRowHover", 0x18FFFFFF),
		LIST_ORDERED_ROW_DRAG_GAP("listOrderedRowDragGap", 0x28000000),
		LIST_ORDERED_ROW_DROP_TARGET("listOrderedRowDropTarget", 0x52000000),
		LIST_ORDERED_ROW_MOVED_BACKGROUND("listOrderedRowMovedBackground", 0x285E9AD6),
		LIST_ORDERED_ROW_MOVED_ACCENT("listOrderedRowMovedAccent", 0xFF7DB6F2),
		LIST_ORDERED_ROW_DRAG_FLOAT_BACKGROUND("listOrderedRowDragFloatBackground", 0xFF404A59),
		LIST_ORDERED_ROW_DRAG_FLOAT_SHADOW("listOrderedRowDragFloatShadow", 0x70000000),
		LIST_ORDERED_ROW_DRAG_FLOAT_BORDER("listOrderedRowDragFloatBorder", 0xD0D7E6FF),
		LIST_ORDERED_ROW_DRAG_FLOAT_ACCENT("listOrderedRowDragFloatAccent", 0xFFEAF2FF),
		LIST_ORDERED_ROW_DIVIDER("listOrderedRowDivider", 0x18FFFFFF),
		LIST_UNUSED_ROW_BACKGROUND("listUnusedRowBackground", 0x30000000),
		LIST_UNUSED_ROW_TEXT("listUnusedRowText", 0xFF707070),

		COLOR_SWATCH_BORDER("colorSwatchBorder", 0xFF08090C),
		COLOR_SWATCH_CHECKER_LIGHT("colorSwatchCheckerLight", 0xFFB8B8B8),
		COLOR_SWATCH_CHECKER_DARK("colorSwatchCheckerDark", 0xFF727272),

		KEY_MAPPING_CONFLICT_ACCENT("keyMappingConflictAccent", 0xFFFFD34D),
		KEY_MAPPING_LISTENING_TEXT("keyMappingListeningText", 0xFFFFFF55),
		KEY_MAPPING_LISTENING_KEY_TEXT("keyMappingListeningKeyText", 0xFFFFFFFF),
		KEY_MAPPING_CONFLICT_INFO_TEXT("keyMappingConflictInfoText", 0xFFFFFF55),

		NAV_ITEM_BACKGROUND("navItemBackground", 0x33000000),
		NAV_ITEM_HOVER_BACKGROUND("navItemHoverBackground", 0x22FFFFFF),
		NAV_ITEM_ACTIVE_BACKGROUND("navItemActiveBackground", 0x66313A46),
		NAV_ITEM_ACTIVE_ACCENT("navItemActiveAccent", 0xFF5E9AD6),
		NAV_ITEM_DIVIDER("navItemDivider", 0x2AFFFFFF),
		NAV_ITEM_TEXT("navItemText", 0xFF20242A),
		NAV_ITEM_HOVER_TEXT("navItemHoverText", 0xFF111820),
		NAV_ITEM_ACTIVE_TEXT("navItemActiveText", 0xFF101A24),

		PENDING_CHANGES_BACKGROUND("pendingChangesBackground", 0xC0101010),
		PENDING_CHANGES_PANEL_BACKGROUND("pendingChangesPanelBackground", 0xF0191D26),
		PENDING_CHANGES_PANEL_BORDER("pendingChangesPanelBorder", 0x90FFFFFF),
		PENDING_CHANGES_ROW_BACKGROUND("pendingChangesRowBackground", 0xFF252B36),
		PENDING_CHANGES_ROW_ALTERNATE_BACKGROUND("pendingChangesRowAlternateBackground", 0xFF2B3342),
		PENDING_CHANGES_ROW_HOVER("pendingChangesRowHover", 0xFF364456),
		PENDING_CHANGES_TITLE_TEXT("pendingChangesTitleText", 0xFFFFFFFF),
		PENDING_CHANGES_MESSAGE_TEXT("pendingChangesMessageText", 0xFFE0E0E0),
		PENDING_CHANGES_ROW_TEXT("pendingChangesRowText", 0xFFE8EEF7),
		PENDING_CHANGES_SCROLLBAR_TRACK("pendingChangesScrollbarTrack", 0xFF07080C),
		PENDING_CHANGES_SCROLLBAR_MARKER("pendingChangesScrollbarMarker", 0xFF9AA8BC),
		PENDING_CHANGES_CHANGE_TEXT("pendingChangesChangeText", 0xFFC9D3E2),
		PENDING_CHANGES_TOOLTIP_TITLE_TEXT("pendingChangesTooltipTitleText", 0xFFFFFFFF),
		PENDING_CHANGES_TOOLTIP_TEXT("pendingChangesTooltipText", 0xFFAAAAAA),

		COLOR_PICKER_BACKGROUND("colorPickerBackground", 0xF0101218),
		COLOR_PICKER_BORDER_DARK("colorPickerBorderDark", 0xFF050609),
		COLOR_PICKER_BORDER_LIGHT("colorPickerBorderLight", 0x667F8A9A),
		COLOR_PICKER_MARKER_DARK("colorPickerMarkerDark", 0xFF000000),
		COLOR_PICKER_MARKER_LIGHT("colorPickerMarkerLight", 0xFFFFFFFF),
		COLOR_PICKER_FIELD_BACKGROUND("colorPickerFieldBackground", 0xFF171A20),
		COLOR_PICKER_FIELD_FOCUSED_BACKGROUND("colorPickerFieldFocusedBackground", 0xFF1F2C3A),
		COLOR_PICKER_FIELD_BORDER("colorPickerFieldBorder", 0x555E6877),
		COLOR_PICKER_FIELD_FOCUSED_BORDER("colorPickerFieldFocusedBorder", 0xFF7DB6F2),
		COLOR_PICKER_FIELD_INVALID_BORDER("colorPickerFieldInvalidBorder", 0xFFFF7070),

		VALUE_SELECTOR_BACKGROUND("valueSelectorBackground", 0xF0101218),
		VALUE_SELECTOR_ROW_BACKGROUND("valueSelectorRowBackground", 0xAA1A1D24),
		VALUE_SELECTOR_ROW_HOVER("valueSelectorRowHover", 0xFF313A46),
		VALUE_SELECTOR_BORDER_DARK("valueSelectorBorderDark", 0xE0000000),
		VALUE_SELECTOR_BORDER_LIGHT("valueSelectorBorderLight", 0x45FFFFFF),
		VALUE_SELECTOR_DIVIDER("valueSelectorDivider", 0x22FFFFFF),
		VALUE_SELECTOR_SCROLLBAR("valueSelectorScrollbar", 0xAA9AA4B2),

		SCREEN_LIST_TITLE_TEXT("screenListTitleText", 0xFF404040),
		SCREEN_LIST_SEARCH_TEXT("screenListSearchText", 0xFFE8EEF7),
		SCREEN_LIST_SEARCH_HINT("screenListSearchHint", 0xFF8F98A6),
		SCREEN_LIST_BACKGROUND("screenListBackground", 0x82000000),
		SCREEN_LIST_INSET_BORDER_DARK("screenListInsetBorderDark", 0xB0000000),
		SCREEN_LIST_INSET_BORDER_LIGHT("screenListInsetBorderLight", 0x35FFFFFF),
		SCREEN_LIST_ROW_BACKGROUND("screenListRowBackground", 0x33000000),
		SCREEN_LIST_ROW_HOVER("screenListRowHover", 0x22FFFFFF),
		SCREEN_LIST_ROW_DIVIDER("screenListRowDivider", 0x2AFFFFFF),
		SCREEN_LIST_ICON_BORDER("screenListIconBorder", 0x40FFFFFF),
		SCREEN_LIST_EMPTY_TEXT("screenListEmptyText", 0xFFA0A0A0),

		DISABLED_BUTTON_ICON_TINT("disabledButtonIconTint", 0xFFA0A0A0);

		private final String key;
		private final int defaultColor;

		GuiColor(String key, int defaultColor) {
			this.key = key;
			this.defaultColor = defaultColor;
		}

		String getKey() {
			return key;
		}

		String getDefaultColorString() {
			return "0x%08X".formatted(defaultColor);
		}

		public int getDefaultColor() {
			return defaultColor;
		}
	}
}
