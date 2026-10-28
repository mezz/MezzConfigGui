package net.mezzdev.config.gui.quickmenu;

import net.mezzdev.config.gui.util.ImmutableRect2i;

import java.util.List;

/** Tracks a reorder gesture and describes its current preview. */
final class QuickMenuReorderDrag<T> {
	private final T id;
	private final List<T> selection;
	private final double startX;
	private final double startY;
	private final double grabOffsetY;
	private double mouseX;
	private double mouseY;
	private boolean active;

	QuickMenuReorderDrag(T id, List<T> selection, ImmutableRect2i row, double x, double y) {
		this.id = id;
		this.selection = List.copyOf(selection);
		this.startX = x;
		this.startY = y;
		this.grabOffsetY = y - row.getY();
		this.mouseX = x;
		this.mouseY = y;
	}

	T id() {
		return id;
	}

	List<T> selection() {
		return selection;
	}

	boolean isActive() {
		return active;
	}

	double mouseY() {
		return mouseY;
	}

	void moveTo(double x, double y) {
		mouseX = x;
		mouseY = y;
		active |= Math.abs(x - startX) + Math.abs(y - startY) >= 4;
	}

	int targetIndex(QuickMenuLayout layout) {
		for (int index = 0; index < selection.size(); index++) {
			ImmutableRect2i row = layout.getRowArea(index);
			if (mouseY < row.getY() + row.getHeight()) {
				return index;
			}
		}
		return selection.size() - 1;
	}

	Preview<T> preview(QuickMenuLayout layout) {
		return new Preview<>(id, selection.size(), targetIndex(layout), mouseX, mouseY, grabOffsetY, active);
	}

	record Preview<T>(T id, int selectionSize, int targetIndex, double mouseX, double mouseY, double grabOffsetY, boolean active) {}
}
