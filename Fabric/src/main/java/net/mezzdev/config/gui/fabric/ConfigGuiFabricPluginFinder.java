package net.mezzdev.config.gui.fabric;

import net.fabricmc.loader.api.EntrypointException;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public final class ConfigGuiFabricPluginFinder {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final String ENTRYPOINT_KEY = "mezz_config_gui_plugin";

	private ConfigGuiFabricPluginFinder() {

	}

	public static List<IConfigGuiPlugin> getPlugins() {
		return getInstances(ENTRYPOINT_KEY, IConfigGuiPlugin.class, "config GUI plugin");
	}

	static <T> List<T> getInstances(String entrypointContainerKey, Class<T> instanceClass, String pluginName) {
		FabricLoader fabricLoader = FabricLoader.getInstance();
		List<EntrypointContainer<T>> pluginContainers = fabricLoader.getEntrypointContainers(entrypointContainerKey, instanceClass);
		return pluginContainers.stream()
			.<T>mapMulti((entrypointContainer, consumer) -> {
				try {
					T entrypoint = entrypointContainer.getEntrypoint();
					consumer.accept(entrypoint);
				} catch (EntrypointException e) {
					String modName = getModName(entrypointContainer);
					LOGGER.error("{} specified an invalid entrypoint for its {}", modName, pluginName, e);
				} catch (RuntimeException | LinkageError e) {
					String modName = getModName(entrypointContainer);
					LOGGER.error("{} specified a broken entrypoint for its {}", modName, pluginName, e);
				}
			})
			.collect(Collectors.toList());
	}

	private static String getModName(EntrypointContainer<?> entrypointContainer) {
		try {
			ModContainer provider = entrypointContainer.getProvider();
			ModMetadata metadata = provider.getMetadata();
			return metadata.getName();
		} catch (RuntimeException | LinkageError ignored) {
			return "unknown";
		}
	}
}
