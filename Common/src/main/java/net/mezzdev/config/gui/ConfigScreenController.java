package net.mezzdev.config.gui;

import net.mezzdev.config.gui.model.ConfigValueChange;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.model.ConfigCategoryWidget;
import net.mezzdev.config.gui.model.ConfigNavItem;
import net.mezzdev.config.gui.model.ConfigScreenModel;
import net.mezzdev.config.gui.model.PendingConfigChange;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates search, category selection, scrolling, and applying or discarding config changes.
 */
final class ConfigScreenController {
	private final ConfigChangesHandler changesHandler;
	private final ConfigScreenModel model;
	private final ConfigScreenLayout layout;
	private final Runnable clearSearchInput;

	public ConfigScreenController(
		ConfigChangesHandler changesHandler,
		ConfigScreenModel model,
		ConfigScreenLayout layout,
		Runnable clearSearchInput
	) {
		this.changesHandler = changesHandler;
		this.model = model;
		this.layout = layout;
		this.clearSearchInput = clearSearchInput;
	}

	public void setSearchText(String searchText) {
		model.setSearchText(searchText);
		layout.resetContentScroll();
		updateContentLayout();
	}

	public void setActiveCategory(int index) {
		if (index < 0 || index >= model.getCategoryWidgets().size()) {
			return;
		}

		model.setActiveCategoryIndex(index);
		model.setSearchText("");
		layout.resetContentScroll();
		clearSearchInput.run();

		for (int i = 0; i < model.getCategoryWidgets().size(); i++) {
			ConfigCategoryWidget widget = model.getCategoryWidgets().get(i);
			if (i != index) {
				widget.resetBounds();
			}
		}

		updateContentLayout();
	}

	public boolean hasPendingChanges() {
		return model.getAllEntryWidgets()
			.anyMatch(ConfigEntryWidget::hasPendingChange);
	}

	public boolean pendingChangesRequireRestart() {
		return ConfigValueChange.requiresRestart(getPendingChanges());
	}

	public boolean applyPendingChanges() {
		boolean requiresRestart = changesHandler.applyChanges(getPendingChanges());
		updateContentLayout();
		return requiresRestart;
	}

	private List<ConfigValueChange<?>> getPendingChanges() {
		List<ConfigValueChange<?>> changes = new ArrayList<>();
		for (ConfigEntryWidget<?> entryWidget : model.getAllEntryWidgets().toList()) {
			entryWidget.getPendingChange().ifPresent(changes::add);
		}
		return changes;
	}

	public List<PendingConfigChange> getPendingConfigChanges() {
		List<PendingConfigChange> changes = new ArrayList<>();
		for (ConfigEntryWidget<?> entryWidget : model.getAllEntryWidgets().toList()) {
			entryWidget.getPendingConfigChange().ifPresent(changes::add);
		}
		return changes;
	}

	public void discardPendingChanges() {
		model.getAllEntryWidgets()
			.filter(ConfigEntryWidget::hasPendingChange)
			.forEach(ConfigEntryWidget::discardPendingChange);
		updateContentLayout();
	}

	public List<ConfigEntryWidget<?>> getVisibleEntryWidgets() {
		return model.getVisibleEntryWidgets();
	}

	public boolean scroll(double mouseX, double mouseY, double scrollY) {
		return layout.scroll(mouseX, mouseY, scrollY);
	}

	public boolean startContentScrollDrag(double mouseX, double mouseY) {
		if (layout.startContentScrollDrag(mouseX, mouseY)) {
			updateContentLayout();
			return true;
		}
		return false;
	}

	public boolean dragContentScroll(double mouseY) {
		if (layout.dragContentScroll(mouseY)) {
			updateContentLayout();
			return true;
		}
		return false;
	}

	public boolean autoScrollContentForDrag(double mouseY) {
		if (layout.autoScrollContentForDrag(mouseY)) {
			updateContentLayout();
			return true;
		}
		return false;
	}

	public boolean stopContentScrollDrag() {
		return layout.stopContentScrollDrag();
	}

	public boolean startNavScrollDrag(double mouseX, double mouseY) {
		if (layout.startNavScrollDrag(mouseX, mouseY)) {
			updateNavLayout();
			return true;
		}
		return false;
	}

	public boolean dragNavScroll(double mouseY) {
		if (layout.dragNavScroll(mouseY)) {
			updateNavLayout();
			return true;
		}
		return false;
	}

	public boolean stopNavScrollDrag() {
		return layout.stopNavScrollDrag();
	}

	public void stepScrollPositions() {
		if (layout.stepContentScroll()) {
			updateContentLayout();
		}
		if (layout.stepNavScroll()) {
			updateNavLayout();
		}
	}

	public void updateContentLayout() {
		if (updateContentLayoutInternal()) {
			updateContentLayoutInternal();
		}
	}

	private boolean updateContentLayoutInternal() {
		ImmutableRect2i contentArea = layout.getContentArea();
		int currentY = contentArea.getY() - (int) layout.getCurrentScrollY();
		int totalContentHeight = 0;

		if (model.isSearching()) {
			for (ConfigCategoryWidget widget : model.getCategoryWidgets()) {
				widget.resetBounds();
			}
			for (ConfigEntryWidget<?> entryWidget : model.getAllEntryWidgets().filter(model::matchesSearch).toList()) {
				int height = updateEntryBounds(entryWidget, currentY);
				currentY += height;
				totalContentHeight += height;
			}
		} else if (model.hasActiveCategory()) {
			for (ConfigCategoryWidget widget : model.getCategoryWidgets()) {
				if (widget != model.getActiveCategoryWidget()) {
					widget.resetBounds();
				}
			}

			ConfigCategoryWidget activeWidget = model.getActiveCategoryWidget();
			for (ConfigEntryWidget<?> entryWidget : activeWidget.getEntryWidgets()) {
				int height = updateEntryBounds(entryWidget, currentY);
				currentY += height;
				totalContentHeight += height;
			}
		}

		return layout.setTotalContentHeight(totalContentHeight);
	}

	private int updateEntryBounds(ConfigEntryWidget<?> entryWidget, int y) {
		ImmutableRect2i contentArea = layout.getContentArea();
		int entryWidth = contentArea.getWidth() - 4;
		entryWidget.updateBounds(new ImmutableRect2i(contentArea.getX() + 2, y, entryWidth, ConfigEntryWidget.getMinimumHeight()));
		int height = entryWidget.getHeight();
		entryWidget.updateBounds(new ImmutableRect2i(contentArea.getX() + 2, y, entryWidth, height));
		return height;
	}

	public void updateNavLayout() {
		if (updateNavLayoutInternal()) {
			updateNavLayoutInternal();
		}
	}

	private boolean updateNavLayoutInternal() {
		ImmutableRect2i navArea = layout.getNavArea();
		int navItemWidth = layout.getNavItemWidth();
		int navY = navArea.getY() - (int) layout.getNavCurrentScrollY();
		int totalNavHeight = 0;
		List<ConfigNavItem> navItems = model.getNavItems();
		for (int i = 0; i < navItems.size(); i++) {
			ConfigNavItem navItem = navItems.get(i);
			int itemHeight = navItem.calculateHeight(navItemWidth);
			int itemHoverHeight = itemHeight;
			if (i < navItems.size() - 1) {
				itemHoverHeight += ConfigScreenLayout.NAV_ITEM_GAP;
			}
			navItem.updateBounds(new ImmutableRect2i(
				navArea.getX(),
				navY,
				navItemWidth,
				itemHeight
			), itemHoverHeight);
			navY += itemHeight + ConfigScreenLayout.NAV_ITEM_GAP;
			totalNavHeight += itemHeight + ConfigScreenLayout.NAV_ITEM_GAP;
		}
		if (!model.getNavItems().isEmpty()) {
			totalNavHeight -= ConfigScreenLayout.NAV_ITEM_GAP;
		}
		return layout.setTotalNavHeight(totalNavHeight);
	}
}
