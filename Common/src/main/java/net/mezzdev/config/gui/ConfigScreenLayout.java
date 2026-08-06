package net.mezzdev.config.gui;

import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/**
 * Calculates and stores screen rectangles, scroll state, and scrollbar positions.
 */
public final class ConfigScreenLayout {
	public static final int NAV_ITEM_HEIGHT = 20;
	public static final int NAV_ITEM_GAP = 2;
	static final int TITLE_HEIGHT = 18;
	static final int SEARCH_HEIGHT = 18;
	static final int INFO_AREA_HEIGHT = 57;

	private static final int NAV_WIDTH = 110;
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
	private static final int DRAG_SCROLL_EDGE_SIZE = 20;
	private static final int RESIZE_HANDLE_SIZE = 5;
	private static final int MIN_RESIZABLE_WIDTH = 320;
	private static final int MIN_RESIZABLE_HEIGHT = 230;
	private static final double SCROLL_LERP = 0.35;

	private ImmutableRect2i area = ImmutableRect2i.EMPTY;
	private ImmutableRect2i titleArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i titleTextArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i applyPendingChangesButtonArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i undoChangesButtonArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i navArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i navScrollBarArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i contentArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i searchBackgroundArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i scrollBarArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i infoArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i contentWithScrollArea = ImmutableRect2i.EMPTY;
	@Nullable
	private ImmutableRect2i customArea;
	@Nullable
	private ResizeDragSession resizeDragSession;

	private int totalContentHeight = 0;
	private double targetScrollY = 0;
	private double currentScrollY = 0;
	private boolean draggingContentScroll = false;
	private double scrollDragOffsetY = 0;
	private boolean contentScrollBarVisible = false;

	private int totalNavHeight = 0;
	private double navTargetScrollY = 0;
	private double navCurrentScrollY = 0;
	private boolean draggingNavScroll = false;
	private double navScrollDragOffsetY = 0;
	private boolean navScrollBarVisible = false;

	public void updateScreenBounds(int screenWidth, int screenHeight, EditBox searchBox) {
		area = getScreenArea(screenWidth, screenHeight);

		ImmutableRect2i innerArea = area.insetBy(BORDER_PADDING);
		titleArea = innerArea.keepTop(TITLE_HEIGHT);
		updateTitleRowAreas();
		infoArea = innerArea.keepBottom(INFO_AREA_HEIGHT);

		ImmutableRect2i mainArea = innerArea
			.cropTop(TITLE_HEIGHT + SECTION_GAP)
			.cropBottom(INFO_AREA_HEIGHT + SECTION_GAP);
		navArea = mainArea.keepLeft(NAV_WIDTH);
		updateNavScrollBarArea();

		contentWithScrollArea = mainArea.cropLeft(NAV_WIDTH + SECTION_GAP);
		contentScrollBarVisible = shouldShowContentScrollBar(totalContentHeight, contentWithScrollArea);
		ImmutableRect2i topBarArea = contentWithScrollArea.keepTop(SEARCH_HEIGHT);

		searchBackgroundArea = topBarArea;
		searchBox.setX(searchBackgroundArea.getX() + SEARCH_TEXT_LEFT_PADDING);
		searchBox.setY(searchBackgroundArea.getY() + (searchBackgroundArea.getHeight() - SEARCH_TEXT_HEIGHT) / 2);
		searchBox.setWidth(searchBackgroundArea.getWidth() - SEARCH_TEXT_LEFT_PADDING - SEARCH_TEXT_RIGHT_PADDING);
		searchBox.setHeight(SEARCH_HEIGHT);

		updateContentAndScrollBarAreas();
	}

	private ImmutableRect2i getScreenArea(int screenWidth, int screenHeight) {
		ImmutableRect2i customArea = this.customArea;
		if (customArea != null) {
			ImmutableRect2i clampedArea = clampResizableArea(customArea, screenWidth, screenHeight);
			this.customArea = clampedArea;
			return clampedArea;
		}
		ConfigGuiOptions.GuiSize guiSize = ConfigGuiOptions.getGuiSize();
		int guiWidth = guiSize.getWidth(screenWidth);
		int guiHeight = guiSize.getHeight(screenHeight);
		int guiLeft = (screenWidth - guiWidth) / 2;
		int guiTop = (screenHeight - guiHeight) / 2;
		return new ImmutableRect2i(guiLeft, guiTop, guiWidth, guiHeight);
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

	public ImmutableRect2i getApplyPendingChangesButtonArea() {
		return applyPendingChangesButtonArea;
	}

	public ImmutableRect2i getUndoChangesButtonArea() {
		return undoChangesButtonArea;
	}

	public ImmutableRect2i getNavArea() {
		return navArea;
	}

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

	public ResizeHandle getResizeHandle(double mouseX, double mouseY) {
		if (area.isEmpty() || !isInResizeArea(mouseX, mouseY)) {
			return ResizeHandle.NONE;
		}
		boolean left = mouseX < area.getX() + RESIZE_HANDLE_SIZE;
		boolean right = mouseX >= area.getX() + area.getWidth() - RESIZE_HANDLE_SIZE;
		boolean top = mouseY < area.getY() + RESIZE_HANDLE_SIZE;
		boolean bottom = mouseY >= area.getY() + area.getHeight() - RESIZE_HANDLE_SIZE;
		return ResizeHandle.get(left, right, top, bottom);
	}

	private boolean isInResizeArea(double mouseX, double mouseY) {
		return mouseX >= area.getX() &&
			mouseY >= area.getY() &&
			mouseX < area.getX() + area.getWidth() &&
			mouseY < area.getY() + area.getHeight();
	}

	public boolean startResizeDrag(double mouseX, double mouseY) {
		ResizeHandle resizeHandle = getResizeHandle(mouseX, mouseY);
		if (resizeHandle == ResizeHandle.NONE) {
			return false;
		}
		resizeDragSession = new ResizeDragSession(resizeHandle, area);
		return true;
	}

	public boolean dragResize(double mouseX, double mouseY, int screenWidth, int screenHeight) {
		ResizeDragSession session = resizeDragSession;
		if (session == null) {
			return false;
		}
		ImmutableRect2i resizedArea = session.resize(mouseX, mouseY, screenWidth, screenHeight);
		if (resizedArea.equals(customArea)) {
			return false;
		}
		customArea = resizedArea;
		return true;
	}

	public boolean isResizing() {
		return resizeDragSession != null;
	}

	public boolean stopResizeDrag() {
		boolean wasResizing = resizeDragSession != null;
		resizeDragSession = null;
		return wasResizing;
	}

	public ImmutableRect2i getScrollMarkerArea() {
		int maxScroll = getMaxContentScroll();
		if (maxScroll <= 0) {
			return ImmutableRect2i.EMPTY;
		}
		return getScrollMarkerArea(scrollBarArea, getScrollMarkerHeight(), currentScrollY, maxScroll);
	}

	public ImmutableRect2i getNavScrollMarkerArea() {
		if (navScrollBarArea.isEmpty()) {
			return ImmutableRect2i.EMPTY;
		}
		int maxScroll = getMaxNavScroll();
		if (maxScroll <= 0) {
			return ImmutableRect2i.EMPTY;
		}
		return getScrollMarkerArea(navScrollBarArea, getNavScrollMarkerHeight(), navCurrentScrollY, maxScroll);
	}

	public int getTotalContentHeight() {
		return totalContentHeight;
	}

	public int getTotalNavHeight() {
		return totalNavHeight;
	}

	public double getCurrentScrollY() {
		return currentScrollY;
	}

	public double getNavCurrentScrollY() {
		return navCurrentScrollY;
	}

	public boolean setTotalContentHeight(int totalContentHeight) {
		this.totalContentHeight = totalContentHeight;
		boolean wasContentScrollBarVisible = contentScrollBarVisible;
		contentScrollBarVisible = shouldShowContentScrollBar(totalContentHeight, contentWithScrollArea);
		if (wasContentScrollBarVisible != contentScrollBarVisible) {
			updateContentAndScrollBarAreas();
		}
		return clampContentScroll() || wasContentScrollBarVisible != contentScrollBarVisible;
	}

	public boolean setTotalNavHeight(int totalNavHeight) {
		this.totalNavHeight = totalNavHeight;
		boolean wasNavScrollBarVisible = navScrollBarVisible;
		navScrollBarVisible = shouldShowNavScrollBar(totalNavHeight, navArea);
		if (wasNavScrollBarVisible != navScrollBarVisible) {
			updateNavScrollBarArea();
		}
		return clampNavScroll() || wasNavScrollBarVisible != navScrollBarVisible;
	}

	public void resetContentScroll() {
		targetScrollY = 0;
		currentScrollY = 0;
	}

	public void resetNavScroll() {
		navTargetScrollY = 0;
		navCurrentScrollY = 0;
	}

	public boolean scroll(double mouseX, double mouseY, double scrollY) {
		if (navArea.contains(mouseX, mouseY)) {
			int maxNavScroll = getMaxNavScroll();
			navTargetScrollY = Mth.clamp(navTargetScrollY - scrollY * ConfigGuiOptions.getScrollSpeed(), 0, maxNavScroll);
			return true;
		}
		if (contentArea.contains(mouseX, mouseY)) {
			int maxScroll = Math.max(0, totalContentHeight - contentArea.getHeight());
			targetScrollY = Mth.clamp(targetScrollY - scrollY * ConfigGuiOptions.getScrollSpeed(), 0, maxScroll);
			return true;
		}
		return false;
	}

	public boolean startContentScrollDrag(double mouseX, double mouseY) {
		ImmutableRect2i markerArea = getScrollMarkerArea();
		if (markerArea.isEmpty() || !scrollBarArea.contains(mouseX, mouseY)) {
			return false;
		}

		if (markerArea.contains(mouseX, mouseY)) {
			scrollDragOffsetY = mouseY - markerArea.getY();
		} else {
			scrollDragOffsetY = markerArea.getHeight() / 2.0;
			setContentScrollFromMarkerY(mouseY - scrollDragOffsetY);
		}
		draggingContentScroll = true;
		return true;
	}

	public boolean dragContentScroll(double mouseY) {
		if (!draggingContentScroll) {
			return false;
		}
		setContentScrollFromMarkerY(mouseY - scrollDragOffsetY);
		return true;
	}

	public boolean autoScrollContentForDrag(double mouseY) {
		if (contentArea.isEmpty()) {
			return false;
		}

		int topEdge = contentArea.getY() + DRAG_SCROLL_EDGE_SIZE;
		int bottomEdge = contentArea.getY() + contentArea.getHeight() - DRAG_SCROLL_EDGE_SIZE;
		if (mouseY < topEdge) {
			return scrollContentImmediately(-getDragScrollAmount(topEdge - mouseY));
		}
		if (mouseY > bottomEdge) {
			return scrollContentImmediately(getDragScrollAmount(mouseY - bottomEdge));
		}
		return false;
	}

	private static double getDragScrollAmount(double edgeDistance) {
		double factor = Math.clamp(edgeDistance / DRAG_SCROLL_EDGE_SIZE, 0.0, 1.0);
		return factor * ConfigGuiOptions.getDragAutoScrollSpeed();
	}

	private boolean scrollContentImmediately(double amount) {
		int maxScroll = getMaxContentScroll();
		double previousScrollY = targetScrollY;
		targetScrollY = Mth.clamp(targetScrollY + amount, 0, maxScroll);
		currentScrollY = targetScrollY;
		return targetScrollY != previousScrollY;
	}

	public boolean stopContentScrollDrag() {
		boolean wasDragging = draggingContentScroll;
		draggingContentScroll = false;
		return wasDragging;
	}

	public boolean startNavScrollDrag(double mouseX, double mouseY) {
		ImmutableRect2i markerArea = getNavScrollMarkerArea();
		if (markerArea.isEmpty() || !navScrollBarArea.contains(mouseX, mouseY)) {
			return false;
		}

		if (markerArea.contains(mouseX, mouseY)) {
			navScrollDragOffsetY = mouseY - markerArea.getY();
		} else {
			navScrollDragOffsetY = markerArea.getHeight() / 2.0;
			setNavScrollFromMarkerY(mouseY - navScrollDragOffsetY);
		}
		draggingNavScroll = true;
		return true;
	}

	public boolean dragNavScroll(double mouseY) {
		if (!draggingNavScroll) {
			return false;
		}
		setNavScrollFromMarkerY(mouseY - navScrollDragOffsetY);
		return true;
	}

	public boolean stopNavScrollDrag() {
		boolean wasDragging = draggingNavScroll;
		draggingNavScroll = false;
		return wasDragging;
	}

	public boolean stepContentScroll() {
		double nextScroll = stepScroll(currentScrollY, targetScrollY);
		if (nextScroll == currentScrollY) {
			return false;
		}
		currentScrollY = nextScroll;
		return true;
	}

	public boolean stepNavScroll() {
		double nextScroll = stepScroll(navCurrentScrollY, navTargetScrollY);
		if (nextScroll == navCurrentScrollY) {
			return false;
		}
		navCurrentScrollY = nextScroll;
		return true;
	}

	private static double stepScroll(double current, double target) {
		if (!ConfigGuiOptions.smoothScrolling()) {
			return target;
		}
		if (Math.abs(target - current) > 0.5) {
			return current + (target - current) * SCROLL_LERP;
		}
		if (current != target) {
			return target;
		}
		return current;
	}

	private int getMaxContentScroll() {
		return Math.max(0, totalContentHeight - contentArea.getHeight());
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
	}

	private void updateNavScrollBarArea() {
		navScrollBarArea = ImmutableRect2i.EMPTY;
		if (navScrollBarVisible) {
			navScrollBarArea = navArea.keepRight(SCROLLBAR_WIDTH);
		}
	}

	private void updateTitleRowAreas() {
		int actionButtonsWidth = ACTION_BUTTON_SIZE * 2 + ACTION_BUTTON_GAP;
		ImmutableRect2i actionButtonsArea = titleArea.keepRight(actionButtonsWidth);
		applyPendingChangesButtonArea = centerVertically(actionButtonsArea.keepRight(ACTION_BUTTON_SIZE), ACTION_BUTTON_SIZE);
		undoChangesButtonArea = centerVertically(
			actionButtonsArea.cropRight(ACTION_BUTTON_SIZE + ACTION_BUTTON_GAP).keepRight(ACTION_BUTTON_SIZE),
			ACTION_BUTTON_SIZE
		);
		titleTextArea = titleArea.cropRight(actionButtonsWidth + SECTION_GAP);
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

	private int getMaxNavScroll() {
		return Math.max(0, totalNavHeight - navArea.getHeight());
	}

	private int getScrollMarkerHeight() {
		ImmutableRect2i markerTrackArea = getScrollMarkerTrackArea(scrollBarArea);
		if (totalContentHeight <= 0) {
			return markerTrackArea.getHeight();
		}
		return getScrollMarkerHeight(markerTrackArea, contentArea.getHeight(), totalContentHeight);
	}

	private int getNavScrollMarkerHeight() {
		ImmutableRect2i markerTrackArea = getScrollMarkerTrackArea(navScrollBarArea);
		if (totalNavHeight <= 0) {
			return markerTrackArea.getHeight();
		}
		return getScrollMarkerHeight(markerTrackArea, navArea.getHeight(), totalNavHeight);
	}

	private static ImmutableRect2i getScrollMarkerTrackArea(ImmutableRect2i scrollBarArea) {
		return scrollBarArea.insetBy(SCROLL_MARKER_TRACK_INSET);
	}

	private static int getScrollMarkerHeight(ImmutableRect2i markerTrackArea, int visibleHeight, int totalHeight) {
		int markerHeight = Math.max(MIN_SCROLL_MARKER_HEIGHT, markerTrackArea.getHeight() * visibleHeight / totalHeight);
		return Math.min(markerTrackArea.getHeight(), markerHeight);
	}

	private static ImmutableRect2i getScrollMarkerArea(
		ImmutableRect2i scrollBarArea,
		int markerHeight,
		double currentScrollY,
		int maxScroll
	) {
		ImmutableRect2i markerTrackArea = getScrollMarkerTrackArea(scrollBarArea);
		int trackSpace = markerTrackArea.getHeight() - markerHeight;
		int markerY = markerTrackArea.getY() + (int) (trackSpace * currentScrollY / maxScroll);
		markerY = Mth.clamp(markerY, markerTrackArea.getY(), markerTrackArea.getY() + trackSpace);
		return new ImmutableRect2i(
			markerTrackArea.getX(),
			markerY,
			markerTrackArea.getWidth(),
			markerHeight
		);
	}

	private void setContentScrollFromMarkerY(double markerY) {
		int maxScroll = getMaxContentScroll();
		int markerHeight = getScrollMarkerHeight();
		ImmutableRect2i markerTrackArea = getScrollMarkerTrackArea(scrollBarArea);
		int trackSpace = markerTrackArea.getHeight() - markerHeight;
		if (maxScroll <= 0 || trackSpace <= 0) {
			targetScrollY = 0;
			currentScrollY = 0;
			return;
		}

		double scrollPercent = (markerY - markerTrackArea.getY()) / trackSpace;
		double nextScroll = Mth.clamp(scrollPercent * maxScroll, 0, maxScroll);
		targetScrollY = nextScroll;
		currentScrollY = nextScroll;
	}

	private void setNavScrollFromMarkerY(double markerY) {
		int maxScroll = getMaxNavScroll();
		int markerHeight = getNavScrollMarkerHeight();
		ImmutableRect2i markerTrackArea = getScrollMarkerTrackArea(navScrollBarArea);
		int trackSpace = markerTrackArea.getHeight() - markerHeight;
		if (maxScroll <= 0 || trackSpace <= 0) {
			navTargetScrollY = 0;
			navCurrentScrollY = 0;
			return;
		}

		double scrollPercent = (markerY - markerTrackArea.getY()) / trackSpace;
		double nextScroll = Mth.clamp(scrollPercent * maxScroll, 0, maxScroll);
		navTargetScrollY = nextScroll;
		navCurrentScrollY = nextScroll;
	}

	private boolean clampContentScroll() {
		int maxScroll = getMaxContentScroll();
		double oldTarget = targetScrollY;
		double oldCurrent = currentScrollY;
		targetScrollY = Mth.clamp(targetScrollY, 0, maxScroll);
		currentScrollY = Mth.clamp(currentScrollY, 0, maxScroll);
		return oldTarget != targetScrollY || oldCurrent != currentScrollY;
	}

	private boolean clampNavScroll() {
		int maxNavScroll = getMaxNavScroll();
		double oldTarget = navTargetScrollY;
		double oldCurrent = navCurrentScrollY;
		navTargetScrollY = Mth.clamp(navTargetScrollY, 0, maxNavScroll);
		navCurrentScrollY = Mth.clamp(navCurrentScrollY, 0, maxNavScroll);
		return oldTarget != navTargetScrollY || oldCurrent != navCurrentScrollY;
	}

	private static ImmutableRect2i clampResizableArea(ImmutableRect2i area, int screenWidth, int screenHeight) {
		int maxWidth = Math.max(1, screenWidth);
		int maxHeight = Math.max(1, screenHeight);
		int minWidth = Math.min(MIN_RESIZABLE_WIDTH, maxWidth);
		int minHeight = Math.min(MIN_RESIZABLE_HEIGHT, maxHeight);
		int width = Math.clamp(area.getWidth(), minWidth, maxWidth);
		int height = Math.clamp(area.getHeight(), minHeight, maxHeight);
		int x = Math.clamp(area.getX(), 0, maxWidth - width);
		int y = Math.clamp(area.getY(), 0, maxHeight - height);
		return new ImmutableRect2i(x, y, width, height);
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

		boolean left() {
			return left;
		}

		boolean right() {
			return right;
		}

		boolean top() {
			return top;
		}

		boolean bottom() {
			return bottom;
		}
	}

	private record ResizeDragSession(
		ResizeHandle resizeHandle,
		ImmutableRect2i startArea
	) {
		private ImmutableRect2i resize(double mouseX, double mouseY, int screenWidth, int screenHeight) {
			int maxWidth = Math.max(1, screenWidth);
			int maxHeight = Math.max(1, screenHeight);
			int minWidth = Math.min(MIN_RESIZABLE_WIDTH, maxWidth);
			int minHeight = Math.min(MIN_RESIZABLE_HEIGHT, maxHeight);
			int left = startArea.getX();
			int top = startArea.getY();
			int right = startArea.getX() + startArea.getWidth();
			int bottom = startArea.getY() + startArea.getHeight();

			if (resizeHandle.left()) {
				left = Math.clamp((int) Math.round(mouseX), 0, right - minWidth);
			}
			if (resizeHandle.right()) {
				right = Math.clamp((int) Math.round(mouseX), left + minWidth, maxWidth);
			}
			if (resizeHandle.top()) {
				top = Math.clamp((int) Math.round(mouseY), 0, bottom - minHeight);
			}
			if (resizeHandle.bottom()) {
				bottom = Math.clamp((int) Math.round(mouseY), top + minHeight, maxHeight);
			}
			return clampResizableArea(new ImmutableRect2i(left, top, right - left, bottom - top), screenWidth, screenHeight);
		}
	}
}
