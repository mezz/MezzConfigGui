package net.mezzdev.config.gui;

import net.mezzdev.config.gui.config.ConfigGuiOptionsTestUtil;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigScrollStateTest {
	private static final ImmutableRect2i VIEWPORT = new ImmutableRect2i(20, 30, 100, 100);

	@ParameterizedTest
	@CsvSource({"10, 1, 12", "3, 0, 6"})
	void draggingKeepsTheGrabOffsetAndReachesBothEnds(int minimumHeight, int inset, int width) {
		ConfigScrollState scroll = new ConfigScrollState(minimumHeight, inset);
		ImmutableRect2i bar = VIEWPORT.keepRight(width);
		scroll.updateBounds(VIEWPORT, 500, bar);
		ImmutableRect2i marker = scroll.getMarkerArea();
		double x = marker.getX() + marker.getWidth() / 2.0;
		double y = marker.getY() + 2;
		assertTrue(scroll.startDrag(x, y));
		assertTrue(scroll.drag(y));
		assertEquals(0, scroll.getPosition());

		assertTrue(scroll.drag(1000));
		assertEquals(400, scroll.getPosition());
		marker = scroll.getMarkerArea();
		assertEquals(bar.getY() + bar.getHeight() - inset, marker.getY() + marker.getHeight());
		assertTrue(scroll.stopDrag());
		assertFalse(scroll.drag(0));
		assertEquals(400, scroll.getPosition());

		y = marker.getY() + 3;
		assertTrue(scroll.startDrag(x, y));
		assertEquals(400, scroll.getPosition());
		assertTrue(scroll.drag(y));
		assertEquals(400, scroll.getPosition());
		assertTrue(scroll.drag(-1000));
		assertEquals(0, scroll.getPosition());
	}

	@ParameterizedTest
	@CsvSource({"10, 1, 12", "3, 0, 6"})
	void trackClickCentersTheMarkerAndTinyTracksStayBounded(int minimumHeight, int inset, int width) {
		ConfigScrollState scroll = new ConfigScrollState(minimumHeight, inset);
		ImmutableRect2i bar = VIEWPORT.keepRight(width);
		scroll.updateBounds(VIEWPORT, 500, bar);
		double x = bar.getX() + width / 2.0;
		double y = bar.getY() + bar.getHeight() / 2.0;
		assertTrue(scroll.startDrag(x, y));
		ImmutableRect2i marker = scroll.getMarkerArea();
		assertEquals(y, marker.getY() + marker.getHeight() / 2.0, 1);
		scroll.stopDrag();
		assertFalse(scroll.startDrag(VIEWPORT.getX(), y));

		ImmutableRect2i tinyViewport = VIEWPORT.keepTop(3);
		scroll.updateBounds(tinyViewport, 500, tinyViewport.keepRight(width));
		marker = scroll.getMarkerArea();
		assertTrue(marker.getHeight() <= tinyViewport.getHeight());
		assertTrue(scroll.startDrag(x, marker.getY()));
		assertTrue(scroll.drag(1000));
		assertTrue(Double.isFinite(scroll.getPosition()));
		assertTrue(scroll.getMarkerArea().getY() >= tinyViewport.getY());
		assertTrue(scroll.getMarkerArea().getY() + scroll.getMarkerArea().getHeight() <= tinyViewport.getY() + tinyViewport.getHeight());
	}

	@ParameterizedTest
	@CsvSource({"true", "false"})
	void shrinkingContentClampsBothTheDisplayedAndPendingScroll(boolean smooth) {
		try (var ignored = ConfigGuiOptionsTestUtil.setValue("smoothScrolling", smooth)) {
			ConfigScrollState scroll = new ConfigScrollState(3, 0);
			scroll.updateBounds(VIEWPORT, 500, VIEWPORT.keepRight(6));
			scroll.scroll(-1000);
			assertTrue(scroll.step());
			assertTrue(scroll.getPosition() > 100);
			assertTrue(scroll.updateBounds(VIEWPORT, 150, VIEWPORT.keepRight(6)));
			assertEquals(50, scroll.getPosition());
			assertFalse(scroll.step());

			assertTrue(scroll.updateBounds(VIEWPORT, 50, VIEWPORT.keepRight(6)));
			assertTrue(scroll.getScrollbarArea().isEmpty());
			assertTrue(scroll.getMarkerArea().isEmpty());
			assertFalse(scroll.startDrag(119, 31));
			scroll.updateBounds(VIEWPORT, 500, VIEWPORT.keepRight(6));
			assertFalse(scroll.step());
			assertEquals(0, scroll.getPosition());
		}
	}
}
