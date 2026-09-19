package net.mezzdev.config.gui.test.neoforge.defaults;

import net.neoforged.testframework.gametest.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
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
		helper.assertTrue(ModList.get().isLoaded(CONFIG_GUI_MOD_ID), net.minecraft.network.chat.Component.literal("MezzConfig GUI must be loaded for the integrated GameTest run."));
		helper.assertTrue(ModList.get().isLoaded(NeoForgeNativeDefaultsTestMod.MOD_ID), net.minecraft.network.chat.Component.literal("NeoForge native defaults test mod must be loaded."));
		helper.assertTrue(NeoForgeNativeDefaultsTestMod.COMMON_SPEC.isLoaded(), net.minecraft.network.chat.Component.literal("Common native config spec must be loaded."));
		helper.assertValueEqual(4096L, NeoForgeNativeDefaultsTestMod.CACHE_BUDGET.get(), net.minecraft.network.chat.Component.literal("Native common long default must load."));
		helper.assertValueEqual(List.of(128L, 256L, 512L), NeoForgeNativeDefaultsTestMod.LONG_BREAKPOINTS.get(), net.minecraft.network.chat.Component.literal("Native common long list default must load."));
		helper.assertValueEqual(1.0D, NeoForgeNativeDefaultsTestMod.SCALE.get(), net.minecraft.network.chat.Component.literal("Native common double default must load."));
		helper.assertValueEqual(List.of(0.25D, 0.5D, 0.75D), NeoForgeNativeDefaultsTestMod.THRESHOLDS.get(), net.minecraft.network.chat.Component.literal("Native common double list default must load."));
		helper.succeed();
	}
	@GameTest
	@EmptyTemplate
	@TestHolder
	public static void nativeCustomConfigLoadsOnServer(GameTestHelper helper) {
		net.mezzdev.config.gui.test.neoforge.custom.NeoForgeNativeCustomTestGameTests.nativeCustomConfigLoadsOnServer(helper);
	}
}
