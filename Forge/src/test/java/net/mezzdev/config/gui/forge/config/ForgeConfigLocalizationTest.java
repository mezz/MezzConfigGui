package net.mezzdev.config.gui.forge.config;

import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.gui.ConfigScreenCategoryNavigationGroup;
import net.mezzdev.config.gui.ConfigValueCategoryPath;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeConfigLocalizationTest {
	@Test
	void onlyCommonFilesOptIntoSharedNavigationWithoutChangingTheirValueCategories() {
		ConfigScreenCategoryNavigationGroup first = ForgeConfigLocalization.getNavigationGroup("test", ModConfig.Type.COMMON, "test.first");
		ConfigScreenCategoryNavigationGroup second = ForgeConfigLocalization.getNavigationGroup("test", ModConfig.Type.COMMON, "test.second");
		assertNotNull(first);
		assertNotNull(second);
		assertEquals(first.name(), second.name());
		assertEquals("Common (local)", first.title().getString());
		for (ModConfig.Type type : List.of(ModConfig.Type.CLIENT, ModConfig.Type.SERVER)) {
			assertNull(ForgeConfigLocalization.getNavigationGroup("test", type, "test.first"));
		}
	}

	@Test
	void repeatedOptionNamesHaveShortLabelsAndSeparateCategoryPaths() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		ForgeConfigSpec.BooleanValue cowValue = builder.define("animals.cow.behavior.removeAI", false);
		ForgeConfigSpec.BooleanValue pigValue = builder.define("animals.pig.behavior.removeAI", false);
		ForgeConfigSpec spec = builder.build();

		assertEquals("Remove Ai", getValueName(spec, cowValue));
		assertEquals("Remove Ai", getValueName(spec, pigValue));
		assertEquals(List.of("Animals", "Cow", "Behavior"), getCategoryTitles(spec, cowValue));
		assertEquals(List.of("Animals", "Pig", "Behavior"), getCategoryTitles(spec, pigValue));
	}

	@Test
	void nestedCategoryAndOptionTranslationsArePreserved() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.comment("Controls these settings.").translation("gui.done").push("category");
		ForgeConfigSpec.BooleanValue value = builder.translation("gui.cancel").define("option", false);
		ForgeConfigSpec spec = builder.build();

		assertEquals("Cancel", getValueName(spec, value));
		assertEquals(List.of("Done"), getCategoryTitles(spec, value));
		assertEquals("Controls these settings.", ForgeConfigLocalization.getCategoryPath("test", spec, value.getPath()).get(0).description().getString());
	}

	@Test
	void topLevelOptionsDoNotHaveAnEmptyCategoryPrefix() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		ForgeConfigSpec.BooleanValue value = builder.define("enabled", false);
		ForgeConfigSpec spec = builder.build();

		assertEquals("Enabled", getValueName(spec, value));
		assertTrue(ForgeConfigLocalization.getCategoryPath("test", spec, value.getPath()).isEmpty());
	}

	private static List<String> getCategoryTitles(ForgeConfigSpec spec, ForgeConfigSpec.ConfigValue<?> value) {
		return ForgeConfigLocalization.getCategoryPath("test", spec, value.getPath()).stream()
			.map(ConfigValueCategoryPath.Category::title)
			.map(title -> title.getString())
			.toList();
	}

	private static String getValueName(ForgeConfigSpec spec, ForgeConfigSpec.ConfigValue<?> value) {
		String key = ForgeConfigLocalization.getValueLocalizationKey("test", value.getPath(), valueSpec(spec, value).getTranslationKey());
		return ForgeConfigLocalization.getValueName(key, value.getPath()).getString();
	}

	@Test
	void descriptionsDoNotAddSpeculativeRestartWarnings() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		ForgeConfigSpec.BooleanValue described = builder.comment("Controls animal behavior.").define("described", false);
		ForgeConfigSpec.BooleanValue plain = builder.define("plain", false);
		ForgeConfigSpec spec = builder.build();
		assertEquals("Controls animal behavior.", ForgeConfigLocalization.getValueDescription("test.setting", valueSpec(spec, described)).getString());
		assertEquals("", ForgeConfigLocalization.getValueDescription("test.setting", valueSpec(spec, plain)).getString());
	}

	@Test
	void numericDescriptionsOmitTheRangeGeneratedByForge() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		String comment = "Controls the number of animals.";
		List<ForgeConfigSpec.ConfigValue<?>> values = List.of(
			builder.comment(comment).defineInRange("bounded", 0, -10, 50),
			builder.comment(comment).defineInRange("unbounded", 0, 0, Integer.MAX_VALUE),
			builder.comment(comment).defineInRange("long", 0L, 0L, Long.MAX_VALUE),
			builder.comment(comment).defineInRange("double", 0.0, 0.0, Double.MAX_VALUE)
		);
		ForgeConfigSpec spec = builder.build();
		for (ForgeConfigSpec.ConfigValue<?> value : values) {
			ForgeConfigSpec.ValueSpec valueSpec = valueSpec(spec, value);
			String originalComment = valueSpec.getComment();
			assertTrue(originalComment.contains("\nRange: "));
			assertEquals(comment,
				ForgeConfigLocalization.getValueDescription("test.setting", valueSpec).getString());
			assertEquals(originalComment, valueSpec.getComment());
		}
	}

	@Test
	void removingGeneratedRangesPreservesAuthorCommentsAndDefaults() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		String comment = " Range: 0 ~ 50\nSmaller ranges may improve performance.";
		ForgeConfigSpec.IntValue described = builder.comment(comment).defineInRange("described", 0, 0, 50);
		ForgeConfigSpec.IntValue plain = builder.defineInRange("plain", 0, 0, 50);
		ForgeConfigSpec spec = builder.build();
		assertEquals(comment, ForgeConfigLocalization.getValueDescription("test.setting", valueSpec(spec, described)).getString());
		assertEquals("", ForgeConfigLocalization.getValueDescription("test.setting", valueSpec(spec, plain)).getString());
	}

	@Test
	void descriptionsKeepRangesNotDisplayedByNumericEditors() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		ForgeConfigSpec.BooleanValue manual = builder.comment(" Range: 0 ~ 50").define("manual", false);
		ForgeConfigSpec.ConfigValue<String> text = builder.defineInRange("text", "m", "a", "z", String.class);
		ForgeConfigSpec spec = builder.build();
		assertEquals(valueSpec(spec, manual).getComment(), ForgeConfigLocalization.getValueDescription("test.setting", valueSpec(spec, manual)).getString());
		assertEquals(valueSpec(spec, text).getComment(), ForgeConfigLocalization.getValueDescription("test.setting", valueSpec(spec, text)).getString());
	}

	@Test
	void valueMetadataPreservesWorldRestartRequirements() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		ForgeConfigSpec.BooleanValue live = builder.define("live", false);
		ForgeConfigSpec.BooleanValue world = builder.worldRestart().define("world", false);
		ForgeConfigSpec spec = builder.build();
		assertEquals(ConfigValueRestartRequirement.NONE, ForgeConfigValue.getRestartRequirement(valueSpec(spec, live)));
		assertEquals(ConfigValueRestartRequirement.WORLD_RESTART, ForgeConfigValue.getRestartRequirement(valueSpec(spec, world)));
	}

	@Test
	void repeatedCategoryNamesUseDistinctNestedCategoryTitlesInsteadOfFileNames() {
		for (ModConfig.Type type : ModConfig.Type.values()) {
			Component first = ForgeConfigLocalization.getCategoryName("test.first", type);
			Component second = ForgeConfigLocalization.getCategoryName("test.second", type);
			List<Component> names = ForgeConfigLocalization.getDistinctCategoryNames(List.of(
				new ForgeConfigLocalization.CategoryName(first, List.of(Component.literal("Animals"))),
				new ForgeConfigLocalization.CategoryName(second, List.of(Component.literal("Items")))
			));
			assertEquals(first.getString() + " · Animals", names.get(0).getString());
			assertEquals(second.getString() + " · Items", names.get(names.size() - 1).getString());
		}
	}

	@Test
	void singleCommonFileStillMakesItsLocalScopeVisible() {
		Component common = ForgeConfigLocalization.getCategoryName("test.common", ModConfig.Type.COMMON);
		assertEquals("Common (local)", common.getString());
		assertEquals("Client", ForgeConfigLocalization.getCategoryName("test.client", ModConfig.Type.CLIENT).getString());
		assertEquals(List.of(common), ForgeConfigLocalization.getDistinctCategoryNames(List.of(
			new ForgeConfigLocalization.CategoryName(common, List.of(Component.literal("Animals")))
		)));
	}

	@Test
	void commonNestedCategoryNamesAreSkippedAndRepeatedValuesDoNotCountAsDifferentFiles() {
		assertEquals(List.of("Common · Animals", "Common · Items"), distinctNames(
			categoryName("Common", "General", "Animals", "Animals"),
			categoryName("Common", "General", "Items")
		));
	}

	@Test
	void identicalOrMissingNestedCategoriesGetCompactNumbersInFileOrder() {
		assertEquals(List.of("Common (1)", "Common (2)", "Common (3)", "Common (4)"), distinctNames(
			categoryName("Common", "General"),
			categoryName("Common", "General"),
			categoryName("Common"),
			categoryName("Common", " ")
		));
	}

	@Test
	void distinctModProvidedTitlesArePreservedAndDoNotCompeteForNestedCategoryNames() {
		assertEquals(List.of("Animal Settings", "Common · Animals", "Common · Items"), distinctNames(
			categoryName("Animal Settings", "Animals"),
			categoryName("Common", "Animals"),
			categoryName("Common", "Items")
		));
		Component translatedCategory = Component.translatable("gui.done");
		List<Component> names = ForgeConfigLocalization.getDistinctCategoryNames(List.of(
			new ForgeConfigLocalization.CategoryName(Component.literal("Client"), List.of(translatedCategory)),
			categoryName("Client", "Items")
		));
		assertEquals("Client · Done", names.get(0).getString());
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
	void broadCategoriesArePreferredBeforeDistinctNestedCategories() {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		ForgeConfigSpec.BooleanValue cow = builder.define("general.animals.cow.enabled", false);
		ForgeConfigSpec.BooleanValue items = builder.define("items.enabled", false);
		ForgeConfigSpec spec = builder.build();
		List<Component> categories = ForgeConfigLocalization.getNestedCategoryNames(List.of(
			ForgeConfigLocalization.getCategoryPath("test", spec, cow.getPath()),
			ForgeConfigLocalization.getCategoryPath("test", spec, items.getPath()),
			ForgeConfigLocalization.getCategoryPath("test", spec, cow.getPath())
		));
		assertEquals(List.of("General", "Items", "Animals", "Cow"), categories.stream().map(Component::getString).toList());
		assertEquals(List.of("Common · Items", "Common (2)"), distinctNames(
			new ForgeConfigLocalization.CategoryName(Component.literal("Common"), categories),
			categoryName("Common", "General")
		));
		assertEquals(List.of("Common · Animals", "Common · Blocks"), distinctNames(
			categoryName("Common", "General", "Animals"),
			categoryName("Common", "General", "Blocks")
		));
	}

	private static ForgeConfigLocalization.CategoryName categoryName(String title, String... nestedCategories) {
		return new ForgeConfigLocalization.CategoryName(Component.literal(title), List.of(nestedCategories).stream()
			.<Component>map(Component::literal)
			.toList());
	}

	private static List<String> distinctNames(ForgeConfigLocalization.CategoryName... categories) {
		return ForgeConfigLocalization.getDistinctCategoryNames(List.of(categories)).stream().map(Component::getString).toList();
	}
	private static ForgeConfigSpec.ValueSpec valueSpec(ForgeConfigSpec spec, ForgeConfigSpec.ConfigValue<?> value) {
		return spec.getSpec().get(value.getPath());
	}

}
