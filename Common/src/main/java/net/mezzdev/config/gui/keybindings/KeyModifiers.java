package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.gui.ConfigInputUtil;

final class KeyModifiers {
	private KeyModifiers() {

	}

	public static ConfigKeyModifier getActive() {
		if (ConfigInputUtil.hasShiftDown()) {
			return ConfigKeyModifier.SHIFT;
		}
		if (ConfigInputUtil.hasControlDown()) {
			return ConfigKeyModifier.CONTROL_OR_COMMAND;
		}
		if (ConfigInputUtil.hasAltDown()) {
			return ConfigKeyModifier.ALT;
		}
		return ConfigKeyModifier.NONE;
	}
}
