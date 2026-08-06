package net.mezzdev.config.gui;

import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.config.ConfigGuiOptionsTestUtil;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigScreenLayoutOptionsTest {
	@Test
	void smallGuiSizeOptionControlsScreenBounds() {
		ConfigScreenLayout layout = new ConfigScreenLayout();
		EditBox searchBox = createSearchBox();

		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("guiSize", ConfigGuiOptions.GuiSize.SMALL)) {
			layout.updateScreenBounds(1000, 800, searchBox);
		}

		ImmutableRect2i area = layout.getArea();
		assertEquals(340, area.getWidth());
		assertEquals(260, area.getHeight());
		assertEquals(330, area.getX());
		assertEquals(270, area.getY());
	}

	@Test
	void fullscreenGuiSizeOptionFillsScreenBounds() {
		ConfigScreenLayout layout = new ConfigScreenLayout();
		EditBox searchBox = createSearchBox();

		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("guiSize", ConfigGuiOptions.GuiSize.FULLSCREEN)) {
			layout.updateScreenBounds(1000, 800, searchBox);
		}

		ImmutableRect2i area = layout.getArea();
		assertEquals(1000, area.getWidth());
		assertEquals(800, area.getHeight());
		assertEquals(0, area.getX());
		assertEquals(0, area.getY());
	}

	@Test
	void draggingResizeHandleUpdatesScreenBounds() {
		ConfigScreenLayout layout = new ConfigScreenLayout();
		EditBox searchBox = createSearchBox();

		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("guiSize", ConfigGuiOptions.GuiSize.MEDIUM)) {
			layout.updateScreenBounds(1000, 800, searchBox);
		}

		ImmutableRect2i area = layout.getArea();
		double resizeX = area.getX() + area.getWidth() - 1;
		double resizeY = area.getY() + area.getHeight() - 1;
		assertEquals(ConfigScreenLayout.ResizeHandle.BOTTOM_RIGHT, layout.getResizeHandle(resizeX, resizeY));

		assertTrue(layout.startResizeDrag(resizeX, resizeY));
		assertTrue(layout.dragResize(area.getX() + area.getWidth() + 80, area.getY() + area.getHeight() + 60, 1000, 800));
		layout.updateScreenBounds(1000, 800, searchBox);

		ImmutableRect2i resizedArea = layout.getArea();
		assertEquals(area.getX(), resizedArea.getX());
		assertEquals(area.getY(), resizedArea.getY());
		assertEquals(area.getWidth() + 80, resizedArea.getWidth());
		assertEquals(area.getHeight() + 60, resizedArea.getHeight());
		assertTrue(layout.stopResizeDrag());
	}

	@Test
	void scrollSpeedAndSmoothScrollingOptionsControlWheelScrollStep() {
		ConfigScreenLayout layout = createScrollableLayout();

		try (ConfigGuiOptionsTestUtil.OptionOverride scrollSpeed = ConfigGuiOptionsTestUtil.setValue("scrollSpeed", 17);
			ConfigGuiOptionsTestUtil.OptionOverride smoothScrolling = ConfigGuiOptionsTestUtil.setValue("smoothScrolling", false)
		) {
			ImmutableRect2i contentArea = layout.getContentArea();
			assertTrue(layout.scroll(contentArea.getX() + 1, contentArea.getY() + 1, -1));
			assertTrue(layout.stepContentScroll());
			assertEquals(17, layout.getCurrentScrollY());

			ImmutableRect2i navArea = layout.getNavArea();
			assertTrue(layout.scroll(navArea.getX() + 1, navArea.getY() + 1, -1));
			assertTrue(layout.stepNavScroll());
			assertEquals(17, layout.getNavCurrentScrollY());
		}
	}

	@Test
	void dragAutoScrollSpeedOptionControlsDragAutoScrollAmount() {
		ConfigScreenLayout layout = createScrollableLayout();

		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("dragAutoScrollSpeed", 9)) {
			ImmutableRect2i contentArea = layout.getContentArea();
			double bottomOutsideAutoScrollEdge = contentArea.getY() + contentArea.getHeight() + 1;

			assertTrue(layout.autoScrollContentForDrag(bottomOutsideAutoScrollEdge));
			assertEquals(9, layout.getCurrentScrollY());
		}
	}

	private static ConfigScreenLayout createScrollableLayout() {
		ConfigScreenLayout layout = new ConfigScreenLayout();
		layout.updateScreenBounds(1000, 800, createSearchBox());
		layout.setTotalContentHeight(layout.getContentArea().getHeight() + 100);
		layout.setTotalNavHeight(layout.getNavArea().getHeight() + 100);
		return layout;
	}

	@SuppressWarnings("DataFlowIssue")
	private static EditBox createSearchBox() {
		return new EditBox(null, 0, 0, 0, 0, Component.empty());
	}
}
