package net.mezzdev.config.gui.input;

public enum InputType {
	/** Called on mouse-down or to see if a click would be handled. */
	SIMULATE,
	/** Called on mouse-up after a successful {@link InputType#SIMULATE}. */
	EXECUTE,
	/** Called on key-down, or mouse-down to execute a click from a vanilla GUI without waiting for mouse-up. */
	IMMEDIATE,
}
