package net.mezzdev.config.gui.api;

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
	default IConfigScreenValueBuilder setRequiresRestart() {
		return setRequiresRestart(true);
	}

	/**
	 * Set whether this value requires a restart or larger reload after it is saved.
	 *
	 * @param requiresRestart true if saving this value requires a restart or larger reload
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenValueBuilder setRequiresRestart(boolean requiresRestart);
}
