package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.ConfigListOrdering;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.IConfigListValueEditorOptions;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

	@Test
	void draggingDownShiftsRowsUpIntoTheSourceSpace() {
		int rowHeight = 24;

		assertEquals(0, ListConfigEntry.getDragRowOffset(0, 1, 3, rowHeight));
		assertEquals(0, ListConfigEntry.getDragRowOffset(1, 1, 3, rowHeight));
		assertEquals(-rowHeight, ListConfigEntry.getDragRowOffset(2, 1, 3, rowHeight));
		assertEquals(-rowHeight, ListConfigEntry.getDragRowOffset(3, 1, 3, rowHeight));
		assertEquals(0, ListConfigEntry.getDragRowOffset(4, 1, 3, rowHeight));
	}

	@Test
	void draggingUpShiftsRowsDownIntoTheSourceSpace() {
		int rowHeight = 24;

		assertEquals(0, ListConfigEntry.getDragRowOffset(0, 3, 1, rowHeight));
		assertEquals(rowHeight, ListConfigEntry.getDragRowOffset(1, 3, 1, rowHeight));
		assertEquals(rowHeight, ListConfigEntry.getDragRowOffset(2, 3, 1, rowHeight));
		assertEquals(0, ListConfigEntry.getDragRowOffset(3, 3, 1, rowHeight));
		assertEquals(0, ListConfigEntry.getDragRowOffset(4, 3, 1, rowHeight));
	}

	@Test
	void draggedRowCanMoveHorizontallyWithinALimitedRange() {
		assertEquals(88, ListConfigEntry.getDragRowX(100, 93.0, 5.0));
		assertEquals(124, ListConfigEntry.getDragRowX(100, 200.0, 5.0));
		assertEquals(76, ListConfigEntry.getDragRowX(100, 0.0, 5.0));
	}

	@Test
	void honorsUnorderedListMetadata() {
		TestListSerializer serializer = new TestListSerializer(
			true,
			Optional.empty(),
			ConfigListOrdering.UNORDERED
		);
		ListConfigEntry<String> entry = new ListConfigEntry<>(
			new TestConfigValue(serializer),
			serializer,
			selector -> {},
			() -> {},
			null
		);

		assertFalse(getBooleanField(entry, "ordered"));
	}

	private static ListConfigEntry<String> createEntry(
		boolean allowsRemovingValues,
		Optional<List<String>> allValidValues
	) {
		TestListSerializer serializer = new TestListSerializer(
			allowsRemovingValues,
			allValidValues,
			ConfigListOrdering.ORDERED
		);
		return new ListConfigEntry<>(
			new TestConfigValue(serializer),
			serializer,
			selector -> {},
			() -> {},
			null
		);
	}

	private static boolean getAllowsTypedInput(ListConfigEntry<String> entry) {
		return getBooleanField(entry, "allowsTypedInput");
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
		public Runnable addListener(Consumer<List<String>> listener) {
			return () -> {};
		}

		@Override
		public IConfigValueSerializer<List<String>> getSerializer() {
			return serializer;
		}
	}

	private static final class TestListSerializer implements IConfigListValueEditorSerializer<String>, IConfigListValueEditorOptions {
		private final boolean allowsRemovingValues;
		private final TestElementSerializer elementSerializer;
		private final ConfigListOrdering ordering;

		private TestListSerializer(
			boolean allowsRemovingValues,
			Optional<List<String>> allValidValues,
			ConfigListOrdering ordering
		) {
			this.allowsRemovingValues = allowsRemovingValues;
			this.elementSerializer = new TestElementSerializer(allValidValues);
			this.ordering = ordering;
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
			return "";
		}

		@Override
		public IConfigValueSerializer<String> getElementSerializer() {
			return elementSerializer;
		}

		@Override
		public ConfigListOrdering getOrdering() {
			return ordering;
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
		private final Optional<List<String>> allValidValues;

		private TestElementSerializer(Optional<List<String>> allValidValues) {
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
		public Optional<List<String>> getAllValidValues() {
			return allValidValues;
		}

		@Override
		public String getValidValuesDescription() {
			return "";
		}
	}
}
