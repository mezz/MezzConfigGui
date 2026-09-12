package net.mezzdev.config.gui.api;

import java.util.Optional;

/**
 * Provides visual icons for config value options.
 *
 * @param <T> the config value option type
 *
 * @since 0.1.0
 */
public interface IConfigValueIconProvider<T> {
	/**
	 * Get the icon to display for one value option.
	 *
	 * @param value the displayed value option
	 * @return the icon to draw, or empty when this value option has no icon
	 *
	 * @since 0.1.0
	 */
	Optional<IConfigValueIcon> getIcon(T value);
}
