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
 * Lays out and draws a scrollable strip of mod-icon tabs along the left side of a config screen.
 */
final class ConfigScreenModTabs {
	static final int TAB_WIDTH = 36;
	static final int TAB_HEIGHT = 32;
	private static final int TAB_GUI_OVERLAP = 3;
	private static final int INACTIVE_TAB_INSET = 4;
	private static final int TAB_SCREEN_MARGIN = 2;
	private static final int TAB_VERTICAL_MARGIN = 4;
	private static final int SCROLL_BUTTON_GAP = 2;
	private static final int SCROLL_BUTTON_SIZE = TAB_WIDTH - TAB_GUI_OVERLAP - 1;
	private static final int ICON_SIZE = 24;

	private final String activeModId;
	private final List<ConfigScreenListEntry> entries;
	private final List<ModTab> visibleTabs = new ArrayList<>();

	private ImmutableRect2i screenArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i tabsArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i scrollUpArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i scrollDownArea = ImmutableRect2i.EMPTY;
	private int firstVisibleIndex;
	private int visibleTabCount;
	private boolean scrollPositionSelected;
	private boolean showScrollButtons;
	private double scrollRemainder;
	@Nullable
	private ClickTarget pressedTarget;

	public ConfigScreenModTabs(String activeModId, List<ConfigScreenListEntry> entries) {
		this.activeModId = Objects.requireNonNull(activeModId, "activeModId");
		this.entries = List.copyOf(entries);
	}

	int getRequiredScreenLeftInset() {
		if (entries.isEmpty()) {
			return 0;
		}
		return TAB_WIDTH - TAB_GUI_OVERLAP + TAB_SCREEN_MARGIN;
	}

	int getRequiredScreenRightInset() {
		if (entries.isEmpty()) {
			return 0;
		}
		return TAB_SCREEN_MARGIN;
	}

	public void updateLayout(ImmutableRect2i screenArea) {
		this.screenArea = screenArea;
		int previousVisibleTabCount = visibleTabCount;
		visibleTabs.clear();
		scrollUpArea = ImmutableRect2i.EMPTY;
		scrollDownArea = ImmutableRect2i.EMPTY;
		tabsArea = ImmutableRect2i.EMPTY;
		if (screenArea.isEmpty() || entries.isEmpty()) {
			return;
		}

		int availableHeight = Math.max(0, screenArea.getHeight() - TAB_VERTICAL_MARGIN * 2);
		int availableSlots = availableHeight / TAB_HEIGHT;
		if (availableSlots <= 0) {
			return;
		}

		boolean overflowing = entries.size() > availableSlots;
		int scrollableSlots = (availableHeight - 2 * (SCROLL_BUTTON_SIZE + SCROLL_BUTTON_GAP)) / TAB_HEIGHT;
		showScrollButtons = overflowing && scrollableSlots > 0;
		if (showScrollButtons) {
			visibleTabCount = scrollableSlots;
		} else {
			visibleTabCount = Math.min(entries.size(), availableSlots);
		}
		if (!scrollPositionSelected || visibleTabCount != previousVisibleTabCount) {
			int activeIndex = getActiveEntryIndex();
			if (activeIndex < 0) {
				firstVisibleIndex = 0;
			} else {
				firstVisibleIndex = activeIndex - visibleTabCount / 2;
			}
			scrollPositionSelected = true;
		}
		firstVisibleIndex = Math.clamp(firstVisibleIndex, 0, getMaximumScroll());
		updateVisibleTabs();
	}

	private void updateVisibleTabs() {
		visibleTabs.clear();
		scrollUpArea = ImmutableRect2i.EMPTY;
		scrollDownArea = ImmutableRect2i.EMPTY;
		if (screenArea.isEmpty() || visibleTabCount <= 0) {
			tabsArea = ImmutableRect2i.EMPTY;
			return;
		}

		int x = Math.max(0, screenArea.getX() - TAB_WIDTH + TAB_GUI_OVERLAP);
		int y = screenArea.getY() + TAB_VERTICAL_MARGIN;
		int firstY = y;
		if (showScrollButtons) {
			scrollUpArea = new ImmutableRect2i(x, y, SCROLL_BUTTON_SIZE, SCROLL_BUTTON_SIZE);
			y += SCROLL_BUTTON_SIZE + SCROLL_BUTTON_GAP;
		}

		int startIndex = firstVisibleIndex;
		int endIndex = Math.min(entries.size(), startIndex + visibleTabCount);
		for (int i = startIndex; i < endIndex; i++) {
			ConfigScreenListEntry entry = entries.get(i);
			int inset = INACTIVE_TAB_INSET;
			if (entry.modId().equals(activeModId)) {
				inset = 0;
			}
			visibleTabs.add(new ModTab(entry, new ImmutableRect2i(x + inset, y, TAB_WIDTH - inset, TAB_HEIGHT)));
			y += TAB_HEIGHT;
		}

		if (showScrollButtons) {
			y = screenArea.getY() + screenArea.getHeight() - TAB_VERTICAL_MARGIN;
			scrollDownArea = new ImmutableRect2i(x, y - SCROLL_BUTTON_SIZE, SCROLL_BUTTON_SIZE, SCROLL_BUTTON_SIZE);
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
		if (!scrollUpArea.isEmpty()) {
			drawScrollButton(guiGraphics, textures, scrollUpArea, textures.getArrowUp(), firstVisibleIndex > 0, mouseX, mouseY);
		}
		for (ModTab tab : visibleTabs) {
			drawModTab(guiGraphics, font, textures, tab);
		}
		if (!scrollDownArea.isEmpty()) {
			drawScrollButton(guiGraphics, textures, scrollDownArea, textures.getArrowDown(), firstVisibleIndex < getMaximumScroll(), mouseX, mouseY);
		}
	}

	private void drawModTab(
		GuiGraphics guiGraphics,
		Font font,
		ConfigTextures textures,
		ModTab tab
	) {
		boolean selected = tab.entry().modId().equals(activeModId);
		textures.getModTab(selected).draw(guiGraphics, tab.area());
		ImmutableRect2i iconArea = new ImmutableRect2i(
			tab.area().getX() + (tab.area().getWidth() - ICON_SIZE) / 2,
			tab.area().getY() + (TAB_HEIGHT - ICON_SIZE) / 2,
			ICON_SIZE,
			ICON_SIZE
		);
		tab.entry().icon().draw(guiGraphics, font, iconArea);
	}

	private void drawScrollButton(
		GuiGraphics guiGraphics,
		ConfigTextures textures,
		ImmutableRect2i area,
		ConfigDrawableStatic arrow,
		boolean enabled,
		int mouseX,
		int mouseY
	) {
		boolean hovered = enabled && area.contains(mouseX, mouseY);
		boolean pressed = isPressed(null, area);
		textures.getButtonForState(pressed, enabled, hovered).draw(guiGraphics, area);
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
		if (pressedTarget.area().equals(scrollUpArea)) {
			if (scrollBy(-1)) {
				return ClickResult.SCROLLED;
			}
			return ClickResult.HANDLED;
		}
		if (pressedTarget.area().equals(scrollDownArea)) {
			if (scrollBy(1)) {
				return ClickResult.SCROLLED;
			}
			return ClickResult.HANDLED;
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
		if (getMaximumScroll() == 0 || !Double.isFinite(scrollY) || scrollY == 0 || !tabsArea.contains(mouseX, mouseY)) {
			return false;
		}
		if (Math.signum(scrollRemainder) != Math.signum(scrollY)) {
			scrollRemainder = 0;
		}
		scrollRemainder += scrollY;
		int steps = (int) scrollRemainder;
		scrollRemainder -= steps;
		scrollBy(-(long) steps);
		return true;
	}

	private int getMaximumScroll() {
		return Math.max(0, entries.size() - visibleTabCount);
	}

	private boolean scrollBy(long steps) {
		int nextIndex = (int) Math.clamp(firstVisibleIndex + steps, 0, getMaximumScroll());
		if (nextIndex == firstVisibleIndex) {
			return false;
		}
		firstVisibleIndex = nextIndex;
		pressedTarget = null;
		updateVisibleTabs();
		return true;
	}

	@Nullable
	private ClickTarget getClickTarget(double mouseX, double mouseY) {
		if (scrollUpArea.contains(mouseX, mouseY)) {
			return new ClickTarget(null, scrollUpArea);
		}
		Optional<ModTab> hoveredTab = findHoveredTab(mouseX, mouseY);
		if (hoveredTab.isPresent()) {
			ModTab tab = hoveredTab.get();
			return new ClickTarget(tab.entry(), tab.area());
		}
		if (scrollDownArea.contains(mouseX, mouseY)) {
			return new ClickTarget(null, scrollDownArea);
		}
		return null;
	}

	private Optional<ModTab> findHoveredTab(double mouseX, double mouseY) {
		return visibleTabs.stream()
			.filter(tab -> tab.area().contains(mouseX, mouseY))
			.findFirst();
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

	ImmutableRect2i getResizeExclusionArea(double mouseY) {
		for (ModTab tab : visibleTabs) {
			ImmutableRect2i area = tab.area();
			if (mouseY >= area.getY() && mouseY < area.getY() + area.getHeight()) {
				return area;
			}
		}
		return ImmutableRect2i.EMPTY;
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
		private static final ClickResult SCROLLED = new ClickResult(true, true, Optional.empty());

		ClickResult {
			Objects.requireNonNull(entry, "entry");
		}
	}
}
