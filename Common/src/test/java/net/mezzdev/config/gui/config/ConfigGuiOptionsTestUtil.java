package net.mezzdev.config.gui.config;

import net.mezzdev.config.api.schema.category.IConfigEditorCategory;
import net.mezzdev.config.api.value.editor.ConfigValueEditMode;
import net.mezzdev.config.api.value.editor.IConfigValueEditorInfo;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.change.IConfigValueBatchChangeListener;
import net.mezzdev.config.api.value.change.IConfigValueChangeListener;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ConfigGuiOptionsTestUtil {
	private ConfigGuiOptionsTestUtil() {

	}

	public static <T> OptionOverride setValue(String fieldName, T value) {
		try {
			Field field = ConfigGuiOptions.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			Object previousValue = field.get(null);
			field.set(null, new TestConfigValue<>(fieldName, value));
			return new OptionOverride(field, previousValue);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Failed to override ConfigGuiOptions field: " + fieldName, e);
		}
	}

	public static final class OptionOverride implements AutoCloseable {
		private final Field field;
		private final Object previousValue;
		private boolean closed;

		private OptionOverride(Field field, Object previousValue) {
			this.field = field;
			this.previousValue = previousValue;
		}

		@Override
		public void close() {
			if (closed) {
				return;
			}
			try {
				field.set(null, previousValue);
			} catch (IllegalAccessException e) {
				throw new AssertionError("Failed to restore ConfigGuiOptions field: " + field.getName(), e);
			}
			closed = true;
		}
	}

	private static final class TestConfigValue<T> implements IConfigValue<T>, IConfigValueEditorInfo<T> {
		private final String name;
		private final T defaultValue;
		private final IConfigValueSerializer<T> serializer = new TestConfigValueSerializer<>();
		private T value;

		private TestConfigValue(String name, T value) {
			this.name = name;
			this.defaultValue = value;
			this.value = value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getLocalizationKey() {
			return "test." + name;
		}

		@Override
		public T get() {
			return value;
		}

		@Override
		public IConfigValueEditorInfo<T> getEditorInfo() {
			return this;
		}

		@Override
		public T getPendingValue() {
			return value;
		}

		@Override
		public T getDefaultValue() {
			return defaultValue;
		}

		@Override
		public ConfigValueEditMode getEditMode() {
			return ConfigValueEditMode.BATCH;
		}

		@Override
		public ConfigValueRestartRequirement getRestartRequirement() {
			return ConfigValueRestartRequirement.NONE;
		}

		@Override
		public List<? extends IConfigEditorCategory> getEditorCategories() {
			return List.of();
		}

		@Override
		public boolean set(T value) {
			boolean changed = !Objects.equals(this.value, value);
			this.value = value;
			return changed;
		}

		@Override
		public Runnable addListener(IConfigValueChangeListener<T> listener) {
			return () -> {};
		}

		@Override
		public Runnable addPendingListener(IConfigValueChangeListener<T> listener) {
			return () -> {};
		}

		@Override
		public Runnable addBatchListener(IConfigValueBatchChangeListener listener) {
			return () -> {};
		}

		@Override
		public Runnable addPendingBatchListener(IConfigValueBatchChangeListener listener) {
			return () -> {};
		}

		@Override
		public IConfigValueSerializer<T> getSerializer() {
			return serializer;
		}
	}

	private static final class TestConfigValueSerializer<T> implements IConfigValueSerializer<T> {
		@Override
		public String serialize(T value) {
			return String.valueOf(value);
		}

		@Override
		public IDeserializeResult<T> deserialize(String string) {
			return IDeserializeResult.failure("Not supported by test serializer.");
		}

		@Override
		public boolean isValid(T value) {
			return true;
		}

		@Override
		public Optional<List<T>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public String getValidValuesDescription() {
			return "";
		}
	}
}
