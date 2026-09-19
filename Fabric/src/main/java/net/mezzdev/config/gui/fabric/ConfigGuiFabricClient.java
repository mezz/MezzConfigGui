package net.mezzdev.config.gui.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.MezzConfigScreen;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
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
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof MezzConfigScreen configScreen) {
				ScreenEvents.beforeRender(screen).register((renderedScreen, graphics, mouseX, mouseY, tickDelta) -> configScreen.clearTooltipForNextRenderPass());
				// Runs after renderWithTooltip, including JEI's foreground injection.
				ScreenEvents.afterRender(screen).register(
					(renderedScreen, graphics, mouseX, mouseY, tickDelta) -> configScreen.renderDeferredTooltip(graphics, mouseX, mouseY)
				);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(RemoteConfigResponseChunkPayload.ID, (client, handler, buffer, sender) -> {
			var payload = new RemoteConfigResponseChunkPayload(buffer.readByteArray(RemoteConfigResponseChunkPayload.MAX_NETWORK_PAYLOAD_LENGTH));
			client.execute(() -> RemoteConfigEditor.handleResponseChunk(payload));
		});
		RemoteConfigNetworking.setClientSender(payload -> {
			if (!ClientPlayNetworking.canSend(RemoteConfigRequestChunkPayload.ID)) {
				return false;
			}
			var buffer = PacketByteBufs.create();
			buffer.writeByteArray(payload.payload());
			ClientPlayNetworking.send(RemoteConfigRequestChunkPayload.ID, buffer);
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
		return ClientPlayNetworking.canSend(RemoteConfigRequestChunkPayload.ID);
	}
}
