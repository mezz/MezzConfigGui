package net.mezzdev.config.gui.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge entry point for the config GUI mod.
 */
@Mod(ConfigGuiNeoForge.MOD_ID)
public final class ConfigGuiNeoForge {
	public static final String MOD_ID = "mezz_config_gui";

	public ConfigGuiNeoForge(IEventBus modEventBus, Dist dist) {
		if (dist.isClient()) {
			ConfigGuiNeoForgeClient.register(modEventBus);
		}
	}
}
