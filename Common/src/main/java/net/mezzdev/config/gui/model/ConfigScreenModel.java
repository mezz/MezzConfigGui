package net.mezzdev.config.gui.model;

import net.mezzdev.config.gui.ConfigScreenCategory;
import net.mezzdev.config.gui.ConfigValueSections;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.util.ConfigLocale;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Stores the config screen's category, entry, navigation, and search state.
 */
public final class ConfigScreenModel {
	private final List<ConfigScreenCategory> categories;
	private final List<ConfigCategoryTree.Node> categoryTree;
	private final Set<Integer> collapsedCategories = new HashSet<>();
	private final List<ConfigCategoryWidget> categoryWidgets = new ArrayList<>();
	private final List<ConfigNavItem> navItems = new ArrayList<>();

	private int activeCategoryIndex = 0;
	private String searchText = "";

	public ConfigScreenModel(List<ConfigScreenCategory> categories) {
		this.categoryTree = ConfigCategoryTree.create(categories);
		this.categories = categoryTree.stream().map(ConfigCategoryTree.Node::category).toList();
	}

	public List<ConfigScreenCategory> getCategories() {
		return categories;
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
		if (activeCategoryIndex >= 0 && activeCategoryIndex < categoryTree.size()) {
			for (int parent = categoryTree.get(activeCategoryIndex).parentIndex(); parent >= 0; parent = categoryTree.get(parent).parentIndex()) {
				collapsedCategories.remove(parent);
			}
		}
	}

	public int getCategoryDepth(int index) {
		return categoryTree.get(index).depth();
	}

	public boolean hasSubcategories(int index) {
		return categoryTree.get(index).hasChildren();
	}

	public boolean isCategoryExpanded(int index) {
		return !collapsedCategories.contains(index);
	}

	public boolean isCategoryVisible(int index) {
		for (int parent = categoryTree.get(index).parentIndex(); parent >= 0; parent = categoryTree.get(parent).parentIndex()) {
			if (collapsedCategories.contains(parent)) {
				return false;
			}
		}
		return true;
	}

	public void toggleCategoryExpanded(int index) {
		if (!hasSubcategories(index)) {
			return;
		}
		if (!collapsedCategories.remove(index)) {
			collapsedCategories.add(index);
			if (activeCategoryIndex >= 0 && activeCategoryIndex < categories.size() && !isCategoryVisible(activeCategoryIndex)) {
				setActiveCategoryIndex(index);
			}
		}
	}

	public int getFirstContentCategory(int index) {
		if (index < 0 || index >= categories.size() || !categories.get(index).getConfigValues().isEmpty()) {
			return index;
		}
		for (int child = index + 1; child < categories.size() && getCategoryDepth(child) > getCategoryDepth(index); child++) {
			if (!categories.get(child).getConfigValues().isEmpty()) {
				return child;
			}
		}
		return index;
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

	public String getSearchText() {
		return searchText;
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
		return List.copyOf(getActiveCategoryWidget().getEntryWidgets());
	}

	public Stream<ConfigEntryWidget<?>> getAllEntryWidgets() {
		return categoryWidgets.stream()
			.flatMap(widget -> widget.getEntryWidgets().stream())
			.distinct();
	}
}
