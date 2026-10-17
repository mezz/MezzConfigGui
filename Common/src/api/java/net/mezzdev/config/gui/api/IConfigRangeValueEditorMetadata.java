package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.serializer.ConfigValueRange;

/**
 * Editor metadata for an inclusive numeric interval with two slider handles.
 * Integer, Long, and Double endpoints support sliders; unsupported bounds use text editing.
 *
 * @param <T> the numeric endpoint type
 *
 * @since 0.6.0
 */
public interface IConfigRangeValueEditorMetadata<T> extends IConfigValueEditorMetadata<ConfigValueRange<T>> {
	/**
	 * Get the inclusive bounds within which both handles can move.
	 *
	 * @since 0.6.0
	 */
	ConfigValueRange<T> getBounds();
}
