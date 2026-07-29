package net.mezzdev.config.gui.test.neoforge.custom;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

@Mod(NeoForgeNativeCustomTestMod.MOD_ID)
public final class NeoForgeNativeCustomTestMod {
	public static final String MOD_ID = "mezz_config_gui_test_neoforge_custom";
	public static final String CLIENT_FILE_NAME = MOD_ID + "-client.toml";
	public static final ModConfigSpec CLIENT_SPEC;
	public static final ModConfigSpec.ConfigValue<Boolean> ENABLED;
	public static final ModConfigSpec.ConfigValue<Boolean> EXTRA_EFFECTS;
	public static final ModConfigSpec.ConfigValue<Boolean> SECRET_DIAGNOSTICS;
	public static final ModConfigSpec.ConfigValue<TestMode> MODE;
	public static final ModConfigSpec.ConfigValue<String> LABEL;
	public static final ModConfigSpec.ConfigValue<Integer> ROW_COUNT;
	public static final ModConfigSpec.ConfigValue<Double> OPACITY;

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		builder.push("client");
		ENABLED = builder.comment("Boolean value moved into a custom quick category.")
			.translation(MOD_ID + ".configuration.client.enabled")
			.define("enabled", true);
		EXTRA_EFFECTS = builder.comment("Boolean value omitted from the manually configured GUI.")
			.translation(MOD_ID + ".configuration.client.extraEffects")
			.define("extraEffects", true);
		SECRET_DIAGNOSTICS = builder.comment("Boolean value omitted from the manually configured GUI.")
			.translation(MOD_ID + ".configuration.client.secretDiagnostics")
			.define("secretDiagnostics", false);
		MODE = builder.comment("Enum value displayed with a custom cycling selection editor.")
			.translation(MOD_ID + ".configuration.client.mode")
			.defineEnum("mode", TestMode.BALANCED);
		LABEL = builder.comment("String value omitted from the manually configured GUI.")
			.translation(MOD_ID + ".configuration.client.label")
			.define("label", "NeoForge Custom", value -> value instanceof String string && !string.isBlank() && string.length() <= 40);
		ROW_COUNT = builder.comment("Integer value omitted from the manually configured GUI.")
			.translation(MOD_ID + ".configuration.client.rowCount")
			.defineInRange("rowCount", 6, 1, 12);
		OPACITY = builder.comment("Double value omitted from the manually configured GUI.")
			.translation(MOD_ID + ".configuration.client.opacity")
			.gameRestart()
			.defineInRange("opacity", 0.85D, 0.1D, 1.0D);
		builder.pop();
		CLIENT_SPEC = builder.build();
	}

	public NeoForgeNativeCustomTestMod(ModContainer modContainer) {
		modContainer.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, CLIENT_FILE_NAME);
	}

	public enum TestMode {
		SLOW,
		BALANCED,
		FAST
	}
}
