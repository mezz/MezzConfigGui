package net.mezzdev.config.gui.api;

import java.util.List;

/**
 * Presentation and editing options for a list and its elements.
 *
 * @param <T> the list element type
 *
 * @since 0.6.0
 */
public interface IConfigListValueEditorMetadata<T> extends IConfigValueEditorMetadata<List<T>> {
	/**
	 * Get the editor metadata for a list element.
	 *
	 * @param value the element to display or edit
	 * @since 0.6.0
	 */
	IConfigValueEditorMetadata<T> getElementMetadata(T value);

	/**
	 * Return true when entries may be removed from the displayed list.
	 *
	 * @since 0.6.0
	 */
	boolean allowsRemovingValues();
}
