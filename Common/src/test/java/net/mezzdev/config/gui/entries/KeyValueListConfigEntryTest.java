package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.IConfigKeyValueSerializer;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.IConfigListValueEditorOptions;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyValueListConfigEntryTest {
	private static final KeyValueListSerializer SERIALIZER = new KeyValueListSerializer();

	@Test
	void allowsAddingAndRemovingKeyValueEntries() {
		ListConfigEntry<NamedNumber> entry = createEntry();

		assertTrue(getBooleanField(entry, "allowsRemovingValues"));
		assertTrue(getBooleanField(entry, "allowsTypedInput"));
	}

	@Test
	void resetsAValueByMatchingItsKeyWithoutChangingTheKey() {
		ListConfigEntry<NamedNumber> entry = createEntry();

		assertTrue(entry.resetComponentValue(0));

		assertEquals(
			List.of(new NamedNumber("second", 2), new NamedNumber("first", 88)),
			entry.getValue()
		);
	}

	@SuppressWarnings("DataFlowIssue")
	private static ListConfigEntry<NamedNumber> createEntry() {
		return new ListConfigEntry<>(
			new TestConfigValue(),
			SERIALIZER,
			selector -> {},
			() -> {},
			null
		);
	}

	private static boolean getBooleanField(ListConfigEntry<?> entry, String fieldName) {
		try {
			Field field = ListConfigEntry.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.getBoolean(entry);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private record NamedNumber(String name, int number) {
	}

	private static final class TestConfigValue implements IConfigScreenValue<List<NamedNumber>> {
		@Override
		public String getName() {
			return "namedNumbers";
		}

		@Override
		public String getLocalizationKey() {
			return "test.namedNumbers";
		}

		@Override
		public List<NamedNumber> getValue() {
			return List.of(new NamedNumber("second", 99), new NamedNumber("first", 88));
		}

		@Override
		public List<NamedNumber> getDefaultValue() {
			return List.of(new NamedNumber("first", 1), new NamedNumber("second", 2));
		}

		@Override
		public boolean set(List<NamedNumber> value) {
			return true;
		}

		@Override
		public Runnable addListener(Consumer<List<NamedNumber>> listener) {
			return () -> {};
		}

		@Override
		public IConfigValueSerializer<List<NamedNumber>> getSerializer() {
			return SERIALIZER;
		}
	}

	private static final class KeyValueListSerializer implements IConfigListValueEditorSerializer<NamedNumber>, IConfigListValueEditorOptions {
		private static final NamedNumberSerializer ELEMENT_SERIALIZER = new NamedNumberSerializer();

		@Override
		public IConfigValueSerializer<NamedNumber> getElementSerializer() {
			return ELEMENT_SERIALIZER;
		}

		@Override
		public String serialize(List<NamedNumber> value) {
			return value.toString();
		}

		@Override
		public IDeserializeResult<List<NamedNumber>> deserialize(String string) {
			return IDeserializeResult.failure("Not used by this test");
		}

		@Override
		public boolean isValid(List<NamedNumber> value) {
			return value.stream().map(NamedNumber::name).distinct().count() == value.size();
		}

		@Override
		public String getValidValuesDescription() {
			return "name:number entries";
		}

		@Override
		public Component getLocalizedValueName(String configValueLocalizationKey, List<NamedNumber> value) {
			return Component.literal(value.toString());
		}

		@Override
		public boolean allowsRemovingValues() {
			return false;
		}
	}

	private static final class NamedNumberSerializer implements IConfigKeyValueSerializer<NamedNumber, String, Integer> {
		private static final StringSerializer KEY_SERIALIZER = new StringSerializer();
		private static final IntegerSerializer VALUE_SERIALIZER = new IntegerSerializer();

		@Override
		public IConfigValueSerializer<String> getKeySerializer() {
			return KEY_SERIALIZER;
		}

		@Override
		public IConfigValueSerializer<Integer> getValueSerializer() {
			return VALUE_SERIALIZER;
		}

		@Override
		public String getKey(NamedNumber entry) {
			return entry.name();
		}

		@Override
		public Integer getValue(NamedNumber entry) {
			return entry.number();
		}

		@Override
		public NamedNumber createEntry(String key, Integer value) {
			return new NamedNumber(key, value);
		}

		@Override
		public String serialize(NamedNumber value) {
			return value.name() + ":" + value.number();
		}

		@Override
		public IDeserializeResult<NamedNumber> deserialize(String string) {
			String[] parts = string.split(":", 2);
			if (parts.length != 2) {
				return IDeserializeResult.failure("Expected name:number");
			}
			return VALUE_SERIALIZER.deserialize(parts[1]).getResult()
				.map(value -> IDeserializeResult.success(new NamedNumber(parts[0], value)))
				.orElseGet(() -> IDeserializeResult.failure("Invalid number"));
		}

		@Override
		public boolean isValid(NamedNumber value) {
			return KEY_SERIALIZER.isValid(value.name()) && VALUE_SERIALIZER.isValid(value.number());
		}

		@Override
		public String getValidValuesDescription() {
			return "name:number";
		}
	}

	private static final class StringSerializer implements IConfigValueSerializer<String> {
		@Override
		public String serialize(String value) {
			return value;
		}

		@Override
		public IDeserializeResult<String> deserialize(String string) {
			return IDeserializeResult.success(string);
		}

		@Override
		public boolean isValid(String value) {
			return !value.isBlank();
		}

		@Override
		public String getValidValuesDescription() {
			return "non-blank text";
		}
	}

	private static final class IntegerSerializer implements IConfigValueSerializer<Integer> {
		@Override
		public String serialize(Integer value) {
			return value.toString();
		}

		@Override
		public IDeserializeResult<Integer> deserialize(String string) {
			try {
				return IDeserializeResult.success(Integer.parseInt(string));
			} catch (NumberFormatException e) {
				return IDeserializeResult.failure("Invalid integer");
			}
		}

		@Override
		public boolean isValid(Integer value) {
			return value >= 0;
		}

		@Override
		public String getValidValuesDescription() {
			return "non-negative integer";
		}
	}
}
