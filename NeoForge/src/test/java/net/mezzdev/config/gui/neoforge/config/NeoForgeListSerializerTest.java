package net.mezzdev.config.gui.neoforge.config;

import com.electronwill.nightconfig.toml.TomlParser;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeListSerializerTest {
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
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		var nativeValue = builder.defineList("values", defaults, defaults::getFirst, validator);
		builder.build();
		var valueSpec = nativeValue.getSpec();
		IConfigValueSerializer<T> elementSerializer = (IConfigValueSerializer<T>) NeoForgeListElementSerializers.create(defaults, valueSpec).orElseThrow();
		NeoForgeListSerializer<T> serializer = new NeoForgeListSerializer<>(valueSpec, elementSerializer);
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
}
