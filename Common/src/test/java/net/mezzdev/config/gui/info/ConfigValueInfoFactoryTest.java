package net.mezzdev.config.gui.info;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigValueInfoFactoryTest {
	@Test
	void booleanToggleActionDescribesTheOppositeState() {
		assertEquals("Click to enable this setting.", ConfigValueInfoFactory.getBooleanToggleAction(false).getString());
		assertEquals("Click to disable this setting.", ConfigValueInfoFactory.getBooleanToggleAction(true).getString());
	}
}
