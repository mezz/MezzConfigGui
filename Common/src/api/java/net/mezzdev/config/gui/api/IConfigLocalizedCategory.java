package net.mezzdev.config.gui.api;

import net.minecraft.network.chat.Component;

/**
 * Optional GUI display metadata for config categories that cannot be represented by a localization key alone.
 *
 * @since 0.1.0
 */
public interface IConfigLocalizedCategory {
	/**
	 * The localized category name.
	 *
	 * @since 0.1.0
	 */
	Component getLocalizedName();

	/**
	 * The localized category description.
	 *
	 * @since 0.1.0
	 */
	Component getLocalizedDescription();
}
