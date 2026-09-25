package net.mezzdev.config.gui.neoforge;

import net.minecraft.resources.Identifier;
import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.mezzdev.config.gui.ConfigGui;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.ConfigScreenConfig;
import net.mezzdev.config.gui.MezzConfigScreen;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.neoforge.config.NeoForgeConfigScreenConfigs;
import net.mezzdev.config.gui.remote.RemoteConfigEditor;
import net.mezzdev.config.gui.remote.RemoteConfigNetworking;
import net.mezzdev.config.gui.remote.RemoteConfigRequestChunkPayload;
import net.mezzdev.config.gui.screenlist.ConfigScreenFactoryRegistry;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public final class ConfigGuiNeoForgeClient {
	private static final Logger LOGGER = LogManager.getLogger();

	private ConfigGuiNeoForgeClient() {

	}

	public static void register(IEventBus modEventBus) {
		ConfigGuiOptions.register();
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ScreenEvent.Render.Pre event) -> {
			if (event.getScreen() instanceof MezzConfigScreen screen) {
				screen.clearTooltipForNextRenderPass();
			}
		});
		// JEI renders its overlay in Render.Post, before this final tooltip pass.
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ScreenEvent.Render.Post event) -> {
			if (event.getScreen() instanceof MezzConfigScreen screen) {
				screen.renderDeferredTooltip(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
			}
		});
		RemoteConfigNetworking.setClientSender(payload -> {
			ClientPacketListener connection = Minecraft.getInstance().getConnection();
			if (connection == null || !connection.hasChannel(payload.type())) {
				return false;
			}
			connection.send(payload);
			return true;
		});
		NeoForge.EVENT_BUS.addListener(
			(ClientTickEvent.Post event) -> RemoteConfigEditor.onClientTick(isChannelAvailable())
		);
		NeoForge.EVENT_BUS.addListener(
			(ClientPlayerNetworkEvent.LoggingIn event) -> RemoteConfigEditor.onClientConnected(isChannelAvailable())
		);
		NeoForge.EVENT_BUS.addListener(
			(ClientPlayerNetworkEvent.LoggingOut event) -> RemoteConfigEditor.onClientDisconnect()
		);
		modEventBus.addListener(ConfigGuiNeoForgeClient::onClientSetup);
		modEventBus.addListener(ConfigGuiNeoForgeClient::onRegisterClientReloadListeners);
	}

	private static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(ConfigGuiNeoForgeClient::registerConfigScreens);
	}

	private static void registerConfigScreens() {
		Collection<? extends ConfigScreenConfig> configScreens = List.of();
		if (ConfigGuiOptions.enableNativeConfigDiscovery()) {
			configScreens = NeoForgeConfigScreenConfigs.getConfigScreens();
		}
		ConfigScreenFactoryRegistry registry = ConfigGui.createScreenFactoryRegistryFromInternalConfigs(configScreens, ConfigGuiNeoForgePluginFinder.getPlugins());
		ConfigGui.createScreenListFactory(registry, NeoForgeConfigScreenOwnerMetadata::get);
		registry.getFactories().forEach(ConfigGuiNeoForgeClient::registerConfigScreen);
	}

	private static void registerConfigScreen(String modId, IConfigScreenFactory configScreenFactory) {
		Supplier<net.neoforged.neoforge.client.gui.IConfigScreenFactory> factorySupplier = () -> (container, parent) -> configScreenFactory.create(parent);
		ModList.get()
			.getModContainerById(modId)
			.ifPresentOrElse(
				modContainer -> {
					if (modContainer.getCustomExtension(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class).isPresent()) {
						LOGGER.debug("Config screen already registered for mod id: {}", modId);
						return;
					}
					modContainer.registerExtensionPoint(
						net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
						factorySupplier
					);
				},
				() -> LOGGER.error("No mod container found for config screen mod id: {}", modId)
			);
	}

	private static void onRegisterClientReloadListeners(AddClientReloadListenersEvent event) {
		event.addListener(Identifier.fromNamespaceAndPath("mezz_config_gui", "sprites"), ConfigTextures.get().getGuiSpriteManager());
		event.addListener(Identifier.fromNamespaceAndPath("mezz_config_gui", "colors"), ConfigGuiColors.createReloadListener());
	}

	private static boolean isChannelAvailable() {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		return connection != null && connection.hasChannel(RemoteConfigRequestChunkPayload.TYPE);
	}
}
