package net.mezzdev.config.gui.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge entry point for the config GUI mod.
 */
@Mod(ConfigGuiForge.MOD_ID)
public final class ConfigGuiForge {
	public static final String MOD_ID = "mezz_config_gui";

	public ConfigGuiForge(FMLJavaModLoadingContext context) {
		ConfigGuiForgeClientSafeRunner clientSafeRunner = new ConfigGuiForgeClientSafeRunner(context.getModEventBus());
		DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> clientSafeRunner::registerClient);
	}
}
