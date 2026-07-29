package net.mezzdev.config.gui;

import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.mezzdev.config.gui.api.IConfigScreenConfig;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Runtime setup for the config GUI implementation.
 */
public final class ConfigGui {
	private ConfigGui() {

	}

	/**
	 * Create config screen factories from discovered config GUI plugins.
	 */
	public static Map<String, IConfigScreenFactory> createScreenFactories(List<? extends IConfigGuiPlugin> plugins) {
		return createScreenFactories(List.of(), plugins);
	}

	/**
	 * Create config screen factories from config screen metadata and discovered config GUI plugins.
	 */
	public static Map<String, IConfigScreenFactory> createScreenFactories(
		Collection<? extends IConfigScreenConfig> configScreens,
		List<? extends IConfigGuiPlugin> plugins
	) {
		return ConfigGuiPluginLoader.createScreenFactories(configScreens, plugins);
	}
}
