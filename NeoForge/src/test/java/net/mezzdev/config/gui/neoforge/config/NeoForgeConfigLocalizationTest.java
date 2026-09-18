package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.gui.ConfigValueSections;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeConfigLocalizationTest {
	@Test
	void repeatedOptionNamesHaveShortLabelsAndSeparateSectionMetadata() {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		ModConfigSpec.BooleanValue cowValue = builder.define("animals.cow.behavior.removeAI", false);
		ModConfigSpec.BooleanValue pigValue = builder.define("animals.pig.behavior.removeAI", false);
		ModConfigSpec spec = builder.build();

		assertEquals("Remove Ai", getValueName(cowValue));
		assertEquals("Remove Ai", getValueName(pigValue));
		assertEquals(List.of("Animals", "Cow", "Behavior"), getSectionTitles(spec, cowValue));
		assertEquals(List.of("Animals", "Pig", "Behavior"), getSectionTitles(spec, pigValue));
	}

	@Test
	void sectionAndOptionTranslationsArePreserved() {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		builder.comment("Controls these settings.").translation("gui.done").push("section");
		ModConfigSpec.BooleanValue value = builder.translation("gui.cancel").define("option", false);
		ModConfigSpec spec = builder.build();

		assertEquals("Cancel", getValueName(value));
		assertEquals(List.of("Done"), getSectionTitles(spec, value));
		assertEquals("Controls these settings.", NeoForgeConfigLocalization.getSections("test", spec, value.getPath()).getFirst().description().getString());
	}

	@Test
	void topLevelOptionsDoNotHaveAnEmptySectionPrefix() {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		ModConfigSpec.BooleanValue value = builder.define("enabled", false);
		ModConfigSpec spec = builder.build();

		assertEquals("Enabled", getValueName(value));
		assertTrue(NeoForgeConfigLocalization.getSections("test", spec, value.getPath()).isEmpty());
	}

	private static List<String> getSectionTitles(ModConfigSpec spec, ModConfigSpec.ConfigValue<?> value) {
		return NeoForgeConfigLocalization.getSections("test", spec, value.getPath()).stream()
			.map(ConfigValueSections.Section::title)
			.map(title -> title.getString())
			.toList();
	}

	private static String getValueName(ModConfigSpec.ConfigValue<?> value) {
		String key = NeoForgeConfigLocalization.getValueLocalizationKey("test", value.getPath(), value.getSpec().getTranslationKey());
		return NeoForgeConfigLocalization.getValueName(key, value.getPath()).getString();
	}

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
