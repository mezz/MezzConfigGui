package net.mezzdev.config.gui;

import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.gui.model.ConfigValueChange;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.model.ConfigCategoryWidget;
import net.mezzdev.config.gui.model.ConfigNavItem;
import net.mezzdev.config.gui.model.ConfigScreenModel;
import net.mezzdev.config.gui.model.ConfigSectionHeader;
import net.mezzdev.config.gui.model.PendingConfigChange;
import net.mezzdev.config.gui.model.AppliedConfigValueChange;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntConsumer;

/**
 * Coordinates search, category selection, scrolling, and applying or discarding config changes.
 */
final class ConfigScreenController {
	private static final int SECTION_INDENT = 12;
	private static final int MIN_SECTION_WIDTH = 80;
	private final ConfigChangesHandler changesHandler;
	private final ConfigScreenModel model;
	private final ConfigScreenLayout layout;
	private final Runnable clearSearchInput;
	private final IntConsumer activeCategoryListener;
	private final AppliedConfigChangeTracker appliedChangeTracker = new AppliedConfigChangeTracker();
	private final List<ConfigEntryWidget<?>> visibleEntryWidgets = new ArrayList<>();
	private final List<ConfigSectionHeader> visibleSectionHeaders = new ArrayList<>();

	public ConfigScreenController(
		ConfigChangesHandler changesHandler,
		ConfigScreenModel model,
		ConfigScreenLayout layout,
		Runnable clearSearchInput,
		IntConsumer activeCategoryListener
	) {
		this.changesHandler = changesHandler;
		this.model = model;
		this.layout = layout;
		this.clearSearchInput = clearSearchInput;
		this.activeCategoryListener = activeCategoryListener;
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
		selectCategory(index);
	}

	private void selectCategory(int index) {
		model.setActiveCategoryIndex(index);
		activeCategoryListener.accept(index);
		model.setSearchText("");
		layout.resetContentScroll();
		clearSearchInput.run();

		updateNavLayout();
		updateContentLayout();
	}

	public boolean hasPendingChanges() {
		return model.getAllEntryWidgets()
			.anyMatch(ConfigEntryWidget::hasPendingChange);
	}

	public void startListening() {
		model.getAllEntryWidgets()
			.forEach(ConfigEntryWidget::subscribeToConfigValue);
	}

	public void stopListening() {
		model.getAllEntryWidgets()
			.forEach(ConfigEntryWidget::unsubscribeFromConfigValue);
	}

	public boolean hasUndoableChanges() {
		return hasPendingChanges() || appliedChangeTracker.hasChanges();
	}

	private void recordAppliedChange(AppliedConfigValueChange<?> change) {
		appliedChangeTracker.add(change);
	}

	public ConfigValueRestartRequirement getPendingChangesRestartRequirement() {
		return ConfigValueChange.getRestartRequirement(getPendingChanges());
	}

	public CompletableFuture<ConfigChangesResult> applyPendingChanges() {
		List<ConfigValueChange<?>> changes = getPendingChanges();
		return changesHandler.applyChanges(changes)
			.thenApply(result -> {
				recordAppliedChanges(result.appliedChanges());
				return result;
			});
	}

	public boolean applyImmediateChange(ConfigValueChange<?> change) {
		CompletableFuture<ConfigChangesResult> resultFuture = changesHandler.applyChanges(List.of(change));
		if (!resultFuture.isDone()) {
			throw new IllegalStateException("Immediate config changes must complete synchronously.");
		}
		ConfigChangesResult result = resultFuture.join();
		recordAppliedChanges(result.appliedChanges());
		updateContentLayout();
		return result.succeeded();
	}

	private void recordAppliedChanges(List<AppliedConfigValueChange<?>> changes) {
		for (AppliedConfigValueChange<?> change : changes) {
			recordAppliedChange(change);
		}
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
		discardPendingChangesInternal();
		updateContentLayout();
	}

	private void discardPendingChangesInternal() {
		model.getAllEntryWidgets()
			.filter(ConfigEntryWidget::hasPendingChange)
			.forEach(ConfigEntryWidget::discardPendingChange);
	}

	public CompletableFuture<ConfigChangesResult> undoChanges() {
		discardPendingChangesInternal();
		List<ConfigValueChange<?>> undoChanges = appliedChangeTracker.getUndoChanges();
		return changesHandler.applyChanges(undoChanges)
			.thenApply(result -> {
				recordAppliedChanges(result.appliedChanges());
				model.getAllEntryWidgets()
					.forEach(ConfigEntryWidget::discardPendingChange);
				return result;
			});
	}

	public List<ConfigEntryWidget<?>> getVisibleEntryWidgets() {
		return List.copyOf(visibleEntryWidgets);
	}

	public List<ConfigSectionHeader> getVisibleSectionHeaders() {
		return List.copyOf(visibleSectionHeaders);
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
		LayoutCursor cursor = new LayoutCursor(contentArea.getY() - (int) layout.getCurrentScrollY());
		visibleEntryWidgets.clear();
		visibleSectionHeaders.clear();
		for (ConfigCategoryWidget widget : model.getCategoryWidgets()) {
			widget.resetBounds();
		}

		if (model.isSearching()) {
			for (ConfigEntryWidget<?> entryWidget : model.getAllEntryWidgets().filter(model::matchesSearch).toList()) {
				layoutEntry(entryWidget, 0, cursor);
			}
		} else if (model.hasActiveCategory()) {
			int activeIndex = model.getActiveCategoryIndex();
			layoutWidgetContents(model.getActiveCategoryWidget(), 0, cursor);
			for (int childIndex : model.getChildCategoryIndexes(activeIndex)) {
				layoutCategorySection(childIndex, 0, cursor);
			}
		}

		int contentStart = contentArea.getY() - (int) layout.getCurrentScrollY();
		return layout.setTotalContentHeight(cursor.y - contentStart);
	}

	private void layoutCategorySection(int categoryIndex, int depth, LayoutCursor cursor) {
		ConfigCategoryWidget widget = model.getCategoryWidgets().get(categoryIndex);
		ConfigSectionHeader header = widget.getCategoryHeader();
		layoutHeader(header, depth, cursor);
		if (!header.isCollapsed()) {
			layoutWidgetContents(widget, depth + 1, cursor);
			for (int childIndex : model.getChildCategoryIndexes(categoryIndex)) {
				layoutCategorySection(childIndex, depth + 1, cursor);
			}
		}
		header.setContentBottom(cursor.y);
	}

	private void layoutWidgetContents(ConfigCategoryWidget widget, int depth, LayoutCursor cursor) {
		ConfigSectionHeader currentHeader = null;
		boolean sectionCollapsed = false;
		List<ConfigEntryWidget<?>> entries = widget.getEntryWidgets();
		for (int i = 0; i < entries.size(); i++) {
			ConfigSectionHeader header = widget.getSectionHeader(i);
			if (header != null) {
				if (currentHeader != null) {
					currentHeader.setContentBottom(cursor.y);
				}
				currentHeader = header;
				layoutHeader(header, depth, cursor);
				sectionCollapsed = header.isCollapsed();
			}
			if (!sectionCollapsed) {
				int entryDepth = depth;
				if (currentHeader != null) {
					entryDepth++;
				}
				layoutEntry(entries.get(i), entryDepth, cursor);
			}
		}
		if (currentHeader != null) {
			currentHeader.setContentBottom(cursor.y);
		}
	}

	private void layoutHeader(ConfigSectionHeader header, int depth, LayoutCursor cursor) {
		ImmutableRect2i contentArea = layout.getContentArea();
		int indent = getSectionIndent(depth, contentArea.getWidth());
		int height = header.updateBounds(
			contentArea.getX() + 2 + indent,
			cursor.y,
			Math.max(1, contentArea.getWidth() - 4 - indent)
		);
		cursor.y += height;
		visibleSectionHeaders.add(header);
	}

	private void layoutEntry(ConfigEntryWidget<?> entryWidget, int depth, LayoutCursor cursor) {
		entryWidget.setShowSectionPath(model.isSearching());
		ImmutableRect2i contentArea = layout.getContentArea();
		int indent = getSectionIndent(depth, contentArea.getWidth());
		int entryWidth = Math.max(1, contentArea.getWidth() - 4 - indent);
		int x = contentArea.getX() + 2 + indent;
		entryWidget.updateBounds(new ImmutableRect2i(x, cursor.y, entryWidth, ConfigEntryWidget.getMinimumHeight()));
		int height = entryWidget.getHeight();
		entryWidget.updateBounds(new ImmutableRect2i(x, cursor.y, entryWidth, height));
		cursor.y += height;
		visibleEntryWidgets.add(entryWidget);
	}

	private static int getSectionIndent(int depth, int contentWidth) {
		return Math.min(depth * SECTION_INDENT, Math.max(0, contentWidth - 4 - MIN_SECTION_WIDTH));
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

	private static final class LayoutCursor {
		private int y;

		private LayoutCursor(int y) {
			this.y = y;
		}
	}
}
