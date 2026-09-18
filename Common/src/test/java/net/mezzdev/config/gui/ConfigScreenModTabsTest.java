package net.mezzdev.config.gui;

import net.mezzdev.config.gui.screenlist.ConfigScreenListEntry;
import net.mezzdev.config.gui.screenlist.ConfigScreenOwnerIcon;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigScreenModTabsTest {
	private static final ImmutableRect2i SCREEN_AREA = new ImmutableRect2i(100, 20, 500, 128);

	@Test
	void selectsPageContainingActiveMod() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod7", createEntries(10));

		tabs.updateLayout(SCREEN_AREA);

		assertEquals(2, tabs.getPageNumber());
		assertEquals(4, tabs.getPageCount());
		assertEquals(List.of("mod6", "mod7", "mod8"), tabs.getVisibleModIds());
		assertEquals(new ImmutableRect2i(79, 24, 24, 120), tabs.getTabsArea());
	}

	@Test
	void resizingSelectsTheActiveModsNewPage() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod7", createEntries(10));
		tabs.updateLayout(new ImmutableRect2i(100, 20, 500, 320));
		assertEquals(0, tabs.getPageNumber());

		tabs.updateLayout(SCREEN_AREA);

		assertEquals(2, tabs.getPageNumber());
		assertEquals(List.of("mod6", "mod7", "mod8"), tabs.getVisibleModIds());
	}

	@Test
	void mouseWheelPagesAndWrapsOverTabStrip() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod0", createEntries(10));
		tabs.updateLayout(SCREEN_AREA);

		assertTrue(tabs.mouseScrolled(80, 25, 1));
		assertEquals(3, tabs.getPageNumber());
		assertEquals(List.of("mod9"), tabs.getVisibleModIds());

		assertTrue(tabs.mouseScrolled(80, 25, -1));
		assertEquals(0, tabs.getPageNumber());
		assertEquals(List.of("mod0", "mod1", "mod2"), tabs.getVisibleModIds());
		assertFalse(tabs.mouseScrolled(200, 25, -1));
	}

	@Test
	void pagingButtonsStayOutsideTheWindowAndKeepTheirPositionOnShortPages() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod0", createEntries(10));
		tabs.updateLayout(SCREEN_AREA);

		assertFalse(tabs.mouseClicked(SCREEN_AREA.getX(), 25, 0));
		assertFalse(tabs.mouseClicked(SCREEN_AREA.getX(), 138, 0));
		for (int page = 1; page <= 4; page++) {
			assertTrue(tabs.mouseClicked(90, 138, 0));
			assertTrue(tabs.mouseReleased(90, 138, 0).handled());
			assertEquals(page % 4, tabs.getPageNumber());
		}
		assertTrue(tabs.mouseClicked(90, 25, 0));
		tabs.mouseReleased(90, 25, 0);
		assertEquals(3, tabs.getPageNumber());
	}

	@Test
	void clickingAnotherModReturnsItsNavigationEntry() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod1", createEntries(4));
		tabs.updateLayout(new ImmutableRect2i(100, 0, 500, 176));

		assertTrue(tabs.mouseClicked(84, 5, 0));
		ConfigScreenModTabs.ClickResult result = tabs.mouseReleased(84, 5, 0);

		assertTrue(result.handled());
		assertTrue(result.playSound());
		assertEquals("mod0", result.entry().orElseThrow().modId());
	}

	@Test
	void selectedTabProtrudesBeyondInactiveTabsAndUsesItsWholeHitArea() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod1", createEntries(3));
		tabs.updateLayout(SCREEN_AREA);

		assertFalse(tabs.mouseClicked(80, 25, 0));
		assertTrue(tabs.mouseClicked(80, 49, 0));
		assertTrue(tabs.mouseReleased(80, 49, 0).handled());
		assertFalse(tabs.mouseClicked(80, 73, 0));
		assertTrue(tabs.mouseClicked(84, 73, 0));
		assertEquals("mod2", tabs.mouseReleased(84, 73, 0).entry().orElseThrow().modId());
	}

	@Test
	void resizeExclusionCoversTabsButLeavesPagingGapsAvailable() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod0", createEntries(10));
		tabs.updateLayout(SCREEN_AREA);

		assertFalse(tabs.getResizeExclusionArea(40).isEmpty());
		assertTrue(tabs.getResizeExclusionArea(37).isEmpty());
		assertTrue(tabs.getResizeExclusionArea(120).isEmpty());
		tabs.mouseScrolled(90, 25, 1);
		assertFalse(tabs.getResizeExclusionArea(40).isEmpty());
		assertTrue(tabs.getResizeExclusionArea(70).isEmpty());
	}

	private static List<ConfigScreenListEntry> createEntries(int count) {
		return IntStream.range(0, count)
			.mapToObj(ConfigScreenModTabsTest::createEntry)
			.toList();
	}

	private static ConfigScreenListEntry createEntry(int index) {
		String modId = "mod" + index;
		Component title = Component.literal("Mod " + index);
		return new ConfigScreenListEntry(
			modId,
			title,
			parent -> {
				throw new AssertionError("Factory must not be called by tab layout tests");
			},
			new ConfigScreenOwnerIcon(modId, title, Optional.empty())
		);
	}
}
