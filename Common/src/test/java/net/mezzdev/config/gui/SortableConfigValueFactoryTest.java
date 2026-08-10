package net.mezzdev.config.gui;

import net.mezzdev.config.api.sorting.ISortingConfig;
import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigListValueEditorOptions;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigValueIcon;
import net.mezzdev.config.gui.api.IConfigValueIconProvider;
import net.mezzdev.config.gui.api.IConfigValueLocalizationProvider;
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
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortableConfigValueFactoryTest {
	private static final ISortableConfigValueFactory SORTABLE_CONFIG_VALUES = SortableConfigValueFactory.INSTANCE;

	@Test
	void createsStringListConfigValueBackedBySortingConfig() {
		TestSortingConfig sortingConfig = new TestSortingConfig(true);
		IConfigValueIcon firstIcon = (guiGraphics, area) -> {};
		IConfigScreenValue<List<String>> configValue = SORTABLE_CONFIG_VALUES.createStringList(
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
		assertEquals(ConfigValueApplyMode.ON_APPLY, configValue.getApplyMode());

		IConfigListValueEditorSerializer<String> listSerializer = getListSerializer(configValue);
		assertSame(ConfigValueEditorTypes.getList(), getEditorSerializer(listSerializer).getEditorType());
		assertEquals(List.of("first", "second"), List.copyOf(listSerializer.getElementSerializer().getAllValidValues().orElseThrow()));
		assertEquals("First", ConfigValueLocalization
			.getValueName(listSerializer.getElementSerializer(), "test.sortOrder", "first")
			.getString());
		assertEquals("First description", ConfigValueLocalization
			.getValueDescription(listSerializer.getElementSerializer(), "test.sortOrder", "first")
			.orElseThrow()
			.getString());
		IConfigValueIconProvider<String> iconProvider = getIconProvider(listSerializer.getElementSerializer());
		assertSame(firstIcon, iconProvider.getIcon("first").orElseThrow());
		IConfigListValueEditorOptions editorOptions = assertInstanceOf(IConfigListValueEditorOptions.class, listSerializer);
		assertTrue(editorOptions.allowsRemovingValues());
	}

	@Test
	void rejectsBlankOrDuplicateStringValues() {
		TestSortingConfig sortingConfig = new TestSortingConfig(true);
		IConfigScreenValue<List<String>> configValue = SORTABLE_CONFIG_VALUES.createStringList(
			"sortOrder",
			"test.sortOrder",
			sortingConfig,
			List.of("first", "second"),
			Map.of(),
			Map.of(),
			Map.of()
		);

		assertThrows(IllegalArgumentException.class, () -> configValue.set(List.of("first", "first")));
		assertThrows(IllegalArgumentException.class, () -> configValue.set(List.of("first", " ")));
		assertEquals(List.of(), sortingConfig.savedValues);
	}

	@Test
	void rejectsStringValuesOutsideRuntimeCandidates() {
		TestSortingConfig sortingConfig = new TestSortingConfig(true);
		IConfigScreenValue<List<String>> configValue = SORTABLE_CONFIG_VALUES.createStringList(
			"sortOrder",
			"test.sortOrder",
			sortingConfig,
			List.of("first", "second"),
			Map.of(),
			Map.of(),
			Map.of()
		);
		IConfigListValueEditorSerializer<String> listSerializer = getListSerializer(configValue);
		IConfigValueSerializer<String> elementSerializer = listSerializer.getElementSerializer();

		assertFalse(elementSerializer.isValid("third"));
		IDeserializeResult<String> elementResult = elementSerializer.deserialize("third");
		assertTrue(elementResult.getResult().isEmpty());
		assertTrue(elementResult.getDiagnostics().stream().anyMatch(error -> error.contains("third")));
		IDeserializeResult<List<String>> listResult = listSerializer.deserialize("first, third");
		assertTrue(listResult.getDiagnostics().stream().anyMatch(error -> error.contains("third")));
		IDeserializeResult<List<String>> allInvalidResult = listSerializer.deserialize("third");
		assertTrue(allInvalidResult.getResult().isEmpty());
		assertThrows(IllegalArgumentException.class, () -> configValue.set(List.of("first", "third")));
		assertEquals(List.of(), sortingConfig.savedValues);
	}

	@Test
	void savesValidValuesAndNotifiesListeners() {
		TestSortingConfig sortingConfig = new TestSortingConfig(true);
		IConfigScreenValue<List<String>> configValue = SORTABLE_CONFIG_VALUES.createStringList(
			"sortOrder",
			"test.sortOrder",
			sortingConfig,
			List.of("first", "second"),
			Map.of(),
			Map.of(),
			Map.of()
		);
		List<List<String>> listenerValues = new ArrayList<>();
		configValue.addListener((Consumer<List<String>>) listenerValues::add);

		assertTrue(configValue.set(List.of("second", "first")));
		assertFalse(configValue.set(List.of("second", "first")));

		assertEquals(List.of(List.of("second", "first")), sortingConfig.savedValues);
		assertEquals(List.of(List.of("second", "first")), listenerValues);
	}

	@Test
	void genericFactoryExposesRuntimeValuesThroughElementSerializer() {
		TestSortingConfig sortingConfig = new TestSortingConfig(false);
		IConfigScreenValue<List<String>> configValue = SORTABLE_CONFIG_VALUES.create(
			"sortOrder",
			"test.sortOrder",
			sortingConfig,
			List.of("second", "first"),
			TestStringSerializer.INSTANCE
		);

		IConfigListValueEditorSerializer<String> listSerializer = getListSerializer(configValue);
		assertEquals(List.of("first", "second"), List.copyOf(listSerializer.getElementSerializer().getAllValidValues().orElseThrow()));
		IConfigListValueEditorOptions editorOptions = assertInstanceOf(IConfigListValueEditorOptions.class, listSerializer);
		assertFalse(editorOptions.allowsRemovingValues());
	}

	@Test
	void genericFactoryRejectsValuesOutsideRuntimeCandidates() {
		TestSortingConfig sortingConfig = new TestSortingConfig(true);
		IConfigScreenValue<List<String>> configValue = SORTABLE_CONFIG_VALUES.create(
			"sortOrder",
			"test.sortOrder",
			sortingConfig,
			List.of("first", "second"),
			TestStringSerializer.INSTANCE
		);

		IConfigValueSerializer<String> elementSerializer = getListSerializer(configValue).getElementSerializer();
		assertFalse(elementSerializer.isValid("third"));
		assertTrue(elementSerializer.deserialize("third").getDiagnostics().stream().anyMatch(error -> error.contains("third")));
		assertThrows(IllegalArgumentException.class, () -> configValue.set(List.of("first", "third")));
		assertEquals(List.of(), sortingConfig.savedValues);
	}

	private static IConfigListValueEditorSerializer<String> getListSerializer(IConfigScreenValue<List<String>> configValue) {
		return getListSerializer(configValue.getSerializer());
	}

	@SuppressWarnings("unchecked")
	private static IConfigListValueEditorSerializer<String> getListSerializer(Object serializer) {
		return (IConfigListValueEditorSerializer<String>) assertInstanceOf(IConfigListValueEditorSerializer.class, serializer);
	}

	@SuppressWarnings("unchecked")
	private static IConfigValueEditorSerializer<List<String>> getEditorSerializer(Object serializer) {
		return (IConfigValueEditorSerializer<List<String>>) assertInstanceOf(IConfigValueEditorSerializer.class, serializer);
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

	private enum TestStringSerializer implements IConfigValueSerializer<String>, IConfigValueLocalizationProvider<String> {
		INSTANCE;

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
		public Component getLocalizedValueName(String configValueLocalizationKey, String value) {
			return Component.literal(value);
		}

		@Override
		public String getValidValuesDescription() {
			return "";
		}
	}

}
