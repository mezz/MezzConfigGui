package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.api.value.IConfigKeyValueSerializer;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyValueElementSerializerAdapterTest {

	private static final NamedNumberSerializer SERIALIZER = new NamedNumberSerializer();

	@Test
	void detectsKeyValueSerializerAndExposesComponents() {
		NamedNumber entry = new NamedNumber("one", 1);

		KeyValueElementSerializerAdapter<NamedNumber> adapter = KeyValueElementSerializerAdapter.create(SERIALIZER).orElseThrow();

		assertEquals("one", adapter.getKey(entry));
		assertEquals(1, adapter.getValue(entry));
		assertEquals("one", adapter.getKeySerializer().serialize(adapter.getKey(entry)));
		assertEquals("1", adapter.getValueSerializer().serialize(adapter.getValue(entry)));
	}

	@Test
	void rebuildsEntryAfterEditingEitherComponent() {
		NamedNumber entry = new NamedNumber("one", 1);
		KeyValueElementSerializerAdapter<NamedNumber> adapter = KeyValueElementSerializerAdapter.create(SERIALIZER).orElseThrow();

		assertEquals(new NamedNumber("first", 1), adapter.withSerializedKey(entry, "first").orElseThrow());
		assertEquals(new NamedNumber("one", 2), adapter.withSerializedValue(entry, "2").orElseThrow());
		assertEquals(new NamedNumber("direct", 1), adapter.withKey(entry, "direct").orElseThrow());
		assertEquals(new NamedNumber("one", 3), adapter.withValue(entry, 3).orElseThrow());
	}

	@Test
	void rejectsInvalidComponentsAndInvalidRebuiltEntries() {
		NamedNumber entry = new NamedNumber("one", 1);
		KeyValueElementSerializerAdapter<NamedNumber> adapter = KeyValueElementSerializerAdapter.create(SERIALIZER).orElseThrow();

		assertTrue(adapter.withSerializedKey(entry, "").isEmpty());
		assertTrue(adapter.withSerializedValue(entry, "not a number").isEmpty());
		assertTrue(adapter.withSerializedValue(entry, "-1").isEmpty());
		assertTrue(adapter.withKey(entry, "").isEmpty());
	}

	@Test
	void ignoresOrdinaryElementSerializers() {
		assertEquals(Optional.empty(), KeyValueElementSerializerAdapter.create(new StringSerializer()));
	}

	private record NamedNumber(String name, int number) {}

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
			IDeserializeResult<Integer> number = VALUE_SERIALIZER.deserialize(parts[1]);
			return number.getResult()
				.map(value -> IDeserializeResult.success(new NamedNumber(parts[0], value)))
				.orElseGet(() -> IDeserializeResult.failure("Invalid number"));
		}

		@Override
		public boolean isValid(NamedNumber value) {
			return KEY_SERIALIZER.isValid(value.name()) && VALUE_SERIALIZER.isValid(value.number());
		}

		@Override
		public String getValidValuesDescription() {
			return "A name and non-negative number separated by ':'";
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
			return "A non-blank string";
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
			return "A non-negative integer";
		}
	}
}
