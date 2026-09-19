package net.mezzdev.config.gui;

import net.mezzdev.config.gui.api.LegacyGuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Adapts pre-1.20.2 screen hooks to the shared screen handlers. */
public abstract class ConfigScreenBase extends Screen {
	@Override
	public void render(com.mojang.blaze3d.vertex.PoseStack pose, int x, int y, float tick) { render(new LegacyGuiGraphics(pose), x, y, tick); }
	public void render(LegacyGuiGraphics graphics, int x, int y, float tick) { super.render(graphics.pose(), x, y, tick); }
	protected ConfigScreenBase(Component title) { super(title); }
	@Override
	public boolean mouseScrolled(double x, double y, double scroll) { return mouseScrolled(x, y, 0, scroll); }
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) { return super.mouseScrolled(x, y, scrollY); }
	public void renderBackground(LegacyGuiGraphics graphics, int x, int y, float partialTick) { super.renderBackground(graphics.pose()); }
	public void renderTransparentBackground(LegacyGuiGraphics graphics) { graphics.fill(0, 0, width, height, 0xA0000000); }
}
