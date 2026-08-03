package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.IConfigValue;

/**
 * Customizes one value in one config screen category.
 *
 * @since 0.1.0
 */
public interface IConfigScreenValueBuilder {
	/**
	 * Set when this value's edits are saved.
	 *
	 * @param applyMode when edits for this value are saved
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenValueBuilder setApplyMode(ConfigValueApplyMode applyMode);

	/**
	 * Mark this value as requiring a restart or larger reload after it is saved.
	 *
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenValueBuilder setRequiresRestart();

	/**
	 * Set whether this value requires a restart or larger reload after it is saved.
	 *
	 * @param requiresRestart true if saving this value requires a restart or larger reload
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenValueBuilder setRequiresRestart(boolean requiresRestart);

	/**
	 * Hide this value from this category.
	 *
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenValueBuilder hide();

	/**
	 * Insert a config value before this value in this category.
	 *
	 * @param value config value to insert
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenValueBuilder insertBefore(IConfigValue<?> value);

	/**
	 * Insert a config screen value before this value in this category.
	 *
	 * @param value config screen value to insert
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenValueBuilder insertBefore(IConfigScreenValue<?> value);

	/**
	 * Insert a config value after this value in this category.
	 *
	 * @param value config value to insert
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenValueBuilder insertAfter(IConfigValue<?> value);

	/**
	 * Insert a config screen value after this value in this category.
	 *
	 * @param value config screen value to insert
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenValueBuilder insertAfter(IConfigScreenValue<?> value);
}
