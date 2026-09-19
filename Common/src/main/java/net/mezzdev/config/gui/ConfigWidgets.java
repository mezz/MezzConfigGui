package net.mezzdev.config.gui;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
public final class ConfigWidgets {
	private ConfigWidgets() {}
	public static Button button(Component label, Button.OnPress action, int x, int y, int width, int height, Component tooltip) {
		return Button.builder(label, action).bounds(x, y, width, height).tooltip(Tooltip.create(tooltip)).build();
	}
}
