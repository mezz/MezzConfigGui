package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.ConfigListOrdering;
import net.mezzdev.config.api.value.serializer.IConfigListValueSerializer;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigListValueEditorSerializersTest {
	@Test
	void adaptsConfigListValueSerializer() {
		StringSerializer elementSerializer = new StringSerializer();
		StringListSerializer listSerializer = new StringListSerializer(elementSerializer);

		assertTrue(ConfigListValueEditorSerializers.canAdapt(listSerializer));

		IConfigListValueEditorSerializer<String> adapter = ConfigListValueEditorSerializers.adapt(listSerializer);
		assertSame(elementSerializer, adapter.getElementSerializer());
		assertEquals(ConfigListOrdering.UNORDERED, adapter.getOrdering());
		assertEquals("alpha, beta", adapter.serialize(List.of("alpha", "beta")));
		assertEquals(List.of("alpha", "beta"), adapter.deserialize("alpha, beta").getResult().orElseThrow());
	}

	@Test
	void doesNotAdaptPlainValueSerializer() {
		StringSerializer serializer = new StringSerializer();

		assertFalse(ConfigListValueEditorSerializers.canAdapt(serializer));
	}

	private static final class StringListSerializer implements IConfigListValueSerializer<String> {
		private final IConfigValueSerializer<String> elementSerializer;

		private StringListSerializer(IConfigValueSerializer<String> elementSerializer) {
			this.elementSerializer = elementSerializer;
		}

		public IConfigValueSerializer<String> getElementSerializer() {
			return elementSerializer;
		}

		@Override
		public ConfigListOrdering getOrdering() {
			return ConfigListOrdering.UNORDERED;
		}

		@Override
		public String serialize(List<String> value) {
			return String.join(", ", value);
		}

		@Override
		public IDeserializeResult<List<String>> deserialize(String string) {
			return IDeserializeResult.success(List.of(string.split(", ")));
		}

		@Override
		public boolean isValid(List<String> value) {
			return true;
		}

		@Override
		public Optional<List<List<String>>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public String getValidValuesDescription() {
			return "String list";
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
			return true;
		}

		@Override
		public Optional<List<String>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public String getValidValuesDescription() {
			return "String";
		}
	}
}
