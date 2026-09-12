package net.mezzdev.config.gui.api;

import net.minecraft.network.chat.Component;

/**
 * Optional GUI display metadata for config values that cannot be represented by a localization key alone.
 *
 * @since 0.1.0
 */
public interface IConfigLocalizedValue {
	/**
	 * The localized config value name.
	 *
	 * @since 0.1.0
	 */
	Component getLocalizedName();

	/**
	 * The localized config value description.
	 *
	 * @since 0.1.0
	 */
	Component getLocalizedDescription();
}
