package net.mezzdev.config.gui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigNameUtilTest {

	@Test
	void formatsCamelCaseName() {
		assertEquals("Enable Cheat Mode For Op", ConfigNameUtil.getDisplayNameFallback("enableCheatModeForOp"));
	}

	@Test
	void formatsSeparatedName() {
		assertEquals("Enable Cheat Mode", ConfigNameUtil.getDisplayNameFallback("enable-cheat_mode"));
	}
}
