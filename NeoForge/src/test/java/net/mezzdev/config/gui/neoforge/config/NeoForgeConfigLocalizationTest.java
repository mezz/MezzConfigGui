package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.gui.ConfigValueSections;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeConfigLocalizationTest {
	@Test
	void onlyCommonFilesOptIntoSharedNavigationWithoutChangingTheirValueCategories() {
		ConfigValueSections.CategoryGroup first = NeoForgeConfigLocalization.getCategoryGroup("test", ModConfig.Type.COMMON, "test.first").orElseThrow();
		ConfigValueSections.CategoryGroup second = NeoForgeConfigLocalization.getCategoryGroup("test", ModConfig.Type.COMMON, "test.second").orElseThrow();
		assertEquals(first.name(), second.name());
		assertEquals("Common (local)", first.title().getString());
		for (ModConfig.Type type : List.of(ModConfig.Type.CLIENT, ModConfig.Type.SERVER, ModConfig.Type.STARTUP)) {
			assertTrue(NeoForgeConfigLocalization.getCategoryGroup("test", type, "test.first").isEmpty());
		}
	}

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
	void repeatedCategoryNamesUseDistinctSectionTitlesInsteadOfFileNames() {
		for (ModConfig.Type type : ModConfig.Type.values()) {
			Component first = NeoForgeConfigLocalization.getCategoryName("test.first", type);
			Component second = NeoForgeConfigLocalization.getCategoryName("test.second", type);
			List<Component> names = NeoForgeConfigLocalization.getDistinctCategoryNames(List.of(
				new NeoForgeConfigLocalization.CategoryName(first, List.of(Component.literal("Animals"))),
				new NeoForgeConfigLocalization.CategoryName(second, List.of(Component.literal("Items")))
			));
			assertEquals(first.getString() + " · Animals", names.getFirst().getString());
			assertEquals(second.getString() + " · Items", names.getLast().getString());
		}
	}

	@Test
	void singleCommonFileStillMakesItsLocalScopeVisible() {
		Component common = NeoForgeConfigLocalization.getCategoryName("test.common", ModConfig.Type.COMMON);
		assertEquals("Common (local)", common.getString());
		assertEquals("Client", NeoForgeConfigLocalization.getCategoryName("test.client", ModConfig.Type.CLIENT).getString());
		assertEquals(List.of(common), NeoForgeConfigLocalization.getDistinctCategoryNames(List.of(
			new NeoForgeConfigLocalization.CategoryName(common, List.of(Component.literal("Animals")))
		)));
	}

	@Test
	void commonSectionNamesAreSkippedAndRepeatedValuesDoNotCountAsDifferentFiles() {
		assertEquals(List.of("Common · Animals", "Common · Items"), distinctNames(
			categoryName("Common", "General", "Animals", "Animals"),
			categoryName("Common", "General", "Items")
		));
	}

	@Test
	void identicalOrMissingSectionsGetCompactNumbersInFileOrder() {
		assertEquals(List.of("Common (1)", "Common (2)", "Common (3)", "Common (4)"), distinctNames(
			categoryName("Common", "General"),
			categoryName("Common", "General"),
			categoryName("Common"),
			categoryName("Common", " ")
		));
	}

	@Test
	void distinctModProvidedTitlesArePreservedAndDoNotCompeteForSectionNames() {
		assertEquals(List.of("Animal Settings", "Common · Animals", "Common · Items"), distinctNames(
			categoryName("Animal Settings", "Animals"),
			categoryName("Common", "Animals"),
			categoryName("Common", "Items")
		));
		Component translatedSection = Component.translatable("gui.done");
		List<Component> names = NeoForgeConfigLocalization.getDistinctCategoryNames(List.of(
			new NeoForgeConfigLocalization.CategoryName(Component.literal("Client"), List.of(translatedSection)),
			categoryName("Client", "Items")
		));
		assertEquals("Client · Done", names.getFirst().getString());
	}

	@Test
	void generatedLabelsDoNotDuplicateAModProvidedTitle() {
		assertEquals(List.of("Common · Animals", "Common · Animals (1)", "Common · Items"), distinctNames(
			categoryName("Common · Animals"),
			categoryName("Common", "Animals"),
			categoryName("Common", "Items")
		));
		assertEquals(List.of("Common (1)", "Common (1) (1)", "Common (2)"), distinctNames(
			categoryName("Common (1)"),
			categoryName("Common"),
			categoryName("Common")
		));
	}

	@Test
	void broadSectionsArePreferredBeforeDistinctNestedSections() {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		ModConfigSpec.BooleanValue cow = builder.define("general.animals.cow.enabled", false);
		ModConfigSpec.BooleanValue items = builder.define("items.enabled", false);
		ModConfigSpec spec = builder.build();
		List<Component> sections = NeoForgeConfigLocalization.getSectionNames(List.of(
			NeoForgeConfigLocalization.getSections("test", spec, cow.getPath()),
			NeoForgeConfigLocalization.getSections("test", spec, items.getPath()),
			NeoForgeConfigLocalization.getSections("test", spec, cow.getPath())
		));
		assertEquals(List.of("General", "Items", "Animals", "Cow"), sections.stream().map(Component::getString).toList());
		assertEquals(List.of("Common · Items", "Common (2)"), distinctNames(
			new NeoForgeConfigLocalization.CategoryName(Component.literal("Common"), sections),
			categoryName("Common", "General")
		));
		assertEquals(List.of("Common · Animals", "Common · Blocks"), distinctNames(
			categoryName("Common", "General", "Animals"),
			categoryName("Common", "General", "Blocks")
		));
	}

	private static NeoForgeConfigLocalization.CategoryName categoryName(String title, String... sections) {
		return new NeoForgeConfigLocalization.CategoryName(Component.literal(title), List.of(sections).stream()
			.<Component>map(Component::literal)
			.toList());
	}

	private static List<String> distinctNames(NeoForgeConfigLocalization.CategoryName... categories) {
		return NeoForgeConfigLocalization.getDistinctCategoryNames(List.of(categories)).stream().map(Component::getString).toList();
	}
}
