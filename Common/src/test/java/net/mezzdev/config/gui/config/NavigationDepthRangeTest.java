package net.mezzdev.config.gui.config;

import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.editor.ConfigValueEditMode;
import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import net.mezzdev.config.file.ConfigFileWatcherSettings;
import net.mezzdev.config.file.ConfigManager;
import net.mezzdev.config.file.ConfigSerializer;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.model.AppliedConfigValueChange;
import net.mezzdev.config.schema.ConfigSchema;
import net.mezzdev.config.schema.ConfigSchemaBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationDepthRangeTest {
	@Test
	void persistsResetsAndUndoesTheRangeAsOneValue(@TempDir Path directory) throws IOException {
		Path path = directory.resolve("gui.ini");
		Files.writeString(path, "[navigation]\nnavigationDepth = 3..8\n");
		Fixture fixture = create(path);
		IConfigScreenValue<ConfigValueRange<Integer>> value = IConfigScreenValue.configValue(fixture.value());
		ConfigValueRange<Integer> original = new ConfigValueRange<>(3, 8);
		assertEquals(original, value.getValue());
		List<ConfigValueRange<Integer>> observed = new ArrayList<>();
		Runnable remove = value.addListener(observed::add);
		ConfigValueRange<Integer> changed = new ConfigValueRange<>(0, 2);
		assertTrue(value.set(changed));
		assertEquals(List.of(changed), observed);
		assertEquals(changed, fixture.value().get());
		assertFalse(value.set(changed));
		assertEquals(1, observed.size());
		ConfigSerializer.save(path, fixture.schema().getCategories(), ConfigSerializer.Settings.withLiteralComments(List.of()));
		String saved = Files.readString(path);
		assertTrue(saved.contains("navigationDepth = 0..2"), saved);
		assertFalse(saved.contains("minimumNavigationDepth"), saved);
		assertFalse(saved.contains("maximumNavigationDepth"), saved);
		assertEquals(changed, create(path).value().get());

		assertTrue(new AppliedConfigValueChange<>(value, original, changed).toUndoChange().apply());
		assertEquals(original, value.getValue());
		assertEquals(List.of(changed, original), observed);
		value.set(value.getDefaultValue());
		assertEquals(new ConfigValueRange<>(1, 10), value.getValue());
		assertEquals(3, observed.size());

		remove.run();
		value.set(new ConfigValueRange<>(0, 0));
		assertEquals(3, observed.size());
		assertEquals(new ConfigValueRange<>(0, 0), fixture.value().get());
	}

	@Test
	void invalidStoredRangesFallBackToTheDefault(@TempDir Path directory) throws IOException {
		List<String> invalidRanges = List.of("5..2", "-1..3", "0..11", "1..*", "3");
		for (int i = 0; i < invalidRanges.size(); i++) {
			Path path = directory.resolve("invalid-" + i + ".ini");
			Files.writeString(path, "[navigation]\nnavigationDepth = " + invalidRanges.get(i) + "\n");
			assertEquals(new ConfigValueRange<>(1, 10), create(path).value().get());
		}
	}

	@Test
	void invalidEditsAreRejectedAndFileReloadNotifiesOnce(@TempDir Path directory) throws IOException {
		Path path = directory.resolve("gui.ini");
		Fixture fixture = create(path);
		IConfigScreenValue<ConfigValueRange<Integer>> value = IConfigScreenValue.configValue(fixture.value());
		ConfigValueRange<Integer> original = value.getValue();
		List<ConfigValueRange<Integer>> observed = new ArrayList<>();
		value.addListener(observed::add);
		assertThrows(IllegalArgumentException.class, () -> value.set(new ConfigValueRange<>(5, 2)));
		assertThrows(IllegalArgumentException.class, () -> value.set(new ConfigValueRange<>(0, 12)));
		assertEquals(original, value.getValue());
		assertTrue(observed.isEmpty());

		Files.writeString(path, "[navigation]\nnavigationDepth = 2..4\n");
		ConfigSerializer.load(path, fixture.schema().getCategories());
		assertEquals(List.of(new ConfigValueRange<>(2, 4)), observed);
		assertEquals(new ConfigValueRange<>(2, 4), fixture.value().get());
	}

	@Test
	void textFormatRoundTripsEveryRangeAndRejectsMalformedOrReversedRanges() {
		NavigationDepthRangeSerializer serializer = NavigationDepthRangeSerializer.INSTANCE;
		for (int min = 0; min <= 10; min++) {
			for (int max = min; max <= 10; max++) {
				ConfigValueRange<Integer> range = new ConfigValueRange<>(min, max);
				assertEquals(range, serializer.deserialize(serializer.serialize(range)).getResult().orElseThrow());
			}
		}
		for (String invalid : List.of("", "1", "3..2", "-1..3", "1..*", "0..11", "0..12", "1..", "a..b", "1..2..3")) {
			assertTrue(serializer.deserialize(invalid).getResult().isEmpty(), invalid);
		}
		assertEquals(new ConfigValueRange<>(2, 10), serializer.deserialize(" 2 .. 10 ").getResult().orElseThrow());
		assertEquals("1 – 10", serializer.getLocalizedValueName("depth", new ConfigValueRange<>(1, 10)).getString());
	}

	private static Fixture create(Path path) {
		ConfigFileWatcherSettings disabled = ConfigFileWatcherSettings.clientDefaults().withEnabled(false);
		ConfigSchemaBuilder builder = new ConfigSchemaBuilder(path, "test", new ConfigManager("Range Test", disabled, disabled));
		var category = builder.addCategory("navigation");
		IConfigValue<ConfigValueRange<Integer>> value = category.addValue("navigationDepth", new ConfigValueRange<>(1, 10), NavigationDepthRangeSerializer.INSTANCE)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		ConfigSchema schema = builder.build();
		return new Fixture(value, schema);
	}

	private record Fixture(IConfigValue<ConfigValueRange<Integer>> value, ConfigSchema schema) {
	}
}
