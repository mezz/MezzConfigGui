package net.mezzdev.config.gui.popup;

import net.mezzdev.config.gui.util.ImmutableRect2i;

/**
 * Places popup selectors near their anchor while keeping them clipped to visible config content.
 */
public final class ConfigPopupPlacement {
	private static final int BORDER_OVERLAP = 1;

	private ConfigPopupPlacement() {

	}

	public static ImmutableRect2i placeNearAnchor(
		ImmutableRect2i anchorArea,
		ImmutableRect2i clipArea,
		int width,
		int height
	) {
		int popupWidth = Math.min(Math.max(width, anchorArea.getWidth()), clipArea.getWidth());
		int preferredX = getPreferredX(anchorArea, popupWidth);
		return placeNearAnchor(anchorArea, clipArea, preferredX, popupWidth, height);
	}

	public static ImmutableRect2i placeNearAnchor(
		ImmutableRect2i anchorArea,
		ImmutableRect2i clipArea,
		int preferredX,
		int width,
		int height
	) {
		if (anchorArea.isEmpty() || clipArea.isEmpty() || width <= 0 || height <= 0 || !anchorArea.intersects(clipArea)) {
			return ImmutableRect2i.EMPTY;
		}

		width = Math.min(width, clipArea.getWidth());
		height = Math.min(height, clipArea.getHeight());
		int clipRight = clipArea.getX() + clipArea.getWidth();
		int clipBottom = clipArea.getY() + clipArea.getHeight();
		int x = clamp(preferredX, clipArea.getX(), clipRight - width);
		int belowY = anchorArea.getY() + anchorArea.getHeight() - BORDER_OVERLAP;
		int aboveY = anchorArea.getY() - height + BORDER_OVERLAP;
		int y = getBestY(clipArea.getY(), clipBottom, belowY, aboveY, height);
		return new ImmutableRect2i(x, y, width, height);
	}

	private static int getPreferredX(ImmutableRect2i anchorArea, int width) {
		if (width > anchorArea.getWidth()) {
			return anchorArea.getX() + anchorArea.getWidth() - width;
		}
		return anchorArea.getX();
	}

	private static int getBestY(int clipTop, int clipBottom, int belowY, int aboveY, int height) {
		if (belowY + height <= clipBottom) {
			return belowY;
		}
		if (aboveY >= clipTop) {
			return aboveY;
		}

		int visibleBelow = getVisibleHeight(clipTop, clipBottom, belowY, height);
		int visibleAbove = getVisibleHeight(clipTop, clipBottom, aboveY, height);
		int preferredY = belowY;
		if (visibleAbove > visibleBelow) {
			preferredY = aboveY;
		}
		return clamp(preferredY, clipTop, clipBottom - height);
	}

	private static int getVisibleHeight(int clipTop, int clipBottom, int y, int height) {
		int visibleTop = Math.max(clipTop, y);
		int visibleBottom = Math.min(clipBottom, y + height);
		return Math.max(0, visibleBottom - visibleTop);
	}

	private static int clamp(int value, int min, int max) {
		if (max < min) {
			return min;
		}
		return Math.max(min, Math.min(max, value));
	}
}
