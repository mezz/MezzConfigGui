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
	void guiSizeOptionControlsScreenBounds() {
		ConfigScreenLayout layout = new ConfigScreenLayout();
		EditBox searchBox = createSearchBox();

		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("guiSize", ConfigGuiOptions.GuiSize.WIDE)) {
			layout.updateScreenBounds(1000, 800, searchBox);
		}

		ImmutableRect2i area = layout.getArea();
		assertEquals(560, area.getWidth());
		assertEquals(420, area.getHeight());
		assertEquals(220, area.getX());
		assertEquals(190, area.getY());
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
