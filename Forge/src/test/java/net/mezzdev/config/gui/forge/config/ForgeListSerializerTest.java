package net.mezzdev.config.gui.forge.config;

import com.electronwill.nightconfig.toml.TomlParser;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeListSerializerTest {
	@Test
	@SuppressWarnings("unchecked")
	void nativeEnumListsWithoutASizeLimitCanBeClearedAndRepopulated() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		List<TestMode> defaults = List.of(TestMode.BALANCED);
		var nativeValue = builder.defineListAllowEmpty("values", defaults,
			value -> value instanceof TestMode || value instanceof String);
		ForgeConfigSpec spec = builder.build();
		var valueSpec = valueSpec(spec, nativeValue);
		IConfigValueSerializer<TestMode> elementSerializer = (IConfigValueSerializer<TestMode>) ForgeListElementSerializers.create(defaults, valueSpec).orElseThrow();
		ForgeListSerializer<TestMode> serializer = new ForgeListSerializer<>(valueSpec, elementSerializer);
		var config = new TomlParser().parse("values = []");
		assertTrue(spec.isCorrect(config));
		assertTrue(serializer.isValid(serializer.normalize(config.get("values"))));
		assertEquals(List.of(), serializer.deserialize("[]").getResult().orElseThrow());
		assertTrue(serializer.isValid(List.of(TestMode.FAST)));
	}

	@Test
	@SuppressWarnings("unchecked")
	void nativeIntegerListConstraintsApplyToParsedElementsAndRangeHints() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		var nativeValue = builder.defineList("favoriteNumbers", List.of(1, 2, 3),
			value -> value instanceof Integer integer && integer >= 0 && integer <= 16);
		ForgeConfigSpec spec = builder.build();
		var valueSpec = valueSpec(spec, nativeValue);
		IConfigValueSerializer<Integer> elementSerializer = (IConfigValueSerializer<Integer>) ForgeListElementSerializers.create(List.of(1, 2, 3), valueSpec).orElseThrow();
		ForgeListSerializer<Integer> serializer = new ForgeListSerializer<>(valueSpec, elementSerializer);
		boolean acceptsFive = serializer.isValid(List.of(5));
		boolean acceptsSeventeen = serializer.isValid(List.of(17));
		String description = elementSerializer.getValidValuesDescription();

		assertTrue(acceptsFive);
		assertFalse(acceptsSeventeen);
		for (int valid : List.of(0, 5, 16)) {
			assertEquals(valid, elementSerializer.deserialize(Integer.toString(valid)).getResult().orElseThrow());
		}
		for (int invalid : List.of(-1, 17, Integer.MAX_VALUE)) {
			assertFalse(elementSerializer.isValid(invalid));
			assertTrue(elementSerializer.deserialize(Integer.toString(invalid)).getResult().isEmpty());
		}
		assertTrue(elementSerializer.getRange().isEmpty(), "An arbitrary native validator does not expose a numeric range");
		assertFalse(description.startsWith("Any"), "The element hint must not promise unrestricted input");
	}

	@Test
	void nativeTextAndNumericListParsersRejectValuesOutsideTheirElementConstraints() {
		assertParsedElementConstraints(List.of("default"), value -> value instanceof String string && !string.isBlank(), "name", "  ");
		assertParsedElementConstraints(List.of(128L), value -> value instanceof Long number && number >= 0 && number <= 1024, "1024", "1025");
		assertParsedElementConstraints(List.of(0.5D), value -> value instanceof Double number && number >= 0 && number <= 1, "1", "1.1");
	}

	private static void assertParsedElementConstraints(List<?> defaults, Predicate<Object> validator, String valid, String invalid) {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		var nativeValue = builder.defineList("values", defaults, validator);
		ForgeConfigSpec spec = builder.build();
		IConfigValueSerializer<?> serializer = ForgeListElementSerializers.create(defaults, valueSpec(spec, nativeValue)).orElseThrow();
		assertTrue(serializer.deserialize(valid).getResult().isPresent());
		assertTrue(serializer.deserialize(invalid).getResult().isEmpty());
		assertFalse(serializer.deserialize(invalid).getDiagnostics().isEmpty());
	}

	@Test
	void longListsKeepTheirEditorTypeAfterTomlLoadsSmallNumbersAsIntegers() {
		assertReloadedList(List.of(128L, 256L, 512L), value -> value instanceof Integer || value instanceof Long,
			"values = [0, 1024, 2147483648]", List.of(0L, 1024L, 2147483648L));
	}

	@Test
	void enumListsKeepTheirEditorTypeAfterTomlLoadsEnumNamesAsStrings() {
		assertReloadedList(List.of(TestMode.BALANCED), value -> value instanceof TestMode || value instanceof String,
			"values = [\"FAST\", \"BALANCED\"]", List.of(TestMode.FAST, TestMode.BALANCED));
	}

	@Test
	void stringListsPreserveSpacesCommasAndEmptyElements() {
		assertReloadedList(List.of("default"), value -> value instanceof String,
			"values = [\"  left, right  \", \"\"]", List.of("  left, right  ", ""));
	}

	@SuppressWarnings("unchecked")
	private static <T> void assertReloadedList(List<T> defaults, Predicate<Object> validator, String toml, List<T> expected) {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		var nativeValue = builder.defineList("values", defaults, validator);
		ForgeConfigSpec spec = builder.build();
		var valueSpec = valueSpec(spec, nativeValue);
		IConfigValueSerializer<T> elementSerializer = (IConfigValueSerializer<T>) ForgeListElementSerializers.create(defaults, valueSpec).orElseThrow();
		ForgeListSerializer<T> serializer = new ForgeListSerializer<>(valueSpec, elementSerializer);
		List<?> loaded = new TomlParser().parse(toml).get("values");

		assertTrue(valueSpec.test(loaded), "The persisted representation must pass the native validator");
		List<T> normalized = serializer.normalize(loaded);
		assertEquals(expected, normalized);
		assertTrue(serializer.isValid(normalized));
		for (T value : normalized) {
			assertEquals(value, elementSerializer.deserialize(elementSerializer.serialize(value)).getResult().orElseThrow());
		}
		assertEquals(defaults, serializer.normalize(defaults));
	}

	private enum TestMode {
		BALANCED,
		FAST;

		@Override
		public String toString() {
			return "Display " + name();
		}
	}
	private static ForgeConfigSpec.ValueSpec valueSpec(ForgeConfigSpec spec, ForgeConfigSpec.ConfigValue<?> value) {
		return spec.getSpec().get(value.getPath());
	}

}
