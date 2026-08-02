package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

final class ConfigListValueEditorSerializers {
	private static final String CORE_LIST_SERIALIZER_CLASS_NAME = "net.mezzdev.config.serializers.ListSerializer";
	private static final String CORE_LIST_VALUE_SERIALIZER_FIELD_NAME = "valueSerializer";
	private static final String ELEMENT_SERIALIZER_METHOD_NAME = "getElementSerializer";

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
		Optional<IConfigValueSerializer<?>> serializerFromMethod = getElementSerializerFromMethod(serializer);
		if (serializerFromMethod.isPresent()) {
			return serializerFromMethod;
		}
		return getElementSerializerFromCoreListSerializerField(serializer);
	}

	private static Optional<IConfigValueSerializer<?>> getElementSerializerFromMethod(IConfigValueSerializer<?> serializer) {
		try {
			Method method = serializer.getClass().getMethod(ELEMENT_SERIALIZER_METHOD_NAME);
			if (!IConfigValueSerializer.class.isAssignableFrom(method.getReturnType())) {
				return Optional.empty();
			}
			method.setAccessible(true);
			Object elementSerializer = method.invoke(serializer);
			if (elementSerializer instanceof IConfigValueSerializer<?> typedElementSerializer) {
				return Optional.of(typedElementSerializer);
			}
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			return Optional.empty();
		}
		return Optional.empty();
	}

	private static Optional<IConfigValueSerializer<?>> getElementSerializerFromCoreListSerializerField(IConfigValueSerializer<?> serializer) {
		Class<?> serializerClass = serializer.getClass();
		if (!serializerClass.getName().equals(CORE_LIST_SERIALIZER_CLASS_NAME)) {
			return Optional.empty();
		}
		try {
			Field field = serializerClass.getDeclaredField(CORE_LIST_VALUE_SERIALIZER_FIELD_NAME);
			field.setAccessible(true);
			Object elementSerializer = field.get(serializer);
			if (elementSerializer instanceof IConfigValueSerializer<?> typedElementSerializer) {
				return Optional.of(typedElementSerializer);
			}
		} catch (IllegalAccessException | NoSuchFieldException e) {
			return Optional.empty();
		}
		return Optional.empty();
	}
}
