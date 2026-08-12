package net.mezzdev.config.gui.forge;

import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.mezzdev.config.gui.ConfigGui;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.screenlist.ConfigScreenFactoryRegistry;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigGuiForgeClient {
	private static final Logger LOGGER = LogManager.getLogger();

	private ConfigGuiForgeClient() {

	}

	public static void register(IEventBus modEventBus) {
		registerConfigScreens(ConfigGui.createScreenFactoryRegistry(ConfigGuiForgePluginFinder.getPlugins()));
		modEventBus.addListener(ConfigGuiForgeClient::onRegisterClientReloadListeners);
	}

	private static void registerConfigScreens(ConfigScreenFactoryRegistry registry) {
		Map<String, IConfigScreenFactory> factories = new LinkedHashMap<>(registry.getFactories());
		factories.put(
			ConfigGuiOptions.MOD_ID,
			ConfigGui.createScreenListFactory(registry, ForgeConfigScreenOwnerMetadata::get)
		);
		factories.forEach(ConfigGuiForgeClient::registerConfigScreen);
	}

	private static void registerConfigScreen(String modId, IConfigScreenFactory configScreenFactory) {
		ModList.get()
			.getModContainerById(modId)
			.ifPresentOrElse(
				modContainer -> modContainer.registerExtensionPoint(
					ConfigScreenHandler.ConfigScreenFactory.class,
					() -> new ConfigScreenHandler.ConfigScreenFactory(configScreenFactory::create)
				),
				() -> LOGGER.error("No mod container found for config screen mod id: {}", modId)
			);
	}

	private static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener(ConfigTextures.get().getGuiSpriteManager());
		event.registerReloadListener(ConfigGuiColors.createReloadListener());
	}
}
