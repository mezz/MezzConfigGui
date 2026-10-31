package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.gui.ConfigInputUtil;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
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
		// Subclasses can store their binding separately from KeyMapping's private key field.
		return getKey(keyMapping.saveString());
	}

	public static Component getKeyDisplayName(InputConstants.Key key) {
		if (key.getType() == InputConstants.Type.MOUSE) {
			int value = key.getValue();
			if (value == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				return Component.translatable("mezz_config_gui.key.mouse.left");
			} else if (value == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				return Component.translatable("mezz_config_gui.key.mouse.right");
			}
		}
		if (key.getType() == InputConstants.Type.KEYSYM) {
			int value = key.getValue();
			if (ConfigInputUtil.isMac() && (value == GLFW.GLFW_KEY_LEFT_SUPER || value == GLFW.GLFW_KEY_RIGHT_SUPER)) {
				return Component.translatable("mezz_config_gui.key.modifier.command");
			}
			return switch (value) {
				case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> Component.translatable("mezz_config_gui.key.modifier.shift");
				case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> Component.translatable("mezz_config_gui.key.modifier.alt");
				case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> Component.translatable("mezz_config_gui.key.modifier.control");
				default -> key.getDisplayName();
			};
		}
		return key.getDisplayName();
	}

}
