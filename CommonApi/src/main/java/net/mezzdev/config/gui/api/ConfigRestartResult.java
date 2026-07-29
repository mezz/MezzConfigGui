package net.mezzdev.config.gui.api;

/**
 * Result from handling config changes that require a mod restart or reload.
 *
 * @since 0.1.0
 */
public enum ConfigRestartResult {
	/**
	 * The owner mod handled the restart or reload immediately.
	 *
	 * @since 0.1.0
	 */
	HANDLED,

	/**
	 * The owner mod cannot restart or reload now, so changes will be applied on the next game start.
	 *
	 * @since 0.1.0
	 */
	NEXT_GAME_START
}
