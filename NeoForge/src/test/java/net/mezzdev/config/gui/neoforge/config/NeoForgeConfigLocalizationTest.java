package net.mezzdev.config.gui.neoforge.config;

import net.neoforged.fml.config.ModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class NeoForgeConfigLocalizationTest {
	@Test
	void multipleFilesOfTheSameTypeHaveDistinctVisibleNames() {
		for (ModConfig.Type type : ModConfig.Type.values()) {
			String first = NeoForgeConfigLocalization.getCategoryName("test.first", type, "animals/settings.toml", true).getString();
			String second = NeoForgeConfigLocalization.getCategoryName("test.second", type, "items/settings.toml", true).getString();

			assertNotEquals(first, second);
			assertEquals("animals/settings.toml", first.substring(first.indexOf('\n') + 1));
			assertEquals("items/settings.toml", second.substring(second.indexOf('\n') + 1));
		}
	}

	@Test
	void singleCommonFileStillMakesItsLocalScopeVisible() {
		assertEquals("Common (local)", NeoForgeConfigLocalization.getCategoryName("test.common", ModConfig.Type.COMMON, "test-common.toml", false).getString());
		assertEquals("Client", NeoForgeConfigLocalization.getCategoryName("test.client", ModConfig.Type.CLIENT, "test-client.toml", false).getString());
	}
}
