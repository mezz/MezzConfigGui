package net.mezzdev.config.gui.quickmenu;

import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.renderer.Rect2i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickMenuPlacementTest {
	@Test
	void opensBelowTopAnchorsAndAboveBottomAnchors() {
		var topAnchor = new Rect2i(300, 2, 20, 20);
		var below = QuickMenuPlacement.create(topAnchor, 640, 360);
		assertFalse(below.aboveAnchor());
		assertTrue(below.place(220, 200).getY() > topAnchor.getY() + topAnchor.getHeight());
		assertEquals(200, below.place(220, 200).getHeight());

		var bottomAnchor = new Rect2i(300, 338, 20, 20);
		var above = QuickMenuPlacement.create(bottomAnchor, 640, 360);
		assertTrue(above.aboveAnchor());
		var area = above.place(220, 200);
		assertTrue(area.getY() + area.getHeight() < bottomAnchor.getY());
		assertEquals(200, area.getHeight());
	}

	@Test
	void resizingKeepsTheAnchorFacingEdgeFixedOnBothSides() {
		for (int anchorY : new int[]{2, 338}) {
			var placement = QuickMenuPlacement.create(new Rect2i(300, anchorY, 20, 20), 640, 360);
			var small = placement.place(180, 60);
			var large = placement.place(260, 240);
			assertEquals(small.getX() + small.getWidth(), large.getX() + large.getWidth());
			if (placement.aboveAnchor()) {
				assertEquals(small.getY() + small.getHeight(), large.getY() + large.getHeight());
			} else {
				assertEquals(small.getY(), large.getY());
			}
		}
	}

	@Test
	void staysInsideTheViewportWithoutCoveringTheAnchor() {
		for (int width : new int[]{140, 320, 640}) {
			for (int height : new int[]{100, 180, 360}) {
				for (int x : new int[]{0, width / 2, width - 20}) {
					for (int y : new int[]{0, height / 2, height - 20}) {
						var anchor = new Rect2i(x, y, 20, 20);
						var placement = QuickMenuPlacement.create(anchor, width, height);
						var area = placement.place(1000, 1000);
						assertFalse(area.isEmpty());
						assertTrue(area.getX() >= 2 && area.getY() >= 2);
						assertTrue(area.getX() + area.getWidth() <= width - 2);
						assertTrue(area.getY() + area.getHeight() <= height - 2);
						assertFalse(area.intersects(new ImmutableRect2i(x, y, 20, 20)));
					}
				}
			}
		}
	}

	@Test
	void unavailableAnchorsAndTinyViewportsHaveNoPlacement() {
		assertTrue(QuickMenuPlacement.create(new Rect2i(0, 0, 0, 20), 640, 360).place(220, 200).isEmpty());
		assertTrue(QuickMenuPlacement.create(new Rect2i(0, 0, 20, 0), 640, 360).place(220, 200).isEmpty());
		assertTrue(QuickMenuPlacement.create(new Rect2i(0, 0, 20, 20), 2, 2).place(220, 200).isEmpty());
	}
}
