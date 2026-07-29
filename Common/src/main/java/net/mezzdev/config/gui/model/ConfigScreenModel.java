package net.mezzdev.config.gui.model;

import net.mezzdev.config.api.schema.IConfigCategory;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.util.ConfigLocale;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Stores the config screen's category, entry, navigation, and search state.
 */
public final class ConfigScreenModel {
	private final List<IConfigCategory> categories;
	private final List<ConfigCategoryWidget> categoryWidgets = new ArrayList<>();
	private final List<ConfigNavItem> navItems = new ArrayList<>();

	private int activeCategoryIndex = 0;
	private String searchText = "";

	public ConfigScreenModel(List<IConfigCategory> categories) {
		this.categories = List.copyOf(categories);
	}

	public List<IConfigCategory> getCategories() {
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
		return matches(entry.getFullName().getString()) ||
			matches(entry.getConfigValue().getLocalizedDescription().getString());
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
