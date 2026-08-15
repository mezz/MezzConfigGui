package net.mezzdev.config.gui;

import net.mezzdev.config.gui.ConfigGuiColors.GuiColor;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Handles centered edge and corner resizing shared by MezzConfig GUI screens.
 */
public final class ConfigScreenResizer {
	private static final int RESIZE_HANDLE_SIZE = 5;
	private static final int MIN_RESIZABLE_WIDTH = 320;
	private static final int MIN_RESIZABLE_HEIGHT = 230;
	private static final int RESIZE_GRIP_SIZE = 11;
	private static final int RESIZE_GRIP_LINE_GAP = 3;
	private static final int RESIZE_EDGE_HIGHLIGHT_SIZE = 2;

	private ImmutableRect2i area = ImmutableRect2i.EMPTY;
	@Nullable
	private ImmutableRect2i customArea;
	@Nullable
	private ResizeDragSession resizeDragSession;
	private boolean resizeDragChanged;

	public ImmutableRect2i updateScreenBounds(int screenWidth, int screenHeight) {
		ImmutableRect2i customArea = this.customArea;
		if (customArea != null) {
			area = centerResizableArea(customArea.getWidth(), customArea.getHeight(), screenWidth, screenHeight);
			this.customArea = area;
			return area;
		}
		int guiWidth = ConfigGuiOptions.getGuiWidth(screenWidth);
		int guiHeight = ConfigGuiOptions.getGuiHeight(screenHeight);
		int guiLeft = (screenWidth - guiWidth) / 2;
		int guiTop = (screenHeight - guiHeight) / 2;
		area = new ImmutableRect2i(guiLeft, guiTop, guiWidth, guiHeight);
		return area;
	}

	public ConfigScreenLayout.ResizeHandle getResizeHandle(double mouseX, double mouseY) {
		if (!ConfigGuiOptions.enableWindowResizing() || area.isEmpty() || !isInResizeArea(mouseX, mouseY)) {
			return ConfigScreenLayout.ResizeHandle.NONE;
		}
		boolean left = mouseX < area.getX() + RESIZE_HANDLE_SIZE;
		boolean right = mouseX >= area.getX() + area.getWidth() - RESIZE_HANDLE_SIZE;
		boolean top = mouseY < area.getY() + RESIZE_HANDLE_SIZE;
		boolean bottom = mouseY >= area.getY() + area.getHeight() - RESIZE_HANDLE_SIZE;
		return ConfigScreenLayout.ResizeHandle.get(left, right, top, bottom);
	}

	public ConfigScreenLayout.ResizeHandle getActiveResizeHandle(double mouseX, double mouseY) {
		ResizeDragSession resizeDragSession = this.resizeDragSession;
		if (resizeDragSession != null) {
			return resizeDragSession.resizeHandle();
		}
		return getResizeHandle(mouseX, mouseY);
	}

	private boolean isInResizeArea(double mouseX, double mouseY) {
		return mouseX >= area.getX() &&
			mouseY >= area.getY() &&
			mouseX < area.getX() + area.getWidth() &&
			mouseY < area.getY() + area.getHeight();
	}

	public boolean startResizeDrag(double mouseX, double mouseY) {
		ConfigScreenLayout.ResizeHandle resizeHandle = getResizeHandle(mouseX, mouseY);
		if (resizeHandle == ConfigScreenLayout.ResizeHandle.NONE) {
			return false;
		}
		resizeDragSession = new ResizeDragSession(resizeHandle, area);
		resizeDragChanged = false;
		return true;
	}

	public boolean dragResize(double mouseX, double mouseY, int screenWidth, int screenHeight) {
		ResizeDragSession session = resizeDragSession;
		if (session == null) {
			return false;
		}
		ImmutableRect2i resizedArea = session.resize(mouseX, mouseY, screenWidth, screenHeight);
		if (resizedArea.equals(area)) {
			return false;
		}
		area = resizedArea;
		customArea = resizedArea;
		resizeDragChanged = true;
		return true;
	}

	public boolean isResizing() {
		return resizeDragSession != null;
	}

	public Optional<ImmutableRect2i> finishResizeDrag() {
		if (resizeDragSession == null) {
			return Optional.empty();
		}
		resizeDragSession = null;
		if (!resizeDragChanged || customArea == null) {
			resizeDragChanged = false;
			return Optional.empty();
		}
		ImmutableRect2i resizedArea = customArea;
		customArea = null;
		resizeDragChanged = false;
		return Optional.of(resizedArea);
	}

	public static void drawResizeHandles(
		GuiGraphics guiGraphics,
		ImmutableRect2i area,
		ConfigScreenLayout.ResizeHandle resizeHandle
	) {
		if (!ConfigGuiOptions.enableWindowResizing() || area.isEmpty()) {
			return;
		}
		drawResizeGrip(guiGraphics, area, resizeHandle == ConfigScreenLayout.ResizeHandle.BOTTOM_RIGHT);
		if (resizeHandle != ConfigScreenLayout.ResizeHandle.NONE) {
			drawResizeEdgeHighlight(guiGraphics, area, resizeHandle);
		}
	}

	private static void drawResizeGrip(GuiGraphics guiGraphics, ImmutableRect2i area, boolean hovered) {
		int color = getResizeGripColor(hovered);
		int right = area.getX() + area.getWidth() - 4;
		int bottom = area.getY() + area.getHeight() - 4;
		for (int i = 0; i < 3; i++) {
			int lineLength = RESIZE_GRIP_SIZE - i * RESIZE_GRIP_LINE_GAP;
			int lineRight = right - i * RESIZE_GRIP_LINE_GAP;
			drawResizeGripLine(guiGraphics, lineRight - lineLength, bottom, lineRight, bottom - lineLength, color);
		}
	}

	private static int getResizeGripColor(boolean hovered) {
		if (hovered) {
			return ConfigGuiColors.getColor(GuiColor.CONFIG_SCREEN_RESIZE_HANDLE_HOVER);
		}
		return ConfigGuiColors.getColor(GuiColor.CONFIG_SCREEN_RESIZE_GRIP);
	}

	private static void drawResizeGripLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
		int length = Math.min(x2 - x1, y1 - y2);
		for (int i = 0; i <= length; i++) {
			guiGraphics.fill(x1 + i, y1 - i, x1 + i + 1, y1 - i + 1, color);
		}
	}

	private static void drawResizeEdgeHighlight(
		GuiGraphics guiGraphics,
		ImmutableRect2i area,
		ConfigScreenLayout.ResizeHandle resizeHandle
	) {
		int x = area.getX();
		int y = area.getY();
		int right = x + area.getWidth();
		int bottom = y + area.getHeight();
		int color = ConfigGuiColors.getColor(GuiColor.CONFIG_SCREEN_RESIZE_HANDLE_HOVER);
		if (resizeHandle.left()) {
			guiGraphics.fill(x, y, x + RESIZE_EDGE_HIGHLIGHT_SIZE, bottom, color);
		}
		if (resizeHandle.right()) {
			guiGraphics.fill(right - RESIZE_EDGE_HIGHLIGHT_SIZE, y, right, bottom, color);
		}
		if (resizeHandle.top()) {
			guiGraphics.fill(x, y, right, y + RESIZE_EDGE_HIGHLIGHT_SIZE, color);
		}
		if (resizeHandle.bottom()) {
			guiGraphics.fill(x, bottom - RESIZE_EDGE_HIGHLIGHT_SIZE, right, bottom, color);
		}
	}

	private static ImmutableRect2i centerResizableArea(int areaWidth, int areaHeight, int screenWidth, int screenHeight) {
		int maxWidth = Math.max(1, screenWidth);
		int maxHeight = Math.max(1, screenHeight);
		int minWidth = Math.min(MIN_RESIZABLE_WIDTH, maxWidth);
		int minHeight = Math.min(MIN_RESIZABLE_HEIGHT, maxHeight);
		int width = Math.clamp(areaWidth, minWidth, maxWidth);
		int height = Math.clamp(areaHeight, minHeight, maxHeight);
		int x = (maxWidth - width) / 2;
		int y = (maxHeight - height) / 2;
		return new ImmutableRect2i(x, y, width, height);
	}

	private record ResizeDragSession(
		ConfigScreenLayout.ResizeHandle resizeHandle,
		ImmutableRect2i startArea
	) {
		private ImmutableRect2i resize(double mouseX, double mouseY, int screenWidth, int screenHeight) {
			int width = startArea.getWidth();
			int height = startArea.getHeight();
			if (resizeHandle.left() || resizeHandle.right()) {
				width = getCenteredWidth(mouseX, screenWidth, resizeHandle);
			}
			if (resizeHandle.top() || resizeHandle.bottom()) {
				height = getCenteredHeight(mouseY, screenHeight, resizeHandle);
			}
			return centerResizableArea(width, height, screenWidth, screenHeight);
		}

		private static int getCenteredWidth(
			double mouseX,
			int screenWidth,
			ConfigScreenLayout.ResizeHandle resizeHandle
		) {
			double centerX = screenWidth / 2.0;
			if (resizeHandle.left()) {
				return (int) Math.round((centerX - mouseX) * 2.0);
			}
			return (int) Math.round((mouseX - centerX) * 2.0);
		}

		private static int getCenteredHeight(
			double mouseY,
			int screenHeight,
			ConfigScreenLayout.ResizeHandle resizeHandle
		) {
			double centerY = screenHeight / 2.0;
			if (resizeHandle.top()) {
				return (int) Math.round((centerY - mouseY) * 2.0);
			}
			return (int) Math.round((mouseY - centerY) * 2.0);
		}
	}
}
