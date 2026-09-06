package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.serializer.IConfigListValueSerializer;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;

import java.util.List;

/**
 * Serializer for config list values that exposes the list element serializer to the config GUI.
 *
 * @param <T> the list element type
 *
 * @since 0.1.0
 */
public interface IConfigListValueEditorSerializer<T> extends IConfigValueEditorSerializer<List<T>>, IConfigListValueSerializer<T> {
	/**
	 * Get the serializer for each list element.
	 *
	 * @since 0.1.0
	 */
	IConfigValueSerializer<T> getElementSerializer();

	@Override
	default ConfigValueEditorType<List<T>> getEditorType() {
		return ConfigValueEditorTypes.getList();
	}
}
