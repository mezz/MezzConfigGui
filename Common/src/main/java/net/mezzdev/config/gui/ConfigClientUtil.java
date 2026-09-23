package net.mezzdev.config.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.jspecify.annotations.Nullable;

/** Access to the active screen, which moved to Minecraft's GUI manager in 26.2. */
public final class ConfigClientUtil {
	private ConfigClientUtil() {}
	public static void sendMessage(Component message) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null)
			player.sendSystemMessage(message);
	}

	@Nullable
	public static Screen screen() { return Minecraft.getInstance().gui.screen(); }
	public static void setScreen(@Nullable Screen screen) { Minecraft.getInstance().gui.setScreen(screen); }
}
