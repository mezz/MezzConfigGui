package net.mezzdev.config.gui;

import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.config.ConfigGuiOptionsTestUtil;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigInfoPanelLayoutTest {
	@Test
	@SuppressWarnings("DataFlowIssue")
	void growingInfoLeavesTheWindowAndRowOriginsInPlaceAndUpdatesScrolling() {
		try (var ignored = ConfigGuiOptionsTestUtil.setValue("guiMode", ConfigGuiOptions.GuiMode.FULLSCREEN)) {
			ConfigScreenLayout layout = new ConfigScreenLayout();
			EditBox searchBox = new EditBox(null, 0, 0, 100, 18, Component.empty());
			layout.updateScreenBounds(800, 600, searchBox);
			ImmutableRect2i window = layout.getArea();
			ImmutableRect2i content = layout.getContentArea();
			ImmutableRect2i navigation = layout.getNavArea();
			layout.setTotalContentHeight(content.getHeight());
			layout.setTotalNavHeight(navigation.getHeight());
			assertTrue(layout.getScrollBarArea().isEmpty());
			assertTrue(layout.getNavScrollBarArea().isEmpty());

			assertTrue(layout.requestInfoAreaHeight(150));
			layout.updateScreenBounds(800, 600, searchBox);
			layout.setTotalNavHeight(navigation.getHeight());

			assertEquals(150, layout.getInfoArea().getHeight());
			assertEquals(window, layout.getArea());
			assertEquals(content.getY(), layout.getContentArea().getY());
			assertEquals(navigation.getY(), layout.getNavArea().getY());
			assertEquals(content.getHeight() - (150 - ConfigScreenLayout.INFO_AREA_HEIGHT), layout.getContentArea().getHeight());
			assertFalse(layout.getContentArea().intersects(layout.getInfoArea()));
			assertFalse(layout.getNavArea().intersects(layout.getInfoArea()));
			assertFalse(layout.getScrollBarArea().isEmpty());
			assertFalse(layout.getNavScrollBarArea().isEmpty());

			assertFalse(layout.requestInfoAreaHeight(25));
			assertFalse(layout.requestInfoAreaHeight(150));
		}
	}

	@Test
	@SuppressWarnings("DataFlowIssue")
	void longDescriptionsAreCappedAndTheCapFollowsWindowResizing() {
		try (var ignored = ConfigGuiOptionsTestUtil.setValue("guiMode", ConfigGuiOptions.GuiMode.FULLSCREEN)) {
			ConfigScreenLayout layout = new ConfigScreenLayout();
			EditBox searchBox = new EditBox(null, 0, 0, 100, 18, Component.empty());
			layout.updateScreenBounds(800, 600, searchBox);
			assertTrue(layout.requestInfoAreaHeight(10000));
			layout.updateScreenBounds(800, 600, searchBox);
			assertEquals((600 - 12) / 3, layout.getInfoArea().getHeight());
			assertFalse(layout.requestInfoAreaHeight(20000));

			layout.updateScreenBounds(320, 240, searchBox);
			assertEquals((240 - 12) / 3, layout.getInfoArea().getHeight());
			assertTrue(layout.getContentArea().getHeight() > layout.getInfoArea().getHeight());
			assertFalse(layout.getContentArea().intersects(layout.getInfoArea()));
		}
	}
}
