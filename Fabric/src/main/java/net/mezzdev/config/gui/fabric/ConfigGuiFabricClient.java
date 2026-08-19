package net.mezzdev.config.gui.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.remote.RemoteConfigEditor;
import net.mezzdev.config.gui.remote.RemoteConfigNetworking;
import net.mezzdev.config.gui.remote.RemoteConfigRequestChunkPayload;
import net.mezzdev.config.gui.remote.RemoteConfigResponseChunkPayload;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.minecraft.server.packs.PackType;

/**
 * Fabric client entry point for the config GUI mod.
 */
public final class ConfigGuiFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ConfigGuiOptions.register();
		ClientPlayNetworking.registerGlobalReceiver(
			RemoteConfigResponseChunkPayload.TYPE,
			(payload, context) -> RemoteConfigEditor.handleResponseChunk(payload)
		);
		RemoteConfigNetworking.setClientSender(payload -> {
			if (!ClientPlayNetworking.canSend(payload.type())) {
				return false;
			}
			ClientPlayNetworking.send(payload);
			return true;
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> RemoteConfigEditor.onClientTick(isChannelAvailable()));
		ClientPlayConnectionEvents.JOIN.register(
			(handler, sender, client) -> RemoteConfigEditor.onClientConnected(isChannelAvailable())
		);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RemoteConfigEditor.onClientDisconnect());
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
			.registerReloadListener(new ConfigGuiIdentifiableResourceReloadListener(
				"config_gui_sprite_manager",
				() -> ConfigTextures.get().getGuiSpriteManager()
			));
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
			.registerReloadListener(new ConfigGuiIdentifiableResourceReloadListener(
				"config_gui_colors",
				ConfigGuiColors::createReloadListener
			));
	}

	private static boolean isChannelAvailable() {
		return ClientPlayNetworking.canSend(RemoteConfigRequestChunkPayload.TYPE);
	}
}
