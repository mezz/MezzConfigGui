package net.mezzdev.config.gui;

import net.mezzdev.config.gui.util.ImmutableRect2i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigScreenViewTest {
	private static final ImmutableRect2i CONTENT_AREA = new ImmutableRect2i(20, 30, 100, 80);

	@Test
	void entryHoverIsLimitedToTheScissoredContentArea() {
		assertTrue(ConfigScreenView.isEntryHoverAllowed(CONTENT_AREA, 50, 50, true));
		assertFalse(ConfigScreenView.isEntryHoverAllowed(CONTENT_AREA, 50, 115, true));
		assertFalse(ConfigScreenView.isEntryHoverAllowed(CONTENT_AREA, 50, 50, false));
	}
}
