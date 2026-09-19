package net.mezzdev.config.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Adapts pre-1.20.2 screen hooks to the shared screen handlers. */
public abstract class ConfigScreenBase extends Screen {
	protected ConfigScreenBase(Component title) { super(title); }
	@Override
	public boolean mouseScrolled(double x, double y, double scroll) { return mouseScrolled(x, y, 0, scroll); }
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) { return super.mouseScrolled(x, y, scrollY); }
	public void renderBackground(GuiGraphics graphics, int x, int y, float partialTick) { super.renderBackground(graphics); }
	public void renderTransparentBackground(GuiGraphics graphics) { graphics.fill(0, 0, width, height, 0xA0000000); }
}
