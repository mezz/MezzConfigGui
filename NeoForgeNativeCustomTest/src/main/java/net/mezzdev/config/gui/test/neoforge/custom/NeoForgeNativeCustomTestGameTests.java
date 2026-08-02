package net.mezzdev.config.gui.test.neoforge.custom;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

public final class NeoForgeNativeCustomTestGameTests {
	private static final String CONFIG_GUI_MOD_ID = "mezz_config_gui";

	private NeoForgeNativeCustomTestGameTests() {

	}

	@GameTest(templateNamespace = "minecraft", template = "bastion/blocks/air")
	@PrefixGameTestTemplate(false)
	public static void nativeCustomConfigLoadsOnServer(GameTestHelper helper) {
		helper.assertTrue(ModList.get().isLoaded(CONFIG_GUI_MOD_ID), "Mezz Config GUI must be loaded for the integrated GameTest run.");
		helper.assertTrue(ModList.get().isLoaded(NeoForgeNativeCustomTestMod.MOD_ID), "NeoForge native custom test mod must be loaded.");
		helper.assertTrue(NeoForgeNativeCustomTestMod.COMMON_SPEC.isLoaded(), "Common native custom config spec must be loaded.");
		helper.assertValueEqual(true, NeoForgeNativeCustomTestMod.COMMON_ENABLED.get(), "Native common boolean default must load.");
		helper.assertValueEqual(List.of("common", "native"), NeoForgeNativeCustomTestMod.COMMON_ALIASES.get(), "Native common string list default must load.");
		helper.assertValueEqual(4096L, NeoForgeNativeCustomTestMod.COMMON_CACHE_BUDGET.get(), "Native common long default must load.");
		helper.succeed();
	}
}
