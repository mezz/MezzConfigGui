package net.mezzdev.config.gui.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.mezzdev.config.gui.ConfigGui;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.MezzConfigScreen;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.remote.RemoteConfigEditor;
import net.mezzdev.config.gui.remote.RemoteConfigNetworking;
import net.mezzdev.config.gui.remote.RemoteConfigRequestChunkPayload;
import net.mezzdev.config.gui.remote.RemoteConfigResponseChunkPayload;
import net.mezzdev.config.gui.screenlist.ConfigScreenFactoryRegistry;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.minecraft.server.packs.PackType;

/**
 * Fabric client entry point for the config GUI mod.
 */
public final class ConfigGuiFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ConfigGuiOptions.register();
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> getScreenFactoryRegistry());
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (screen instanceof MezzConfigScreen configScreen) {
				ScreenEvents.beforeExtract(screen).register((renderedScreen, graphics, mouseX, mouseY, tickDelta) -> configScreen.clearTooltipForNextRenderPass());
				// Runs after renderWithTooltip, including JEI's foreground injection.
				ScreenEvents.afterExtract(screen).register(
					(renderedScreen, graphics, mouseX, mouseY, tickDelta) -> configScreen.renderDeferredTooltip(graphics, mouseX, mouseY)
				);
			}
		});
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
		var resources = ResourceLoader.get(PackType.CLIENT_RESOURCES);
		var sprites = new ConfigGuiIdentifiableResourceReloadListener("config_gui_sprite_manager", () -> ConfigTextures.get().getGuiSpriteManager());
		var colors = new ConfigGuiIdentifiableResourceReloadListener("config_gui_colors", ConfigGuiColors::createReloadListener);
		resources.registerReloadListener(sprites.getFabricId(), sprites);
		resources.registerReloadListener(colors.getFabricId(), colors);
	}

	static ConfigScreenFactoryRegistry getScreenFactoryRegistry() {
		return RegistryHolder.REGISTRY;
	}

	private static final class RegistryHolder {
		private static final ConfigScreenFactoryRegistry REGISTRY = createRegistry();

		private static ConfigScreenFactoryRegistry createRegistry() {
			ConfigScreenFactoryRegistry registry = ConfigGui.createScreenFactoryRegistry(ConfigGuiFabricPluginFinder.getPlugins());
			ConfigGui.createScreenListFactory(registry, FabricConfigScreenOwnerMetadata::get);
			return registry;
		}
	}

	private static boolean isChannelAvailable() {
		return ClientPlayNetworking.canSend(RemoteConfigRequestChunkPayload.TYPE);
	}
}
