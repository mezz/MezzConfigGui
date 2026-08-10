package net.mezzdev.config.gui;

import net.mezzdev.config.api.files.IConfigManager;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.mezzdev.config.gui.screenlist.ConfigScreenFactoryRegistry;
import net.mezzdev.config.gui.screenlist.ConfigScreenListScreen;
import net.mezzdev.config.gui.screenlist.ConfigScreenOwnerMetadataProvider;

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
		return createScreenFactoriesFromInternalConfigs(List.of(), plugins);
	}

	/**
	 * Create a config screen factory registry from discovered config GUI plugins.
	 */
	public static ConfigScreenFactoryRegistry createScreenFactoryRegistry(List<? extends IConfigGuiPlugin> plugins) {
		return createScreenFactoryRegistryFromInternalConfigs(List.of(), plugins);
	}

	/**
	 * Create config screen factories from all schemas registered with a MezzConfig config manager.
	 */
	public static Map<String, IConfigScreenFactory> createScreenFactories(
		IConfigManager configManager,
		List<? extends IConfigGuiPlugin> plugins
	) {
		return createScreenFactoryRegistry(configManager, plugins).getFactories();
	}

	/**
	 * Create a config screen factory registry from all schemas registered with a MezzConfig config manager.
	 */
	public static ConfigScreenFactoryRegistry createScreenFactoryRegistry(
		IConfigManager configManager,
		List<? extends IConfigGuiPlugin> plugins
	) {
		return ConfigGuiPluginLoader.createScreenFactoryRegistryFromInternalConfigs(
			MezzConfigScreenConfigs.getConfigScreens(configManager),
			plugins,
			false
		);
	}

	/**
	 * Create config screen factories from internal config screen metadata and discovered config GUI plugins.
	 */
	public static Map<String, IConfigScreenFactory> createScreenFactoriesFromInternalConfigs(
		Collection<? extends ConfigScreenConfig> configScreens,
		List<? extends IConfigGuiPlugin> plugins
	) {
		return createScreenFactoryRegistryFromInternalConfigs(configScreens, plugins).getFactories();
	}

	/**
	 * Create config screen factories from internal config screen metadata and discovered config GUI plugins.
	 */
	public static ConfigScreenFactoryRegistry createScreenFactoryRegistryFromInternalConfigs(
		Collection<? extends ConfigScreenConfig> configScreens,
		List<? extends IConfigGuiPlugin> plugins
	) {
		return ConfigGuiPluginLoader.createScreenFactoryRegistryFromInternalConfigs(configScreens, plugins);
	}

	/**
	 * Create a screen that lists all discovered config screens.
	 */
	public static IConfigScreenFactory createScreenListFactory(
		ConfigScreenFactoryRegistry registry,
		ConfigScreenOwnerMetadataProvider metadataProvider
	) {
		IConfigScreenFactory screenListFactory = parent -> ConfigScreenListScreen.create(parent, registry.getEntries(), metadataProvider);
		registry.setScreenListFactory(screenListFactory);
		return screenListFactory;
	}
}
