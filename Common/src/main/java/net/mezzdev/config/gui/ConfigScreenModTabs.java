package net.mezzdev.config.gui;

import net.mezzdev.config.gui.screenlist.ConfigScreenListEntry;
import net.mezzdev.config.gui.textures.ConfigDrawableStatic;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Lays out and draws paged mod-icon tabs along the left side of a config screen.
 */
final class ConfigScreenModTabs {
	static final int TAB_WIDTH = 24;
	static final int TAB_HEIGHT = 24;
	private static final int TAB_GUI_OVERLAP = 3;
	private static final int TAB_VERTICAL_MARGIN = 4;
	private static final int ICON_SIZE = 16;

	private final String activeModId;
	private final List<ConfigScreenListEntry> entries;
	private final List<ModTab> visibleTabs = new ArrayList<>();

	private ImmutableRect2i screenArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i tabsArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i previousPageArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i nextPageArea = ImmutableRect2i.EMPTY;
	private int pageNumber;
	private int pageCount = 1;
	private int entriesPerPage;
	private boolean pageSelected;
	@Nullable
	private ClickTarget pressedTarget;

	public ConfigScreenModTabs(String activeModId, List<ConfigScreenListEntry> entries) {
		this.activeModId = Objects.requireNonNull(activeModId, "activeModId");
		this.entries = List.copyOf(entries);
	}

	public void updateLayout(ImmutableRect2i screenArea) {
		this.screenArea = screenArea;
		int previousEntriesPerPage = entriesPerPage;
		visibleTabs.clear();
		previousPageArea = ImmutableRect2i.EMPTY;
		nextPageArea = ImmutableRect2i.EMPTY;
		tabsArea = ImmutableRect2i.EMPTY;
		if (screenArea.isEmpty() || entries.isEmpty()) {
			return;
		}

		int availableHeight = Math.max(0, screenArea.getHeight() - TAB_VERTICAL_MARGIN * 2);
		int availableSlots = availableHeight / TAB_HEIGHT;
		if (availableSlots <= 0) {
			return;
		}

		boolean paged = entries.size() > availableSlots;
		if (paged && availableSlots >= 3) {
			entriesPerPage = availableSlots - 2;
			pageCount = divideCeil(entries.size(), entriesPerPage);
		} else {
			entriesPerPage = Math.min(entries.size(), availableSlots);
			pageCount = 1;
		}
		if (!pageSelected || entriesPerPage != previousEntriesPerPage) {
			int activeIndex = getActiveEntryIndex();
			if (activeIndex < 0) {
				pageNumber = 0;
			} else {
				pageNumber = activeIndex / entriesPerPage;
			}
			pageSelected = true;
		}
		pageNumber = Math.clamp(pageNumber, 0, pageCount - 1);
		updateVisibleTabs();
	}

	private void updateVisibleTabs() {
		visibleTabs.clear();
		previousPageArea = ImmutableRect2i.EMPTY;
		nextPageArea = ImmutableRect2i.EMPTY;
		if (screenArea.isEmpty() || entriesPerPage <= 0) {
			tabsArea = ImmutableRect2i.EMPTY;
			return;
		}

		int x = Math.max(0, screenArea.getX() - TAB_WIDTH + TAB_GUI_OVERLAP);
		int y = screenArea.getY() + TAB_VERTICAL_MARGIN;
		int firstY = y;
		if (pageCount > 1) {
			previousPageArea = new ImmutableRect2i(x, y, TAB_WIDTH, TAB_HEIGHT);
			y += TAB_HEIGHT;
		}

		int startIndex = pageNumber * entriesPerPage;
		int endIndex = Math.min(entries.size(), startIndex + entriesPerPage);
		for (int i = startIndex; i < endIndex; i++) {
			ConfigScreenListEntry entry = entries.get(i);
			visibleTabs.add(new ModTab(entry, new ImmutableRect2i(x, y, TAB_WIDTH, TAB_HEIGHT)));
			y += TAB_HEIGHT;
		}

		if (pageCount > 1) {
			nextPageArea = new ImmutableRect2i(x, y, TAB_WIDTH, TAB_HEIGHT);
			y += TAB_HEIGHT;
		}
		tabsArea = new ImmutableRect2i(x, firstY, TAB_WIDTH, y - firstY);
	}

	private int getActiveEntryIndex() {
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).modId().equals(activeModId)) {
				return i;
			}
		}
		return -1;
	}

	public void draw(GuiGraphics guiGraphics, Font font, ConfigTextures textures, int mouseX, int mouseY) {
		if (!previousPageArea.isEmpty()) {
			drawPageButton(guiGraphics, textures, previousPageArea, textures.getArrowUp(), mouseX, mouseY);
		}
		for (ModTab tab : visibleTabs) {
			drawModTab(guiGraphics, font, textures, tab);
		}
		if (!nextPageArea.isEmpty()) {
			drawPageButton(guiGraphics, textures, nextPageArea, textures.getArrowDown(), mouseX, mouseY);
		}
	}

	private void drawModTab(
		GuiGraphics guiGraphics,
		Font font,
		ConfigTextures textures,
		ModTab tab
	) {
		boolean selected = tab.entry().modId().equals(activeModId);
		textures.getModTab(selected).draw(guiGraphics, tab.area().getX(), tab.area().getY());
		ImmutableRect2i iconArea = new ImmutableRect2i(
			tab.area().getX() + (TAB_WIDTH - ICON_SIZE) / 2,
			tab.area().getY() + (TAB_HEIGHT - ICON_SIZE) / 2,
			ICON_SIZE,
			ICON_SIZE
		);
		tab.entry().icon().draw(guiGraphics, font, iconArea);
	}

	private void drawPageButton(
		GuiGraphics guiGraphics,
		ConfigTextures textures,
		ImmutableRect2i area,
		ConfigDrawableStatic arrow,
		int mouseX,
		int mouseY
	) {
		boolean hovered = area.contains(mouseX, mouseY);
		boolean pressed = isPressed(null, area);
		textures.getButtonForState(pressed, true, hovered).draw(guiGraphics, area);
		int arrowX = area.getX() + (area.getWidth() - arrow.getWidth()) / 2;
		int arrowY = area.getY() + (area.getHeight() - arrow.getHeight()) / 2;
		arrow.draw(guiGraphics, arrowX, arrowY);
	}

	private boolean isPressed(@Nullable ConfigScreenListEntry entry, ImmutableRect2i area) {
		ClickTarget pressedTarget = this.pressedTarget;
		return pressedTarget != null && pressedTarget.entry() == entry && pressedTarget.area().equals(area);
	}

	public void drawTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		findHoveredTab(mouseX, mouseY)
			.map(ModTab::entry)
			.map(ConfigScreenListEntry::title)
			.ifPresent(title -> {
				ConfigTooltip tooltip = new ConfigTooltip();
				tooltip.add(title);
				tooltip.draw(guiGraphics, mouseX, mouseY);
			});
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return false;
		}
		@Nullable
		ClickTarget target = getClickTarget(mouseX, mouseY);
		if (target == null) {
			return false;
		}
		pressedTarget = target;
		return true;
	}

	public ClickResult mouseReleased(double mouseX, double mouseY, int button) {
		ClickTarget pressedTarget = this.pressedTarget;
		this.pressedTarget = null;
		if (button != 0 || pressedTarget == null) {
			return ClickResult.NOT_HANDLED;
		}
		if (!pressedTarget.area().contains(mouseX, mouseY)) {
			return ClickResult.HANDLED;
		}
		if (pressedTarget.area().equals(previousPageArea)) {
			previousPage();
			return ClickResult.PAGE_CHANGED;
		}
		if (pressedTarget.area().equals(nextPageArea)) {
			nextPage();
			return ClickResult.PAGE_CHANGED;
		}
		ConfigScreenListEntry entry = pressedTarget.entry();
		if (entry != null && !entry.modId().equals(activeModId)) {
			return new ClickResult(true, true, Optional.of(entry));
		}
		return ClickResult.HANDLED;
	}

	public boolean isPressing() {
		return pressedTarget != null;
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
		if (pageCount <= 1 || scrollY == 0 || !tabsArea.contains(mouseX, mouseY)) {
			return false;
		}
		if (scrollY > 0) {
			previousPage();
		} else {
			nextPage();
		}
		return true;
	}

	private void previousPage() {
		if (pageCount <= 1) {
			return;
		}
		pageNumber--;
		if (pageNumber < 0) {
			pageNumber = pageCount - 1;
		}
		updateVisibleTabs();
	}

	private void nextPage() {
		if (pageCount <= 1) {
			return;
		}
		pageNumber++;
		if (pageNumber >= pageCount) {
			pageNumber = 0;
		}
		updateVisibleTabs();
	}

	@Nullable
	private ClickTarget getClickTarget(double mouseX, double mouseY) {
		if (previousPageArea.contains(mouseX, mouseY)) {
			return new ClickTarget(null, previousPageArea);
		}
		Optional<ModTab> hoveredTab = findHoveredTab(mouseX, mouseY);
		if (hoveredTab.isPresent()) {
			ModTab tab = hoveredTab.get();
			return new ClickTarget(tab.entry(), tab.area());
		}
		if (nextPageArea.contains(mouseX, mouseY)) {
			return new ClickTarget(null, nextPageArea);
		}
		return null;
	}

	private Optional<ModTab> findHoveredTab(double mouseX, double mouseY) {
		return visibleTabs.stream()
			.filter(tab -> tab.area().contains(mouseX, mouseY))
			.findFirst();
	}

	private static int divideCeil(int numerator, int denominator) {
		return (numerator + denominator - 1) / denominator;
	}

	int getPageNumber() {
		return pageNumber;
	}

	int getPageCount() {
		return pageCount;
	}

	List<String> getVisibleModIds() {
		return visibleTabs.stream()
			.map(ModTab::entry)
			.map(ConfigScreenListEntry::modId)
			.toList();
	}

	ImmutableRect2i getTabsArea() {
		return tabsArea;
	}

	private record ModTab(
		ConfigScreenListEntry entry,
		ImmutableRect2i area
	) {

	}

	private record ClickTarget(
		@Nullable ConfigScreenListEntry entry,
		ImmutableRect2i area
	) {

	}

	record ClickResult(
		boolean handled,
		boolean playSound,
		Optional<ConfigScreenListEntry> entry
	) {
		private static final ClickResult NOT_HANDLED = new ClickResult(false, false, Optional.empty());
		private static final ClickResult HANDLED = new ClickResult(true, false, Optional.empty());
		private static final ClickResult PAGE_CHANGED = new ClickResult(true, true, Optional.empty());

		ClickResult {
			Objects.requireNonNull(entry, "entry");
		}
	}
}
