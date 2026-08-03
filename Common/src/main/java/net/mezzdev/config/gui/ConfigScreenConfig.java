package net.mezzdev.config.gui;

import net.minecraft.network.chat.Component;

/**
 * Internal config screen metadata.
 */
public interface ConfigScreenConfig {
	/**
	 * The mod id that owns this config screen.
	 */
	String getModId();

	/**
	 * The title shown at the top of the config screen.
	 */
	Component getTitle();

	/**
	 * The schema shown by this config screen.
	 */
	ConfigScreenSchema getSchema();
}
