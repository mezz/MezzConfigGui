package net.mezzdev.config.gui.screenlist;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigScreenOwnerIconTest {
	@Test
	void placeholdersUseTwoWordInitialsOrTheFirstTwoLetters() {
		assertEquals("JE", initials("Just Enough Items", "jei"));
		assertEquals("AS", initials("AppleSkin", "appleskin"));
		assertEquals("CR", initials("Create", "create"));
		assertEquals("X", initials("X", "x"));
	}

	@Test
	void punctuationAndMissingNamesFallBackToUsefulLetters() {
		assertEquals("MC", initials(" [My Config] ", "test"));
		assertEquals("TM", initials("", "test_mod"));
		assertEquals("TM", initials("!!!", "test-mod"));
		assertEquals("?", initials("", ""));
	}

	@Test
	void initialsPreserveUnicodeCodePoints() {
		assertEquals("日本", initials("日本語", "test"));
		assertEquals("𐐀A", initials("𐐀animals", "test"));
	}

	private static String initials(String name, String modId) {
		return ConfigScreenOwnerIcon.getInitials(Component.literal(name), modId);
	}
}
