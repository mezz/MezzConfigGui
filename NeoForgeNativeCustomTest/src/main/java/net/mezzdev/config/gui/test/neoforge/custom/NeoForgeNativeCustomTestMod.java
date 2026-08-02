package net.mezzdev.config.gui.test.neoforge.custom;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;

@Mod(NeoForgeNativeCustomTestMod.MOD_ID)
public final class NeoForgeNativeCustomTestMod {
	public static final String MOD_ID = "mezz_config_gui_test_neoforge_custom";
	public static final String CLIENT_FILE_NAME = MOD_ID + "-client.toml";
	public static final String COMMON_FILE_NAME = MOD_ID + "-common.toml";
	public static final ModConfigSpec CLIENT_SPEC;
	static final ModConfigSpec COMMON_SPEC;
	public static final ModConfigSpec.ConfigValue<Boolean> ENABLED;
	public static final ModConfigSpec.ConfigValue<Boolean> EXTRA_EFFECTS;
	public static final ModConfigSpec.ConfigValue<Boolean> SECRET_DIAGNOSTICS;
	public static final ModConfigSpec.ConfigValue<TestMode> MODE;
	public static final ModConfigSpec.ConfigValue<String> LABEL;
	public static final ModConfigSpec.ConfigValue<Integer> ROW_COUNT;
	public static final ModConfigSpec.ConfigValue<Double> OPACITY;
	public static final ModConfigSpec.ConfigValue<List<? extends Boolean>> ENABLED_HISTORY;
	public static final ModConfigSpec.ConfigValue<List<? extends Integer>> FAVORITE_ROWS;
	public static final ModConfigSpec.ConfigValue<List<? extends TestMode>> FAVORITE_MODES;
	public static final ModConfigSpec.ConfigValue<List<? extends String>> ALIASES;
	public static final ModConfigSpec.ConfigValue<Long> CACHE_BUDGET;
	public static final ModConfigSpec.ConfigValue<List<? extends Long>> CACHE_BREAKPOINTS;
	public static final ModConfigSpec.ConfigValue<List<? extends Double>> OPACITY_STEPS;
	static final ModConfigSpec.ConfigValue<Boolean> COMMON_ENABLED;
	static final ModConfigSpec.ConfigValue<List<? extends String>> COMMON_ALIASES;
	static final ModConfigSpec.ConfigValue<Long> COMMON_CACHE_BUDGET;

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		builder.push("client");
		ENABLED = builder.comment("Boolean value moved into a custom quick category.")
			.translation(MOD_ID + ".configuration.client.enabled")
			.define("enabled", true);
		EXTRA_EFFECTS = builder.comment("Boolean value kept in the native config category.")
			.translation(MOD_ID + ".configuration.client.extraEffects")
			.define("extraEffects", true);
		SECRET_DIAGNOSTICS = builder.comment("Boolean value hidden from the GUI by the plugin.")
			.translation(MOD_ID + ".configuration.client.secretDiagnostics")
			.define("secretDiagnostics", false);
		MODE = builder.comment("Enum value displayed with a custom cycling selection editor.")
			.translation(MOD_ID + ".configuration.client.mode")
			.defineEnum("mode", TestMode.BALANCED);
		LABEL = builder.comment("String value kept in the native config category.")
			.translation(MOD_ID + ".configuration.client.label")
			.define("label", "NeoForge Custom", value -> value instanceof String string && !string.isBlank() && string.length() <= 40);
		ROW_COUNT = builder.comment("Integer value moved into a custom quick category.")
			.translation(MOD_ID + ".configuration.client.rowCount")
			.defineInRange("rowCount", 6, 1, 12);
		OPACITY = builder.comment("Double value kept in the native config category and marked game-restart.")
			.translation(MOD_ID + ".configuration.client.opacity")
			.gameRestart()
			.defineInRange("opacity", 0.85D, 0.1D, 1.0D);
		ENABLED_HISTORY = builder.comment("Boolean list moved into a custom list category.")
			.translation(MOD_ID + ".configuration.client.enabledHistory")
			.defineList("enabledHistory", List.of(true, false, true), () -> true, value -> value instanceof Boolean);
		FAVORITE_ROWS = builder.comment("Integer list moved into a custom list category.")
			.translation(MOD_ID + ".configuration.client.favoriteRows")
			.defineList("favoriteRows", List.of(2, 4, 8), () -> 1, value -> value instanceof Integer integer && integer >= 1 && integer <= 12);
		FAVORITE_MODES = builder.comment("Enum list moved into a custom list category.")
			.translation(MOD_ID + ".configuration.client.favoriteModes")
			.defineList("favoriteModes", List.of(TestMode.SLOW, TestMode.BALANCED), () -> TestMode.BALANCED, value -> value instanceof TestMode);
		ALIASES = builder.comment("String list moved into a custom list category.")
			.translation(MOD_ID + ".configuration.client.aliases")
			.defineList("aliases", List.of("custom", "native"), () -> "custom", value -> value instanceof String string && !string.isBlank() && string.length() <= 40);
		CACHE_BUDGET = builder.comment("Long value kept in the native config category.")
			.translation(MOD_ID + ".configuration.client.cacheBudget")
			.defineInRange("cacheBudget", 2048L, 0L, 65_536L);
		CACHE_BREAKPOINTS = builder.comment("Long list moved into a custom list category.")
			.translation(MOD_ID + ".configuration.client.cacheBreakpoints")
			.defineList("cacheBreakpoints", List.of(128L, 256L, 512L), () -> 0L, value -> value instanceof Long longValue && longValue >= 0L && longValue <= 1024L);
		OPACITY_STEPS = builder.comment("Double list moved into a custom list category.")
			.translation(MOD_ID + ".configuration.client.opacitySteps")
			.defineList("opacitySteps", List.of(0.25D, 0.5D, 0.85D), () -> 0.1D, value -> value instanceof Double doubleValue && doubleValue >= 0.1D && doubleValue <= 1.0D);
		builder.pop();
		CLIENT_SPEC = builder.build();

		ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
		commonBuilder.push("common");
		COMMON_ENABLED = commonBuilder.comment("Common boolean kept in its native config category.")
			.translation(MOD_ID + ".configuration.common.enabled")
			.define("enabled", true);
		COMMON_ALIASES = commonBuilder.comment("Common string list kept in its native config category.")
			.translation(MOD_ID + ".configuration.common.aliases")
			.defineList("aliases", List.of("common", "native"), () -> "common", value -> value instanceof String string && !string.isBlank() && string.length() <= 40);
		COMMON_CACHE_BUDGET = commonBuilder.comment("Common long value kept in its native config category.")
			.translation(MOD_ID + ".configuration.common.cacheBudget")
			.defineInRange("cacheBudget", 4096L, 0L, 65_536L);
		commonBuilder.pop();
		COMMON_SPEC = commonBuilder.build();
	}

	public NeoForgeNativeCustomTestMod(IEventBus modEventBus, ModContainer modContainer) {
		modEventBus.addListener(NeoForgeNativeCustomTestMod::registerGameTests);
		modContainer.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, CLIENT_FILE_NAME);
		modContainer.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, COMMON_FILE_NAME);
	}

	private static void registerGameTests(RegisterGameTestsEvent event) {
		event.register(NeoForgeNativeCustomTestGameTests.class);
	}

	public enum TestMode {
		SLOW,
		BALANCED,
		FAST
	}
}
