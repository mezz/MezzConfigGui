package net.mezzdev.config.gui.api;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayDeque;
import java.util.Deque;

/** Immediate rendering adapter for Minecraft versions before LegacyGuiGraphics. */
public final class LegacyGuiGraphics {
	private static final class Gradient extends GuiComponent {
		static void draw(PoseStack pose, int left, int top, int right, int bottom, int start, int end) { fillGradient(pose, left, top, right, bottom, start, end, 0); }
	}
	private final PoseStack pose;
	private final Deque<Rect2i> scissors = new ArrayDeque<>();
	public LegacyGuiGraphics(PoseStack pose) { this.pose = pose; }
	public PoseStack pose() { return pose; }
	public void flush() { Minecraft.getInstance().renderBuffers().bufferSource().endBatch(); }
	public void setColor(float red, float green, float blue, float alpha) { RenderSystem.setShaderColor(red, green, blue, alpha); }
	public void fill(int left, int top, int right, int bottom, int color) { GuiComponent.fill(pose, left, top, right, bottom, color); }
	public void fillGradient(int left, int top, int right, int bottom, int start, int end) { Gradient.draw(pose, left, top, right, bottom, start, end); }
	public void drawString(Font font, FormattedCharSequence text, int x, int y, int color) { drawString(font, text, x, y, color, true); }
	public void drawString(Font font, Component text, int x, int y, int color) { drawString(font, text, x, y, color, true); }
	public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
		if (shadow)
			font.drawShadow(pose, text, x, y, color);
		else
			font.draw(pose, text, x, y, color);
	}
	public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) { drawString(font, text.getVisualOrderText(), x, y, color, shadow); }
	public void drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
		if (shadow)
			font.drawShadow(pose, text, x, y, color);
		else
			font.draw(pose, text, x, y, color);
	}
	public void drawCenteredString(Font font, Component text, int x, int y, int color) { GuiComponent.drawCenteredString(pose, font, text, x, y, color); }
	public void drawCenteredString(Font font, String text, int x, int y, int color) { GuiComponent.drawCenteredString(pose, font, text, x, y, color); }
	public void blit(ResourceLocation texture, int x, int y, int width, int height, float u, float v, int uSize, int vSize, int textureWidth, int textureHeight) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		GuiComponent.blit(pose, x, y, width, height, u, v, uSize, vSize, textureWidth, textureHeight);
	}
	public void renderFakeItem(ItemStack stack, int x, int y) {
		PoseStack model = RenderSystem.getModelViewStack();
		model.pushPose();
		model.mulPoseMatrix(pose.last().pose());
		RenderSystem.applyModelViewMatrix();
		Minecraft.getInstance().getItemRenderer().renderAndDecorateFakeItem(stack, x, y);
		model.popPose();
		RenderSystem.applyModelViewMatrix();
	}
	public void enableScissor(int left, int top, int right, int bottom) {
		if (!scissors.isEmpty()) {
			Rect2i parent = scissors.peek();
			left = Math.max(left, parent.getX());
			top = Math.max(top, parent.getY());
			right = Math.min(right, parent.getX() + parent.getWidth());
			bottom = Math.min(bottom, parent.getY() + parent.getHeight());
		}
		scissors.push(new Rect2i(left, top, Math.max(0, right - left), Math.max(0, bottom - top)));
		applyScissor();
	}
	public void disableScissor() { scissors.pop(); applyScissor(); }
	private void applyScissor() {
		flush();
		if (scissors.isEmpty()) {
			RenderSystem.disableScissor();
			return;
		}
		Rect2i area = scissors.peek();
		var window = Minecraft.getInstance().getWindow();
		double scale = window.getGuiScale();
		RenderSystem.enableScissor((int) (area.getX() * scale), (int) (window.getHeight() - (area.getY() + area.getHeight()) * scale), (int) (area.getWidth() * scale), (int) (area.getHeight() * scale));
	}
}
