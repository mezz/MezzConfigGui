package net.mezzdev.config.gui.api;

/**
 * Provides display options for config list value editors.
 *
 * @since 0.1.0
 */
public interface IConfigListValueEditorOptions {
	/**
	 * Return true when entries may be removed from the displayed list.
	 *
	 * @since 0.1.0
	 */
	boolean allowsRemovingValues();
}
