package net.mezzdev.config.gui.model;

import net.mezzdev.config.gui.ConfigScreenCategory;
import net.mezzdev.config.gui.ConfigValueSections;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.util.ConfigLocale;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Stores the config screen's category, entry, navigation, and search state.
 */
public final class ConfigScreenModel {
	private final List<ConfigScreenCategory> categories;
	private final List<ConfigCategoryTree.Node> categoryTree;
	private final List<List<Integer>> childCategoryIndexes;
	private final List<ConfigCategoryWidget> categoryWidgets = new ArrayList<>();
	private final List<ConfigNavItem> navItems = new ArrayList<>();

	private int activeCategoryIndex = 0;
	private String searchText = "";

	public ConfigScreenModel(List<ConfigScreenCategory> categories) {
		this.categoryTree = ConfigCategoryTree.create(categories, ConfigGuiOptions.getInlineSubsectionLimit());
		this.categories = categoryTree.stream().map(ConfigCategoryTree.Node::category).toList();
		List<List<Integer>> children = new ArrayList<>();
		for (int index = 0; index < categoryTree.size(); index++) {
			children.add(new ArrayList<>());
		}
		for (int index = 0; index < categoryTree.size(); index++) {
			int parentIndex = categoryTree.get(index).parentIndex();
			if (parentIndex >= 0) {
				children.get(parentIndex).add(index);
			}
		}
		this.childCategoryIndexes = children.stream().map(List::copyOf).toList();
	}

	public List<ConfigScreenCategory> getCategories() {
		return categories;
	}

	public List<ConfigCategoryWidget.Section> getInlineSections(int index) {
		return categoryTree.get(index).inlineSections();
	}

	public void addCategoryWidget(ConfigCategoryWidget categoryWidget) {
		categoryWidgets.add(categoryWidget);
	}

	public List<ConfigCategoryWidget> getCategoryWidgets() {
		return categoryWidgets;
	}

	public void addNavItem(ConfigNavItem navItem) {
		navItems.add(navItem);
	}

	public List<ConfigNavItem> getNavItems() {
		return navItems;
	}

	public int getActiveCategoryIndex() {
		return activeCategoryIndex;
	}

	public void setActiveCategoryIndex(int activeCategoryIndex) {
		this.activeCategoryIndex = activeCategoryIndex;
	}

	public int getCategoryDepth(int index) {
		return categoryTree.get(index).depth();
	}

	public boolean hasSubcategories(int index) {
		return categoryTree.get(index).hasChildren();
	}

	public int getFirstContentCategory(int index) {
		return index;
	}

	public List<Integer> getChildCategoryIndexes(int index) {
		if (index < 0 || index >= categoryTree.size()) {
			return List.of();
		}
		return childCategoryIndexes.get(index);
	}

	public List<Integer> getActiveCategoryIndexes() {
		return getCategoryIndexes(activeCategoryIndex);
	}

	public List<Integer> getCategoryIndexes(int categoryIndex) {
		if (categoryIndex < 0 || categoryIndex >= categoryTree.size()) {
			return List.of();
		}
		List<Integer> indexes = new ArrayList<>();
		indexes.add(categoryIndex);
		int activeDepth = getCategoryDepth(categoryIndex);
		for (int index = categoryIndex + 1; index < categoryTree.size() && getCategoryDepth(index) > activeDepth; index++) {
			indexes.add(index);
		}
		return List.copyOf(indexes);
	}

	public boolean hasActiveCategory() {
		return activeCategoryIndex >= 0 && activeCategoryIndex < categoryWidgets.size();
	}

	public ConfigCategoryWidget getActiveCategoryWidget() {
		return categoryWidgets.get(activeCategoryIndex);
	}

	public void setSearchText(String searchText) {
		this.searchText = ConfigLocale.toLowercase(searchText);
	}

	public boolean isSearching() {
		return !searchText.isEmpty();
	}

	public boolean matchesSearch(ConfigEntryWidget<?> entry) {
		if (!isSearching()) {
			return true;
		}
		if (matches(entry.getFullName().getString())) {
			return true;
		}
		if (matches(ConfigValueSections.getContextualName(entry.getConfigValue(), entry.getFullName()).getString())) {
			return true;
		}
		return ConfigGuiOptions.searchDescriptions() &&
			matches(ConfigValueLocalization.getDescription(entry.getConfigValue()).getString());
	}

	private boolean matches(String text) {
		return ConfigLocale.toLowercase(text).contains(searchText);
	}

	public List<ConfigEntryWidget<?>> getVisibleEntryWidgets() {
		if (isSearching()) {
			return getAllEntryWidgets()
				.filter(this::matchesSearch)
				.toList();
		}
		if (!hasActiveCategory()) {
			return List.of();
		}
		return getActiveCategoryIndexes().stream()
			.flatMap(index -> categoryWidgets.get(index).getEntryWidgets().stream())
			.distinct()
			.toList();
	}

	public Stream<ConfigEntryWidget<?>> getAllEntryWidgets() {
		return categoryWidgets.stream()
			.flatMap(widget -> widget.getEntryWidgets().stream())
			.distinct();
	}
}
