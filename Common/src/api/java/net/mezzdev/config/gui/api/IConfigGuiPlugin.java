package net.mezzdev.config.gui.api;

/**
 * Plugin for registering or customizing a config screen.
 * <p>
 * Use {@link IConfigGuiRegistration#registerScreen} to provide a full screen from a schema. Use
 * {@link IConfigGuiRegistration#configureScreen} to customize a screen that the config GUI detects from a
 * platform-native config system. Custom value editors can also be registered here.
 * <p>
 * Forge and NeoForge plugins are discovered by scanning for the {@link ConfigGuiPlugin} annotation.
 * Annotate your plugin class and provide a constructor with no arguments.
 * <p>
 * Fabric plugins are discovered from {@code fabric.mod.json}. Add your plugin class to the
 * {@code mezz_config_gui_plugin} entrypoint.
 * <p>
 * A shared plugin class can support all loaders by using the annotation and registering the same class as the
 * Fabric entrypoint.
 *
 * @since 0.1.0
 */
public interface IConfigGuiPlugin {
	/**
	 * The mod id that owns these config GUI registrations.
	 *
	 * @since 0.1.0
	 */
	String getModId();

	/**
	 * Register config GUI customizations for this mod.
	 *
	 * @param registration the config GUI registration
	 *
	 * @since 0.1.0
	 */
	void register(IConfigGuiRegistration registration);
}
