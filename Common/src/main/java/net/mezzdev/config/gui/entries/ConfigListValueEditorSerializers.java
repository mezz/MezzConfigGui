package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.IConfigListValueSerializer;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;

import java.util.List;
import java.util.Optional;

final class ConfigListValueEditorSerializers {
	private ConfigListValueEditorSerializers() {

	}

	public static boolean canAdapt(IConfigValueSerializer<?> serializer) {
		return serializer instanceof IConfigListValueEditorSerializer<?> ||
			getElementSerializer(serializer).isPresent();
	}

	@SuppressWarnings("unchecked")
	public static <T> IConfigListValueEditorSerializer<T> adapt(IConfigValueSerializer<List<T>> serializer) {
		if (serializer instanceof IConfigListValueEditorSerializer<?> listSerializer) {
			return (IConfigListValueEditorSerializer<T>) listSerializer;
		}
		IConfigValueSerializer<T> elementSerializer = (IConfigValueSerializer<T>) getElementSerializer(serializer)
			.orElseThrow(() -> new UnsupportedOperationException("List editor requires an element serializer."));
		return new ListValueEditorSerializerAdapter<>(serializer, elementSerializer);
	}

	private static Optional<IConfigValueSerializer<?>> getElementSerializer(IConfigValueSerializer<?> serializer) {
		if (serializer instanceof IConfigListValueSerializer<?> listSerializer) {
			return Optional.of(listSerializer.getElementSerializer());
		}
		return Optional.empty();
	}
}
