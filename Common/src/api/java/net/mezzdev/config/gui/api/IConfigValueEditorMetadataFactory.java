package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import org.jetbrains.annotations.ApiStatus;

/**
 * Creates editor metadata for values with a text serializer.
 * Obtain this factory from {@link IConfigGuiRegistration#getEditorMetadataFactory()}.
 *
 * @since 0.6.0
 */
@ApiStatus.NonExtendable
public interface IConfigValueEditorMetadataFactory {
	/**
	 * Create metadata for a standard editor appropriate for the value and its storage constraints.
	 *
	 * @param serializer the value's serialization and validation rules
	 * @param value a representative value for choosing the editor
	 * @return editor metadata with storage validation, available choices, and standard localized text
	 *
	 * @since 0.6.0
	 */
	<T> IConfigValueEditorMetadata<T> create(IConfigValueSerializer<T> serializer, T value);

	/**
	 * Create metadata for a chosen editor with standard localized text.
	 *
	 * @param serializer the value's serialization and validation rules
	 * @param editorType the editor to use
	 * @throws IllegalArgumentException if a list editor has no element serializer, or a range editor is requested
	 *                                  without bounds; use {@link #createRange} for ranges
	 * @since 0.6.0
	 */
	<T> IConfigValueEditorMetadata<T> create(IConfigValueSerializer<T> serializer, ConfigValueEditorType<T> editorType);

	/**
	 * Create metadata for editing a numeric interval with two slider handles.
	 *
	 * @param serializer the interval's serialization and validation rules
	 * @param bounds the inclusive bounds for both endpoints
	 * @since 0.6.0
	 */
	<T> IConfigRangeValueEditorMetadata<T> createRange(IConfigValueSerializer<ConfigValueRange<T>> serializer, ConfigValueRange<T> bounds);
}
