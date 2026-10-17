package net.mezzdev.config.gui.api;

import java.util.List;
import java.util.Optional;

/**
 * Editor selection, validation, and localized text for values edited by a config screen.
 *
 * @param <T> the value type
 *
 * @since 0.6.0
 */
public interface IConfigValueEditorMetadata<T> extends IConfigValueLocalizationProvider<T> {
	/**
	 * Get the kind of editor used for this value.
	 *
	 * @since 0.6.0
	 */
	ConfigValueEditorType<T> getEditorType();

	/**
	 * Check whether a proposed value can be applied.
	 *
	 * @since 0.6.0
	 */
	boolean isValid(T value);

	/**
	 * Get the available choices when the value has a finite set of valid options.
	 *
	 * @return the valid options, or empty when they cannot be enumerated
	 *
	 * @since 0.6.0
	 */
	Optional<List<T>> getAllValidValues();
}
