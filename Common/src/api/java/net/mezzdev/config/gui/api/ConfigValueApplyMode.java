package net.mezzdev.config.gui.api;

/**
 * Controls when the config GUI saves edits made to a config screen value.
 *
 * @since 0.1.0
 */
public enum ConfigValueApplyMode {
	/**
	 * Save edits as soon as the user makes them.
	 *
	 * @since 0.1.0
	 */
	IMMEDIATE,

	/**
	 * Stage edits until the user applies all pending config screen changes.
	 *
	 * @since 0.1.0
	 */
	ON_APPLY
}
