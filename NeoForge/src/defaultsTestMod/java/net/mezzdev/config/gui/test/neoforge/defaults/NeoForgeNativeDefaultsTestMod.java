package net.mezzdev.config.gui.test.neoforge.defaults;

import net.minecraft.resources.Identifier;
import net.neoforged.testframework.conf.FrameworkConfiguration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;

@Mod(NeoForgeNativeDefaultsTestMod.MOD_ID)
public final class NeoForgeNativeDefaultsTestMod {
	public static final String MOD_ID = "mezz_config_gui_test_neoforge_defaults";
	static final ModConfigSpec CLIENT_SPEC;
	static final ModConfigSpec COMMON_SPEC;
	static final ModConfigSpec.BooleanValue ENABLED;
	static final ModConfigSpec.ConfigValue<List<? extends Boolean>> ENABLED_HISTORY;
	static final ModConfigSpec.IntValue MAX_VISIBLE_ROWS;
	static final ModConfigSpec.ConfigValue<List<? extends Integer>> FAVORITE_NUMBERS;
	static final ModConfigSpec.EnumValue<TestMode> MODE;
	static final ModConfigSpec.ConfigValue<List<?>> FAVORITE_MODES;
	static final ModConfigSpec.ConfigValue<String> SCREEN_LABEL;
	static final ModConfigSpec.ConfigValue<List<? extends String>> ALIASES;
	static final ModConfigSpec.LongValue CACHE_BUDGET;
	static final ModConfigSpec.ConfigValue<List<? extends Number>> LONG_BREAKPOINTS;
	static final ModConfigSpec.DoubleValue SCALE;
	static final ModConfigSpec.ConfigValue<List<? extends Double>> THRESHOLDS;

	static {
		ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
		clientBuilder.push("general");
		ENABLED = clientBuilder.comment("Immediate boolean adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.enabled")
			.define("enabled", true);
		ENABLED_HISTORY = clientBuilder.comment("Boolean list adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.enabledHistory")
			.defineList("enabledHistory", List.of(true, false, true), () -> true, value -> value instanceof Boolean);
		MAX_VISIBLE_ROWS = clientBuilder.comment("Bounded integer adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.maxVisibleRows")
			.defineInRange("maxVisibleRows", 9, 1, 18);
		FAVORITE_NUMBERS = clientBuilder.comment("Integer list adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.favoriteNumbers")
			.defineList("favoriteNumbers", List.of(1, 2, 3), () -> 0, value -> value instanceof Integer integer && integer >= 0 && integer <= 16);
		MODE = clientBuilder.comment("Enum adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.mode")
			.defineEnum("mode", TestMode.BALANCED);
		FAVORITE_MODES = clientBuilder.comment("Enum list adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.favoriteModes")
			.defineList("favoriteModes", List.of(TestMode.BALANCED, TestMode.FAST), () -> TestMode.BALANCED,
				value -> value instanceof TestMode || value instanceof String name && Arrays.stream(TestMode.values()).anyMatch(mode -> mode.name().equals(name)));
		SCREEN_LABEL = clientBuilder.comment("String adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.screenLabel")
			.define("screenLabel", "NeoForge Defaults", value -> value instanceof String string && !string.isBlank() && string.length() <= 40);
		ALIASES = clientBuilder.comment("String list adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.general.aliases")
			.defineList("aliases", List.of("default", "native"), () -> "", value -> value instanceof String string && !string.isBlank() && string.length() <= 40);
		clientBuilder.pop();
		CLIENT_SPEC = clientBuilder.build();

		ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();
		commonBuilder.push("limits");
		CACHE_BUDGET = commonBuilder.comment("Long value adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.limits.cacheBudget")
			.defineInRange("cacheBudget", 4096L, 0L, 65_536L);
		LONG_BREAKPOINTS = commonBuilder.comment("Long list adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.limits.longBreakpoints")
			.defineList("longBreakpoints", List.of(128L, 256L, 512L), () -> 0L,
				value -> (value instanceof Integer || value instanceof Long) && ((Number) value).longValue() >= 0L && ((Number) value).longValue() <= 1024L);
		SCALE = commonBuilder.comment("Double value adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.limits.scale")
			.defineInRange("scale", 1.0D, 0.25D, 4.0D);
		THRESHOLDS = commonBuilder.comment("Double list adapted from a native NeoForge config.")
			.translation(MOD_ID + ".configuration.limits.thresholds")
			.defineList("thresholds", List.of(0.25D, 0.5D, 0.75D), () -> 0.0D, value -> value instanceof Double doubleValue && doubleValue >= 0.0D && doubleValue <= 1.0D);
		commonBuilder.pop();
		COMMON_SPEC = commonBuilder.build();
	}

	public NeoForgeNativeDefaultsTestMod(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
		if (dist.isClient()) {
			NeoForgeNativeDefaultsTestClient.register(modEventBus);
		}
		FrameworkConfiguration.builder(
				Identifier.fromNamespaceAndPath(MOD_ID, "tests")
			)
			.build().create().init(modEventBus, modContainer);
		modContainer.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, "%s-client.toml".formatted(MOD_ID));
		modContainer.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "%s-common.toml".formatted(MOD_ID));
	}

	enum TestMode {
		SLOW,
		BALANCED,
		FAST
	}
}
