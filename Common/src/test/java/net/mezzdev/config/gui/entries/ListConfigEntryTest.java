package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.ConfigListOrdering;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.IConfigListValueEditorOptions;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListConfigEntryTest {
	@Test
	void openingLargeSortingListsDoesNotRepeatedlyScanSelectedValues() {
		int size = 2000;
		List<String> allValues = IntStream.range(0, size).mapToObj(index -> "value" + index).toList();
		CountingList selectedValues = new CountingList(allValues);
		TestListSerializer serializer = new TestListSerializer(true, allValues, ConfigListOrdering.ORDERED);
		new ListConfigEntry<>(new TestConfigValue(serializer, selectedValues), serializer, selector -> {}, () -> {}, null);
		assertTrue(selectedValues.reads <= size * 3,
			"Opening a sorting list repeatedly scanned its selected values: " + selectedValues.reads);
	}

	@Test
	void onlyVisibleRowsAreVisitedEvenInVeryLargeSortingLists() {
		ImmutableRect2i viewport = new ImmutableRect2i(0, 0, 200, 240);
		assertEquals(new ListConfigEntry.RowRange(25000, 25010), ListConfigEntry.getVisibleRowRange(-600000, 24, 50000, viewport, 0));
		assertEquals(new ListConfigEntry.RowRange(25000, 25011), ListConfigEntry.getVisibleRowRange(-600001, 24, 50000, viewport, 0));
		assertEquals(new ListConfigEntry.RowRange(0, 0), ListConfigEntry.getVisibleRowRange(240, 24, 50000, viewport, 0));
		assertEquals(new ListConfigEntry.RowRange(10, 10), ListConfigEntry.getVisibleRowRange(-240, 24, 10, viewport, 0));
		assertEquals(new ListConfigEntry.RowRange(0, 0), ListConfigEntry.getVisibleRowRange(0, 24, 10, ImmutableRect2i.EMPTY, 0));
	}

	@Test
	void visibleRangesIncludePartiallyVisibleAndShiftedDragRowsAtEitherEdge() {
		ImmutableRect2i viewport = new ImmutableRect2i(10, 70, 200, 91);
		for (int firstY = -400; firstY <= 200; firstY++) {
			for (int source = 0; source < 30; source += 5) {
				for (int target = 0; target < 30; target += 5) {
					ListConfigEntry.RowRange range = ListConfigEntry.getVisibleRowRange(firstY, 24, 30, viewport, 1);
					for (int row = 0; row < 30; row++) {
						int shiftedY = firstY + row * 24 + ListConfigEntry.getDragRowOffset(row, source, target, 24);
						if (new ImmutableRect2i(10, shiftedY, 200, 24).intersects(viewport)) {
							assertTrue(row >= range.first() && row < range.end(), "Visible drag row was culled");
						}
					}
				}
			}
		}
	}

	@Test
	void stationaryRangesMatchTheViewportExactlyForPartialRowsAndScrolledLists() {
		ImmutableRect2i viewport = new ImmutableRect2i(10, 70, 200, 91);
		for (int firstY = -400; firstY <= 200; firstY++) {
			ListConfigEntry.RowRange range = ListConfigEntry.getVisibleRowRange(firstY, 24, 30, viewport, 0);
			for (int row = 0; row < 30; row++) {
				boolean visible = new ImmutableRect2i(10, firstY + row * 24, 200, 24).intersects(viewport);
				assertEquals(visible, row >= range.first() && row < range.end());
			}
		}
	}

	@Test
	void hidesTypedInputWhenElementSerializerHasFiniteValidValues() {
		ListConfigEntry<String> entry = createEntry(
			true,
			List.of("first", "second")
		);

		assertFalse(getAllowsTypedInput(entry));
	}

	@Test
	void showsTypedInputWhenElementSerializerIsOpenEndedAndRemovingValuesIsAllowed() {
		ListConfigEntry<String> entry = createEntry(
			true,
			null
		);

		assertTrue(getAllowsTypedInput(entry));
	}

	@Test
	void hidesTypedInputWhenRemovingValuesIsDisallowed() {
		ListConfigEntry<String> entry = createEntry(
			false,
			null
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
			null,
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
		@Nullable List<String> allValidValues
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
		private final List<String> values;

		private TestConfigValue(TestListSerializer serializer) {
			this(serializer, List.of("first"));
		}

		private TestConfigValue(TestListSerializer serializer, List<String> values) {
			this.serializer = serializer;
			this.values = values;
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
			return values;
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

	private static final class CountingList extends AbstractList<String> {
		private final List<String> values;
		private int reads;

		private CountingList(List<String> values) {
			this.values = values;
		}

		@Override
		public String get(int index) {
			reads++;
			return values.get(index);
		}

		@Override
		public int size() {
			return values.size();
		}
	}

	private static final class TestListSerializer implements IConfigListValueEditorSerializer<String>, IConfigListValueEditorOptions {
		private final boolean allowsRemovingValues;
		private final TestElementSerializer elementSerializer;
		private final ConfigListOrdering ordering;

		private TestListSerializer(
			boolean allowsRemovingValues,
			@Nullable List<String> allValidValues,
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
		@Nullable
		private final List<String> allValidValues;

		private TestElementSerializer(@Nullable List<String> allValidValues) {
			if (allValidValues == null) {
				this.allValidValues = null;
			} else {
				this.allValidValues = List.copyOf(allValidValues);
			}
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
			return Optional.ofNullable(allValidValues);
		}

		@Override
		public String getValidValuesDescription() {
			return "";
		}
	}
}
