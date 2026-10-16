package net.mezzdev.config.gui.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeConfigDiscoveryTest {
	@Test
	void findingOneSupportedValueStopsTraversalAcrossNestedSections() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		AtomicInteger checks = new AtomicInteger();
		for (int i = 0; i < 1000; i++) {
			builder.defineEnum("section" + i + ".nested.mode", TestMode.DEFAULT, value -> {
				checks.incrementAndGet();
				return value instanceof TestMode;
			});
		}
		ForgeConfigSpec spec = builder.build();
		checks.set(0);
		assertTrue(ForgeConfigScreenConfigs.hasSupportedValues(spec));
		assertEquals(1, checks.get(), "Discovery must stop at the first supported option");
	}

	@Test
	void unsupportedAndEmptySpecsDoNotProduceScreens() {
		assertFalse(ForgeConfigScreenConfigs.hasSupportedValues(new ForgeConfigSpec.Builder().build()));
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.define("nested.unsupported", 1.0F, value -> value instanceof Float);
		assertFalse(ForgeConfigScreenConfigs.hasSupportedValues(builder.build()));
	}

	@Test
	void fullTraversalStillVisitsEveryValueWithItsOwnPath() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.define("first.nested.enabled", true);
		builder.define("first.other", false);
		builder.define("second.enabled", true);
		builder.define("top", true);
		List<String> visited = new ArrayList<>();
		assertTrue(ForgeConfigScreenConfigs.visitValues(builder.build(), (path, value, valueSpec) -> {
			assertEquals(value.getPath(), path);
			visited.add(String.join(".", path));
			return true;
		}));
		assertEquals(List.of("first.nested.enabled", "first.other", "second.enabled", "top"), visited.stream().sorted().toList());
	}

	private enum TestMode {
		DEFAULT
	}
}
