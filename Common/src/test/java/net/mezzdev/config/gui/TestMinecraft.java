package net.mezzdev.config.gui;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
public final class TestMinecraft {
	private TestMinecraft() {}
	public static KeyMapping keyMapping(String name, int key, String category) { return keyMapping(name, InputConstants.Type.KEYSYM, key, category); }
	public static KeyMapping keyMapping(String name, InputConstants.Type type, int key, String category) {
		if (key == -1) {
			key = InputConstants.UNKNOWN.getValue();
		}
		return new KeyMapping(name, type, key, category);
	}
}
