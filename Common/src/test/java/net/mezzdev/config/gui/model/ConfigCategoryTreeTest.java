package net.mezzdev.config.gui.model;

import com.google.common.collect.Iterables;
import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.ConfigScreenCategory;
import net.mezzdev.config.gui.ConfigScreenCategoryGroup;
import net.mezzdev.config.gui.ConfigScreenCategoryNavigationGroup;
import net.mezzdev.config.gui.ConfigValueCategoryPath;
import net.mezzdev.config.gui.ConfigScreenSchema;
import net.mezzdev.config.gui.ConfigValueAccess;
import net.mezzdev.config.gui.info.ConfigServerInfo;
import net.mezzdev.config.gui.info.ServerConfigAccess;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.config.ConfigGuiOptionsTestUtil;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.input.InputType;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigCategoryTreeTest {
	@Test
	void serverAccessSurvivesWrappersAndRemainsLiveOnParentsSectionsAndValues() {
		AtomicReference<ServerConfigAccess> state = new AtomicReference<>(ServerConfigAccess.LOCAL);
		AtomicInteger reads = new AtomicInteger();
		Supplier<ServerConfigAccess> provider = () -> {
			reads.incrementAndGet();
			return state.get();
		};
		TestValue cow = value("server.toml", "Animals", "Cow");
		TestValue pig = value("server.toml", "Animals", "Pig");
		IConfigScreenValue<Boolean> wrappedCow = IConfigScreenValue.withApplyMode(
			new TestValue(cow.category(), cow.categories(), provider), ConfigValueApplyMode.ON_APPLY);
		IConfigScreenValue<Boolean> wrappedPig = IConfigScreenValue.withApplyMode(
			new TestValue(pig.category(), pig.categories(), provider), ConfigValueApplyMode.ON_APPLY);
		ConfigScreenSchema schema = () -> List.of(category("server.toml", wrappedCow, wrappedPig));
		ConfigScreenModel model = navigationModel(List.copyOf(schema.getCategories()));
		ConfigServerInfo info = new ConfigServerInfo(schema);
		var descriptions = info.forValues(model.getCategoryIndexes(0).stream()
			.flatMap(index -> model.getCategories().get(index).getConfigValues().stream()));
		assertTrue(model.getCategories().get(0).getConfigValues().isEmpty());
		ConfigCategoryWidget parent = new ConfigCategoryWidget(model.getCategories().get(0), List.of(), List.of(), () -> {}, descriptions);
		assertEquals(ServerConfigAccess.LOCAL.getDescription(), parent.getInfo().lines().get(0));
		assertEquals(1, reads.get());

		state.set(ServerConfigAccess.READ_ONLY);
		assertEquals(ServerConfigAccess.READ_ONLY.getDescription(), parent.getCategoryHeader().getInfo().lines().get(0));
		TestEntry entry = new TestEntry(wrappedCow);
		entry.setAccessDescriptions(info.forValues(Stream.of(wrappedCow)));
		assertEquals(ServerConfigAccess.READ_ONLY.getDescription(), entry.getInfoWithAccess(0, 0).lines().get(0));
		assertFalse(entry.isEditable());
	}

	@Test
	void compatibleNativeFilesShareOneRootButKeepTheirValuesAndStableSubsectionIdentities() {
		ConfigScreenCategoryNavigationGroup group = new ConfigScreenCategoryNavigationGroup("common", Component.literal("Common (local)"), Component.literal("Local settings"));
		TestCategory animals = groupedCategory("animals.toml", group, "Animals");
		TestCategory items = groupedCategory("items.toml", group, "Items");
		List<ConfigCategoryTree.Node> tree = ConfigCategoryTree.create(List.of(animals, items), 0, 1, 10);
		assertEquals(List.of("Common (local)", "Animals", "Items"), tree.stream().map(node -> node.category().getLocalizedName().getString()).toList());
		assertEquals(List.of(-1, 0, 0), tree.stream().map(ConfigCategoryTree.Node::parentIndex).toList());
		assertEquals(Iterables.getLast(ConfigCategoryTree.create(List.of(animals), 0, 1, 10)).category().getName(), tree.get(1).category().getName());
		assertEquals(Iterables.getLast(ConfigCategoryTree.create(List.of(items), 0, 1, 10)).category().getName(), tree.get(2).category().getName());
		assertEquals(animals.values(), tree.get(1).category().getConfigValues());
		assertEquals(items.values(), tree.get(2).category().getConfigValues());
		assertEquals("File: animals.toml", tree.get(1).category().getLocalizedDescription().getString());
		assertEquals("File: items.toml", tree.get(2).category().getLocalizedDescription().getString());

		assertEquals(tree, ConfigCategoryTree.create(List.of(animals, items), 10, 1, 10));
	}

	@Test
	void groupedFilesWithIdenticalSectionsOrDirectValuesRemainDistinguishable() {
		ConfigScreenCategoryNavigationGroup group = new ConfigScreenCategoryNavigationGroup("common", Component.literal("Common (local)"), Component.empty());
		List<ConfigCategoryTree.Node> tree = ConfigCategoryTree.create(List.of(
			groupedCategory("first.toml", group, "General"), groupedCategory("second.toml", group, "General"),
			groupedCategory("third.toml", group), groupedCategory("fourth.toml", group)
		), 0, 1, 10);
		assertEquals(List.of("Common (local)", "General (1)", "General (2)", "Settings (1)", "Settings (2)"), tree.stream().map(node -> node.category().getLocalizedName().getString()).toList());
		assertEquals(4, tree.stream().mapToInt(node -> node.category().getConfigValues().size()).sum());
		assertEquals(5, tree.stream().map(node -> node.category().getName()).distinct().count());
	}

	@Test
	void pluginsCanRelocateOrRenameNativeCategoriesWithoutBeingRegrouped() {
		ConfigScreenCategoryNavigationGroup group = new ConfigScreenCategoryNavigationGroup("common", Component.literal("Common (local)"), Component.empty());
		TestCategory first = groupedCategory("first.toml", group, "Animals");
		TestCategory second = groupedCategory("second.toml", group, "Items");
		TestCategory renamed = new TestCategory(second.name(), second.values(), Component.literal("Quick Settings"), second.navigationGroup());
		List<ConfigCategoryTree.Node> tree = ConfigCategoryTree.create(List.of(first, renamed), 10, 1, 10);
		assertEquals(List.of("Common (local)", "Animals", "Quick Settings", "Items"), tree.stream().map(node -> node.category().getLocalizedName().getString()).toList());
		TestCategory relocated = new TestCategory("quick", second.values(), group.title());
		tree = ConfigCategoryTree.create(List.of(first, relocated), 10, 1, 10);
		assertEquals(3, tree.size());
		assertTrue(Iterables.getLast(tree).inlineSections().isEmpty());
	}

	private static TestCategory groupedCategory(String file, ConfigScreenCategoryNavigationGroup group, String... sections) {
		TestValue original = value(file, sections);
		return new TestCategory(file, List.of(original), group.title(), group);
	}

	@Test
	void redundantSameNameOnlyChildrenCollapseIntoOneSelectableSection() {
		for (int limit : List.of(0, 10)) {
			TestValue value = value("General", "General", "General");
			List<ConfigCategoryTree.Node> tree = ConfigCategoryTree.create(List.of(category("General", value)), limit, 1, 10);
			assertEquals(1, tree.size());
			assertEquals("General", tree.get(0).category().getLocalizedName().getString());
			assertEquals("General", tree.get(0).category().getName());
			assertFalse(tree.get(0).hasChildren());
			assertTrue(tree.get(0).inlineSections().isEmpty());
			assertEquals(List.of(value), tree.get(0).category().getConfigValues());
		}
	}

	@Test
	void aDifferentOnlyChildKeepsItsSectionNameAndDuplicateNamesWithOwnValuesStaySeparate() {
		List<ConfigCategoryTree.Node> tree = ConfigCategoryTree.create(List.of(category("General", value("General", "Animals"))), 0, 1, 10);
		assertEquals(List.of("General", "Animals"), tree.stream().map(node -> node.category().getLocalizedName().getString()).toList());
		tree = ConfigCategoryTree.create(List.of(category("General", value("General"), value("General", "General"))), 0, 1, 10);
		assertEquals(List.of("General", "Settings", "General"), tree.stream().map(node -> node.category().getLocalizedName().getString()).toList());
		assertEquals(2, tree.stream().mapToInt(node -> node.category().getConfigValues().size()).sum());
	}

	@Test
	void defaultLimitKeepsFirstLevelSectionsAndGroupsSmallDeeperSectionsWithoutLosingValues() {
		List<IConfigScreenValue<?>> values = new ArrayList<>();
		TestValue direct = value("common.toml");
		values.add(direct);
		values.addAll(values(11, "common.toml", "Nested", "Large"));
		values.addAll(values(10, "common.toml", "Nested", "Small"));
		values.add(value("common.toml", "Nested", "Tiny"));
		values.add(value("common.toml", "First-level"));
		ConfigScreenModel model = new ConfigScreenModel(List.of(new TestCategory("common.toml", values)));

		assertEquals(1, ConfigGuiOptions.getMinimumNavigationDepth());
		assertEquals(10, ConfigGuiOptions.getInlineSubsectionLimit());
		assertEquals(List.of("common.toml", "Settings", "Nested", "Settings", "Large", "First-level"), model.getCategories().stream().map(category -> category.getLocalizedName().getString()).toList());
		assertTrue(model.getCategories().get(0).getConfigValues().isEmpty());
		assertEquals(1, model.getCategories().get(1).getConfigValues().size());
		assertEquals(11, model.getCategories().get(3).getConfigValues().size());
		assertEquals(11, model.getCategories().get(4).getConfigValues().size());
		assertEquals(1, Iterables.getLast(model.getCategories()).getConfigValues().size());
		assertEquals(List.of("Small", "Tiny"), model.getInlineSections(3).stream().map(section -> section.title().getString()).toList());
		assertEquals(List.of(0, 10), model.getInlineSections(3).stream().map(ConfigCategoryWidget.Section::firstEntryIndex).toList());
		assertEquals(direct, model.getCategories().get(1).getConfigValues().iterator().next());
		var displayedValues = Collections.newSetFromMap(new IdentityHashMap<IConfigScreenValue<?>, Boolean>());
		model.getCategories().forEach(category -> category.getConfigValues().forEach(value -> assertTrue(displayedValues.add(value))));
		assertEquals(values.size(), displayedValues.size());
		assertTrue(displayedValues.containsAll(values));
	}

	@Test
	void zeroNavigationDepthAllowsSmallFirstLevelSectionsToBeInlined() {
		List<IConfigScreenValue<?>> values = new ArrayList<>();
		TestValue small = value("common.toml", "Small");
		values.add(small);
		values.addAll(values(11, "common.toml", "Large"));
		values.add(value("common.toml", "Nested", "Child"));
		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("navigationDepth", new ConfigValueRange<>(0, 10))) {
			ConfigScreenModel model = new ConfigScreenModel(List.of(new TestCategory("common.toml", values)));

			assertEquals(List.of("common.toml", "Settings", "Large", "Nested"), model.getCategories().stream().map(category -> category.getLocalizedName().getString()).toList());
			assertEquals(List.of(small), model.getCategories().get(1).getConfigValues());
			assertEquals(List.of("Small"), model.getInlineSections(1).stream().map(section -> section.title().getString()).toList());
			assertEquals(List.of("Child"), model.getInlineSections(3).stream().map(section -> section.title().getString()).toList());
			assertEquals(values, model.getCategories().stream().flatMap(category -> category.getConfigValues().stream()).toList());
		}
	}

	@Test
	void configuredNavigationDepthKeepsRequestedLevelsAndInlinesDeeperSections() {
		List<IConfigScreenValue<?>> values = List.of(
			value("common.toml"),
			value("common.toml", "First-level"),
			value("common.toml", "Nested", "Second-level"),
			value("common.toml", "Nested", "Deep", "Third-level")
		);
		List<ConfigScreenCategory> categories = List.of(new TestCategory("common.toml", values));
		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("navigationDepth", new ConfigValueRange<>(2, 10))) {
			ConfigScreenModel model = new ConfigScreenModel(categories);

			assertEquals(List.of("common.toml", "Settings", "First-level", "Nested", "Second-level", "Deep"), model.getCategories().stream().map(category -> category.getLocalizedName().getString()).toList());
			assertEquals(2, model.getCategoryDepth(4));
			assertTrue(model.getInlineSections(4).isEmpty());
			assertEquals(2, model.getCategoryDepth(5));
			assertEquals(List.of("Third-level"), model.getInlineSections(5).stream().map(section -> section.title().getString()).toList());
			assertEquals(values, model.getCategories().stream().flatMap(category -> category.getConfigValues().stream()).toList());
		}
		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("navigationDepth", new ConfigValueRange<>(10, 10))) {
			ConfigScreenModel model = new ConfigScreenModel(categories);

			assertEquals(List.of("common.toml", "Settings", "First-level", "Nested", "Second-level", "Deep", "Third-level"), model.getCategories().stream().map(category -> category.getLocalizedName().getString()).toList());
			assertEquals(3, model.getCategoryDepth(6));
			for (int i = 0; i < model.getCategories().size(); i++) {
				assertTrue(model.getInlineSections(i).isEmpty());
			}
			assertEquals(values, model.getCategories().stream().flatMap(category -> category.getConfigValues().stream()).toList());
		}
	}

	@Test
	void fixedNavigationDepthGroupsWholeBranchesEvenWhenSmallSectionGroupingIsOff() {
		List<IConfigScreenValue<?>> values = new ArrayList<>();
		values.add(value("common.toml"));
		values.add(value("common.toml", "Animals"));
		values.addAll(values(11, "common.toml", "Animals", "Cows", "Traits"));
		values.add(value("common.toml", "Animals", "Pigs", "Traits"));
		try (ConfigGuiOptionsTestUtil.OptionOverride ignoredDepth = ConfigGuiOptionsTestUtil.setValue("navigationDepth", new ConfigValueRange<>(1, 1));
			ConfigGuiOptionsTestUtil.OptionOverride ignoredSize = ConfigGuiOptionsTestUtil.setValue("inlineSubsectionLimit", 0)
		) {
			ConfigScreenModel model = new ConfigScreenModel(List.of(new TestCategory("common.toml", values)));
			assertEquals(List.of("common.toml", "Settings", "Animals"), model.getCategories().stream().map(category -> category.getLocalizedName().getString()).toList());
			assertEquals(1, model.getCategoryDepth(2));
			assertFalse(model.hasSubcategories(2));
			assertEquals(List.of("Cows › Traits", "Pigs › Traits"), model.getInlineSections(2).stream().map(section -> section.title().getString()).toList());
			assertEquals(List.of(1, 12), model.getInlineSections(2).stream().map(ConfigCategoryWidget.Section::firstEntryIndex).toList());
			var displayedValues = Collections.newSetFromMap(new IdentityHashMap<IConfigScreenValue<?>, Boolean>());
			model.getCategories().forEach(category -> category.getConfigValues().forEach(value -> assertTrue(displayedValues.add(value))));
			assertEquals(values.size(), displayedValues.size());
			assertTrue(displayedValues.containsAll(values));
		}
	}

	@Test
	void zeroMaximumGroupsAtTheRootAndLargerMaximumPreservesMoreNavigation() {
		List<IConfigScreenValue<?>> values = List.of(
			value("common.toml", "Animals", "Cows", "Traits"),
			value("common.toml", "Animals", "Pigs")
		);
		List<ConfigScreenCategory> categories = List.of(new TestCategory("common.toml", values));
		List<ConfigCategoryTree.Node> flat = ConfigCategoryTree.create(categories, 0, 0, 0);
		assertEquals(1, flat.size());
		assertEquals(values, flat.get(0).category().getConfigValues());
		assertEquals(List.of("Animals › Cows › Traits", "Animals › Pigs"), flat.get(0).inlineSections().stream().map(section -> section.title().getString()).toList());

		List<ConfigCategoryTree.Node> capped = ConfigCategoryTree.create(categories, 0, 2, 2);
		assertEquals(List.of("common.toml", "Animals", "Cows", "Pigs"), capped.stream().map(node -> node.category().getLocalizedName().getString()).toList());
		assertEquals(List.of(0, 1, 2, 2), capped.stream().map(ConfigCategoryTree.Node::depth).toList());
		assertEquals("Traits", capped.get(2).inlineSections().get(0).title().getString());
		assertEquals(values, capped.stream().flatMap(node -> node.category().getConfigValues().stream()).toList());

		List<ConfigCategoryTree.Node> deeper = ConfigCategoryTree.create(categories, 0, 2, 10);
		assertEquals(List.of("common.toml", "Animals", "Cows", "Traits", "Pigs"), deeper.stream().map(node -> node.category().getLocalizedName().getString()).toList());
		assertTrue(deeper.stream().allMatch(node -> node.inlineSections().isEmpty()));
	}

	@Test
	void defaultMaximumKeepsTheSidebarWithinTenLevels() {
		String[] sections = new String[11];
		for (int i = 0; i < sections.length; i++) {
			sections[i] = "Level " + (i + 1);
		}
		TestValue value = value("common.toml", sections);
		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("inlineSubsectionLimit", 0)) {
			ConfigScreenModel model = new ConfigScreenModel(List.of(category("common.toml", value)));
			assertEquals(11, model.getCategories().size());
			assertEquals(10, model.getCategoryDepth(10));
			assertEquals(List.of(value), model.getCategories().get(10).getConfigValues());
			assertEquals("Level 11", model.getInlineSections(10).get(0).title().getString());
		}
	}

	@Test
	void cappedBranchesKeepDescriptionsAndSearchContextForWrappedValues() {
		TestValue nativeValue = new TestValue("common.toml", List.of(
			new ConfigValueCategoryPath.Category("animals", Component.literal("Animals"), Component.empty()),
			new ConfigValueCategoryPath.Category("mammals", Component.literal("Mammals"), Component.literal("Mammal settings.")),
			new ConfigValueCategoryPath.Category("cows", Component.literal("Cows"), Component.literal("Cow settings."))
		));
		IConfigScreenValue<Boolean> wrapped = IConfigScreenValue.withApplyMode(nativeValue, ConfigValueApplyMode.IMMEDIATE);
		try (ConfigGuiOptionsTestUtil.OptionOverride ignoredMax = ConfigGuiOptionsTestUtil.setValue("navigationDepth", new ConfigValueRange<>(1, 1));
			ConfigGuiOptionsTestUtil.OptionOverride ignoredSearch = ConfigGuiOptionsTestUtil.setValue("searchDescriptions", false)
		) {
			ConfigScreenModel model = new ConfigScreenModel(List.of(category("common.toml", wrapped)));
			TestEntry entry = new TestEntry(wrapped);
			model.addCategoryWidget(new ConfigCategoryWidget(model.getCategories().get(0), List.of()));
			ConfigCategoryWidget widget = new ConfigCategoryWidget(model.getCategories().get(1), List.of(entry), model.getInlineSections(1));
			model.addCategoryWidget(widget);
			ConfigSectionHeader header = widget.getSectionHeader(0);
			assertEquals("Mammals › Cows", header.getInfo().title().getString());
			assertEquals(List.of("Mammal settings.\n\nCow settings."), header.getInfo().lines().stream().map(Component::getString).toList());
			model.setSearchText("cows");
			assertEquals(List.of(entry), model.getVisibleEntryWidgets());
			entry.setShowSectionPath(true);
			assertEquals("Animals › Mammals › Cows › Remove AI", entry.getDisplayName().getString());
		}
	}

	@Test
	void branchesRemainInNavigationEvenWhenAllTheirChildrenBecomeInlineGroups() {
		List<ConfigCategoryTree.Node> tree = ConfigCategoryTree.create(List.of(category("common.toml",
			value("common.toml", "Animals"), value("common.toml", "Animals", "Cow"), value("common.toml", "Animals", "Pig")
		)), 10, 1, 10);

		assertEquals(List.of("common.toml", "Animals"), tree.stream().map(node -> node.category().getLocalizedName().getString()).toList());
		assertEquals(List.of(-1, 0), tree.stream().map(ConfigCategoryTree.Node::parentIndex).toList());
		assertEquals(List.of(0, 1), tree.stream().map(ConfigCategoryTree.Node::depth).toList());
		assertTrue(tree.get(0).hasChildren());
		assertFalse(Iterables.getLast(tree).hasChildren());
		assertTrue(tree.get(0).inlineSections().isEmpty());
		assertEquals(List.of("Cow", "Pig"), Iterables.getLast(tree).inlineSections().stream().map(section -> section.title().getString()).toList());
		assertEquals(List.of(1, 2), Iterables.getLast(tree).inlineSections().stream().map(ConfigCategoryWidget.Section::firstEntryIndex).toList());
	}

	@Test
	void configuredLimitAndZeroControlGroupingWithoutMergingFilesOrCustomCategories() {
		List<ConfigScreenCategory> categories = List.of(
			new TestCategory("first.toml", values(3, "first.toml", "Shared", "Options")),
			category("second.toml", value("second.toml", "Shared", "Options")),
			category("quick", value("first.toml", "Shared"))
		);
		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("inlineSubsectionLimit", 2)) {
			ConfigScreenModel model = new ConfigScreenModel(categories);
			assertEquals(List.of("first.toml", "Shared", "Options", "second.toml", "Shared", "quick"), model.getCategories().stream().map(category -> category.getLocalizedName().getString()).toList());
			assertTrue(model.getInlineSections(0).isEmpty());
			assertEquals("Options", model.getInlineSections(4).get(0).title().getString());
			assertTrue(model.getInlineSections(5).isEmpty());
		}
		try (ConfigGuiOptionsTestUtil.OptionOverride ignoredLimit = ConfigGuiOptionsTestUtil.setValue("inlineSubsectionLimit", 0);
			ConfigGuiOptionsTestUtil.OptionOverride ignoredDepth = ConfigGuiOptionsTestUtil.setValue("navigationDepth", new ConfigValueRange<>(0, 10))
		) {
			ConfigScreenModel model = new ConfigScreenModel(categories);
			assertEquals(7, model.getCategories().size());
			for (int i = 0; i < model.getCategories().size(); i++) {
				assertTrue(model.getInlineSections(i).isEmpty());
			}
		}
	}

	@Test
	void inlineGroupsPreserveDescriptionsAndSearchContextForWrappedValues() {
		Component title = Component.literal("Cows");
		Component description = Component.literal("Settings that only affect cows.");
		TestValue nativeValue = new TestValue("common.toml", List.of(
			new ConfigValueCategoryPath.Category("animals", Component.literal("Animals"), Component.empty()),
			new ConfigValueCategoryPath.Category("cow", title, description)
		));
		IConfigScreenValue<Boolean> wrapped = IConfigScreenValue.withApplyMode(nativeValue, ConfigValueApplyMode.IMMEDIATE);
		ConfigScreenModel model = new ConfigScreenModel(List.of(category("common.toml", wrapped)));
		TestEntry entry = new TestEntry(wrapped);
		model.addCategoryWidget(new ConfigCategoryWidget(model.getCategories().get(0), List.of()));
		ConfigCategoryWidget widget = new ConfigCategoryWidget(model.getCategories().get(1), List.of(entry), model.getInlineSections(1));
		model.addCategoryWidget(widget);

		assertFalse(model.hasSubcategories(1));
		assertEquals(1, model.getFirstContentCategory(1));
		ConfigSectionHeader header = widget.getSectionHeader(0);
		assertEquals(title, header.getInfo().title());
		assertEquals(List.of(description), header.getInfo().lines());
		assertEquals(List.of(wrapped), model.getCategories().get(1).getConfigValues());
		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("searchDescriptions", false)) {
			model.setSearchText("cows");
			assertEquals(List.of(entry), model.getVisibleEntryWidgets());
			entry.setShowSectionPath(true);
			assertEquals("Animals › Cows › Remove AI", entry.getDisplayName().getString());
			model.setSearchText("");
			entry.setShowSectionPath(false);
			assertEquals(List.of(entry), model.getVisibleEntryWidgets());
			assertEquals("Remove AI", entry.getDisplayName().getString());
		}
	}

	private static List<IConfigScreenValue<?>> values(int count, String category, String... sections) {
		List<IConfigScreenValue<?>> values = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			values.add(value(category, sections));
		}
		return values;
	}

	@Test
	void branchSettingsAppearOnceInTheirOwnLeafAndBranchesContainNoValues() {
		TestValue root = value("common.toml");
		TestValue animals = value("common.toml", "Animals");
		TestValue cow = value("common.toml", "Animals", "Cow");
		TestValue pig = value("common.toml", "Animals", "Pig");
		List<ConfigCategoryTree.Node> tree = ConfigCategoryTree.create(List.of(category("common.toml", root, animals, cow, pig)), 0, 1, 10);

		assertEquals(List.of("common.toml", "Settings", "Animals", "Settings", "Cow", "Pig"), tree.stream().map(node -> node.category().getLocalizedName().getString()).toList());
		assertEquals(List.of(-1, 0, 0, 2, 2, 2), tree.stream().map(ConfigCategoryTree.Node::parentIndex).toList());
		assertEquals(List.of(0, 1, 1, 2, 2, 2), tree.stream().map(ConfigCategoryTree.Node::depth).toList());
		assertEquals(List.of(true, false, true, false, false, false), tree.stream().map(ConfigCategoryTree.Node::hasChildren).toList());
		assertTrue(tree.stream().filter(ConfigCategoryTree.Node::hasChildren).allMatch(node -> node.category().getConfigValues().isEmpty()));
		assertEquals(List.of(root, animals, cow, pig), tree.stream().flatMap(node -> node.category().getConfigValues().stream()).toList());
	}

	@Test
	void identicalSectionsInSeparateFilesAndAmbiguousPathSegmentsStaySeparate() {
		List<ConfigCategoryTree.Node> tree = ConfigCategoryTree.create(List.of(
			category("first.toml", value("first.toml", "Animals", "Cow"), value("first.toml", "Animals/3:Cow")),
			category("second.toml", value("second.toml", "Animals", "Cow"))
		), 0, 1, 10);

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
		List<ConfigCategoryTree.Node> nativeTree = ConfigCategoryTree.create(List.of(category("common.toml", wrapped)), 0, 1, 10);
		List<ConfigCategoryTree.Node> customTree = ConfigCategoryTree.create(List.of(category("quick", wrapped)), 0, 1, 10);

		assertEquals(3, nativeTree.size());
		assertEquals(List.of(wrapped), Iterables.getLast(nativeTree).category().getConfigValues());
		assertEquals(1, customTree.size());
		assertEquals(List.of(wrapped), customTree.get(0).category().getConfigValues());
		assertEquals("Animals › Cow › Remove AI", ConfigValueCategoryPath.getContextualName(wrapped, Component.literal("Remove AI")).getString());
	}

	@Test
	void selectingAParentExposesItsWholeNestedSubtreeAndSelectingALeafStaysFocused() {
		ConfigScreenModel model = animalModel();
		TestEntry cow = new TestEntry(value("common.toml", "Animals", "Cow"));
		TestEntry pig = new TestEntry(value("common.toml", "Animals", "Pig"));
		model.addCategoryWidget(new ConfigCategoryWidget(model.getCategories().get(0), List.of()));
		model.addCategoryWidget(new ConfigCategoryWidget(model.getCategories().get(1), List.of()));
		model.addCategoryWidget(new ConfigCategoryWidget(model.getCategories().get(2), List.of(cow)));
		model.addCategoryWidget(new ConfigCategoryWidget(model.getCategories().get(3), List.of(pig)));

		model.setActiveCategoryIndex(0);
		assertEquals(List.of(0, 1, 2, 3), model.getActiveCategoryIndexes());
		assertEquals(List.of(1), model.getChildCategoryIndexes(0));
		assertEquals(List.of(2, 3), model.getChildCategoryIndexes(1));
		assertEquals(0, model.getFirstContentCategory(0));
		assertEquals(List.of(cow, pig), model.getVisibleEntryWidgets());
		model.setActiveCategoryIndex(3);
		assertEquals(List.of(3), model.getActiveCategoryIndexes());
		assertTrue(model.getChildCategoryIndexes(3).isEmpty());
		assertEquals(List.of(pig), model.getVisibleEntryWidgets());
	}

	@Test
	void historyRestoresTheSameSubsectionWhenEarlierSectionsAreAdded() {
		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("rememberLastCategory", true)) {
			ConfigScreenModel original = animalModel();
			ConfigScreenHistory.rememberCategory("nested_test", original.getCategories().get(3));
			ConfigScreenModel changed = navigationModel(List.of(category("common.toml",
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
	void clickingAnyNavigationRowSelectsItIncludingParents() {
		ConfigScreenModel model = animalModel();
		int[] selected = {-1};
		ConfigNavItem branch = new ConfigNavItem(Component.literal("Common"), 0,
			new ConfigCategoryWidget(model.getCategories().get(0), List.of()),
			() -> new ImmutableRect2i(10, 10, 100, 100), index -> selected[0] = index,
			model);
		branch.updateBounds(new ImmutableRect2i(10, 10, 100, 20), 22);
		for (int x : List.of(10, 29, 89, 90, 109)) {
			assertTrue(branch.handleUserInput(null, mouse(x, 29, InputType.SIMULATE)).isPresent());
			branch.handleUserInput(null, mouse(x, 29, InputType.EXECUTE));
			assertEquals(0, selected[0]);
			selected[0] = -1;
		}
		branch.resetBounds();
		assertTrue(branch.handleUserInput(null, mouse(30, 29, InputType.EXECUTE)).isEmpty());

		ConfigNavItem leaf = new ConfigNavItem(Component.literal("Cow"), 2,
			new ConfigCategoryWidget(model.getCategories().get(2), List.of()),
			() -> new ImmutableRect2i(10, 10, 100, 100), index -> selected[0] = index,
			model);
		leaf.updateBounds(new ImmutableRect2i(10, 40, 100, 20), 22);
		leaf.handleUserInput(null, mouse(50, 50, InputType.EXECUTE));
		assertEquals(2, selected[0]);
	}

	@Test
	void nestedRowGuttersDoNotHoverOrSelectButTheWholeVisibleRowDoes() {
		ConfigScreenModel model = animalModel();
		int[] selected = {-1};
		ConfigNavItem item = new ConfigNavItem(Component.literal("Animals"), 1,
			new ConfigCategoryWidget(model.getCategories().get(1), List.of()),
			() -> new ImmutableRect2i(10, 10, 100, 100), index -> selected[0] = index,
			model);
		item.updateBounds(new ImmutableRect2i(10, 10, 100, 20), 22);

		assertFalse(item.isMouseOver(21, 20));
		assertTrue(item.handleUserInput(null, mouse(21, 20, InputType.EXECUTE)).isEmpty());
		assertEquals(-1, selected[0]);
		assertTrue(item.isMouseOver(22, 20));
		assertTrue(item.handleUserInput(null, mouse(22, 20, InputType.EXECUTE)).isPresent());
		assertEquals(1, selected[0]);
		selected[0] = -1;
		item.handleUserInput(null, mouse(109, 20, InputType.EXECUTE));
		assertEquals(1, selected[0]);

		item.updateBounds(new ImmutableRect2i(10, 10, 80, 20), 22);
		assertFalse(item.isMouseOver(21, 20));
		selected[0] = -1;
		item.handleUserInput(null, mouse(89, 20, InputType.EXECUTE));
		assertEquals(1, selected[0]);
	}

	@Test
	void rightSideSectionHeadersToggleTheirOwnContentState() throws ReflectiveOperationException {
		AtomicInteger updates = new AtomicInteger();
		ConfigSectionHeader header = new ConfigSectionHeader(Component.literal("Animals"), Component.empty(), updates::incrementAndGet);
		Field area = ConfigSectionHeader.class.getDeclaredField("area");
		area.setAccessible(true);
		area.set(header, new ImmutableRect2i(20, 30, 160, 24));

		assertFalse(header.isCollapsed());
		assertTrue(header.handleUserInput(null, mouse(30, 40, InputType.SIMULATE)).isPresent());
		assertFalse(header.isCollapsed());
		assertEquals(0, updates.get());
		assertTrue(header.handleUserInput(null, mouse(30, 40, InputType.EXECUTE)).isPresent());
		assertTrue(header.isCollapsed());
		assertEquals(1, updates.get());
		assertTrue(header.handleUserInput(null, mouse(30, 40, InputType.EXECUTE)).isPresent());
		assertFalse(header.isCollapsed());
		assertEquals(2, updates.get());
		assertTrue(header.handleUserInput(null, mouse(10, 40, InputType.EXECUTE)).isEmpty());
	}

	private static UserInput mouse(double x, double y, InputType type) {
		return UserInput.fromVanilla(x, y, InputConstants.MOUSE_BUTTON_LEFT, type).orElseThrow();
	}

	private static ConfigScreenModel animalModel() {
		return navigationModel(List.of(category("common.toml", value("common.toml", "Animals", "Cow"), value("common.toml", "Animals", "Pig"))));
	}

	private static ConfigScreenModel navigationModel(List<ConfigScreenCategory> categories) {
		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("inlineSubsectionLimit", 0)) {
			return new ConfigScreenModel(categories);
		}
	}

	private static TestCategory category(String name, IConfigScreenValue<?>... values) {
		return new TestCategory(name, List.of(values));
	}

	private static TestValue value(String category, String... sections) {
		return new TestValue(category, List.of(sections).stream()
			.map(name -> new ConfigValueCategoryPath.Category(name, Component.literal(name), Component.empty()))
			.toList());
	}

	private record TestCategory(
		String name,
		List<IConfigScreenValue<?>> values,
		Component title,
		@Nullable ConfigScreenCategoryNavigationGroup navigationGroup
	) implements ConfigScreenCategory {
		private TestCategory(String name, List<IConfigScreenValue<?>> values) {
			this(name, values, Component.literal(name), null);
		}

		private TestCategory(String name, List<IConfigScreenValue<?>> values, Component title) {
			this(name, values, title, null);
		}
		@Override
		public ConfigScreenCategoryGroup getGroup() {
			return ConfigScreenCategoryGroup.LOADER_NATIVE;
		}

		@Override
		public @Nullable ConfigScreenCategoryNavigationGroup getNavigationGroup() {
			return navigationGroup;
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
			return title;
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

	private record TestValue(String category, List<ConfigValueCategoryPath.Category> categories, @Nullable Supplier<ServerConfigAccess> serverAccess) implements IConfigScreenValue<Boolean>, IConfigLocalizedValue, ConfigValueCategoryPath, ConfigValueAccess {
		private TestValue(String category, List<ConfigValueCategoryPath.Category> categories) {
			this(category, categories, null);
		}

		@Override
		public boolean isEditable() {
			return serverAccess == null || serverAccess.get() == ServerConfigAccess.LOCAL;
		}

		@Override
		public Optional<Supplier<ServerConfigAccess>> getServerAccess() {
			return Optional.ofNullable(serverAccess);
		}

		@Override
		public String getRootCategoryName() {
			return category;
		}

		@Override
		public List<ConfigValueCategoryPath.Category> getCategories() {
			return categories;
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
		@SuppressWarnings("DataFlowIssue")
		TestEntry(IConfigScreenValue<Boolean> value) {
			super(value, null);
		}

		@Override
		protected void drawContent(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {

		}
	}
}
