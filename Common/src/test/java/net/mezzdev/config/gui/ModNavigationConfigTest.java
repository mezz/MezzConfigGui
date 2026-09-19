package net.mezzdev.config.gui;

import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.api.IConfigListValueEditorOptions;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueIconProvider;
import net.mezzdev.config.gui.config.ModNavigationConfig;
import net.mezzdev.config.gui.screenlist.ConfigScreenListEntry;
import net.mezzdev.config.gui.screenlist.ConfigScreenOwnerIcon;
import net.mezzdev.config.serializers.StringSerializer;
import net.mezzdev.config.sorting.SortingConfig;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModNavigationConfigTest {
	@Test
	void editorContainsAllModsWithNamesAndIconsAndRemovalHidesThem(@TempDir Path directory) {
		ModNavigationConfig config = createConfig(directory.resolve("mods.ini"));
		List<ConfigScreenListEntry> entries = List.of(entry("a", "Zebra"), entry("b", "Alpha"), entry("c", "Beta"));
		config.setEntries(entries);
		IConfigScreenValue<List<String>> editor = createEditor(config);

		assertEquals(List.of("b", "c", "a"), editor.getValue());
		IConfigListValueEditorSerializer<String> serializer = listSerializer(editor);
		assertTrue(assertInstanceOf(IConfigListValueEditorOptions.class, serializer).allowsRemovingValues());
		assertEquals("Alpha", ConfigValueLocalization.getValueName(serializer.getElementSerializer(), "mods", "b").getString());
		assertTrue(iconProvider(serializer).getIcon("b").isPresent());

		editor.set(List.of("a", "b"));
		assertEquals(List.of("a", "b"), ids(config.applyPreferences(entries)));
		IConfigScreenValue<List<String>> reopened = createEditor(config);
		assertEquals(List.of("a", "b"), reopened.getValue());
		assertEquals(List.of("b", "c", "a"), listSerializer(reopened).getElementSerializer().getAllValidValues().orElseThrow());
		reopened.set(List.of("c", "a", "b"));
		assertEquals(List.of("c", "a", "b"), ids(config.applyPreferences(entries)));
	}

	@Test
	void removedModsStayHiddenAfterReloadAndNewModsAreVisible(@TempDir Path directory) {
		Path path = directory.resolve("mods.ini");
		List<ConfigScreenListEntry> original = List.of(entry("alpha"), entry("beta"), entry("gamma"));
		ModNavigationConfig config = createConfig(path);
		config.setEntries(original);
		createEditor(config).set(List.of("gamma", "alpha"));

		ModNavigationConfig reloaded = createConfig(path);
		List<ConfigScreenListEntry> expanded = List.of(entry("alpha"), entry("beta"), entry("gamma"), entry("delta"));
		assertEquals(List.of("gamma", "alpha", "delta"), ids(reloaded.applyPreferences(expanded)));
		assertEquals(List.of("gamma", "alpha", "delta"), ids(reloaded.applyPreferences(List.of(entry("alpha"), entry("gamma"), entry("delta")))));
		assertEquals(List.of("gamma", "alpha", "delta"), ids(reloaded.applyPreferences(expanded)));
	}

	@Test
	void migratesLegacyOrderAndHiddenIdsOnceIncludingCurrentlyAbsentMods(@TempDir Path directory) throws IOException {
		Path path = directory.resolve("mods.ini");
		Path legacy = directory.resolve("mezz_config_gui.ini");
		Files.writeString(legacy, "Legacy schema is kept intact\n");
		List<ConfigScreenListEntry> entries = List.of(entry("alpha"), entry("beta"), entry("gamma"));
		ModNavigationConfig config = createConfig(path);
		config.migrateLegacyPreferences(legacy, List.of(" GAMMA ", "beta", "gamma"), List.of(" BETA ", "future_mod"));
		assertEquals(List.of("gamma", "alpha"), ids(config.applyPreferences(entries)));
		assertEquals("Legacy schema is kept intact\n", Files.readString(legacy));
		createEditor(config).set(List.of("alpha", "beta", "gamma"));

		ModNavigationConfig reloaded = createConfig(path);
		reloaded.migrateLegacyPreferences(legacy, List.of(" GAMMA ", "beta", "gamma"), List.of(" BETA ", "future_mod"));
		assertEquals(List.of("alpha", "beta", "gamma"), ids(reloaded.applyPreferences(entries)));
		assertEquals(List.of("alpha", "beta", "gamma", "new_mod"), ids(reloaded.applyPreferences(List.of(
			entry("alpha"), entry("beta"), entry("gamma"), entry("future_mod"), entry("new_mod")
		))));
	}

	@Test
	void sortingEditorReplacesBothLegacyStringLists(@TempDir Path directory) {
		ModNavigationConfig config = createConfig(directory.resolve("mods.ini"));
		config.setEntries(List.of(entry("alpha")));
		SortingConfig<String> legacySorting = SortingConfig.inMemory(StringSerializer.INSTANCE, Comparator.naturalOrder(), true);
		List<IConfigScreenValue<List<String>>> legacyValues = List.of("modOrder", "hiddenMods").stream()
			.map(name -> SortableConfigValueFactory.INSTANCE.stringBuilder(name, "test." + name, legacySorting, List.of()).build())
			.toList();
		ConfigScreenCategory legacyCategory = new ConfigScreenCategory() {
			@Override
			public ConfigScreenCategoryGroup getGroup() { return ConfigScreenCategoryGroup.MOD_OWNED; }
			@Override
			public String getName() { return "modList"; }
			@Override
			public String getLocalizationKey() { return "test.modList"; }
			@Override
			public Component getLocalizedName() { return Component.literal("Mod Navigation"); }
			@Override
			public Component getLocalizedDescription() { return Component.empty(); }
			@Override
			public Collection<? extends IConfigScreenValue<?>> getConfigValues() {
				return legacyValues;
			}
		};
		List<ConfigScreenCategory> categories = ConfigGuiPluginLoader.createScreenCategoriesForTests(
			"mezz_config_gui", List.of(legacyCategory), screen -> config.configureCategory(screen.configureCategory("modList")), lookup -> List.of()
		);
		assertEquals(1, categories.size());
		assertEquals(List.of("modOrder"), categories.get(0).getConfigValues().stream().map(IConfigScreenValue::getName).toList());
	}

	private static ModNavigationConfig createConfig(Path path) {
		return new ModNavigationConfig(comparator -> new SortingConfig<>(path, StringSerializer.INSTANCE, comparator, true));
	}

	@SuppressWarnings("unchecked")
	private static IConfigScreenValue<List<String>> createEditor(ModNavigationConfig config) {
		List<ConfigScreenCategory> categories = ConfigGuiPluginLoader.createScreenCategoriesForTests(
			"mezz_config_gui", List.of(), screen -> config.configureCategory(screen.configureCategory("modList")), lookup -> List.of()
		);
		return (IConfigScreenValue<List<String>>) categories.get(0).getConfigValues().iterator().next();
	}

	@SuppressWarnings("unchecked")
	private static IConfigListValueEditorSerializer<String> listSerializer(IConfigScreenValue<List<String>> editor) {
		return (IConfigListValueEditorSerializer<String>) assertInstanceOf(IConfigListValueEditorSerializer.class, editor.getSerializer());
	}

	@SuppressWarnings("unchecked")
	private static IConfigValueIconProvider<String> iconProvider(IConfigListValueEditorSerializer<String> serializer) {
		return (IConfigValueIconProvider<String>) assertInstanceOf(IConfigValueIconProvider.class, serializer.getElementSerializer());
	}

	private static List<String> ids(List<ConfigScreenListEntry> entries) {
		return entries.stream().map(ConfigScreenListEntry::modId).toList();
	}

	private static ConfigScreenListEntry entry(String modId) {
		return entry(modId, modId);
	}

	private static ConfigScreenListEntry entry(String modId, String name) {
		Component title = Component.literal(name);
		return new ConfigScreenListEntry(modId, title, parent -> {
			throw new AssertionError("Sorting must not open screens");
		}, new ConfigScreenOwnerIcon(modId, title, Optional.empty()));
	}
}
