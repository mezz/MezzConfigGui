package net.mezzdev.config.gui.api.keybindings;

/**
 * Standard modifier requirements for configured bindings, including portable aliases.
 *
 * @since 0.6.0
 */
public enum KeyMappingModifier {
	/**
	 * Either Shift key.
	 *
	 * @since 0.6.0
	 */
	SHIFT,

	/**
	 * Either physical Control key, including on macOS.
	 *
	 * @since 0.6.0
	 */
	CONTROL,

	/**
	 * Either Alt key.
	 *
	 * @since 0.6.0
	 */
	ALT,

	/**
	 * Either Command/Super key.
	 *
	 * @since 0.6.0
	 */
	COMMAND,

	/**
	 * A portable alias for Command on macOS and Control elsewhere.
	 *
	 * @since 0.6.0
	 */
	CONTROL_OR_COMMAND
}
