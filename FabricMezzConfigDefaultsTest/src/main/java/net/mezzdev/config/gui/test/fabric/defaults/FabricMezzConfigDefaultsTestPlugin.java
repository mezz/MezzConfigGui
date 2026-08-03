package net.mezzdev.config.gui.test.fabric.defaults;

import net.mezzdev.config.api.plugin.IConfigPlugin;
import net.mezzdev.config.api.plugin.IConfigRegistration;
import net.mezzdev.config.api.schema.IConfigCategoryBuilder;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.schema.IConfigSchemaBuilder;
import net.mezzdev.config.api.value.ConfigValueEditMode;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public final class FabricMezzConfigDefaultsTestPlugin implements IConfigPlugin, IConfigGuiPlugin {
	private static final String MOD_ID = "mezz_config_gui_test_fabric_defaults";
	private static final String LOCALIZATION_PATH = "mezz_config_gui_test.fabric.defaults";
	@Nullable
	private static IConfigSchema schema;

	@Override
	public String getModId() {
		return MOD_ID;
	}

	@Override
	public void registerConfigFiles(IConfigRegistration registration) {
		IConfigSchemaBuilder schemaBuilder = registration.createSchemaBuilder("config-gui-fabric-defaults-test.ini", LOCALIZATION_PATH);
		IConfigCategoryBuilder general = schemaBuilder.addCategory("general");
		general.addBoolean("enabled", true)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		general.addBoolean("requiresRestart", false)
			.setEditMode(ConfigValueEditMode.RESTART)
			.build();
		general.addBooleanList("enabledHistory", List.of(true, false, true)).build();

		IConfigCategoryBuilder numbers = schemaBuilder.addCategory("numbers");
		numbers.addInteger("maxVisibleRows", 8, 1, 16).build();
		numbers.addInteger("unboundedInteger", 1024).build();
		numbers.addInteger("tinySelectionRange", 2, 0, 4)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		numbers.addIntegerList("favoriteNumbers", List.of(1, 2, 3), 0, 16).build();
		numbers.addColor("accentColor", 0xFF33AA55).build();
		numbers.addColorList("palette", List.of(0xFF33AA55, 0xFF4477DD, 0xFFE0AA22)).build();
		numbers.addLong("maxEnergy", 10_000_000_000L).build();
		numbers.addLong("boundedLong", 64L, 0L, 1024L).build();
		numbers.addLongList("longBreakpoints", List.of(128L, 256L, 512L), 0L, 1024L).build();
		numbers.addDouble("scale", 1.25, 0.25, 4.0).build();
		numbers.addDoubleList("thresholds", List.of(0.25, 0.5, 0.75), 0.0, 1.0).build();

		IConfigCategoryBuilder text = schemaBuilder.addCategory("text");
		text.addString("screenLabel", "Fabric Defaults").build();
		text.addStringList("aliases", List.of("default", "sample")).build();

		IConfigCategoryBuilder selections = schemaBuilder.addCategory("selections");
		selections.addEnum("mode", TestMode.BALANCED).build();
		selections.addEnum("restrictedMode", TestMode.FAST, List.of(TestMode.BALANCED, TestMode.FAST)).build();
		selections.addEnumList("favoriteModes", List.of(TestMode.BALANCED, TestMode.FAST), TestMode.class).build();
		selections.addEnumList("fastModes", List.of(TestMode.FAST), List.of(TestMode.BALANCED, TestMode.FAST)).build();
		schema = schemaBuilder.build();
	}

	@Override
	public void register(IConfigGuiRegistration registration) {
		registration.registerScreen(
			Component.translatable(MOD_ID + ".config.screen.title"),
			FabricMezzConfigDefaultsTestPlugin::getSchema
		);
	}

	private static IConfigSchema getSchema() {
		return Objects.requireNonNull(schema, "schema");
	}

	private enum TestMode {
		SLOW,
		BALANCED,
		FAST
	}
}
