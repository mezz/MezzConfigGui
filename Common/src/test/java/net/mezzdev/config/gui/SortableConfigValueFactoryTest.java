package net.mezzdev.config.gui;

import net.mezzdev.config.api.sorting.ISortingConfig;
import net.mezzdev.config.api.value.ConfigValueEditorTypes;
import net.mezzdev.config.api.value.ConfigValueUpdateType;
import net.mezzdev.config.api.value.IConfigListValueSerializer;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.gui.api.IConfigListValueEditorOptions;
import net.mezzdev.config.gui.api.IConfigValueIcon;
import net.mezzdev.config.gui.api.IConfigValueIconProvider;
import net.mezzdev.config.gui.api.ISortableConfigValueFactory;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortableConfigValueFactoryTest {
	private static final ISortableConfigValueFactory SORTABLE_CONFIG_VALUES = SortableConfigValueFactory.INSTANCE;

	@Test
	void createsStringListConfigValueBackedBySortingConfig() {
		TestSortingConfig sortingConfig = new TestSortingConfig(true);
		IConfigValueIcon firstIcon = (guiGraphics, area) -> {};
		IConfigValue<List<String>> configValue = SORTABLE_CONFIG_VALUES.createStringList(
			"sortOrder",
			"test.sortOrder",
			sortingConfig,
			List.of("second", "first"),
			Map.of("first", Component.literal("First")),
			Map.of("first", Component.literal("First description")),
			Map.of("first", firstIcon)
		);

		assertEquals("sortOrder", configValue.getName());
		assertEquals("test.sortOrder", configValue.getLocalizationKey());
		assertEquals(List.of("first", "second"), configValue.getDefaultValue());
		assertEquals(List.of("first", "second"), configValue.getValue());
		assertEquals(ConfigValueUpdateType.ON_APPLY, configValue.getUpdateType());

		IConfigListValueSerializer<String> listSerializer = getListSerializer(configValue);
		assertSame(ConfigValueEditorTypes.getList(), listSerializer.getEditorType());
		assertEquals(List.of("first", "second"), List.copyOf(listSerializer.getListValueSerializer().getAllValidValues().orElseThrow()));
		assertEquals("First", listSerializer.getListValueSerializer()
			.getLocalizedValueName("test.sortOrder", "first")
			.getString());
		assertEquals("First description", listSerializer.getListValueSerializer()
			.getLocalizedValueDescription("test.sortOrder", "first")
			.orElseThrow()
			.getString());
		IConfigValueIconProvider<String> iconProvider = getIconProvider(listSerializer.getListValueSerializer());
		assertSame(firstIcon, iconProvider.getIcon("first").orElseThrow());
		IConfigListValueEditorOptions editorOptions = assertInstanceOf(IConfigListValueEditorOptions.class, listSerializer);
		assertTrue(editorOptions.allowsRemovingValues());
	}

	@Test
	void rejectsBlankOrDuplicateStringValues() {
		TestSortingConfig sortingConfig = new TestSortingConfig(true);
		IConfigValue<List<String>> configValue = SORTABLE_CONFIG_VALUES.createStringList(
			"sortOrder",
			"test.sortOrder",
			sortingConfig,
			List.of("first", "second"),
			Map.of(),
			Map.of(),
			Map.of()
		);

		assertFalse(configValue.set(List.of("first", "first")));
		assertFalse(configValue.set(List.of("first", " ")));
		assertEquals(List.of(), sortingConfig.savedValues);
	}

	@Test
	void savesValidValuesAndNotifiesListeners() {
		TestSortingConfig sortingConfig = new TestSortingConfig(true);
		IConfigValue<List<String>> configValue = SORTABLE_CONFIG_VALUES.createStringList(
			"sortOrder",
			"test.sortOrder",
			sortingConfig,
			List.of("first", "second"),
			Map.of(),
			Map.of(),
			Map.of()
		);
		List<List<String>> listenerValues = new ArrayList<>();
		configValue.addListener(listenerValues::add);

		assertTrue(configValue.set(List.of("second", "first")));

		assertEquals(List.of(List.of("second", "first")), sortingConfig.savedValues);
		assertEquals(List.of(List.of("second", "first")), listenerValues);
	}

	@Test
	void genericFactoryExposesRuntimeValuesThroughElementSerializer() {
		TestSortingConfig sortingConfig = new TestSortingConfig(false);
		IConfigValue<List<String>> configValue = SORTABLE_CONFIG_VALUES.create(
			"sortOrder",
			"test.sortOrder",
			sortingConfig,
			List.of("second", "first"),
			TestStringSerializer.INSTANCE
		);

		IConfigListValueSerializer<String> listSerializer = getListSerializer(configValue);
		assertEquals(List.of("first", "second"), List.copyOf(listSerializer.getListValueSerializer().getAllValidValues().orElseThrow()));
		IConfigListValueEditorOptions editorOptions = assertInstanceOf(IConfigListValueEditorOptions.class, listSerializer);
		assertFalse(editorOptions.allowsRemovingValues());
	}

	private static IConfigListValueSerializer<String> getListSerializer(IConfigValue<List<String>> configValue) {
		return getListSerializer(configValue.getSerializer());
	}

	@SuppressWarnings("unchecked")
	private static IConfigListValueSerializer<String> getListSerializer(Object serializer) {
		return (IConfigListValueSerializer<String>) assertInstanceOf(IConfigListValueSerializer.class, serializer);
	}

	@SuppressWarnings("unchecked")
	private static IConfigValueIconProvider<String> getIconProvider(Object serializer) {
		return (IConfigValueIconProvider<String>) assertInstanceOf(IConfigValueIconProvider.class, serializer);
	}

	private static final class TestSortingConfig implements ISortingConfig<String> {
		private final boolean allowsRemovingValues;
		private final List<List<String>> savedValues = new ArrayList<>();
		private final List<Runnable> listeners = new ArrayList<>();
		@Nullable
		private List<String> sortedValues;

		private TestSortingConfig(boolean allowsRemovingValues) {
			this.allowsRemovingValues = allowsRemovingValues;
		}

		@Override
		public List<String> getSortedValues(Collection<String> allValues) {
			if (sortedValues != null) {
				return sortedValues;
			}
			return getDefaultSortedValues(allValues);
		}

		@Override
		public List<String> getDefaultSortedValues(Collection<String> allValues) {
			return allValues.stream()
				.distinct()
				.sorted()
				.toList();
		}

		@Override
		public boolean setSortedValues(List<String> sortedValues) {
			this.sortedValues = List.copyOf(sortedValues);
			savedValues.add(this.sortedValues);
			for (Runnable listener : List.copyOf(listeners)) {
				listener.run();
			}
			return true;
		}

		@Override
		public Comparator<String> getComparator(Collection<String> allValues) {
			return Comparator.comparingInt(value -> getSortedValues(allValues).indexOf(value));
		}

		@Override
		public boolean isVisible(Collection<String> allValues, String value) {
			return getSortedValues(allValues).contains(value);
		}

		@Override
		public boolean allowsRemovingValues() {
			return allowsRemovingValues;
		}

		@Override
		public Runnable addChangeListener(Runnable listener) {
			listeners.add(listener);
			return () -> listeners.remove(listener);
		}
	}

	private enum TestStringSerializer implements IConfigValueSerializer<String> {
		INSTANCE;

		@Override
		public String serialize(String value) {
			return value;
		}

		@Override
		public IDeserializeResult<String> deserialize(String string) {
			return new TestDeserializeResult(string);
		}

		@Override
		public boolean isValid(String value) {
			return true;
		}

		@Override
		public Optional<Collection<String>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public Component getLocalizedValueName(String configValueLocalizationKey, String value) {
			return Component.literal(value);
		}

		@Override
		public String getValidValuesDescription() {
			return "";
		}
	}

	private record TestDeserializeResult(
		String value
	) implements IConfigValueSerializer.IDeserializeResult<String> {
		@Override
		public Optional<String> getResult() {
			return Optional.of(value);
		}

		@Override
		public List<String> getErrors() {
			return List.of();
		}
	}
}
