package net.mezzdev.config.gui.quickmenu;

import net.mezzdev.config.gui.util.ConfigMath;
import net.mezzdev.config.gui.ConfigContentPage;
import net.mezzdev.config.gui.ConfigScreenLayout.ResizeHandle;
import net.mezzdev.config.gui.ConfigScreenResizer;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

/** Measures and places the menu and its controls. */
final class QuickMenuLayout {
	static final int DEFAULT_WIDTH = 220;
	static final int MIN_WIDTH = 180;
	static final int PADDING = 6;
	static final int TITLE_HEIGHT = 22;
	static final int INFO_HEIGHT = 57;
	static final int ERROR_HEIGHT = 24;
	static final int ROW_HEIGHT = 22;
	static final int CONTROL_SIZE = 18;
	static final int SCROLLBAR_WIDTH = 6;
	private final ConfigContentPage page;
	private ImmutableRect2i area = ImmutableRect2i.EMPTY;
	private QuickMenuPlacement placement = QuickMenuPlacement.EMPTY;
	private int viewportWidth;
	private int viewportHeight;
	private int requestedInfoHeight = INFO_HEIGHT;
	private int preferredWidth = DEFAULT_WIDTH;
	// Change the preferred height only when resizing so shorter content does not erase the user's choice.
	private int preferredBodyHeight;

	QuickMenuLayout(ConfigContentPage page) {
		this.page = page;
	}

	ImmutableRect2i area() {
		return area;
	}

	void requestInfoHeight(int height) {
		requestedInfoHeight = Math.max(requestedInfoHeight, height);
	}

	void close() {
		area = ImmutableRect2i.EMPTY;
		placement = QuickMenuPlacement.EMPTY;
	}

	boolean update(Rect2i button, int screenWidth, int screenHeight, Runnable cancelInput) {
		int frameHeight = getFrameHeight();
		if (viewportWidth != screenWidth || viewportHeight != screenHeight) {
			cancelInput.run();
			viewportWidth = screenWidth;
			viewportHeight = screenHeight;
		}
		QuickMenuPlacement nextPlacement = QuickMenuPlacement.create(button, screenWidth, screenHeight);
		if (nextPlacement.aboveAnchor() != placement.aboveAnchor()) {
			cancelInput.run();
		}
		placement = nextPlacement;
		int availableHeight = placement.availableArea().getHeight() - frameHeight;
		if (availableHeight < ROW_HEIGHT || screenWidth < 140) {
			return false;
		}
		int width = ConfigMath.clamp(preferredWidth, Math.min(MIN_WIDTH, screenWidth - 4), screenWidth - 4);
		int maxBodyHeight = Math.min(availableHeight, page.preferredHeight(width - PADDING * 2 - 2));
		int bodyHeight = maxBodyHeight;
		if (preferredBodyHeight > 0) {
			bodyHeight = ConfigMath.clamp(preferredBodyHeight, Math.min(ROW_HEIGHT, maxBodyHeight), maxBodyHeight);
		}
		int height = bodyHeight + frameHeight;
		area = placement.place(width, height);
		page.updateBounds(getListArea().insetBy(1), new ImmutableRect2i(0, 0, screenWidth, screenHeight).insetBy(2));
		return true;
	}

	void resize(ResizeHandle handle, ImmutableRect2i original, double startX, double startY, double x, double y) {
		if (handle.left()) {
			preferredWidth = original.getWidth() + (int) Math.round(startX - x);
		} else if (handle.right()) {
			preferredWidth = original.getWidth() + (int) Math.round(x - startX);
		}
		if (handle.top()) {
			preferredBodyHeight = original.getHeight() - getFrameHeight() + (int) Math.round(startY - y);
		} else if (handle.bottom()) {
			preferredBodyHeight = original.getHeight() - getFrameHeight() + (int) Math.round(y - startY);
		}
		int minWidth = Math.min(MIN_WIDTH, viewportWidth - 4);
		int maxWidth = viewportWidth - 4;
		if (handle.left()) {
			maxWidth = original.getX() + original.getWidth() - 2;
		} else if (handle.right()) {
			minWidth = Math.max(minWidth, Math.min(maxWidth, placement.anchorRight() - 2));
		}
		preferredWidth = ConfigMath.clamp(preferredWidth, minWidth, maxWidth);
		if (handle.top() || handle.bottom()) {
			int maxBodyHeight = placement.availableArea().getHeight() - getFrameHeight();
			preferredBodyHeight = ConfigMath.clamp(preferredBodyHeight, Math.min(ROW_HEIGHT, maxBodyHeight), maxBodyHeight);
		}
	}

	ResizeHandle getResizeHandle(double mouseX, double mouseY) {
		if (page.hasPopup()) {
			return ResizeHandle.NONE;
		}
		ResizeHandle handle = ConfigScreenResizer.getResizeHandle(area, mouseX, mouseY);
		int anchorRight = placement.anchorRight();
		int right = area.getX() + area.getWidth();
		int minWidth = Math.min(MIN_WIDTH, viewportWidth - 4);
		boolean left = handle.left() && right <= anchorRight && right - 2 > minWidth;
		boolean rightEdge = handle.right() && area.getX() == 2 && viewportWidth - 4 > Math.max(minWidth, anchorRight - 2);
		boolean vertical = page.getContentHeight() > ROW_HEIGHT && placement.availableArea().getHeight() > getFrameHeight() + ROW_HEIGHT;
		boolean top = handle.top() && placement.aboveAnchor() && vertical;
		boolean bottom = handle.bottom() && !placement.aboveAnchor() && vertical;
		if (top && left) {
			return ResizeHandle.TOP_LEFT;
		}
		if (top && rightEdge) {
			return ResizeHandle.TOP_RIGHT;
		}
		if (bottom && left) {
			return ResizeHandle.BOTTOM_LEFT;
		}
		if (bottom && rightEdge) {
			return ResizeHandle.BOTTOM_RIGHT;
		}
		if (left) {
			return ResizeHandle.LEFT;
		}
		if (rightEdge) {
			return ResizeHandle.RIGHT;
		}
		if (top) {
			return ResizeHandle.TOP;
		}
		if (bottom) {
			return ResizeHandle.BOTTOM;
		}
		return ResizeHandle.NONE;
	}

	int getInfoHeight() {
		return Math.min(requestedInfoHeight, Math.max(INFO_HEIGHT, area.getHeight() / 3));
	}

	ImmutableRect2i getInfoArea() {
		return new ImmutableRect2i(area.getX() + PADDING, area.getY() + area.getHeight() - PADDING - getInfoHeight(),
			area.getWidth() - PADDING * 2, getInfoHeight());
	}

	ImmutableRect2i fullConfigButtonArea() {
		return new ImmutableRect2i(headerButtonArea().getX() - 24, area.getY() + PADDING, 20, 20);
	}

	ImmutableRect2i headerButtonArea() {
		var font = Minecraft.getInstance().font;
		int width = Math.max(40, Math.max(font.width(Component.translatable("mezz_config_gui.quickMenu.edit")), font.width(Component.translatable("gui.done"))) + 12);
		return new ImmutableRect2i(area.getX() + area.getWidth() - PADDING - width, area.getY() + PADDING, width, 20);
	}

	ImmutableRect2i getListArea() {
		return new ImmutableRect2i(area.getX() + PADDING, area.getY() + PADDING + TITLE_HEIGHT, area.getWidth() - PADDING * 2, area.getHeight() - getFrameHeight() - (getInfoHeight() - INFO_HEIGHT) + 2);
	}

	int getFrameHeight() {
		int height = PADDING * 2 + TITLE_HEIGHT + 2 + INFO_HEIGHT;
		if (!page.getFailure().isEmpty()) {
			height += ERROR_HEIGHT;
		}
		return height;
	}

	ImmutableRect2i editorArea() {
		return getListArea().insetBy(1).cropRight(SCROLLBAR_WIDTH);
	}

	int firstVisibleRow() {
		int row = 0;
		while (row < page.getEntries().size() && getRowArea(row).getY() + getRowArea(row).getHeight() <= editorArea().getY()) {
			row++;
		}
		return row;
	}

	int lastVisibleRow() {
		int row = firstVisibleRow();
		int bottom = editorArea().getY() + editorArea().getHeight();
		while (row < page.getEntries().size() && getRowArea(row).getY() < bottom) {
			row++;
		}
		return row;
	}

	ImmutableRect2i getRowArea(int row) {
		if (page.getEntries().isEmpty()) {
			return editorArea().keepTop(ROW_HEIGHT);
		}
		return page.getEntries().get(row).getArea();
	}

	ImmutableRect2i controlArea(int row, int column) {
		ImmutableRect2i rowArea = getRowArea(row);
		ImmutableRect2i label = page.getEntries().get(row).getLabelArea();
		return new ImmutableRect2i(rowArea.getX() + rowArea.getWidth() - 3 - CONTROL_SIZE - column * 20, label.getY() + (label.getHeight() - CONTROL_SIZE) / 2, CONTROL_SIZE, CONTROL_SIZE);
	}

}
