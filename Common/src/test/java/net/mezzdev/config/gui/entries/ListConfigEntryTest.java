package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.gui.api.IConfigListValueEditorOptions;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListConfigEntryTest {

	@Test
	void hidesTypedInputWhenElementSerializerHasFiniteValidValues() {
		ListConfigEntry<String> entry = createEntry(
			true,
			Optional.of(List.of("first", "second"))
		);

		assertFalse(getAllowsTypedInput(entry));
	}

	@Test
	void showsTypedInputWhenElementSerializerIsOpenEndedAndRemovingValuesIsAllowed() {
		ListConfigEntry<String> entry = createEntry(
			true,
			Optional.empty()
		);

		assertTrue(getAllowsTypedInput(entry));
	}

	@Test
	void hidesTypedInputWhenRemovingValuesIsDisallowed() {
		ListConfigEntry<String> entry = createEntry(
			false,
			Optional.empty()
		);

		assertFalse(getAllowsTypedInput(entry));
	}

	private static ListConfigEntry<String> createEntry(
		boolean allowsRemovingValues,
		Optional<Collection<String>> allValidValues
	) {
		TestListSerializer serializer = new TestListSerializer(allowsRemovingValues, allValidValues);
		return new ListConfigEntry<>(
			new TestConfigValue(serializer),
			serializer,
			() -> {},
			null
		);
	}

	private static boolean getAllowsTypedInput(ListConfigEntry<String> entry) {
		try {
			Field field = ListConfigEntry.class.getDeclaredField("allowsTypedInput");
			field.setAccessible(true);
			return field.getBoolean(entry);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static final class TestConfigValue implements IConfigScreenValue<List<String>> {
		private final TestListSerializer serializer;

		private TestConfigValue(TestListSerializer serializer) {
			this.serializer = serializer;
		}

		@Override
		public String getName() {
			return "list";
		}

		@Override
		public String getLocalizationKey() {
			return "test.list";
		}

		@Override
		public List<String> getValue() {
			return List.of("first");
		}

		@Override
		public List<String> getDefaultValue() {
			return List.of("first");
		}

		@Override
		public boolean set(List<String> value) {
			return true;
		}

		@Override
		public void addListener(Consumer<List<String>> listener) {

		}

		@Override
		public IConfigValueSerializer<List<String>> getSerializer() {
			return serializer;
		}
	}

	private static final class TestListSerializer implements IConfigListValueEditorSerializer<String>, IConfigListValueEditorOptions {
		private final boolean allowsRemovingValues;
		private final TestElementSerializer elementSerializer;

		private TestListSerializer(
			boolean allowsRemovingValues,
			Optional<Collection<String>> allValidValues
		) {
			this.allowsRemovingValues = allowsRemovingValues;
			this.elementSerializer = new TestElementSerializer(allValidValues);
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
		public Optional<Collection<List<String>>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public String getValidValuesDescription() {
			return "";
		}

		@Override
		public IConfigValueSerializer<String> getElementSerializer() {
			return elementSerializer;
		}

		@Override
		public Component getLocalizedValueName(String configValueLocalizationKey, List<String> value) {
			return Component.literal(String.join(", ", value));
		}

		@Override
		public boolean allowsRemovingValues() {
			return allowsRemovingValues;
		}
	}

	private static final class TestElementSerializer implements IConfigValueSerializer<String> {
		private final Optional<Collection<String>> allValidValues;

		private TestElementSerializer(Optional<Collection<String>> allValidValues) {
			this.allValidValues = allValidValues.map(List::copyOf);
		}

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
		public Optional<Collection<String>> getAllValidValues() {
			return allValidValues;
		}

		@Override
		public String getValidValuesDescription() {
			return "";
		}
	}
}
