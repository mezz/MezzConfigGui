package net.mezzdev.config.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.input.UserInput;

public final class ConfigInputUtil {
	private ConfigInputUtil() {

	}

	public static boolean isLeftClick(UserInput input) {
		return input.getKey().getType() == InputConstants.Type.MOUSE &&
			input.getKey().getValue() == 0;
	}
}
