package net.mezzdev.config.gui.forge;

import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.mezzdev.config.gui.ConfigGui;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.ConfigScreenConfig;
import net.mezzdev.config.gui.MezzConfigScreen;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.forge.config.ForgeConfigScreenConfigs;
import net.mezzdev.config.gui.remote.RemoteConfigEditor;
import net.mezzdev.config.gui.remote.RemoteConfigNetworking;
import net.mezzdev.config.gui.screenlist.ConfigScreenFactoryRegistry;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;

public final class ConfigGuiForgeClient {
	private static final Logger LOGGER = LogManager.getLogger();

	private ConfigGuiForgeClient() {

	}

	public static void register(IEventBus modEventBus, ConfigGuiForgeNetwork network) {
		ConfigGuiOptions.register();
		MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ScreenEvent.Render.Pre event) -> {
			if (event.getScreen() instanceof MezzConfigScreen screen) {
				screen.clearTooltipForNextRenderPass();
			}
		});
		// JEI renders its overlay in Render.Post, before this final tooltip pass.
		MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ScreenEvent.Render.Post event) -> {
			if (event.getScreen() instanceof MezzConfigScreen screen) {
				screen.renderDeferredTooltip(new net.mezzdev.config.gui.api.LegacyGuiGraphics(event.getPoseStack()), event.getMouseX(), event.getMouseY());
			}
		});
		RemoteConfigNetworking.setClientSender(payload -> {
			ClientPacketListener listener = Minecraft.getInstance().getConnection();
			if (listener == null) {
				return false;
			}
			Connection connection = listener.getConnection();
			SimpleChannel channel = network.getChannel();
			if (!connection.isConnected() || !channel.isRemotePresent(connection)) {
				return false;
			}
			channel.sendToServer(payload);
			return true;
		});
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				RemoteConfigEditor.onClientTick(isChannelAvailable(network));
			}
		});
		MinecraftForge.EVENT_BUS.addListener(
			(ClientPlayerNetworkEvent.LoggingIn event) -> RemoteConfigEditor.onClientConnected(isChannelAvailable(network))
		);
		MinecraftForge.EVENT_BUS.addListener(
			(ClientPlayerNetworkEvent.LoggingOut event) -> RemoteConfigEditor.onClientDisconnect()
		);
		modEventBus.addListener(ConfigGuiForgeClient::onClientSetup);
		modEventBus.addListener(ConfigGuiForgeClient::onRegisterClientReloadListeners);
	}

	private static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(ConfigGuiForgeClient::registerConfigScreens);
	}

	private static void registerConfigScreens() {
		Collection<? extends ConfigScreenConfig> configScreens = List.of();
		if (ConfigGuiOptions.enableNativeConfigDiscovery()) {
			configScreens = ForgeConfigScreenConfigs.getConfigScreens();
		}
		ConfigScreenFactoryRegistry registry = ConfigGui.createScreenFactoryRegistryFromInternalConfigs(configScreens, ConfigGuiForgePluginFinder.getPlugins());
		ConfigGui.createScreenListFactory(registry, ForgeConfigScreenOwnerMetadata::get);
		registry.getFactories().forEach(ConfigGuiForgeClient::registerConfigScreen);
	}

	private static void registerConfigScreen(String modId, IConfigScreenFactory configScreenFactory) {
		ModList.get()
			.getModContainerById(modId)
			.ifPresentOrElse(
				modContainer -> {
					if (modContainer.getCustomExtension(ConfigScreenHandler.ConfigScreenFactory.class).isPresent()) {
						LOGGER.debug("Config screen already registered for mod id: {}", modId);
						return;
					}
					modContainer.registerExtensionPoint(
						ConfigScreenHandler.ConfigScreenFactory.class,
						() -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> configScreenFactory.create(parent))
					);
				},
				() -> LOGGER.error("No mod container found for config screen mod id: {}", modId)
			);
	}

	private static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener(ConfigTextures.get().getGuiSpriteManager());
		event.registerReloadListener(ConfigGuiColors.createReloadListener());
	}

	private static boolean isChannelAvailable(ConfigGuiForgeNetwork network) {
		ClientPacketListener listener = Minecraft.getInstance().getConnection();
		if (listener == null) {
			return false;
		}
		Connection connection = listener.getConnection();
		return connection.isConnected() && network.getChannel().isRemotePresent(connection);
	}
}
