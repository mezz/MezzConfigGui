package net.mezzdev.config.gui.test.neoforge.defaults;

import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import net.mezzdev.config.gui.test.neoforge.custom.NeoForgeNativeCustomTestGameTests;
import net.mezzdev.config.gui.test.neoforge.custom.NeoForgeNativeCustomTestMod;
import net.minecraft.network.chat.Component;
import net.neoforged.testframework.gametest.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.testframework.gametest.EmptyTemplate;
import net.neoforged.testframework.annotation.TestHolder;

import java.util.List;

public final class NeoForgeNativeDefaultsTestGameTests {
	private static final String CONFIG_GUI_MOD_ID = "mezz_config_gui";

	private NeoForgeNativeDefaultsTestGameTests() {

	}

	@GameTest
	@EmptyTemplate
	@TestHolder
	public static void nativeDefaultConfigLoadsOnServer(GameTestHelper helper) {
		helper.assertTrue(ModList.get().isLoaded(CONFIG_GUI_MOD_ID), Component.literal("MezzConfig GUI must be loaded for the integrated GameTest run."));
		helper.assertTrue(ModList.get().isLoaded(NeoForgeNativeDefaultsTestMod.MOD_ID), Component.literal("NeoForge native defaults test mod must be loaded."));
		helper.assertTrue(NeoForgeNativeDefaultsTestMod.COMMON_SPEC.isLoaded(), Component.literal("Common native config spec must be loaded."));
		helper.assertValueEqual(4096L, NeoForgeNativeDefaultsTestMod.CACHE_BUDGET.get(), Component.literal("Native common long default must load."));
		helper.assertValueEqual(List.of(128L, 256L, 512L), NeoForgeNativeDefaultsTestMod.LONG_BREAKPOINTS.get().stream().map(Number::longValue).toList(), Component.literal("Native common long list default must load."));
		helper.assertValueEqual(1.0D, NeoForgeNativeDefaultsTestMod.SCALE.get(), Component.literal("Native common double default must load."));
		helper.assertValueEqual(List.of(0.25D, 0.5D, 0.75D), NeoForgeNativeDefaultsTestMod.THRESHOLDS.get(), Component.literal("Native common double list default must load."));
		helper.succeed();
	}
	@GameTest
	@EmptyTemplate
	@TestHolder
	public static void nativeCustomConfigLoadsOnServer(GameTestHelper helper) {
		NeoForgeNativeCustomTestGameTests.nativeCustomConfigLoadsOnServer(helper);
	}

	@GameTest
	@EmptyTemplate
	@TestHolder
	public static void nativeListsRemainValidAfterSavingAndReloading(GameTestHelper helper) {
		for (ModConfigSpec spec : List.of(NeoForgeNativeDefaultsTestMod.CLIENT_SPEC, NeoForgeNativeDefaultsTestMod.COMMON_SPEC, NeoForgeNativeCustomTestMod.CLIENT_SPEC)) {
			var config = TomlFormat.newConfig();
			spec.correct(config);
			for (int reload = 0; reload < 2; reload++) {
				config = new TomlParser().parse(new TomlWriter().writeToString(config));
				helper.assertTrue(spec.isCorrect(config), Component.literal("Saved native lists must reload without triggering config corrections."));
			}
		}
		helper.succeed();
	}

	@GameTest
	@EmptyTemplate
	@TestHolder
	public static void emptyFavoriteModesSurviveSavingAndReloading(GameTestHelper helper) {
		assertEmptyFavoriteModes(helper, NeoForgeNativeDefaultsTestMod.CLIENT_SPEC, List.of("general", "favoriteModes"));
		assertEmptyFavoriteModes(helper, NeoForgeNativeCustomTestMod.CLIENT_SPEC, List.of("client", "favoriteModes"));
		helper.succeed();
	}

	private static void assertEmptyFavoriteModes(GameTestHelper helper, ModConfigSpec spec, List<String> path) {
		ModConfigSpec.ListValueSpec valueSpec = spec.getSpec().get(path);
		helper.assertTrue(valueSpec.getSizeRange().test(0), Component.literal("Favorite Modes must allow removing the last entry."));
		var config = TomlFormat.newConfig();
		spec.correct(config);
		config.set(path, List.of());
		for (int reload = 0; reload < 2; reload++) {
			config = new TomlParser().parse(new TomlWriter().writeToString(config));
			helper.assertTrue(spec.isCorrect(config), Component.literal("An empty Favorite Modes list must remain valid after reloading."));
			spec.correct(config);
			helper.assertValueEqual(List.of(), config.get(path), Component.literal("Reloading must not restore the default modes over an empty selection."));
		}
	}
}
