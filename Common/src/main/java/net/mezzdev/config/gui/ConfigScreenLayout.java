package net.mezzdev.config.gui;

import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.util.ConfigMath;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Calculates and stores screen rectangles, scroll state, and scrollbar positions.
 */
public final class ConfigScreenLayout {
	public static final int NAV_ITEM_HEIGHT = 20;
	public static final int NAV_ITEM_GAP = 2;
	static final int TITLE_HEIGHT = 18;
	static final int SEARCH_HEIGHT = 18;
	static final int INFO_AREA_HEIGHT = 57;

	private static final int NAV_DIVIDER_WIDTH = 8;
	private static final int MIN_CONTENT_WIDTH = 160;
	private static final int SCROLLBAR_WIDTH = 12;
	private static final int SCROLLBAR_GAP = 2;
	private static final int BORDER_PADDING = 6;
	private static final int SECTION_GAP = BORDER_PADDING / 2;
	private static final int SEARCH_TEXT_LEFT_PADDING = 5;
	private static final int SEARCH_TEXT_RIGHT_PADDING = 4;
	private static final int SEARCH_TEXT_HEIGHT = 8;
	private static final int ACTION_BUTTON_SIZE = 18;
	private static final int ACTION_BUTTON_GAP = 3;
	private static final int MIN_SCROLL_MARKER_HEIGHT = 10;
	private static final int SCROLL_MARKER_TRACK_INSET = 1;
	private final ConfigScreenResizer resizer = new ConfigScreenResizer();

	private ImmutableRect2i area = ImmutableRect2i.EMPTY;
	private ImmutableRect2i titleArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i titleTextArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i screenListButtonArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i applyPendingChangesButtonArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i undoChangesButtonArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i navArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i navDividerArea = ImmutableRect2i.EMPTY;
	@Nullable
	private NavigationResizeDrag navigationResizeDrag;
	private ImmutableRect2i navScrollBarArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i contentArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i searchBackgroundArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i scrollBarArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i infoArea = ImmutableRect2i.EMPTY;
	private int requestedInfoAreaHeight = INFO_AREA_HEIGHT;
	private ImmutableRect2i contentWithScrollArea = ImmutableRect2i.EMPTY;
	private int totalContentHeight = 0;
	private final ConfigScrollState contentScroll = new ConfigScrollState(MIN_SCROLL_MARKER_HEIGHT, SCROLL_MARKER_TRACK_INSET);
	private boolean contentScrollBarVisible = false;

	private int totalNavHeight = 0;
	private final ConfigScrollState navScroll = new ConfigScrollState(MIN_SCROLL_MARKER_HEIGHT, SCROLL_MARKER_TRACK_INSET);
	private boolean navScrollBarVisible = false;

	public void updateScreenBounds(int screenWidth, int screenHeight, LegacyEditBox searchBox) {
		updateScreenBounds(screenWidth, screenHeight, searchBox, false);
	}

	public void updateScreenBounds(int screenWidth, int screenHeight, LegacyEditBox searchBox, boolean hasScreenListButton) {
		updateScreenBounds(screenWidth, screenHeight, searchBox, hasScreenListButton, 0, 0);
	}

	void updateScreenBounds(
		int screenWidth,
		int screenHeight,
		LegacyEditBox searchBox,
		boolean hasScreenListButton,
		int screenLeftInset,
		int screenRightInset
	) {
		area = resizer.updateScreenBounds(screenWidth, screenHeight, screenLeftInset, screenRightInset);

		ImmutableRect2i innerArea = area.insetBy(BORDER_PADDING);
		titleArea = innerArea.keepTop(TITLE_HEIGHT);
		updateTitleRowAreas(hasScreenListButton);
		int infoHeight = getInfoAreaHeight();
		infoArea = innerArea.keepBottom(infoHeight);

		ImmutableRect2i mainArea = innerArea
			.cropTop(TITLE_HEIGHT + SECTION_GAP)
			.cropBottom(infoHeight + SECTION_GAP);
		int requestedNavWidth = ConfigGuiOptions.getNavigationWidth();
		if (navigationResizeDrag != null) {
			requestedNavWidth = navigationResizeDrag.width();
		}
		int navWidth = clampNavigationWidth(requestedNavWidth, mainArea.getWidth());
		navArea = mainArea.keepLeft(navWidth);
		navDividerArea = mainArea.cropLeft(navWidth).keepLeft(NAV_DIVIDER_WIDTH);
		updateNavScrollBarArea();

		contentWithScrollArea = mainArea.cropLeft(navWidth + NAV_DIVIDER_WIDTH);
		contentScrollBarVisible = shouldShowContentScrollBar(totalContentHeight, contentWithScrollArea);
		ImmutableRect2i topBarArea = contentWithScrollArea.keepTop(SEARCH_HEIGHT);

		updateSearchRowAreas(topBarArea);
		searchBox.setX(searchBackgroundArea.getX() + SEARCH_TEXT_LEFT_PADDING);
		searchBox.setY(searchBackgroundArea.getY() + (searchBackgroundArea.getHeight() - SEARCH_TEXT_HEIGHT) / 2);
		searchBox.setWidth(searchBackgroundArea.getWidth() - SEARCH_TEXT_LEFT_PADDING - SEARCH_TEXT_RIGHT_PADDING);
		searchBox.setHeight(SEARCH_HEIGHT);

		updateContentAndScrollBarAreas();
	}

	public ImmutableRect2i getArea() {
		return area;
	}

	public ImmutableRect2i getTitleArea() {
		return titleArea;
	}

	public ImmutableRect2i getTitleTextArea() {
		return titleTextArea;
	}

	public ImmutableRect2i getScreenListButtonArea() {
		return screenListButtonArea;
	}

	public ImmutableRect2i getApplyPendingChangesButtonArea() {
		return applyPendingChangesButtonArea;
	}

	public ImmutableRect2i getUndoChangesButtonArea() {
		return undoChangesButtonArea;
	}

	public ImmutableRect2i getNavArea() {
		return navArea;
	}

	ImmutableRect2i getNavDividerArea() {
		return navDividerArea;
	}

	boolean startNavigationResize(double mouseX, double mouseY) {
		if (!navDividerArea.contains(mouseX, mouseY)) {
			return false;
		}
		navigationResizeDrag = new NavigationResizeDrag(mouseX, navArea.getWidth(), navArea.getWidth());
		return true;
	}

	boolean dragNavigationResize(double mouseX) {
		NavigationResizeDrag drag = navigationResizeDrag;
		if (drag == null) {
			return false;
		}
		int mainWidth = navArea.getWidth() + navDividerArea.getWidth() + contentWithScrollArea.getWidth();
		int requestedWidth = (int) Math.round(drag.startWidth() + mouseX - drag.startMouseX());
		int width = clampNavigationWidth(requestedWidth, mainWidth);
		if (width == drag.width()) {
			return false;
		}
		navigationResizeDrag = new NavigationResizeDrag(drag.startMouseX(), drag.startWidth(), width);
		return true;
	}

	boolean isResizingNavigation() {
		return navigationResizeDrag != null;
	}

	OptionalInt finishNavigationResize() {
		NavigationResizeDrag drag = navigationResizeDrag;
		navigationResizeDrag = null;
		if (drag == null || drag.width() == drag.startWidth()) {
			return OptionalInt.empty();
		}
		return OptionalInt.of(drag.width());
	}

	private static int clampNavigationWidth(int width, int mainWidth) {
		int maxWidth = Math.max(0, mainWidth - NAV_DIVIDER_WIDTH - MIN_CONTENT_WIDTH);
		int minWidth = Math.min(ConfigGuiOptions.MIN_NAVIGATION_WIDTH, maxWidth);
		return ConfigMath.clamp(width, minWidth, maxWidth);
	}

	private record NavigationResizeDrag(double startMouseX, int startWidth, int width) {}

	public int getNavItemWidth() {
		if (navScrollBarVisible) {
			return navArea.getWidth() - SCROLLBAR_WIDTH - SCROLLBAR_GAP;
		}
		return navArea.getWidth();
	}

	public ImmutableRect2i getNavScrollBarArea() {
		return navScrollBarArea;
	}

	public ImmutableRect2i getContentArea() {
		return contentArea;
	}

	public ImmutableRect2i getSearchBackgroundArea() {
		return searchBackgroundArea;
	}

	public ImmutableRect2i getScrollBarArea() {
		return scrollBarArea;
	}

	public ImmutableRect2i getInfoArea() {
		return infoArea;
	}

	boolean requestInfoAreaHeight(int height) {
		// Keep the expanded height for this screen so hovering short and long descriptions
		// does not repeatedly move the rows and scrollbars beneath the pointer.
		requestedInfoAreaHeight = Math.max(requestedInfoAreaHeight, height);
		return getInfoAreaHeight() != infoArea.getHeight();
	}

	private int getInfoAreaHeight() {
		int maximumHeight = area.insetBy(BORDER_PADDING).getHeight() / 3;
		return Math.min(requestedInfoAreaHeight, maximumHeight);
	}

	public ResizeHandle getResizeHandle(double mouseX, double mouseY) {
		return resizer.getResizeHandle(mouseX, mouseY);
	}

	ResizeHandle getResizeHandle(double mouseX, double mouseY, ImmutableRect2i excludedArea) {
		return resizer.getResizeHandle(mouseX, mouseY, excludedArea);
	}

	public ResizeHandle getActiveResizeHandle(double mouseX, double mouseY) {
		return resizer.getActiveResizeHandle(mouseX, mouseY);
	}

	ResizeHandle getActiveResizeHandle(double mouseX, double mouseY, ImmutableRect2i excludedArea) {
		return resizer.getActiveResizeHandle(mouseX, mouseY, excludedArea);
	}

	public boolean startResizeDrag(double mouseX, double mouseY) {
		return resizer.startResizeDrag(mouseX, mouseY);
	}

	boolean startResizeDrag(double mouseX, double mouseY, ImmutableRect2i excludedArea) {
		return resizer.startResizeDrag(mouseX, mouseY, excludedArea);
	}

	public boolean dragResize(double mouseX, double mouseY, int screenWidth, int screenHeight) {
		return resizer.dragResize(mouseX, mouseY, screenWidth, screenHeight);
	}

	public boolean isResizing() {
		return resizer.isResizing();
	}

	public Optional<ImmutableRect2i> finishResizeDrag() {
		return resizer.finishResizeDrag();
	}

	public ImmutableRect2i getScrollMarkerArea() {
		return contentScroll.getMarkerArea();
	}

	public ImmutableRect2i getNavScrollMarkerArea() {
		return navScroll.getMarkerArea();
	}

	public double getCurrentScrollY() {
		return contentScroll.getPosition();
	}

	public double getNavCurrentScrollY() {
		return navScroll.getPosition();
	}

	public boolean setTotalContentHeight(int totalContentHeight) {
		this.totalContentHeight = totalContentHeight;
		boolean wasContentScrollBarVisible = contentScrollBarVisible;
		contentScrollBarVisible = shouldShowContentScrollBar(totalContentHeight, contentWithScrollArea);
		if (wasContentScrollBarVisible != contentScrollBarVisible) {
			updateContentAndScrollBarAreas();
		}
		return contentScroll.updateBounds(contentArea, totalContentHeight, scrollBarArea) || wasContentScrollBarVisible != contentScrollBarVisible;
	}

	public boolean setTotalNavHeight(int totalNavHeight) {
		this.totalNavHeight = totalNavHeight;
		boolean wasNavScrollBarVisible = navScrollBarVisible;
		navScrollBarVisible = shouldShowNavScrollBar(totalNavHeight, navArea);
		if (wasNavScrollBarVisible != navScrollBarVisible) {
			updateNavScrollBarArea();
		}
		return navScroll.updateBounds(navArea, totalNavHeight, navScrollBarArea) || wasNavScrollBarVisible != navScrollBarVisible;
	}

	public void resetContentScroll() {
		contentScroll.reset();
	}

	public void resetNavScroll() {
		navScroll.reset();
	}

	public boolean scroll(double mouseX, double mouseY, double scrollY) {
		if (navArea.contains(mouseX, mouseY)) {
			navScroll.scroll(scrollY);
			return true;
		}
		if (contentArea.contains(mouseX, mouseY)) {
			contentScroll.scroll(scrollY);
			return true;
		}
		return false;
	}

	public boolean startContentScrollDrag(double mouseX, double mouseY) {
		return contentScroll.startDrag(mouseX, mouseY);
	}

	public boolean dragContentScroll(double mouseY) {
		return contentScroll.drag(mouseY);
	}

	public boolean autoScrollContentForDrag(double mouseY) {
		return contentScroll.autoScrollForDrag(mouseY);
	}

	public boolean stopContentScrollDrag() {
		return contentScroll.stopDrag();
	}

	public boolean startNavScrollDrag(double mouseX, double mouseY) {
		return navScroll.startDrag(mouseX, mouseY);
	}

	public boolean dragNavScroll(double mouseY) {
		return navScroll.drag(mouseY);
	}

	public boolean stopNavScrollDrag() {
		return navScroll.stopDrag();
	}

	public boolean stepContentScroll() {
		return contentScroll.step();
	}

	public boolean stepNavScroll() {
		return navScroll.step();
	}

	private static boolean shouldShowContentScrollBar(int totalContentHeight, ImmutableRect2i contentWithScrollArea) {
		return totalContentHeight > getScrollableContentArea(contentWithScrollArea).getHeight();
	}

	private static boolean shouldShowNavScrollBar(int totalNavHeight, ImmutableRect2i navArea) {
		return totalNavHeight > navArea.getHeight();
	}

	private static ImmutableRect2i getScrollableContentArea(ImmutableRect2i contentArea) {
		return contentArea.cropTop(SEARCH_HEIGHT + SECTION_GAP);
	}

	private void updateContentAndScrollBarAreas() {
		ImmutableRect2i contentColumnArea = contentWithScrollArea;
		if (contentScrollBarVisible) {
			contentColumnArea = contentWithScrollArea.cropRight(SCROLLBAR_WIDTH + SCROLLBAR_GAP);
		}
		contentArea = getScrollableContentArea(contentColumnArea);

		scrollBarArea = ImmutableRect2i.EMPTY;
		if (contentScrollBarVisible) {
			scrollBarArea = getScrollableContentArea(contentWithScrollArea.keepRight(SCROLLBAR_WIDTH));
		}
		contentScroll.updateBounds(contentArea, totalContentHeight, scrollBarArea);
	}

	private void updateNavScrollBarArea() {
		navScrollBarArea = ImmutableRect2i.EMPTY;
		if (navScrollBarVisible) {
			navScrollBarArea = navArea.keepRight(SCROLLBAR_WIDTH);
		}
		navScroll.updateBounds(navArea, totalNavHeight, navScrollBarArea);
	}

	private void updateTitleRowAreas(boolean hasScreenListButton) {
		if (hasScreenListButton) {
			screenListButtonArea = centerVertically(titleArea.keepLeft(ACTION_BUTTON_SIZE), ACTION_BUTTON_SIZE);
			int titleSideInset = ACTION_BUTTON_SIZE + SECTION_GAP;
			titleTextArea = titleArea.cropLeft(titleSideInset).cropRight(titleSideInset);
		} else {
			screenListButtonArea = ImmutableRect2i.EMPTY;
			titleTextArea = titleArea;
		}
	}

	private void updateSearchRowAreas(ImmutableRect2i topBarArea) {
		int actionButtonsWidth = ACTION_BUTTON_SIZE * 2 + ACTION_BUTTON_GAP;
		ImmutableRect2i actionButtonsArea = topBarArea.keepRight(actionButtonsWidth);
		applyPendingChangesButtonArea = getActionButtonArea(actionButtonsArea, 0);
		undoChangesButtonArea = getActionButtonArea(actionButtonsArea, 1);
		searchBackgroundArea = topBarArea.cropRight(actionButtonsWidth + SECTION_GAP);
	}

	private static ImmutableRect2i getActionButtonArea(ImmutableRect2i actionButtonsArea, int buttonsFromRight) {
		int rightOffset = (ACTION_BUTTON_SIZE + ACTION_BUTTON_GAP) * buttonsFromRight;
		return centerVertically(actionButtonsArea.cropRight(rightOffset).keepRight(ACTION_BUTTON_SIZE), ACTION_BUTTON_SIZE);
	}

	private static ImmutableRect2i centerVertically(ImmutableRect2i area, int height) {
		int centeredHeight = Math.min(height, area.getHeight());
		return new ImmutableRect2i(
			area.getX(),
			area.getY() + (area.getHeight() - centeredHeight) / 2,
			area.getWidth(),
			centeredHeight
		);
	}

	public enum ResizeHandle {
		NONE(false, false, false, false),
		LEFT(true, false, false, false),
		RIGHT(false, true, false, false),
		TOP(false, false, true, false),
		BOTTOM(false, false, false, true),
		TOP_LEFT(true, false, true, false),
		TOP_RIGHT(false, true, true, false),
		BOTTOM_LEFT(true, false, false, true),
		BOTTOM_RIGHT(false, true, false, true);

		private final boolean left;
		private final boolean right;
		private final boolean top;
		private final boolean bottom;

		ResizeHandle(boolean left, boolean right, boolean top, boolean bottom) {
			this.left = left;
			this.right = right;
			this.top = top;
			this.bottom = bottom;
		}

		static ResizeHandle get(boolean left, boolean right, boolean top, boolean bottom) {
			if (left && top) {
				return TOP_LEFT;
			}
			if (right && top) {
				return TOP_RIGHT;
			}
			if (left && bottom) {
				return BOTTOM_LEFT;
			}
			if (right && bottom) {
				return BOTTOM_RIGHT;
			}
			if (left) {
				return LEFT;
			}
			if (right) {
				return RIGHT;
			}
			if (top) {
				return TOP;
			}
			if (bottom) {
				return BOTTOM;
			}
			return NONE;
		}

		public boolean left() {
			return left;
		}

		public boolean right() {
			return right;
		}

		public boolean top() {
			return top;
		}

		public boolean bottom() {
			return bottom;
		}
	}

}
