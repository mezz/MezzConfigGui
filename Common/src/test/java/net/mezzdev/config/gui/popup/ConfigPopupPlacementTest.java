package net.mezzdev.config.gui.popup;

import net.mezzdev.config.gui.util.ImmutableRect2i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigPopupPlacementTest {
	@Test
	void popupDimensionsAreClampedToTheVisibleArea() {
		ImmutableRect2i anchor = new ImmutableRect2i(20, 20, 80, 18);
		ImmutableRect2i clip = new ImmutableRect2i(10, 10, 180, 160);

		ImmutableRect2i popup = ConfigPopupPlacement.placeNearAnchor(anchor, clip, 256, 225);

		assertEquals(new ImmutableRect2i(10, 10, 180, 160), popup);
	}
}
