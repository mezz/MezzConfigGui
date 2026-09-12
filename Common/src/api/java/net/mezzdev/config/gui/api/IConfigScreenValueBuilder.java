package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.IConfigValue;
import org.jetbrains.annotations.ApiStatus;

/**
 * Customizes one value in one config screen category.
 *
 * @since 0.1.0
 */
@ApiStatus.NonExtendable
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
	 * Set the restart or reload required after this value is saved.
	 *
	 * @param restartRequirement the required restart or reload
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenValueBuilder setRestartRequirement(ConfigValueRestartRequirement restartRequirement);

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
