package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import org.jspecify.annotations.Nullable;

/** Keeps a drag on one endpoint, resolving an overlapping grab from movement instead of click position. */
final class RangeSliderDrag<T> {
	private final RangeSliderModel<T> model;
	private final double startMouseX;
	private final double trackStartX;
	private final double trackWidth;
	private RangeSliderModel.Handle handle;
	private boolean chooseHandleFromDirection;
	@Nullable
	private ConfigValueRange<T> fineAnchor;
	private double finePointerAnchor;
	private double pointerOffset;

	RangeSliderDrag(RangeSliderModel<T> model, ConfigValueRange<T> value, double mouseX, double trackStartX, double trackWidth, int handleWidth, boolean fineControl) {
		this.model = model;
		this.startMouseX = mouseX;
		this.trackStartX = trackStartX;
		this.trackWidth = Math.max(1, trackWidth);
		this.handle = model.getNearestHandle(value, getPosition(mouseX));
		this.chooseHandleFromDirection = containsHandle(value, RangeSliderModel.Handle.MINIMUM, mouseX, handleWidth) &&
			containsHandle(value, RangeSliderModel.Handle.MAXIMUM, mouseX, handleWidth);
		if (fineControl) {
			fineAnchor = value;
			finePointerAnchor = mouseX;
		}
	}

	RangeSliderModel.Handle getHandle() {
		return handle;
	}

	ConfigValueRange<T> update(ConfigValueRange<T> current, double mouseX, boolean fineControl) {
		if (chooseHandleFromDirection) {
			if (mouseX == startMouseX) {
				return current;
			}
			handle = RangeSliderModel.Handle.MAXIMUM;
			if (mouseX < startMouseX) {
				handle = RangeSliderModel.Handle.MINIMUM;
			}
			// Preserve where the shared handle was grabbed, even on the opposite side of its center.
			pointerOffset = getPointer(current, handle) - startMouseX;
			fineAnchor = null;
			if (fineControl) {
				fineAnchor = current;
				finePointerAnchor = startMouseX;
			}
		}

		ConfigValueRange<T> value;
		if (fineControl) {
			if (fineAnchor == null) {
				fineAnchor = current;
				finePointerAnchor = mouseX;
			}
			value = model.step(fineAnchor, handle, Math.round(mouseX - finePointerAnchor));
		} else {
			if (fineAnchor != null) {
				pointerOffset = getPointer(current, handle) - mouseX;
				fineAnchor = null;
			}
			value = model.move(current, handle, getPosition(mouseX + pointerOffset));
		}
		if (!value.equals(current)) {
			chooseHandleFromDirection = false;
		}
		return value;
	}

	private boolean containsHandle(ConfigValueRange<T> value, RangeSliderModel.Handle handle, double mouseX, int handleWidth) {
		double center = trackStartX + Math.round(model.getPosition(value, handle) * trackWidth);
		return Math.abs(mouseX - center) <= handleWidth / 2.0;
	}

	private double getPosition(double mouseX) {
		return (mouseX - trackStartX) / trackWidth;
	}

	private double getPointer(ConfigValueRange<T> value, RangeSliderModel.Handle handle) {
		return trackStartX + model.getPosition(value, handle) * trackWidth;
	}
}
