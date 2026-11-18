package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;

/**
 * Serialization, validation, and editor metadata for config values that need a specific config screen editor.
 *
 * @deprecated Use {@link IConfigValueEditorMetadata} to describe the editor.
 * @since 0.1.0
 */
@Deprecated(since = "0.6.0")
public interface IConfigValueEditorSerializer<T> extends IConfigValueSerializer<T>, IConfigValueLocalizationProvider<T> {
	/**
	 * Get the kind of editor that config screens should use for values serialized by this helper.
	 *
	 * @since 0.1.0
	 */
	ConfigValueEditorType<T> getEditorType();
}
