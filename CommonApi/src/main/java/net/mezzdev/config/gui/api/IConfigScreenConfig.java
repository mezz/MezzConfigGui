package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.schema.IConfigSchema;
import net.minecraft.network.chat.Component;

/**
 * Config screen metadata registered for a config GUI.
 *
 * @since 0.1.0
 */
public interface IConfigScreenConfig {
	/**
	 * The mod id that owns this config screen.
	 *
	 * @since 0.1.0
	 */
	String getModId();

	/**
	 * The title shown at the top of the config screen.
	 *
	 * @since 0.1.0
	 */
	Component getTitle();

	/**
	 * The editable schema shown by this config screen.
	 *
	 * @since 0.1.0
	 */
	IConfigSchema getSchema();

	/**
	 * Called when applying saved changes requires the owner mod to restart or reload.
	 *
	 * @return the result of trying to restart or reload the owner mod
	 *
	 * @since 0.1.0
	 */
	ConfigRestartResult onRestartRequired();
}
