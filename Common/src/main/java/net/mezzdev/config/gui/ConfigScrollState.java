package net.mezzdev.config.gui;

import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.util.ConfigMath;
import net.mezzdev.config.gui.util.ImmutableRect2i;

/** Scroll position, animation, and scrollbar dragging for a bounded viewport. */
public final class ConfigScrollState {
	private static final int DRAG_SCROLL_EDGE_SIZE = 20;
	private static final double SCROLL_LERP = 0.35;
	private final int minimumMarkerHeight;
	private final int markerInset;
	private ImmutableRect2i viewport = ImmutableRect2i.EMPTY;
	private ImmutableRect2i scrollbar = ImmutableRect2i.EMPTY;
	private int contentHeight;
	private double current;
	private double target;
	private boolean dragging;
	private double dragOffset;

	public ConfigScrollState(int minimumMarkerHeight, int markerInset) {
		this.minimumMarkerHeight = minimumMarkerHeight;
		this.markerInset = markerInset;
	}

	public boolean updateBounds(ImmutableRect2i viewport, int contentHeight, ImmutableRect2i scrollbar) {
		this.viewport = viewport;
		this.contentHeight = contentHeight;
		this.scrollbar = scrollbar;
		double oldCurrent = current;
		double oldTarget = target;
		current = ConfigMath.clamp(current, 0, maximum());
		target = ConfigMath.clamp(target, 0, maximum());
		return oldCurrent != current || oldTarget != target;
	}

	public double getPosition() {
		return current;
	}

	public void reset() {
		current = 0;
		target = 0;
		dragging = false;
	}

	public void scroll(double amount) {
		target = ConfigMath.clamp(target - amount * ConfigGuiOptions.getScrollSpeed(), 0, maximum());
	}

	public boolean step() {
		double previous = current;
		current = stepScroll(current, target);
		return previous != current;
	}

	private static double stepScroll(double current, double target) {
		if (ConfigGuiOptions.smoothScrolling() && Math.abs(target - current) > 0.5) {
			return current + (target - current) * SCROLL_LERP;
		}
		return target;
	}

	public ImmutableRect2i getScrollbarArea() {
		if (maximum() <= 0) {
			return ImmutableRect2i.EMPTY;
		}
		return scrollbar;
	}

	public ImmutableRect2i getMarkerArea() {
		ImmutableRect2i track = getScrollbarArea().insetBy(markerInset);
		if (track.isEmpty()) {
			return ImmutableRect2i.EMPTY;
		}
		int height = Math.min(track.getHeight(), Math.max(minimumMarkerHeight, track.getHeight() * viewport.getHeight() / contentHeight));
		int y = track.getY() + (int) ((track.getHeight() - height) * current / maximum());
		return new ImmutableRect2i(track.getX(), y, track.getWidth(), height);
	}

	public boolean startDrag(double x, double y) {
		ImmutableRect2i marker = getMarkerArea();
		if (marker.isEmpty() || !getScrollbarArea().contains(x, y)) {
			return false;
		}
		dragging = true;
		if (marker.contains(x, y)) {
			dragOffset = y - marker.getY();
		} else {
			dragOffset = marker.getHeight() / 2.0;
			drag(y);
		}
		return true;
	}

	public boolean drag(double y) {
		if (!dragging) {
			return false;
		}
		ImmutableRect2i track = getScrollbarArea().insetBy(markerInset);
		int space = track.getHeight() - getMarkerArea().getHeight();
		current = 0;
		if (space > 0) {
			current = ConfigMath.clamp((y - dragOffset - track.getY()) * maximum() / space, 0, maximum());
		}
		target = current;
		return true;
	}

	public boolean stopDrag() {
		boolean wasDragging = dragging;
		dragging = false;
		return wasDragging;
	}

	public boolean autoScrollForDrag(double y) {
		double amount = dragScrollAmount(viewport, y);
		if (amount == 0) {
			return false;
		}
		double previous = current;
		target = ConfigMath.clamp(target + amount, 0, maximum());
		current = target;
		return previous != current;
	}

	private int maximum() {
		return Math.max(0, contentHeight - viewport.getHeight());
	}

	private static double dragScrollAmount(ImmutableRect2i viewport, double y) {
		if (viewport.isEmpty()) {
			return 0;
		}
		double topDistance = y - viewport.getY() - DRAG_SCROLL_EDGE_SIZE;
		double bottomDistance = y - viewport.getY() - viewport.getHeight() + DRAG_SCROLL_EDGE_SIZE;
		if (topDistance < 0) {
			return ConfigMath.clamp(topDistance / DRAG_SCROLL_EDGE_SIZE, -1, 0) * ConfigGuiOptions.getDragAutoScrollSpeed();
		}
		if (bottomDistance > 0) {
			return ConfigMath.clamp(bottomDistance / DRAG_SCROLL_EDGE_SIZE, 0, 1) * ConfigGuiOptions.getDragAutoScrollSpeed();
		}
		return 0;
	}
}
