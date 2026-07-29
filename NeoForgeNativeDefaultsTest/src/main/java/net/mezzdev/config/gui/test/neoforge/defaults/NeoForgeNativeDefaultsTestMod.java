package net.mezzdev.config.gui.test.neoforge.defaults;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(NeoForgeNativeDefaultsTestMod.MOD_ID)
public final class NeoForgeNativeDefaultsTestMod {
	public static final String MOD_ID = "mezz_config_gui_test_neoforge_defaults";
	static final ModConfigSpec CLIENT_SPEC;
	static final ModConfigSpec COMMON_SPEC;
	static final ModConfigSpec.BooleanValue ENABLED;
	static final ModConfigSpec.IntValue MAX_VISIBLE_ROWS;
	static final ModConfigSpec.EnumValue<TestMode> MODE;
	static final ModConfigSpec.ConfigValue<String> SCREEN_LABEL;
	static final ModConfigSpec.LongValue CACHE_BUDGET;
	static final ModConfigSpec.DoubleValue SCALE;

	static {
		ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
		clientBuilder.push("general");
		ENABLED = clientBuilder.comment("Immediate boolean adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.enabled")
			.define("enabled", true);
		MAX_VISIBLE_ROWS = clientBuilder.comment("Bounded integer adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.maxVisibleRows")
			.defineInRange("maxVisibleRows", 9, 1, 18);
		MODE = clientBuilder.comment("Enum adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.mode")
			.defineEnum("mode", TestMode.BALANCED);
		SCREEN_LABEL = clientBuilder.comment("String adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.screenLabel")
			.define("screenLabel", "NeoForge Defaults", value -> value instanceof String string && !string.isBlank() && string.length() <= 40);
		clientBuilder.pop();
		CLIENT_SPEC = clientBuilder.build();

		ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
		commonBuilder.push("limits");
		CACHE_BUDGET = commonBuilder.comment("Long value adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.limits.cacheBudget")
			.defineInRange("cacheBudget", 4096L, 0L, 65_536L);
		SCALE = commonBuilder.comment("Double value adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.limits.scale")
			.defineInRange("scale", 1.0D, 0.25D, 4.0D);
		commonBuilder.pop();
		COMMON_SPEC = commonBuilder.build();
	}

	public NeoForgeNativeDefaultsTestMod(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
		if (dist.isClient()) {
			NeoForgeNativeDefaultsTestClient.register(modEventBus);
		}
		modEventBus.addListener(NeoForgeNativeDefaultsTestMod::registerGameTests);
		modContainer.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, "%s-client.toml".formatted(MOD_ID));
		modContainer.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "%s-common.toml".formatted(MOD_ID));
	}

	private static void registerGameTests(RegisterGameTestsEvent event) {
		event.register(NeoForgeNativeDefaultsTestGameTests.class);
	}

	enum TestMode {
		SLOW,
		BALANCED,
		FAST
	}
}
