package net.mezzdev.config.gui.keybindings;

import net.minecraft.client.gui.screens.Screen;

final class KeyModifiers {
	private KeyModifiers() {

	}

	public static ConfigKeyModifier getActive() {
		if (Screen.hasShiftDown()) {
			return ConfigKeyModifier.SHIFT;
		}
		if (Screen.hasControlDown()) {
			return ConfigKeyModifier.CONTROL_OR_COMMAND;
		}
		if (Screen.hasAltDown()) {
			return ConfigKeyModifier.ALT;
		}
		return ConfigKeyModifier.NONE;
	}
}
