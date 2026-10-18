package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.gui.ConfigInputUtil;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public final class ConfigKeyBindingUtil {
	private ConfigKeyBindingUtil() {

	}

	public static InputConstants.Key getKey(String keyName) {
		try {
			return InputConstants.getKey(keyName);
		} catch (IllegalArgumentException ignored) {
			return InputConstants.UNKNOWN;
		}
	}

	public static InputConstants.Key getKey(KeyMapping keyMapping) {
		return getKey(keyMapping.saveString());
	}

	public static Component getKeyDisplayName(InputConstants.Key key) {
		if (key.getType() == InputConstants.Type.MOUSE) {
			int value = key.getValue();
			if (value == InputConstants.MOUSE_BUTTON_LEFT) {
				return Component.translatable("mezz_config_gui.key.mouse.left");
			} else if (value == InputConstants.MOUSE_BUTTON_RIGHT) {
				return Component.translatable("mezz_config_gui.key.mouse.right");
			}
		}
		if (key.getType() == InputConstants.Type.KEYBOARD) {
			int value = key.getValue();
			if (ConfigInputUtil.isMac() && (value == ConfigInputUtil.KEY_COMMAND_LEFT || value == ConfigInputUtil.KEY_COMMAND_RIGHT)) {
				return Component.translatable("mezz_config_gui.key.modifier.command");
			}
			return switch (value) {
				case InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT -> Component.translatable("mezz_config_gui.key.modifier.shift");
				case InputConstants.KEY_LALT, InputConstants.KEY_RALT -> Component.translatable("mezz_config_gui.key.modifier.alt");
				case InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL -> Component.translatable("mezz_config_gui.key.modifier.control");
				default -> key.getDisplayName();
			};
		}
		return key.getDisplayName();
	}

}
