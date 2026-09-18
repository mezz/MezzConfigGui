package net.mezzdev.config.gui.model;

import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.ConfigScreenCategory;
import net.mezzdev.config.gui.ConfigScreenCategoryGroup;
import net.mezzdev.config.gui.ConfigValueSections;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.config.ConfigGuiOptionsTestUtil;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.input.InputType;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigCategoryTreeTest {
	@Test
	void settingsAppearOnceUnderTheirOwnSectionAndDirectSettingsStayAtTheParent() {
		TestValue root = value("common.toml");
		TestValue animals = value("common.toml", "Animals");
		TestValue cow = value("common.toml", "Animals", "Cow");
		TestValue pig = value("common.toml", "Animals", "Pig");
		List<ConfigCategoryTree.Node> tree = ConfigCategoryTree.create(List.of(category("common.toml", root, animals, cow, pig)));

		assertEquals(List.of("common.toml", "Animals", "Cow", "Pig"), tree.stream().map(node -> node.category().getLocalizedName().getString()).toList());
		assertEquals(List.of(-1, 0, 1, 1), tree.stream().map(ConfigCategoryTree.Node::parentIndex).toList());
		assertEquals(List.of(0, 1, 2, 2), tree.stream().map(ConfigCategoryTree.Node::depth).toList());
		assertEquals(List.of(true, true, false, false), tree.stream().map(ConfigCategoryTree.Node::hasChildren).toList());
		assertEquals(List.of(root, animals, cow, pig), tree.stream().flatMap(node -> node.category().getConfigValues().stream()).toList());
	}

	@Test
	void identicalSectionsInSeparateFilesAndAmbiguousPathSegmentsStaySeparate() {
		List<ConfigCategoryTree.Node> tree = ConfigCategoryTree.create(List.of(
			category("first.toml", value("first.toml", "Animals", "Cow"), value("first.toml", "Animals/3:Cow")),
			category("second.toml", value("second.toml", "Animals", "Cow"))
		));

		assertEquals(7, tree.size());
		assertEquals(7, tree.stream().map(node -> node.category().getName()).distinct().count());
		assertEquals(-1, tree.get(4).parentIndex());
		assertNotEquals(tree.get(2).category().getName(), tree.get(6).category().getName());
	}

	@Test
	void pluginOverridesPreserveNativeSectionsButCustomCategoriesKeepTheirChosenLayout() {
		IConfigScreenValue<Boolean> wrapped = IConfigScreenValue.withRestartRequirement(
			IConfigScreenValue.withApplyMode(value("common.toml", "Animals", "Cow"), ConfigValueApplyMode.IMMEDIATE),
			ConfigValueRestartRequirement.GAME_RESTART
		);
		List<ConfigCategoryTree.Node> nativeTree = ConfigCategoryTree.create(List.of(category("common.toml", wrapped)));
		List<ConfigCategoryTree.Node> customTree = ConfigCategoryTree.create(List.of(category("quick", wrapped)));

		assertEquals(3, nativeTree.size());
		assertEquals(List.of(wrapped), nativeTree.getLast().category().getConfigValues());
		assertEquals(1, customTree.size());
		assertEquals(List.of(wrapped), customTree.getFirst().category().getConfigValues());
		assertEquals("Animals › Cow › Remove AI", ConfigValueSections.getContextualName(wrapped, Component.literal("Remove AI")).getString());
	}

	@Test
	void collapsingTheActiveBranchSelectsItsParentAndSelectingAHiddenLeafReopensAncestors() {
		ConfigScreenModel model = animalModel();
		model.setActiveCategoryIndex(2);
		model.toggleCategoryExpanded(1);

		assertEquals(1, model.getActiveCategoryIndex());
		assertTrue(model.isCategoryVisible(1));
		assertFalse(model.isCategoryVisible(2));
		assertFalse(model.isCategoryVisible(3));
		model.toggleCategoryExpanded(0);
		assertFalse(model.isCategoryVisible(1));
		model.setActiveCategoryIndex(3);
		assertTrue(model.isCategoryExpanded(0));
		assertTrue(model.isCategoryExpanded(1));
		assertTrue(model.isCategoryVisible(3));
		assertEquals(2, model.getFirstContentCategory(0));
	}

	@Test
	void historyRestoresTheSameSubsectionWhenEarlierSectionsAreAdded() {
		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("rememberLastCategory", true)) {
			ConfigScreenModel original = animalModel();
			ConfigScreenHistory.rememberCategory("nested_test", original.getCategories().get(3));
			ConfigScreenModel changed = new ConfigScreenModel(List.of(category("common.toml",
				value("common.toml", "General"), value("common.toml", "Animals", "Cow"), value("common.toml", "Animals", "Pig")
			)));

			assertEquals(4, ConfigScreenHistory.getInitialCategoryIndex("nested_test", changed.getCategories()));
		}
	}

	@Test
	void searchFindsSectionNamesEvenWithDescriptionsDisabledAndShowsContextOnlyInSearchResults() {
		ConfigScreenModel model = animalModel();
		TestEntry cow = new TestEntry(value("common.toml", "Animals", "Cow"));
		TestEntry pig = new TestEntry(value("common.toml", "Animals", "Pig"));
		for (int index = 0; index < model.getCategories().size(); index++) {
			List<ConfigEntryWidget<?>> entries = switch (index) {
				case 2 -> List.of(cow);
				case 3 -> List.of(pig);
				default -> List.of();
			};
			model.addCategoryWidget(new ConfigCategoryWidget(model.getCategories().get(index), entries));
		}
		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("searchDescriptions", false)) {
			model.toggleCategoryExpanded(0);
			model.setSearchText("cOw");
			assertEquals(List.of(cow), model.getVisibleEntryWidgets());
			cow.setShowSectionPath(true);
			assertEquals("Animals › Cow › Remove AI", cow.getDisplayName().getString());
			model.setSearchText("");
			model.setActiveCategoryIndex(2);
			cow.setShowSectionPath(false);
			assertEquals(List.of(cow), model.getVisibleEntryWidgets());
			assertEquals("Remove AI", cow.getDisplayName().getString());
		}
	}

	@Test
	void expansionButtonHasASquareHitTargetSeparateFromSelectionAndHiddenItemsCannotBeClicked() {
		ConfigScreenModel model = animalModel();
		int[] selected = {-1};
		ConfigNavItem item = new ConfigNavItem(Component.literal("Common"), 0,
			new ConfigCategoryWidget(model.getCategories().getFirst(), List.of()),
			() -> new ImmutableRect2i(10, 10, 100, 100), index -> selected[0] = index,
			model, model::toggleCategoryExpanded);
		item.updateBounds(new ImmutableRect2i(10, 10, 100, 20), 22);

		assertTrue(item.handleUserInput(null, mouse(29, 29, InputType.SIMULATE)).isPresent());
		assertTrue(model.isCategoryExpanded(0));
		assertTrue(item.handleUserInput(null, mouse(29, 29, InputType.EXECUTE)).isPresent());
		assertFalse(model.isCategoryExpanded(0));
		assertEquals(-1, selected[0]);
		item.handleUserInput(null, mouse(30, 29, InputType.EXECUTE));
		assertEquals(0, selected[0]);
		item.resetBounds();
		assertTrue(item.handleUserInput(null, mouse(30, 29, InputType.EXECUTE)).isEmpty());
	}

	private static UserInput mouse(double x, double y, InputType type) {
		return UserInput.fromVanilla(x, y, 0, type).orElseThrow();
	}

	private static ConfigScreenModel animalModel() {
		return new ConfigScreenModel(List.of(category("common.toml", value("common.toml", "Animals", "Cow"), value("common.toml", "Animals", "Pig"))));
	}

	private static TestCategory category(String name, IConfigScreenValue<?>... values) {
		return new TestCategory(name, List.of(values));
	}

	private static TestValue value(String category, String... sections) {
		return new TestValue(category, List.of(sections).stream()
			.map(name -> new ConfigValueSections.Section(name, Component.literal(name), Component.empty()))
			.toList());
	}

	private record TestCategory(String name, List<IConfigScreenValue<?>> values) implements ConfigScreenCategory {
		@Override
		public ConfigScreenCategoryGroup getGroup() {
			return ConfigScreenCategoryGroup.LOADER_NATIVE;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getLocalizationKey() {
			return name;
		}

		@Override
		public Component getLocalizedName() {
			return Component.literal(name);
		}

		@Override
		public Component getLocalizedDescription() {
			return Component.empty();
		}

		@Override
		public Collection<? extends IConfigScreenValue<?>> getConfigValues() {
			return values;
		}
	}

	private record TestValue(String category, List<Section> sections) implements IConfigScreenValue<Boolean>, IConfigLocalizedValue, ConfigValueSections {
		@Override
		public String getSectionCategoryName() {
			return category;
		}

		@Override
		public List<Section> getSections() {
			return sections;
		}

		@Override
		public String getName() {
			return "removeAI";
		}

		@Override
		public String getLocalizationKey() {
			return "test.removeAI";
		}

		@Override
		public Component getLocalizedName() {
			return Component.literal("Remove AI");
		}

		@Override
		public Component getLocalizedDescription() {
			return Component.empty();
		}

		@Override
		public Boolean getValue() {
			return false;
		}

		@Override
		public Boolean getDefaultValue() {
			return false;
		}

		@Override
		public boolean set(Boolean value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Runnable addListener(Consumer<Boolean> listener) {
			return () -> {};
		}

		@Override
		public IConfigValueSerializer<Boolean> getSerializer() {
			throw new UnsupportedOperationException();
		}
	}

	private static class TestEntry extends ConfigEntryWidget<Boolean> {
		TestEntry(IConfigScreenValue<Boolean> value) {
			super(value, null);
		}

		@Override
		protected void drawContent(GuiGraphics guiGraphics, double mouseX, double mouseY) {

		}
	}
}
