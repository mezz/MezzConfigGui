package net.mezzdev.config.gui.test.neoforge.defaults;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

@Mod(NeoForgeNativeDefaultsTestMod.MOD_ID)
public final class NeoForgeNativeDefaultsTestMod {
	public static final String MOD_ID = "mezz_config_gui_test_neoforge_defaults";
	private static final ModConfigSpec CLIENT_SPEC;
	private static final ModConfigSpec COMMON_SPEC;

	static {
		ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
		clientBuilder.push("general");
		clientBuilder.comment("Immediate boolean adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.enabled")
			.define("enabled", true);
		clientBuilder.comment("Bounded integer adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.maxVisibleRows")
			.defineInRange("maxVisibleRows", 9, 1, 18);
		clientBuilder.comment("Enum adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.mode")
			.defineEnum("mode", TestMode.BALANCED);
		clientBuilder.comment("String adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.screenLabel")
			.define("screenLabel", "NeoForge Defaults", value -> value instanceof String string && !string.isBlank() && string.length() <= 40);
		clientBuilder.pop();
		CLIENT_SPEC = clientBuilder.build();

		ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
		commonBuilder.push("limits");
		commonBuilder.comment("Long value adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.limits.cacheBudget")
			.defineInRange("cacheBudget", 4096L, 0L, 65_536L);
		commonBuilder.comment("Double value adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.limits.scale")
			.defineInRange("scale", 1.0D, 0.25D, 4.0D);
		commonBuilder.pop();
		COMMON_SPEC = commonBuilder.build();
	}

	public NeoForgeNativeDefaultsTestMod(ModContainer modContainer) {
		modContainer.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, "%s-client.toml".formatted(MOD_ID));
		modContainer.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "%s-common.toml".formatted(MOD_ID));
	}

	private enum TestMode {
		SLOW,
		BALANCED,
		FAST
	}
}
