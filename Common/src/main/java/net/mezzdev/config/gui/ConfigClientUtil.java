package net.mezzdev.config.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/** Access to the active screen, which moved to Minecraft's GUI manager in 26.2. */
public final class ConfigClientUtil {
	private ConfigClientUtil() {}
	public static void sendMessage(net.minecraft.network.chat.Component message) {
		var player = Minecraft.getInstance().player;
		if (player != null)
			player.sendSystemMessage(message);
	}

	@Nullable
	public static Screen screen() { return Minecraft.getInstance().screen; }
	public static void setScreen(@Nullable Screen screen) { Minecraft.getInstance().setScreen(screen); }
}
