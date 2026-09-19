package net.mezzdev.config.gui.entries;

import net.mezzdev.config.gui.textures.ConfigButtonIcon;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.api.LegacyGuiGraphics;

/**
 * Renders the reset-to-default icon used in config value rows.
 */
final class ConfigResetIcon {
	private ConfigResetIcon() {

	}

	public static void draw(LegacyGuiGraphics guiGraphics, ImmutableRect2i buttonArea, boolean active) {
		ConfigButtonIcon.RESET.draw(guiGraphics, buttonArea, active);
	}
}
