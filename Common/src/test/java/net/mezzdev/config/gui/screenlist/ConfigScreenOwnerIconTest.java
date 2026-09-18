package net.mezzdev.config.gui.screenlist;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigScreenOwnerIconTest {
	@Test
	void placeholdersUseUpToThreeWordInitialsOrTheFirstThreeLetters() {
		assertEquals("JEI", initials("Just Enough Items", "jei"));
		assertEquals("MCG", initials("MezzConfigGui", "mezz_config_gui"));
		assertEquals("ABG", initials("Alpha Beta Gamma Delta", "test"));
		assertEquals("AS", initials("AppleSkin", "appleskin"));
		assertEquals("CRE", initials("Create", "create"));
		assertEquals("X", initials("X", "x"));
	}

	@Test
	void punctuationAndMissingNamesFallBackToUsefulLetters() {
		assertEquals("MC", initials(" [My Config] ", "test"));
		assertEquals("TM", initials("", "test_mod"));
		assertEquals("TM", initials("!!!", "test-mod"));
		assertEquals("TMN", initials("!!!", "test_mod_name"));
		assertEquals("?", initials("", ""));
	}

	@Test
	void initialsPreserveUnicodeCodePoints() {
		assertEquals("日本語", initials("日本語", "test"));
		assertEquals("𐐀AN", initials("𐐀animals", "test"));
	}

	private static String initials(String name, String modId) {
		return ConfigScreenOwnerIcon.getInitials(Component.literal(name), modId);
	}
}
