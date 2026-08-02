package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

final class NeoForgeListElementSerializers {
	private NeoForgeListElementSerializers() {

	}

	public static Optional<IConfigValueSerializer<?>> create(List<?> defaultValues, ModConfigSpec.ValueSpec valueSpec) {
		return getElementClass(defaultValues, valueSpec)
			.flatMap(elementClass -> create(elementClass, valueSpec));
	}

	private static Optional<Class<?>> getElementClass(List<?> defaultValues, ModConfigSpec.ValueSpec valueSpec) {
		if (defaultValues.isEmpty()) {
			return getNewElement(valueSpec)
				.map(NeoForgeListElementSerializers::getElementClass);
		}
		Class<?> elementClass = null;
		for (Object defaultValue : defaultValues) {
			if (defaultValue == null) {
				return Optional.empty();
			}
			Class<?> currentElementClass = getElementClass(defaultValue);
			if (elementClass == null) {
				elementClass = currentElementClass;
			} else if (elementClass != currentElementClass) {
				return Optional.empty();
			}
		}
		return Optional.of(elementClass);
	}

	private static Optional<Object> getNewElement(ModConfigSpec.ValueSpec valueSpec) {
		if (valueSpec instanceof ModConfigSpec.ListValueSpec listValueSpec &&
			listValueSpec.getNewElementSupplier() != null
		) {
			return Optional.ofNullable(listValueSpec.getNewElementSupplier().get());
		}
		return Optional.empty();
	}

	private static Class<?> getElementClass(Object value) {
		if (value instanceof Enum<?> enumValue) {
			return enumValue.getDeclaringClass();
		}
		return value.getClass();
	}

	private static Optional<IConfigValueSerializer<?>> create(Class<?> elementClass, ModConfigSpec.ValueSpec valueSpec) {
		if (elementClass == Boolean.class) {
			return Optional.of(NeoForgeBooleanSerializer.INSTANCE);
		}
		if (elementClass == Integer.class) {
			return Optional.of(new NeoForgeIntegerSerializer(Integer.MIN_VALUE, Integer.MAX_VALUE));
		}
		if (elementClass == String.class) {
			return Optional.of(StringListElementSerializer.INSTANCE);
		}
		if (elementClass == Long.class) {
			return Optional.of(LongListElementSerializer.INSTANCE);
		}
		if (elementClass == Double.class) {
			return Optional.of(DoubleListElementSerializer.INSTANCE);
		}
		if (Enum.class.isAssignableFrom(elementClass)) {
			return createEnum(elementClass, valueSpec);
		}
		return Optional.empty();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Optional<IConfigValueSerializer<?>> createEnum(Class<?> elementClass, ModConfigSpec.ValueSpec valueSpec) {
		List validValues = Arrays.stream(elementClass.getEnumConstants())
			.filter(value -> isValidListElement(valueSpec, value))
			.toList();
		if (validValues.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new NeoForgeEnumSerializer(elementClass, validValues));
	}

	private static boolean isValidListElement(ModConfigSpec.ValueSpec valueSpec, Object value) {
		if (valueSpec instanceof ModConfigSpec.ListValueSpec listValueSpec) {
			try {
				return listValueSpec.testElement(value);
			} catch (RuntimeException ignored) {
				return false;
			}
		}
		return true;
	}

	private enum StringListElementSerializer implements IConfigValueSerializer<String> {
		INSTANCE;

		@Override
		public String serialize(String value) {
			return value;
		}

		@Override
		public IDeserializeResult<String> deserialize(String string) {
			if (!isValid(string)) {
				return IDeserializeResult.failure("Invalid text. Must be: " + getValidValuesDescription());
			}
			return IDeserializeResult.success(string);
		}

		@Override
		public boolean isValid(String value) {
			return value != null;
		}

		@Override
		public Optional<Collection<String>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public String getValidValuesDescription() {
			return "Any text";
		}
	}

	private enum LongListElementSerializer implements IConfigValueSerializer<Long> {
		INSTANCE;

		@Override
		public String serialize(Long value) {
			return value.toString();
		}

		@Override
		public IDeserializeResult<Long> deserialize(String string) {
			string = string.trim();
			try {
				long value = Long.parseLong(string);
				return IDeserializeResult.success(value);
			} catch (NumberFormatException e) {
				return IDeserializeResult.failure("Unable to parse long integer: '%s' with error:\n%s".formatted(string, e.getMessage()));
			}
		}

		@Override
		public boolean isValid(Long value) {
			return value != null;
		}

		@Override
		public Optional<Collection<Long>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public String getValidValuesDescription() {
			return "Any long integer";
		}
	}

	private enum DoubleListElementSerializer implements IConfigValueSerializer<Double> {
		INSTANCE;

		@Override
		public String serialize(Double value) {
			return value.toString();
		}

		@Override
		public IDeserializeResult<Double> deserialize(String string) {
			string = string.trim();
			try {
				double value = Double.parseDouble(string);
				if (!isValid(value)) {
					return IDeserializeResult.failure("Invalid decimal number. Must be: " + getValidValuesDescription());
				}
				return IDeserializeResult.success(value);
			} catch (NumberFormatException e) {
				return IDeserializeResult.failure("Unable to parse decimal number: '%s' with error:\n%s".formatted(string, e.getMessage()));
			}
		}

		@Override
		public boolean isValid(Double value) {
			return value != null && Double.isFinite(value);
		}

		@Override
		public Optional<Collection<Double>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public String getValidValuesDescription() {
			return "Any finite decimal number";
		}
	}
}
