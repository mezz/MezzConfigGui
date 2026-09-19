package net.mezzdev.config.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.input.UserInput;

public final class ConfigInputUtil {
	private static final java.util.Map<String, net.minecraft.client.KeyMapping.Category> CATEGORIES = new java.util.HashMap<>();
	public static net.minecraft.client.KeyMapping.Category keyCategory(String modId) { return CATEGORIES.computeIfAbsent(modId, id -> net.minecraft.client.KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(id, "keys"))); }
	public static final int KEY_COMMAND_LEFT = InputConstants.KEY_LGUI;
	public static final int KEY_COMMAND_RIGHT = InputConstants.KEY_RGUI;
	public static final int SHIFT_MODIFIER = InputConstants.MOD_SHIFT;
	private ConfigInputUtil() {

	}

	public static boolean isLeftClick(UserInput input) {
		return input.getKey().getType() == InputConstants.Type.MOUSE &&
			input.getKey().getValue() == InputConstants.MOUSE_BUTTON_LEFT;
	}
	public static boolean hasShiftDown() { return net.minecraft.client.Minecraft.getInstance().hasShiftDown(); }
	public static boolean hasControlDown() { return net.minecraft.client.Minecraft.getInstance().hasControlDown(); }
	public static boolean hasAltDown() { return net.minecraft.client.Minecraft.getInstance().hasAltDown(); }
	public static boolean isPaste(int key) { return key == com.mojang.blaze3d.platform.InputConstants.KEY_V && hasControlDown() && !hasShiftDown() && !hasAltDown(); }
	public static boolean isSelectAll(int key) { return key == com.mojang.blaze3d.platform.InputConstants.KEY_A && hasControlDown() && !hasShiftDown() && !hasAltDown(); }
	public static boolean isMac() { return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("mac"); }
	public static InputConstants.Key getKey(int key, int scanCode) { return InputConstants.getKey(new net.minecraft.client.input.KeyEvent(key, scanCode, 0)); }
	public static boolean keyPressed(net.minecraft.client.gui.components.EditBox box, int key, int scanCode, int modifiers) { return box.keyPressed(new net.minecraft.client.input.KeyEvent(key, scanCode, modifiers)); }
	public static boolean charTyped(net.minecraft.client.gui.components.EditBox box, char character, int modifiers) { return box.charTyped(new net.minecraft.client.input.CharacterEvent(character)); }
	public static boolean matches(net.minecraft.client.KeyMapping mapping, InputConstants.Key key, int modifiers) {
		if (key.getType() == InputConstants.Type.MOUSE) {
			return mapping.matchesMouse(new net.minecraft.client.input.MouseButtonEvent(0, 0, new net.minecraft.client.input.MouseButtonInfo(key.getValue(), modifiers)));
		}
		return mapping.matches(new net.minecraft.client.input.KeyEvent(key.getValue(), 0, modifiers));
	}
	public static String category(net.minecraft.client.KeyMapping mapping) { return mapping.getCategory().id().toLanguageKey("key.category"); }

}
