package net.mezzdev.config.gui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.input.UserInput;

public final class ConfigInputUtil {
	private static final Map<String, Category> CATEGORIES = new HashMap<>();
	public static Category keyCategory(String modId) { return CATEGORIES.computeIfAbsent(modId, id -> Category.register(Identifier.fromNamespaceAndPath(id, "keys"))); }
	public static final int KEY_COMMAND_LEFT = 343;
	public static final int KEY_COMMAND_RIGHT = 347;
	public static final int SHIFT_MODIFIER = 1;
	private ConfigInputUtil() {

	}

	public static boolean isLeftClick(UserInput input) {
		return input.getKey().getType() == InputConstants.Type.MOUSE &&
			input.getKey().getValue() == InputConstants.MOUSE_BUTTON_LEFT;
	}
	public static boolean hasShiftDown() { return Minecraft.getInstance().hasShiftDown(); }
	public static boolean hasControlDown() { return Minecraft.getInstance().hasControlDown(); }
	public static boolean hasAltDown() { return Minecraft.getInstance().hasAltDown(); }
	public static boolean isPaste(int key) { return key == InputConstants.KEY_V && hasControlDown() && !hasShiftDown() && !hasAltDown(); }
	public static boolean isSelectAll(int key) { return key == InputConstants.KEY_A && hasControlDown() && !hasShiftDown() && !hasAltDown(); }
	public static boolean isMac() { return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac"); }
	public static InputConstants.Key getKey(int key, int scanCode) { return InputConstants.getKey(new KeyEvent(key, scanCode, 0)); }
	public static boolean keyPressed(EditBox box, int key, int scanCode, int modifiers) { return box.keyPressed(new KeyEvent(key, scanCode, modifiers)); }
	public static boolean charTyped(EditBox box, char character, int modifiers) { return box.charTyped(new CharacterEvent(character)); }
	public static boolean matches(KeyMapping mapping, InputConstants.Key key, int modifiers) {
		if (key.getType() == InputConstants.Type.MOUSE) {
			return mapping.matchesMouse(new MouseButtonEvent(0, 0, new MouseButtonInfo(key.getValue(), modifiers)));
		}
		return mapping.matches(new KeyEvent(key.getValue(), 0, modifiers));
	}
	public static String category(KeyMapping mapping) { return mapping.getCategory().id().toLanguageKey("key.category"); }

}
