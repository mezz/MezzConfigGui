package net.mezzdev.config.gui.config;

import net.mezzdev.config.api.plugin.IConfigRegistration;
import net.mezzdev.config.api.schema.IConfigCategoryBuilder;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.schema.IConfigSchemaBuilder;
import net.mezzdev.config.api.value.ConfigValueEditMode;
import net.mezzdev.config.api.value.IConfigValue;
import org.jetbrains.annotations.Nullable;

/**
 * User-configurable options for MezzConfigGui's own screen behavior.
 */
public final class ConfigGuiOptions {
	public static final String MOD_ID = "mezz_config_gui";

	private static final String CONFIG_FILE_NAME = "mezz_config_gui.ini";
	private static final String LOCALIZATION_PATH = "mezz_config_gui.config";

	@Nullable
	private static IConfigSchema schema;
	@Nullable
	private static IConfigValue<GuiSize> guiSize;
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
		guiSize = appearance.addEnum("guiSize", GuiSize.MEDIUM)
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
			.setEditMode(ConfigValueEditMode.RESTART)
			.build();
		discoveryLogging = advanced.addEnum("discoveryLogging", DiscoveryLogging.WARNINGS)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();

		schema = schemaBuilder.build();
	}

	public static IConfigSchema getSchema() {
		if (schema == null) {
			throw new IllegalStateException("MezzConfigGui options have not been registered yet.");
		}
		return schema;
	}

	public static GuiSize getGuiSize() {
		return getValue(guiSize, GuiSize.MEDIUM);
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

	public enum GuiSize {
		SMALL(320, 340, 230, 260),
		MEDIUM(320, 380, 230, 300),
		FULLSCREEN(0, 0, 0, 0) {
			@Override
			public int getWidth(int screenWidth) {
				return screenWidth;
			}

			@Override
			public int getHeight(int screenHeight) {
				return screenHeight;
			}
		};

		private final int minWidth;
		private final int maxWidth;
		private final int minHeight;
		private final int maxHeight;

		GuiSize(int minWidth, int maxWidth, int minHeight, int maxHeight) {
			this.minWidth = minWidth;
			this.maxWidth = maxWidth;
			this.minHeight = minHeight;
			this.maxHeight = maxHeight;
		}

		public int getWidth(int screenWidth) {
			return Math.clamp(screenWidth - 40, minWidth, maxWidth);
		}

		public int getHeight(int screenHeight) {
			return Math.clamp(screenHeight - 40, minHeight, maxHeight);
		}
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
