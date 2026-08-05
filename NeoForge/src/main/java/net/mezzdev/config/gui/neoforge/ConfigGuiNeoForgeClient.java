package net.mezzdev.config.gui.neoforge;

import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.mezzdev.config.gui.ConfigGui;
import net.mezzdev.config.gui.ConfigScreenConfig;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.neoforge.config.NeoForgeConfigScreenConfigs;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ConfigGuiNeoForgeClient {
	private static final Logger LOGGER = LogManager.getLogger();

	private ConfigGuiNeoForgeClient() {

	}

	public static void register(IEventBus modEventBus) {
		modEventBus.addListener(ConfigGuiNeoForgeClient::onClientSetup);
		modEventBus.addListener(ConfigGuiNeoForgeClient::onRegisterClientReloadListeners);
	}

	private static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> registerConfigScreens(createScreenFactories()));
	}

	private static Map<String, IConfigScreenFactory> createScreenFactories() {
		Collection<? extends ConfigScreenConfig> configScreens = List.of();
		if (ConfigGuiOptions.enableNativeConfigDiscovery()) {
			configScreens = NeoForgeConfigScreenConfigs.getConfigScreens(List.of());
		}
		return ConfigGui.createScreenFactoriesFromInternalConfigs(configScreens, ConfigGuiNeoForgePluginFinder.getPlugins());
	}

	private static void registerConfigScreens(Map<String, IConfigScreenFactory> factories) {
		factories.forEach(ConfigGuiNeoForgeClient::registerConfigScreen);
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

	private static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener(ConfigTextures.get().getGuiSpriteManager());
	}
}
