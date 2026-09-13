package net.mezzdev.config.gui.config;

import net.mezzdev.config.api.Configs;
import net.mezzdev.config.api.IConfigRegistration;
import net.mezzdev.config.api.schema.builder.IConfigCategoryBuilder;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.schema.builder.IConfigSchemaBuilder;
import net.mezzdev.config.api.value.editor.ConfigValueEditMode;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.IConfigValue;
import org.jetbrains.annotations.Nullable;

/**
 * User-configurable options for MezzConfig GUI's own screen behavior.
 */
public final class ConfigGuiOptions {
	public static final String MOD_ID = "mezz_config_gui";

	private static final String CONFIG_FILE_NAME = "mezz_config_gui.ini";
	private static final String LOCALIZATION_PATH = "mezz_config_gui.config";
	private static final int MIN_WINDOW_WIDTH = 320;
	private static final int MIN_WINDOW_HEIGHT = 230;
	private static final int MAX_WINDOW_SIZE = 8192;
	private static final int DEFAULT_WINDOW_WIDTH = 380;
	private static final int DEFAULT_WINDOW_HEIGHT = 300;

	@Nullable
	private static IConfigSchema schema;
	@Nullable
	private static IConfigValue<GuiMode> guiMode;
	@Nullable
	private static IConfigValue<Boolean> enableWindowResizing;
	@Nullable
	private static IConfigValue<Integer> windowWidth;
	@Nullable
	private static IConfigValue<Integer> windowHeight;
	@Nullable
	private static IConfigValue<RowDensity> rowDensity;
	@Nullable
	private static IConfigValue<NumberDisplayMode> numberDisplayMode;
	@Nullable
	private static IConfigValue<Boolean> showRowStriping;
	@Nullable
	private static IConfigValue<Boolean> rememberLastCategory;
	@Nullable
	private static IConfigValue<Boolean> searchDescriptions;
	@Nullable
	private static IConfigValue<Boolean> focusSearchOnOpen;
	@Nullable
	private static IConfigValue<Boolean> smoothScrolling;
	@Nullable
	private static IConfigValue<Integer> scrollSpeed;
	@Nullable
	private static IConfigValue<Integer> dragAutoScrollSpeed;
	@Nullable
	private static IConfigValue<Boolean> confirmPendingChangesOnClose;
	@Nullable
	private static IConfigValue<Boolean> enableDragReordering;
	@Nullable
	private static IConfigValue<Boolean> showKeyMappings;
	@Nullable
	private static IConfigValue<Boolean> showKeyConflictDetails;
	@Nullable
	private static IConfigValue<Boolean> enableNativeConfigDiscovery;
	@Nullable
	private static IConfigValue<Boolean> showAdvancedValueDetails;
	@Nullable
	private static IConfigValue<DiscoveryLogging> discoveryLogging;

	private ConfigGuiOptions() {

	}

	public static void register() {
		IConfigRegistration registration = Configs.forMod(MOD_ID);
		IConfigSchemaBuilder schemaBuilder = registration.createClientSchemaBuilder(CONFIG_FILE_NAME, LOCALIZATION_PATH);
		IConfigCategoryBuilder appearance = schemaBuilder.addCategory("appearance");
		guiMode = appearance.addEnum("guiMode", GuiMode.WINDOW)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		enableWindowResizing = appearance.addBoolean("enableWindowResizing", true)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		windowWidth = appearance.addInteger("windowWidth", DEFAULT_WINDOW_WIDTH, MIN_WINDOW_WIDTH, MAX_WINDOW_SIZE)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		windowHeight = appearance.addInteger("windowHeight", DEFAULT_WINDOW_HEIGHT, MIN_WINDOW_HEIGHT, MAX_WINDOW_SIZE)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();

		IConfigCategoryBuilder valueDisplay = schemaBuilder.addCategory("valueDisplay");
		rowDensity = valueDisplay.addEnum("rowDensity", RowDensity.COMFORTABLE)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		numberDisplayMode = valueDisplay.addEnum("numberDisplayMode", NumberDisplayMode.SLIDER)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		showRowStriping = valueDisplay.addBoolean("showRowStriping", true)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();

		IConfigCategoryBuilder navigation = schemaBuilder.addCategory("navigation");
		rememberLastCategory = navigation.addBoolean("rememberLastCategory", true)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		searchDescriptions = navigation.addBoolean("searchDescriptions", true)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		focusSearchOnOpen = navigation.addBoolean("focusSearchOnOpen", false)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		smoothScrolling = navigation.addBoolean("smoothScrolling", true)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		scrollSpeed = navigation.addInteger("scrollSpeed", 10, 1, 50)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		dragAutoScrollSpeed = navigation.addInteger("dragAutoScrollSpeed", 6, 1, 50)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();

		IConfigCategoryBuilder editing = schemaBuilder.addCategory("editing");
		confirmPendingChangesOnClose = editing.addBoolean("confirmPendingChangesOnClose", true)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		enableDragReordering = editing.addBoolean("enableDragReordering", true)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();

		IConfigCategoryBuilder integrations = schemaBuilder.addCategory("integrations");
		showKeyMappings = integrations.addBoolean("showKeyMappings", true)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		showKeyConflictDetails = integrations.addBoolean("showKeyConflictDetails", true)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		enableNativeConfigDiscovery = integrations.addBoolean("enableNativeConfigDiscovery", true)
			.setEditMode(ConfigValueEditMode.BATCH)
			.setRestartRequirement(ConfigValueRestartRequirement.GAME_RESTART)
			.build();

		IConfigCategoryBuilder diagnostics = schemaBuilder.addCategory("advanced");
		showAdvancedValueDetails = diagnostics.addBoolean("showAdvancedValueDetails", false)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		discoveryLogging = diagnostics.addEnum("discoveryLogging", DiscoveryLogging.WARNINGS)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();

		schema = schemaBuilder.build();
	}

	public static IConfigSchema getSchema() {
		if (schema == null) {
			throw new IllegalStateException("MezzConfig GUI options have not been registered yet.");
		}
		return schema;
	}

	public static GuiMode getGuiMode() {
		return getValue(guiMode, GuiMode.WINDOW);
	}

	public static int getWindowWidth() {
		return getValue(windowWidth, DEFAULT_WINDOW_WIDTH);
	}

	public static boolean enableWindowResizing() {
		return getValue(enableWindowResizing, true);
	}

	public static int getWindowHeight() {
		return getValue(windowHeight, DEFAULT_WINDOW_HEIGHT);
	}

	public static int getGuiWidth(int screenWidth) {
		int maxWidth = Math.max(1, screenWidth);
		if (getGuiMode() == GuiMode.FULLSCREEN) {
			return maxWidth;
		}
		int minWidth = Math.min(MIN_WINDOW_WIDTH, maxWidth);
		return Math.clamp(getWindowWidth(), minWidth, maxWidth);
	}

	public static int getGuiHeight(int screenHeight) {
		int maxHeight = Math.max(1, screenHeight);
		if (getGuiMode() == GuiMode.FULLSCREEN) {
			return maxHeight;
		}
		int minHeight = Math.min(MIN_WINDOW_HEIGHT, maxHeight);
		return Math.clamp(getWindowHeight(), minHeight, maxHeight);
	}

	public static void setWindowSize(int width, int height) {
		int clampedWidth = Math.clamp(width, MIN_WINDOW_WIDTH, MAX_WINDOW_SIZE);
		int clampedHeight = Math.clamp(height, MIN_WINDOW_HEIGHT, MAX_WINDOW_SIZE);
		IConfigSchema schema = ConfigGuiOptions.schema;
		IConfigValue<Integer> windowWidth = ConfigGuiOptions.windowWidth;
		IConfigValue<Integer> windowHeight = ConfigGuiOptions.windowHeight;
		IConfigValue<GuiMode> guiMode = ConfigGuiOptions.guiMode;
		if (schema != null && windowWidth != null && windowHeight != null && guiMode != null) {
			schema.batchUpdate(updater -> updater
				.set(windowWidth, clampedWidth)
				.set(windowHeight, clampedHeight)
				.set(guiMode, GuiMode.WINDOW));
			return;
		}
		setValue(windowWidth, clampedWidth);
		setValue(windowHeight, clampedHeight);
		setValue(guiMode, GuiMode.WINDOW);
	}

	public static RowDensity getRowDensity() {
		return getValue(rowDensity, RowDensity.COMFORTABLE);
	}

	public static NumberDisplayMode getNumberDisplayMode() {
		return getValue(numberDisplayMode, NumberDisplayMode.SLIDER);
	}

	public static boolean showRowStriping() {
		return getValue(showRowStriping, true);
	}

	public static boolean rememberLastCategory() {
		return getValue(rememberLastCategory, true);
	}

	public static boolean searchDescriptions() {
		return getValue(searchDescriptions, true);
	}

	public static boolean focusSearchOnOpen() {
		return getValue(focusSearchOnOpen, false);
	}

	public static boolean smoothScrolling() {
		return getValue(smoothScrolling, true);
	}

	public static int getScrollSpeed() {
		return getValue(scrollSpeed, 10);
	}

	public static int getDragAutoScrollSpeed() {
		return getValue(dragAutoScrollSpeed, 6);
	}

	public static boolean confirmPendingChangesOnClose() {
		return getValue(confirmPendingChangesOnClose, true);
	}

	public static boolean enableDragReordering() {
		return getValue(enableDragReordering, true);
	}

	public static boolean showKeyMappings() {
		return getValue(showKeyMappings, true);
	}

	public static boolean showKeyConflictDetails() {
		return getValue(showKeyConflictDetails, true);
	}

	public static boolean enableNativeConfigDiscovery() {
		return getValue(enableNativeConfigDiscovery, true);
	}

	public static boolean showAdvancedValueDetails() {
		return getValue(showAdvancedValueDetails, false);
	}

	public static DiscoveryLogging getDiscoveryLogging() {
		return getValue(discoveryLogging, DiscoveryLogging.WARNINGS);
	}

	private static <T> T getValue(@Nullable IConfigValue<T> configValue, T defaultValue) {
		if (configValue == null) {
			return defaultValue;
		}
		return configValue.get();
	}

	private static <T> void setValue(@Nullable IConfigValue<T> configValue, T value) {
		if (configValue != null) {
			configValue.set(value);
		}
	}

	public enum GuiMode {
		WINDOW,
		FULLSCREEN
	}

	public enum RowDensity {
		COMFORTABLE(2, 8, 20),
		COMPACT(1, 6, 18);

		private final int minimumNameLines;
		private final int nameVerticalPadding;
		private final int listRowHeight;

		RowDensity(int minimumNameLines, int nameVerticalPadding, int listRowHeight) {
			this.minimumNameLines = minimumNameLines;
			this.nameVerticalPadding = nameVerticalPadding;
			this.listRowHeight = listRowHeight;
		}

		public int getMinimumNameLines() {
			return minimumNameLines;
		}

		public int getNameVerticalPadding() {
			return nameVerticalPadding;
		}

		public int getListRowHeight() {
			return listRowHeight;
		}
	}

	public enum NumberDisplayMode {
		SLIDER,
		TEXT_AND_BUTTONS
	}

	public enum DiscoveryLogging {
		OFF,
		WARNINGS,
		VERBOSE
	}
}
