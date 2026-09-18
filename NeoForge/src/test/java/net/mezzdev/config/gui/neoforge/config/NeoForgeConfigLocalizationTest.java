package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.neoforged.fml.config.ModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeConfigLocalizationTest {
	@Test
	void undeclaredRestartRequirementAddsCautionWithoutDiscardingTheModsDescription() {
		String description = NeoForgeConfigLocalization.getValueDescription("test.setting", "Controls animal behavior.", ConfigValueRestartRequirement.NONE)
			.getString();

		assertTrue(description.startsWith("Controls animal behavior.\n\n"));
		assertTrue(description.contains("A game restart may be needed"));
	}

	@Test
	void restartCautionDoesNotExposeMissingTooltipsOrAddAnEmptyParagraph() {
		String description = NeoForgeConfigLocalization.getValueDescription("test.setting", null, ConfigValueRestartRequirement.NONE)
			.getString();

		assertTrue(description.startsWith("This mod does not declare a restart requirement."));
	}

	@Test
	void explicitRestartRequirementsDoNotGetTheUncertainRestartNotice() {
		for (ConfigValueRestartRequirement requirement : new ConfigValueRestartRequirement[]{
			ConfigValueRestartRequirement.WORLD_RESTART,
			ConfigValueRestartRequirement.GAME_RESTART
			}
		) {
			assertEquals("Controls animal behavior.", NeoForgeConfigLocalization.getValueDescription("test.setting", "Controls animal behavior.", requirement)
				.getString());
			assertEquals("", NeoForgeConfigLocalization.getValueDescription("test.setting", null, requirement).getString());
		}
	}

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
