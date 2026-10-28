package net.mezzdev.config.gui.quickmenu;

import net.mezzdev.config.gui.util.ConfigMath;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.renderer.Rect2i;

/** Keeps the menu on the roomier side of its anchor, including while resizing. */
record QuickMenuPlacement(ImmutableRect2i availableArea, int anchorRight, boolean aboveAnchor) {
	static final QuickMenuPlacement EMPTY = new QuickMenuPlacement(ImmutableRect2i.EMPTY, 0, true);
	private static final int MARGIN = 2;

	static QuickMenuPlacement create(Rect2i anchor, int screenWidth, int screenHeight) {
		if (anchor.getWidth() <= 0 || anchor.getHeight() <= 0 || screenWidth <= MARGIN * 2 || screenHeight <= MARGIN * 2) {
			return EMPTY;
		}
		int screenBottom = screenHeight - MARGIN;
		int aboveBottom = ConfigMath.clamp(anchor.getY() - MARGIN, MARGIN, screenBottom);
		int belowTop = ConfigMath.clamp(anchor.getY() + anchor.getHeight() + MARGIN, MARGIN, screenBottom);
		boolean above = aboveBottom - MARGIN >= screenBottom - belowTop;
		int top = belowTop;
		int bottom = screenBottom;
		if (above) {
			top = MARGIN;
			bottom = aboveBottom;
		}
		var available = new ImmutableRect2i(MARGIN, top, screenWidth - MARGIN * 2, bottom - top);
		int right = ConfigMath.clamp(anchor.getX() + anchor.getWidth(), MARGIN, screenWidth - MARGIN);
		return new QuickMenuPlacement(available, right, above);
	}

	ImmutableRect2i place(int width, int height) {
		if (availableArea.isEmpty()) {
			return ImmutableRect2i.EMPTY;
		}
		width = ConfigMath.clamp(width, 0, availableArea.getWidth());
		height = ConfigMath.clamp(height, 0, availableArea.getHeight());
		int x = ConfigMath.clamp(anchorRight - width, availableArea.getX(), availableArea.getX() + availableArea.getWidth() - width);
		int y = availableArea.getY();
		if (aboveAnchor) {
			y += availableArea.getHeight() - height;
		}
		return new ImmutableRect2i(x, y, width, height);
	}
}
