package net.mezzdev.config.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.input.UserInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class ConfigInputUtil {
	private ConfigInputUtil() {

	}

	public static boolean isMac() {
		return Minecraft.ON_OSX;
	}

	public static int getMouseModifiers() {
		long window = Minecraft.getInstance().getWindow().getWindow();
		int modifiers = 0;
		if (isEitherKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
			modifiers |= GLFW.GLFW_MOD_SHIFT;
		}
		if (isEitherKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
			modifiers |= GLFW.GLFW_MOD_CONTROL;
		}
		if (isEitherKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT)) {
			modifiers |= GLFW.GLFW_MOD_ALT;
		}
		if (isEitherKeyDown(window, GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER)) {
			modifiers |= GLFW.GLFW_MOD_SUPER;
		}
		return modifiers;
	}

	private static boolean isEitherKeyDown(long window, int left, int right) {
		return InputConstants.isKeyDown(window, left) || InputConstants.isKeyDown(window, right);
	}

	public static boolean isLeftClick(UserInput input) {
		return input.getKey().getType() == InputConstants.Type.MOUSE &&
			input.getKey().getValue() == 0;
	}
}
