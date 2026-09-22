package net.mezzdev.config.gui;

import net.mezzdev.config.gui.screenlist.ConfigScreenListEntry;
import net.mezzdev.config.gui.screenlist.ConfigScreenOwnerIcon;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigScreenModTabsTest {
	private static final ImmutableRect2i SCREEN_AREA = new ImmutableRect2i(100, 20, 500, 188);

	@Test
	void initiallyShowsTheActiveMod() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod7", createEntries(10));

		tabs.updateLayout(SCREEN_AREA);

		assertEquals(List.of("mod5", "mod6", "mod7", "mod8"), tabs.getVisibleModIds());
		assertEquals(new ImmutableRect2i(67, 24, 36, 180), tabs.getTabsArea());
	}

	@Test
	void resizingScrollsOnlyAsFarAsNeededToKeepTheActiveModVisible() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod7", createEntries(10));
		tabs.updateLayout(new ImmutableRect2i(100, 20, 500, 400));
		assertEquals(10, tabs.getVisibleModIds().size());

		tabs.updateLayout(SCREEN_AREA);

		assertEquals(List.of("mod4", "mod5", "mod6", "mod7"), tabs.getVisibleModIds());
		tabs.updateLayout(new ImmutableRect2i(100, 20, 500, 220));
		assertEquals(List.of("mod4", "mod5", "mod6", "mod7", "mod8"), tabs.getVisibleModIds());
	}

	@Test
	void switchingModsKeepsTheClickedTabInItsExistingSlot() {
		List<ConfigScreenListEntry> entries = createEntries(10);
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod0", entries);
		tabs.updateLayout(SCREEN_AREA);
		tabs.mouseScrolled(80, 25, -2);
		assertEquals(List.of("mod2", "mod3", "mod4", "mod5"), tabs.getVisibleModIds());
		assertTrue(tabs.mouseClicked(84, 130, 0));
		ConfigScreenListEntry clicked = Objects.requireNonNull(tabs.mouseReleased(84, 130, 0).entry());
		assertEquals("mod4", clicked.modId());

		ConfigScreenModTabs next = new ConfigScreenModTabs(clicked.modId(), entries);
		next.copyScrollPositionFrom(tabs);
		next.updateLayout(SCREEN_AREA);
		assertEquals(tabs.getVisibleModIds(), next.getVisibleModIds());
		assertTrue(next.mouseClicked(84, 60, 0));
		assertEquals("mod2", Objects.requireNonNull(next.mouseReleased(84, 60, 0).entry()).modId());
		ConfigScreenModTabs back = new ConfigScreenModTabs("mod2", entries);
		back.copyScrollPositionFrom(next);
		back.updateLayout(SCREEN_AREA);
		assertEquals(tabs.getVisibleModIds(), back.getVisibleModIds());
	}

	@Test
	void transferredViewportTracksTheFirstVisibleModWhenTheListChanges() {
		List<ConfigScreenListEntry> entries = createEntries(10);
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod4", entries);
		tabs.updateLayout(SCREEN_AREA);
		assertEquals(List.of("mod2", "mod3", "mod4", "mod5"), tabs.getVisibleModIds());

		ConfigScreenModTabs changed = new ConfigScreenModTabs("mod5", entries.subList(1, entries.size()));
		changed.copyScrollPositionFrom(tabs);
		changed.updateLayout(SCREEN_AREA);
		assertEquals(tabs.getVisibleModIds(), changed.getVisibleModIds());
	}

	@Test
	void transferredViewportClampsWhenItsAnchorWasRemoved() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod8", createEntries(10));
		tabs.updateLayout(SCREEN_AREA);
		ConfigScreenModTabs changed = new ConfigScreenModTabs("mod1", createEntries(6));
		changed.copyScrollPositionFrom(tabs);
		changed.updateLayout(SCREEN_AREA);
		assertEquals(List.of("mod1", "mod2", "mod3", "mod4"), changed.getVisibleModIds());
	}

	@Test
	void mouseWheelMovesOneTabAtATimeAndStopsAtTheEnds() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod0", createEntries(10));
		tabs.updateLayout(SCREEN_AREA);

		assertTrue(tabs.mouseScrolled(80, 25, 1));
		assertEquals(List.of("mod0", "mod1", "mod2", "mod3"), tabs.getVisibleModIds());

		assertTrue(tabs.mouseScrolled(80, 25, -1));
		assertEquals(List.of("mod1", "mod2", "mod3", "mod4"), tabs.getVisibleModIds());
		assertTrue(tabs.mouseScrolled(80, 25, 1));
		assertEquals(List.of("mod0", "mod1", "mod2", "mod3"), tabs.getVisibleModIds());
		tabs.mouseScrolled(80, 25, -100);
		assertEquals(List.of("mod6", "mod7", "mod8", "mod9"), tabs.getVisibleModIds());
		tabs.mouseScrolled(80, 25, -1);
		assertEquals(List.of("mod6", "mod7", "mod8", "mod9"), tabs.getVisibleModIds());
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
			assertTrue(tabs.mouseClicked(90, 190, 0));
			assertTrue(tabs.mouseReleased(90, 190, 0).handled());
			assertEquals("mod" + Math.min(firstIndex, 6), tabs.getVisibleModIds().getFirst());
		}
		assertTrue(tabs.mouseClicked(90, 25, 0));
		tabs.mouseReleased(90, 25, 0);
		assertEquals("mod5", tabs.getVisibleModIds().getFirst());
	}

	@Test
	void applyingModPreferencesUpdatesTabsAndClearsRemovedClickTargets() {
		List<ConfigScreenListEntry> entries = createEntries(10);
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod0", entries);
		tabs.updateLayout(SCREEN_AREA);
		tabs.mouseScrolled(80, 25, -2);
		assertTrue(tabs.mouseClicked(84, 60, 0));

		tabs.updateEntries(entries.subList(3, entries.size()));

		assertFalse(tabs.isPressing());
		assertFalse(tabs.mouseReleased(84, 60, 0).handled());
		assertFalse(tabs.getVisibleModIds().contains("mod2"));
		tabs.updateEntries(List.of());
		assertTrue(tabs.getTabsArea().isEmpty());
		assertTrue(tabs.getVisibleModIds().isEmpty());
		tabs.updateEntries(entries);
		assertFalse(tabs.getTabsArea().isEmpty());
		assertFalse(tabs.getVisibleModIds().isEmpty());
	}

	@Test
	void clickingAnotherModReturnsItsNavigationEntry() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod1", createEntries(4));
		tabs.updateLayout(new ImmutableRect2i(100, 0, 500, 176));

		assertTrue(tabs.mouseClicked(84, 5, 0));
		ConfigScreenModTabs.ClickResult result = tabs.mouseReleased(84, 5, 0);

		assertTrue(result.handled());
		assertTrue(result.playSound());
		assertEquals("mod0", Objects.requireNonNull(result.entry()).modId());
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
		assertEquals("mod2", Objects.requireNonNull(tabs.mouseReleased(72, 89, 0).entry()).modId());
	}

	@Test
	void resizeExclusionCoversTabsButLeavesPagingGapsAvailable() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod0", createEntries(10));
		tabs.updateLayout(SCREEN_AREA);

		assertFalse(tabs.getResizeExclusionArea(60).isEmpty());
		assertTrue(tabs.getResizeExclusionArea(45).isEmpty());
		assertTrue(tabs.getResizeExclusionArea(175).isEmpty());
		tabs.mouseScrolled(90, 25, -100);
		assertFalse(tabs.getResizeExclusionArea(60).isEmpty());
		assertTrue(tabs.getResizeExclusionArea(175).isEmpty());
	}

	@Test
	void scrollButtonsHaveFullSquareHitAreas() {
		ConfigScreenModTabs tabs = new ConfigScreenModTabs("mod0", createEntries(10));
		tabs.updateLayout(SCREEN_AREA);

		assertTrue(tabs.mouseClicked(94, 203, 0));
		assertTrue(tabs.mouseReleased(94, 203, 0).playSound());
		assertEquals("mod1", tabs.getVisibleModIds().getFirst());
		assertTrue(tabs.mouseClicked(94, 43, 0));
		assertTrue(tabs.mouseReleased(94, 43, 0).playSound());
		assertEquals("mod0", tabs.getVisibleModIds().getFirst());
		assertFalse(tabs.mouseClicked(95, 43, 0));
		assertFalse(tabs.mouseClicked(74, 43, 0));
		assertFalse(tabs.mouseClicked(94, 44, 0));
		assertFalse(tabs.mouseClicked(94, 183, 0));
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
			new ConfigScreenOwnerIcon(modId, title, null)
		);
	}
}
