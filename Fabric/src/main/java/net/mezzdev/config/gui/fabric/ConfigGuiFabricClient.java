package net.mezzdev.config.gui.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.minecraft.server.packs.PackType;

/**
 * Fabric client entry point for the config GUI mod.
 */
public final class ConfigGuiFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
			.registerReloadListener(new ConfigGuiIdentifiableResourceReloadListener(
				"config_gui_sprite_manager",
				() -> ConfigTextures.get().getGuiSpriteManager()
			));
	}
}
