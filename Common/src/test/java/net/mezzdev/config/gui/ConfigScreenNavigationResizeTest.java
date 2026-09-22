package net.mezzdev.config.gui;

import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.config.ConfigGuiOptionsTestUtil;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigScreenNavigationResizeTest {
	@Test
	void dividerDragRelayoutsBothColumnsWithoutMovingTheWindowOrJumpingOnClick() {
		ConfigScreenLayout layout = new ConfigScreenLayout();
		LegacyEditBox searchBox = createSearchBox();
		layout.updateScreenBounds(1000, 800, searchBox);
		ImmutableRect2i area = layout.getArea();
		ImmutableRect2i nav = layout.getNavArea();
		ImmutableRect2i content = layout.getContentArea();
		int searchX = searchBox.getX();
		ImmutableRect2i divider = layout.getNavDividerArea();
		double mouseX = divider.getX() + divider.getWidth() - 1;

		assertTrue(layout.startNavigationResize(mouseX, divider.getY() + 1));
		assertFalse(layout.dragNavigationResize(mouseX));
		assertTrue(layout.dragNavigationResize(mouseX + 50));
		layout.updateScreenBounds(1000, 800, searchBox);

		assertEquals(area, layout.getArea());
		assertEquals(nav.getWidth() + 50, layout.getNavArea().getWidth());
		assertEquals(content.getX() + 50, layout.getContentArea().getX());
		assertEquals(content.getWidth() - 50, layout.getContentArea().getWidth());
		assertEquals(searchX + 50, searchBox.getX());
		assertEquals(nav.getWidth() + 50, layout.finishNavigationResize().orElseThrow());
		assertFalse(layout.dragNavigationResize(mouseX + 100));
	}

	@Test
	void draggingPastEitherEdgePreservesUsableColumnWidths() {
		ConfigScreenLayout layout = new ConfigScreenLayout();
		LegacyEditBox searchBox = createSearchBox();
		layout.updateScreenBounds(1000, 800, searchBox);
		ImmutableRect2i divider = layout.getNavDividerArea();
		assertTrue(layout.startNavigationResize(divider.getX(), divider.getY()));

		assertTrue(layout.dragNavigationResize(10000));
		layout.updateScreenBounds(1000, 800, searchBox);
		assertEquals(160, layout.getContentArea().getWidth());
		assertTrue(searchBox.getWidth() > 0);
		assertFalse(layout.getNavArea().intersects(layout.getContentArea()));

		assertTrue(layout.dragNavigationResize(-10000));
		layout.updateScreenBounds(1000, 800, searchBox);
		assertEquals(ConfigGuiOptions.MIN_NAVIGATION_WIDTH, layout.getNavArea().getWidth());
		assertFalse(layout.getNavArea().intersects(layout.getContentArea()));
	}

	@Test
	void savedWidthSurvivesReopeningAndTemporaryWindowClamping() {
		try (ConfigGuiOptionsTestUtil.OptionOverride mode = ConfigGuiOptionsTestUtil.setValue("guiMode", ConfigGuiOptions.GuiMode.FULLSCREEN);
			ConfigGuiOptionsTestUtil.OptionOverride width = ConfigGuiOptionsTestUtil.setValue("navigationWidth", 110)
		) {
			ConfigScreenLayout layout = new ConfigScreenLayout();
			LegacyEditBox searchBox = createSearchBox();
			layout.updateScreenBounds(1000, 800, searchBox);
			ImmutableRect2i divider = layout.getNavDividerArea();
			assertTrue(layout.startNavigationResize(divider.getX(), divider.getY()));
			assertTrue(layout.dragNavigationResize(divider.getX() + 190));
			layout.updateScreenBounds(1000, 800, searchBox);
			assertEquals(110, ConfigGuiOptions.getNavigationWidth());
			layout.finishNavigationResize().ifPresent(ConfigGuiOptions::setNavigationWidth);

			ConfigScreenLayout reopened = new ConfigScreenLayout();
			reopened.updateScreenBounds(1000, 800, searchBox);
			assertEquals(300, reopened.getNavArea().getWidth());
			assertEquals(ConfigGuiOptions.GuiMode.FULLSCREEN, ConfigGuiOptions.getGuiMode());

			reopened.updateScreenBounds(320, 240, searchBox, true, 35, 2);
			assertTrue(reopened.getNavArea().getWidth() < 300);
			assertEquals(160, reopened.getContentArea().getWidth());
			assertEquals(300, ConfigGuiOptions.getNavigationWidth());
			reopened.updateScreenBounds(1000, 800, searchBox);
			assertEquals(300, reopened.getNavArea().getWidth());
		}
	}

	@Test
	void dividerDoesNotTakeClicksFromNavigationScrollbarOrSearch() {
		ConfigScreenLayout layout = new ConfigScreenLayout();
		layout.updateScreenBounds(1000, 800, createSearchBox());
		layout.setTotalNavHeight(1000);
		ImmutableRect2i navScrollbar = layout.getNavScrollBarArea();
		assertFalse(layout.startNavigationResize(navScrollbar.getX(), navScrollbar.getY()));
		assertTrue(layout.startNavScrollDrag(navScrollbar.getX(), navScrollbar.getY()));
		layout.stopNavScrollDrag();
		ImmutableRect2i search = layout.getSearchBackgroundArea();
		assertFalse(layout.startNavigationResize(search.getX(), search.getY()));
		ImmutableRect2i divider = layout.getNavDividerArea();
		assertFalse(layout.startNavigationResize(divider.getX(), divider.getY() - 1));
		assertTrue(layout.startNavigationResize(divider.getX(), divider.getY()));
		assertTrue(layout.finishNavigationResize().isEmpty());
	}

	@Test
	void clickingClampedDividerWithoutDraggingPreservesThePreferredWidth() {
		try (ConfigGuiOptionsTestUtil.OptionOverride width = ConfigGuiOptionsTestUtil.setValue("navigationWidth", 600)) {
			ConfigScreenLayout layout = new ConfigScreenLayout();
			layout.updateScreenBounds(320, 240, createSearchBox());
			ImmutableRect2i divider = layout.getNavDividerArea();
			assertTrue(layout.startNavigationResize(divider.getX(), divider.getY()));
			assertFalse(layout.dragNavigationResize(divider.getX()));
			assertTrue(layout.finishNavigationResize().isEmpty());
			assertEquals(600, ConfigGuiOptions.getNavigationWidth());
		}
	}

	@Test
	void dividerWorksWhenOuterWindowResizingIsDisabled() {
		try (ConfigGuiOptionsTestUtil.OptionOverride resizing = ConfigGuiOptionsTestUtil.setValue("enableWindowResizing", false)) {
			ConfigScreenLayout layout = new ConfigScreenLayout();
			layout.updateScreenBounds(1000, 800, createSearchBox());
			ImmutableRect2i divider = layout.getNavDividerArea();
			assertTrue(layout.startNavigationResize(divider.getX(), divider.getY()));
			assertTrue(layout.dragNavigationResize(divider.getX() + 40));
			assertEquals(150, layout.finishNavigationResize().orElseThrow());
		}
	}

	@SuppressWarnings("DataFlowIssue")
	private static LegacyEditBox createSearchBox() {
		return new LegacyEditBox(null, 0, 0, 0, 0, Component.empty());
	}
}
