package net.mezzdev.config.gui.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeScalarSerializerTest {
	@Test
	void integerParsingHonorsNativeConstraintsWithoutInventingRangeHints() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		var nativeValue = builder.defineInList("size", 4, List.of(4, 8, 16));
		ForgeConfigSpec spec = builder.build();
		ForgeConfigSpec.ValueSpec valueSpec = spec.getSpec().get(nativeValue.getPath());
		ForgeIntegerSerializer serializer = new ForgeIntegerSerializer(valueSpec, Integer.MIN_VALUE, Integer.MAX_VALUE);

		assertEquals(8, serializer.deserialize("8").getResult().orElseThrow());
		assertTrue(serializer.deserialize("7").getResult().isEmpty());
		assertFalse(serializer.isValid(7));
		assertTrue(serializer.getRange().isEmpty());
		assertFalse(serializer.getValidValuesDescription().startsWith("Any"));
	}
}
