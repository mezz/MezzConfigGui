package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.api.value.IConfigKeyValueSerializer;
import net.mezzdev.config.api.value.IConfigValueSerializer;

import java.util.Optional;

/**
 * Type-safe internal adapter for editing the two components exposed by a key-value element serializer.
 */
final class KeyValueElementSerializerAdapter<T> {
	private final IConfigKeyValueSerializer<T, Object, Object> serializer;

	private KeyValueElementSerializerAdapter(IConfigKeyValueSerializer<T, Object, Object> serializer) {
		this.serializer = serializer;
	}

	@SuppressWarnings("unchecked")
	public static <T> Optional<KeyValueElementSerializerAdapter<T>> create(IConfigValueSerializer<T> serializer) {
		if (serializer instanceof IConfigKeyValueSerializer<?, ?, ?> keyValueSerializer) {
			return Optional.of(new KeyValueElementSerializerAdapter<>((IConfigKeyValueSerializer<T, Object, Object>) keyValueSerializer));
		}
		return Optional.empty();
	}

	public Object getKey(T entry) {
		return serializer.getKey(entry);
	}

	public Object getValue(T entry) {
		return serializer.getValue(entry);
	}

	public IConfigValueSerializer<Object> getKeySerializer() {
		return serializer.getKeySerializer();
	}

	public IConfigValueSerializer<Object> getValueSerializer() {
		return serializer.getValueSerializer();
	}

	public Optional<T> withSerializedKey(T entry, String serializedKey) {
		return deserialize(serializer.getKeySerializer(), serializedKey)
			.flatMap(key -> Optional.ofNullable(serializer.createEntry(key, serializer.getValue(entry))))
			.filter(serializer::isValid);
	}

	public Optional<T> withSerializedValue(T entry, String serializedValue) {
		return deserialize(serializer.getValueSerializer(), serializedValue)
			.flatMap(value -> Optional.ofNullable(serializer.createEntry(serializer.getKey(entry), value)))
			.filter(serializer::isValid);
	}

	public Optional<T> withKey(T entry, Object key) {
		if (!serializer.getKeySerializer().isValid(key)) {
			return Optional.empty();
		}
		return Optional.ofNullable(serializer.createEntry(key, serializer.getValue(entry)))
			.filter(serializer::isValid);
	}

	public Optional<T> withValue(T entry, Object value) {
		if (!serializer.getValueSerializer().isValid(value)) {
			return Optional.empty();
		}
		return Optional.ofNullable(serializer.createEntry(serializer.getKey(entry), value))
			.filter(serializer::isValid);
	}

	private static Optional<Object> deserialize(IConfigValueSerializer<Object> serializer, String serializedValue) {
		IDeserializeResult<Object> result = serializer.deserialize(serializedValue);
		if (!result.getErrors().isEmpty()) {
			return Optional.empty();
		}
		return result.getResult()
			.filter(serializer::isValid);
	}

}
