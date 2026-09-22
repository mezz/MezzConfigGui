package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.gui.ConfigScreenCategoryNavigationGroup;
import net.mezzdev.config.gui.ConfigValueCategoryPath;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeConfigLocalizationTest {
	@Test
	void onlyCommonFilesOptIntoSharedNavigationWithoutChangingTheirValueCategories() {
		ConfigScreenCategoryNavigationGroup first = NeoForgeConfigLocalization.getNavigationGroup("test", ModConfig.Type.COMMON, "test.first");
		ConfigScreenCategoryNavigationGroup second = NeoForgeConfigLocalization.getNavigationGroup("test", ModConfig.Type.COMMON, "test.second");
		assertNotNull(first);
		assertNotNull(second);
		assertEquals(first.name(), second.name());
		assertEquals("Common (local)", first.title().getString());
		for (ModConfig.Type type : List.of(ModConfig.Type.CLIENT, ModConfig.Type.SERVER, ModConfig.Type.STARTUP)) {
			assertNull(NeoForgeConfigLocalization.getNavigationGroup("test", type, "test.first"));
		}
	}

	@Test
	void repeatedOptionNamesHaveShortLabelsAndSeparateCategoryPaths() {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		ModConfigSpec.BooleanValue cowValue = builder.define("animals.cow.behavior.removeAI", false);
		ModConfigSpec.BooleanValue pigValue = builder.define("animals.pig.behavior.removeAI", false);
		ModConfigSpec spec = builder.build();

		assertEquals("Remove Ai", getValueName(cowValue));
		assertEquals("Remove Ai", getValueName(pigValue));
		assertEquals(List.of("Animals", "Cow", "Behavior"), getCategoryTitles(spec, cowValue));
		assertEquals(List.of("Animals", "Pig", "Behavior"), getCategoryTitles(spec, pigValue));
	}

	@Test
	void nestedCategoryAndOptionTranslationsArePreserved() {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		builder.comment("Controls these settings.").translation("gui.done").push("category");
		ModConfigSpec.BooleanValue value = builder.translation("gui.cancel").define("option", false);
		ModConfigSpec spec = builder.build();

		assertEquals("Cancel", getValueName(value));
		assertEquals(List.of("Done"), getCategoryTitles(spec, value));
		assertEquals("Controls these settings.", NeoForgeConfigLocalization.getCategoryPath("test", spec, value.getPath()).get(0).description().getString());
	}

	@Test
	void topLevelOptionsDoNotHaveAnEmptyCategoryPrefix() {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		ModConfigSpec.BooleanValue value = builder.define("enabled", false);
		ModConfigSpec spec = builder.build();

		assertEquals("Enabled", getValueName(value));
		assertTrue(NeoForgeConfigLocalization.getCategoryPath("test", spec, value.getPath()).isEmpty());
	}

	private static List<String> getCategoryTitles(ModConfigSpec spec, ModConfigSpec.ConfigValue<?> value) {
		return NeoForgeConfigLocalization.getCategoryPath("test", spec, value.getPath()).stream()
			.map(ConfigValueCategoryPath.Category::title)
			.map(title -> title.getString())
			.toList();
	}

	private static String getValueName(ModConfigSpec.ConfigValue<?> value) {
		String key = NeoForgeConfigLocalization.getValueLocalizationKey("test", value.getPath(), value.getSpec().getTranslationKey());
		return NeoForgeConfigLocalization.getValueName(key, value.getPath()).getString();
	}

	@Test
	void descriptionsDoNotAddSpeculativeRestartWarnings() {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		ModConfigSpec.BooleanValue described = builder.comment("Controls animal behavior.").define("described", false);
		ModConfigSpec.BooleanValue plain = builder.define("plain", false);
		builder.build();
		assertEquals("Controls animal behavior.", NeoForgeConfigLocalization.getValueDescription("test.setting", described.getSpec()).getString());
		assertEquals("", NeoForgeConfigLocalization.getValueDescription("test.setting", plain.getSpec()).getString());
	}

	@Test
	void numericDescriptionsOmitTheRangeGeneratedByNeoForge() {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		String comment = "Controls the number of animals.";
		List<ModConfigSpec.ConfigValue<?>> values = List.of(
			builder.comment(comment).defineInRange("bounded", 0, -10, 50),
			builder.comment(comment).defineInRange("unbounded", 0, 0, Integer.MAX_VALUE),
			builder.comment(comment).defineInRange("long", 0L, 0L, Long.MAX_VALUE),
			builder.comment(comment).defineInRange("double", 0.0, 0.0, Double.MAX_VALUE)
		);
		builder.build();
		for (ModConfigSpec.ConfigValue<?> value : values) {
			ModConfigSpec.ValueSpec spec = value.getSpec();
			String originalComment = spec.getComment();
			assertTrue(originalComment.contains("\n Range: "));
			assertEquals(comment + "\n Default: " + value.getDefault(),
				NeoForgeConfigLocalization.getValueDescription("test.setting", spec).getString());
			assertEquals(originalComment, spec.getComment());
		}
	}

	@Test
	void removingGeneratedRangesPreservesAuthorCommentsAndDefaults() {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		String comment = " Range: 0 ~ 50\nSmaller ranges may improve performance.";
		ModConfigSpec.IntValue described = builder.comment(comment).defineInRange("described", 0, 0, 50);
		ModConfigSpec.IntValue plain = builder.defineInRange("plain", 0, 0, 50);
		builder.build();
		assertEquals(comment + "\n Default: 0", NeoForgeConfigLocalization.getValueDescription("test.setting", described.getSpec()).getString());
		assertEquals(" Default: 0", NeoForgeConfigLocalization.getValueDescription("test.setting", plain.getSpec()).getString());
	}

	@Test
	void descriptionsKeepRangesNotDisplayedByNumericEditors() {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		ModConfigSpec.BooleanValue manual = builder.comment(" Range: 0 ~ 50").define("manual", false);
		ModConfigSpec.ConfigValue<String> text = builder.defineInRange("text", "m", "a", "z", String.class);
		builder.build();
		assertEquals(manual.getSpec().getComment(), NeoForgeConfigLocalization.getValueDescription("test.setting", manual.getSpec()).getString());
		assertEquals(text.getSpec().getComment(), NeoForgeConfigLocalization.getValueDescription("test.setting", text.getSpec()).getString());
	}

	@Test
	void valueMetadataDistinguishesNoRestartWorldRestartAndGameRestart() {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		ModConfigSpec.BooleanValue live = builder.define("live", false);
		ModConfigSpec.BooleanValue world = builder.worldRestart().define("world", false);
		ModConfigSpec.BooleanValue game = builder.gameRestart().define("game", false);
		builder.build();
		for (ModConfig.Type type : List.of(ModConfig.Type.CLIENT, ModConfig.Type.COMMON, ModConfig.Type.SERVER)) {
			assertEquals(ConfigValueRestartRequirement.NONE, NeoForgeConfigValue.getRestartRequirement(type, live.getSpec()));
			assertEquals(ConfigValueRestartRequirement.WORLD_RESTART, NeoForgeConfigValue.getRestartRequirement(type, world.getSpec()));
		}
		for (ModConfig.Type type : List.of(ModConfig.Type.CLIENT, ModConfig.Type.COMMON)) {
			assertEquals(ConfigValueRestartRequirement.GAME_RESTART, NeoForgeConfigValue.getRestartRequirement(type, game.getSpec()));
		}
		assertEquals(ConfigValueRestartRequirement.GAME_RESTART, NeoForgeConfigValue.getRestartRequirement(ModConfig.Type.STARTUP, live.getSpec()));
	}

	@Test
	void repeatedCategoryNamesUseDistinctNestedCategoryTitlesInsteadOfFileNames() {
		for (ModConfig.Type type : ModConfig.Type.values()) {
			Component first = NeoForgeConfigLocalization.getCategoryName("test.first", type);
			Component second = NeoForgeConfigLocalization.getCategoryName("test.second", type);
			List<Component> names = NeoForgeConfigLocalization.getDistinctCategoryNames(List.of(
				new NeoForgeConfigLocalization.CategoryName(first, List.of(Component.literal("Animals"))),
				new NeoForgeConfigLocalization.CategoryName(second, List.of(Component.literal("Items")))
			));
			assertEquals(first.getString() + " · Animals", names.get(0).getString());
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
		List<Component> names = NeoForgeConfigLocalization.getDistinctCategoryNames(List.of(
			new NeoForgeConfigLocalization.CategoryName(Component.literal("Client"), List.of(translatedCategory)),
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
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		ModConfigSpec.BooleanValue cow = builder.define("general.animals.cow.enabled", false);
		ModConfigSpec.BooleanValue items = builder.define("items.enabled", false);
		ModConfigSpec spec = builder.build();
		List<Component> categories = NeoForgeConfigLocalization.getNestedCategoryNames(List.of(
			NeoForgeConfigLocalization.getCategoryPath("test", spec, cow.getPath()),
			NeoForgeConfigLocalization.getCategoryPath("test", spec, items.getPath()),
			NeoForgeConfigLocalization.getCategoryPath("test", spec, cow.getPath())
		));
		assertEquals(List.of("General", "Items", "Animals", "Cow"), categories.stream().map(Component::getString).toList());
		assertEquals(List.of("Common · Items", "Common (2)"), distinctNames(
			new NeoForgeConfigLocalization.CategoryName(Component.literal("Common"), categories),
			categoryName("Common", "General")
		));
		assertEquals(List.of("Common · Animals", "Common · Blocks"), distinctNames(
			categoryName("Common", "General", "Animals"),
			categoryName("Common", "General", "Blocks")
		));
	}

	private static NeoForgeConfigLocalization.CategoryName categoryName(String title, String... nestedCategories) {
		return new NeoForgeConfigLocalization.CategoryName(Component.literal(title), List.of(nestedCategories).stream()
			.<Component>map(Component::literal)
			.toList());
	}

	private static List<String> distinctNames(NeoForgeConfigLocalization.CategoryName... categories) {
		return NeoForgeConfigLocalization.getDistinctCategoryNames(List.of(categories)).stream().map(Component::getString).toList();
	}
}
