package net.mezzdev.config.gui.test.neoforge.defaults;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

public final class NeoForgeNativeDefaultsTestGameTests {
	private static final String CONFIG_GUI_MOD_ID = "mezz_config_gui";

	private NeoForgeNativeDefaultsTestGameTests() {

	}

	@GameTest(templateNamespace = "minecraft", template = "bastion/blocks/air")
	@PrefixGameTestTemplate(false)
	public static void nativeDefaultConfigLoadsOnServer(GameTestHelper helper) {
		helper.assertTrue(ModList.get().isLoaded(CONFIG_GUI_MOD_ID), "Mezz Config GUI must be loaded for the integrated GameTest run.");
		helper.assertTrue(ModList.get().isLoaded(NeoForgeNativeDefaultsTestMod.MOD_ID), "NeoForge native defaults test mod must be loaded.");
		helper.assertTrue(NeoForgeNativeDefaultsTestMod.COMMON_SPEC.isLoaded(), "Common native config spec must be loaded.");
		helper.assertValueEqual(4096L, NeoForgeNativeDefaultsTestMod.CACHE_BUDGET.get(), "Native common long default must load.");
		helper.assertValueEqual(1.0D, NeoForgeNativeDefaultsTestMod.SCALE.get(), "Native common double default must load.");
		helper.succeed();
	}
}
