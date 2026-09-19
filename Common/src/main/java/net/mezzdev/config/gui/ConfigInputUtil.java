package net.mezzdev.config.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.input.UserInput;

public final class ConfigInputUtil {
	public static String keyCategory(String modId) { return "key.categories." + modId; }
	public static final int KEY_COMMAND_LEFT = 343;
	public static final int KEY_COMMAND_RIGHT = 347;
	public static final int SHIFT_MODIFIER = 1;
	private ConfigInputUtil() {

	}

	public static boolean isLeftClick(UserInput input) {
		return input.getKey().getType() == InputConstants.Type.MOUSE &&
			input.getKey().getValue() == InputConstants.MOUSE_BUTTON_LEFT;
	}
	public static boolean hasShiftDown() { return net.minecraft.client.gui.screens.Screen.hasShiftDown(); }
	public static boolean hasControlDown() { return net.minecraft.client.gui.screens.Screen.hasControlDown(); }
	public static boolean hasAltDown() { return net.minecraft.client.gui.screens.Screen.hasAltDown(); }
	public static boolean isPaste(int key) { return key == com.mojang.blaze3d.platform.InputConstants.KEY_V && hasControlDown() && !hasShiftDown() && !hasAltDown(); }
	public static boolean isSelectAll(int key) { return key == com.mojang.blaze3d.platform.InputConstants.KEY_A && hasControlDown() && !hasShiftDown() && !hasAltDown(); }
	public static boolean isMac() { return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("mac"); }
	public static InputConstants.Key getKey(int key, int scanCode) { return InputConstants.getKey(key, scanCode); }
	public static boolean keyPressed(net.minecraft.client.gui.components.EditBox box, int key, int scanCode, int modifiers) { return box.keyPressed(key, scanCode, modifiers); }
	public static boolean charTyped(net.minecraft.client.gui.components.EditBox box, char character, int modifiers) { return box.charTyped(character, modifiers); }
	public static boolean matches(net.minecraft.client.KeyMapping mapping, InputConstants.Key key, int modifiers) {
		if (key.getType() == InputConstants.Type.MOUSE) {
			return mapping.matchesMouse(key.getValue());
		}
		return mapping.matches(key.getValue(), 0);
	}
	public static String category(net.minecraft.client.KeyMapping mapping) { return mapping.getCategory(); }

}
