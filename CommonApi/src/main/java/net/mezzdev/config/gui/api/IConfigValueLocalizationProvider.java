package net.mezzdev.config.gui.api;

import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Provides localized display text for serialized config value options.
 *
 * @param <T> the config value option type
 *
 * @since 0.1.0
 */
public interface IConfigValueLocalizationProvider<T> {
	/**
	 * Get the translated name component for a value option.
	 *
	 * @param configValueLocalizationKey the translation key for the owning config value's name
	 * @param value                      the value option
	 *
	 * @since 0.1.0
	 */
	Component getLocalizedValueName(String configValueLocalizationKey, T value);

	/**
	 * Get the translated description component for a value option.
	 *
	 * @param configValueLocalizationKey the translation key for the owning config value's name
	 * @param value                      the value option
	 *
	 * @since 0.1.0
	 */
	default Optional<Component> getLocalizedValueDescription(String configValueLocalizationKey, T value) {
		return Optional.empty();
	}
}
