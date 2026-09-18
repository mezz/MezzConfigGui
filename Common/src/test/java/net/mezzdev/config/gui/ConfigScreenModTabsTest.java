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
	private static final ImmutableRect2i SCREEN_AREA = new ImmutableRect2i(100, 20, 500, 188);

	@Test
	void initiallyShowsTheActiveMod() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod7", createEntries(10));

		tabs.updateLayout(SCREEN_AREA);

		assertEquals(List.of("mod6", "mod7", "mod8"), tabs.getVisibleModIds());
		assertEquals(new ImmutableRect2i(67, 24, 36, 180), tabs.getTabsArea());
	}

	@Test
	void resizingKeepsTheActiveModVisible() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod7", createEntries(10));
		tabs.updateLayout(new ImmutableRect2i(100, 20, 500, 400));
		assertEquals(10, tabs.getVisibleModIds().size());

		tabs.updateLayout(SCREEN_AREA);

		assertEquals(List.of("mod6", "mod7", "mod8"), tabs.getVisibleModIds());
	}

	@Test
	void mouseWheelMovesOneTabAtATimeAndStopsAtTheEnds() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod0", createEntries(10));
		tabs.updateLayout(SCREEN_AREA);

		assertTrue(tabs.mouseScrolled(80, 25, 1));
		assertEquals(List.of("mod0", "mod1", "mod2"), tabs.getVisibleModIds());

		assertTrue(tabs.mouseScrolled(80, 25, -1));
		assertEquals(List.of("mod1", "mod2", "mod3"), tabs.getVisibleModIds());
		assertTrue(tabs.mouseScrolled(80, 25, 1));
		assertEquals(List.of("mod0", "mod1", "mod2"), tabs.getVisibleModIds());
		tabs.mouseScrolled(80, 25, -100);
		assertEquals(List.of("mod7", "mod8", "mod9"), tabs.getVisibleModIds());
		tabs.mouseScrolled(80, 25, -1);
		assertEquals(List.of("mod7", "mod8", "mod9"), tabs.getVisibleModIds());
		assertFalse(tabs.mouseScrolled(200, 25, -1));
	}

	@Test
	void fractionalTrackpadMovementAccumulatesToOneTab() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod0", createEntries(10));
		tabs.updateLayout(SCREEN_AREA);
		for (int event = 0; event < 3; event++) {
			assertTrue(tabs.mouseScrolled(80, 25, -0.25));
			assertEquals("mod0", tabs.getVisibleModIds().getFirst());
		}
		tabs.mouseScrolled(80, 25, -0.25);
		assertEquals("mod1", tabs.getVisibleModIds().getFirst());
		assertFalse(tabs.mouseScrolled(80, 25, Double.NaN));
	}

	@Test
	void scrollingButtonsStayOutsideTheWindowAndMoveOneTab() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod0", createEntries(10));
		tabs.updateLayout(SCREEN_AREA);

		assertFalse(tabs.mouseClicked(SCREEN_AREA.getX(), 25, 0));
		assertFalse(tabs.mouseClicked(SCREEN_AREA.getX(), 180, 0));
		for (int firstIndex = 1; firstIndex <= 8; firstIndex++) {
			assertTrue(tabs.mouseClicked(90, 180, 0));
			assertTrue(tabs.mouseReleased(90, 180, 0).handled());
			assertEquals("mod" + Math.min(firstIndex, 7), tabs.getVisibleModIds().getFirst());
		}
		assertTrue(tabs.mouseClicked(90, 25, 0));
		tabs.mouseReleased(90, 25, 0);
		assertEquals("mod6", tabs.getVisibleModIds().getFirst());
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

		assertFalse(tabs.mouseClicked(68, 25, 0));
		assertTrue(tabs.mouseClicked(68, 57, 0));
		assertTrue(tabs.mouseReleased(68, 57, 0).handled());
		assertFalse(tabs.mouseClicked(68, 89, 0));
		assertTrue(tabs.mouseClicked(72, 89, 0));
		assertEquals("mod2", tabs.mouseReleased(72, 89, 0).entry().orElseThrow().modId());
	}

	@Test
	void resizeExclusionCoversTabsButLeavesPagingGapsAvailable() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod0", createEntries(10));
		tabs.updateLayout(SCREEN_AREA);

		assertFalse(tabs.getResizeExclusionArea(60).isEmpty());
		assertTrue(tabs.getResizeExclusionArea(57).isEmpty());
		assertTrue(tabs.getResizeExclusionArea(165).isEmpty());
		tabs.mouseScrolled(90, 25, -100);
		assertFalse(tabs.getResizeExclusionArea(60).isEmpty());
		assertTrue(tabs.getResizeExclusionArea(165).isEmpty());
	}

	@Test
	void scrollButtonsHaveFullSquareHitAreas() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod0", createEntries(10));
		tabs.updateLayout(SCREEN_AREA);

		assertTrue(tabs.mouseClicked(98, 203, 0));
		assertTrue(tabs.mouseReleased(98, 203, 0).playSound());
		assertEquals("mod1", tabs.getVisibleModIds().getFirst());
		assertTrue(tabs.mouseClicked(98, 55, 0));
		assertTrue(tabs.mouseReleased(98, 55, 0).playSound());
		assertEquals("mod0", tabs.getVisibleModIds().getFirst());
		assertFalse(tabs.mouseClicked(98, 56, 0));
		assertFalse(tabs.mouseClicked(98, 171, 0));
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
