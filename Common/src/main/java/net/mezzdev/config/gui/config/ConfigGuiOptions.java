package net.mezzdev.config.gui.config;

import net.mezzdev.config.api.plugin.IConfigRegistration;
import net.mezzdev.config.api.schema.IConfigCategoryBuilder;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.schema.IConfigSchemaBuilder;
import net.mezzdev.config.api.value.ConfigValueEditMode;
import net.mezzdev.config.api.value.ConfigValueRestartRequirement;
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
	private static IConfigValue<Integer> windowWidth;
	@Nullable
	private static IConfigValue<Integer> windowHeight;
	@Nullable
	private static IConfigValue<RowDensity> rowDensity;
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

	public static void register(IConfigRegistration registration) {
		IConfigSchemaBuilder schemaBuilder = registration.createSchemaBuilder(CONFIG_FILE_NAME, LOCALIZATION_PATH);
		IConfigCategoryBuilder appearance = schemaBuilder.addCategory("appearance");
		guiMode = appearance.addEnum("guiMode", GuiMode.WINDOW)
			.addLegacyValueMigration("appearance", "guiSize", ConfigGuiOptions::migrateGuiMode)
			.setEditMode(ConfigValueEditMode.BATCH)
			.build();
		windowWidth = appearance.addInteger("windowWidth", DEFAULT_WINDOW_WIDTH, MIN_WINDOW_WIDTH, MAX_WINDOW_SIZE)
			.addLegacyValueMigration("appearance", "guiSize", ConfigGuiOptions::migrateWindowWidth)
			.setEditMode(ConfigValueEditMode.BATCH)
			.build();
		windowHeight = appearance.addInteger("windowHeight", DEFAULT_WINDOW_HEIGHT, MIN_WINDOW_HEIGHT, MAX_WINDOW_SIZE)
			.addLegacyValueMigration("appearance", "guiSize", ConfigGuiOptions::migrateWindowHeight)
			.setEditMode(ConfigValueEditMode.BATCH)
			.build();
		rowDensity = appearance.addEnum("rowDensity", RowDensity.COMFORTABLE)
			.setEditMode(ConfigValueEditMode.BATCH)
			.build();
		showRowStriping = appearance.addBoolean("showRowStriping", true)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		showAdvancedValueDetails = appearance.addBoolean("showAdvancedValueDetails", false)
			.setEditMode(ConfigValueEditMode.BATCH)
			.build();

		IConfigCategoryBuilder navigation = schemaBuilder.addCategory("navigation");
		rememberLastCategory = navigation.addBoolean("rememberLastCategory", true)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		searchDescriptions = navigation.addBoolean("searchDescriptions", true)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		focusSearchOnOpen = navigation.addBoolean("focusSearchOnOpen", false)
			.setEditMode(ConfigValueEditMode.BATCH)
			.build();

		IConfigCategoryBuilder scrolling = schemaBuilder.addCategory("scrolling");
		smoothScrolling = scrolling.addBoolean("smoothScrolling", true)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		scrollSpeed = scrolling.addInteger("scrollSpeed", 10, 1, 50)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		dragAutoScrollSpeed = scrolling.addInteger("dragAutoScrollSpeed", 6, 1, 50)
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

		IConfigCategoryBuilder advanced = schemaBuilder.addCategory("advanced");
		enableNativeConfigDiscovery = advanced.addBoolean("enableNativeConfigDiscovery", true)
			.setEditMode(ConfigValueEditMode.BATCH)
			.setRestartRequirement(ConfigValueRestartRequirement.GAME_RESTART)
			.build();
		discoveryLogging = advanced.addEnum("discoveryLogging", DiscoveryLogging.WARNINGS)
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
		return configValue.getValue();
	}

	private static <T> void setValue(@Nullable IConfigValue<T> configValue, T value) {
		if (configValue != null) {
			configValue.set(value);
		}
	}

	private static GuiMode migrateGuiMode(String legacyGuiSize) {
		if (normalizeLegacyGuiSize(legacyGuiSize).equals("FULLSCREEN")) {
			return GuiMode.FULLSCREEN;
		}
		return GuiMode.WINDOW;
	}

	private static int migrateWindowWidth(String legacyGuiSize) {
		if (normalizeLegacyGuiSize(legacyGuiSize).equals("SMALL")) {
			return 340;
		}
		return DEFAULT_WINDOW_WIDTH;
	}

	private static int migrateWindowHeight(String legacyGuiSize) {
		String normalized = normalizeLegacyGuiSize(legacyGuiSize);
		if (normalized.equals("SMALL")) {
			return 260;
		}
		return DEFAULT_WINDOW_HEIGHT;
	}

	private static String normalizeLegacyGuiSize(String legacyGuiSize) {
		String normalized = legacyGuiSize.trim();
		if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
			normalized = normalized.substring(1, normalized.length() - 1);
		}
		return normalized;
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

	public enum DiscoveryLogging {
		OFF,
		WARNINGS,
		VERBOSE
	}
}
