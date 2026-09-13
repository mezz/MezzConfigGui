package net.mezzdev.config.gui.entries;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigEntryWidgetLayoutTest {
	@Test
	void valueColumnUsesAConsistentPreferredWidth() {
		assertEquals(110, ConfigEntryWidget.getValueColumnWidth(241, 8));
		assertEquals(110, ConfigEntryWidget.getValueColumnWidth(300, 70));
	}

	@Test
	void valueColumnShrinksToProtectTheName() {
		assertEquals(80, ConfigEntryWidget.getValueColumnWidth(191, 8));
		assertEquals(70, ConfigEntryWidget.getValueColumnWidth(181, 8));
	}

	@Test
	void valueColumnKeepsTheControlsUsableWhenSpaceIsTight() {
		assertEquals(78, ConfigEntryWidget.getValueColumnWidth(181, 78));
	}
}
