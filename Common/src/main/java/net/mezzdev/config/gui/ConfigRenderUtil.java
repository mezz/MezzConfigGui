package net.mezzdev.config.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;

/** Minecraft rendering operations whose signatures change between supported targets. */
public final class ConfigRenderUtil {
	private ConfigRenderUtil() {}
	public static void renderEditBox(net.minecraft.client.gui.components.EditBox box, GuiGraphics graphics, int x, int y, float tick) { box.render(graphics, x, y, tick); }

	public static void renderTooltip(GuiGraphics graphics, Font font, List<FormattedCharSequence> lines, int x, int y) {
		renderTooltip(graphics, font, lines, DefaultTooltipPositioner.INSTANCE, x, y);
	}
	public static void blitSprite(GuiGraphics graphics, TextureAtlasSprite sprite, int textureWidth, int textureHeight, int u, int v, int x, int y, int z, int width, int height) {
		blitSprite(graphics, sprite, textureWidth, textureHeight, u, v, x, y, z, width, height, -1);
	}

	public static void pushPose(GuiGraphics graphics) { graphics.pose().pushPose(); }
	public static void popPose(GuiGraphics graphics) { graphics.pose().popPose(); }
	public static void translate(GuiGraphics graphics, float x, float y, float z) { graphics.pose().translate(x, y, z); }
	public static void scale(GuiGraphics graphics, float x, float y, float z) { graphics.pose().scale(x, y, z); }
	public static void flush(GuiGraphics graphics) { graphics.flush(); }
	public static void renderTooltip(GuiGraphics graphics, Font font, List<FormattedCharSequence> lines, ClientTooltipPositioner positioner, int x, int y) {
		graphics.renderTooltip(font, lines, positioner, x, y);
	}
	public static void blitSprite(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height) {
		String path = sprite.getPath();
		int row = 0;
		if (path.contains("handle")) {
			row += 2;
		}
		if (path.contains("highlighted")) {
			row++;
		}
		graphics.blitNineSliced(new ResourceLocation("minecraft", "textures/gui/slider.png"), x, y, width, height, 20, 4, 200, 20, 0, row * 20);
	}
	public static void blitSprite(GuiGraphics graphics, TextureAtlasSprite sprite, int textureWidth, int textureHeight, int u, int v, int x, int y, int z, int width, int height, int color) {
		graphics.setColor(((color >>> 16) & 255) / 255f, ((color >>> 8) & 255) / 255f, (color & 255) / 255f, ((color >>> 24) & 255) / 255f);
		float minU = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * u / textureWidth;
		float minV = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * v / textureHeight;
		int atlasWidth = Math.round(sprite.contents().width() / (sprite.getU1() - sprite.getU0()));
		int atlasHeight = Math.round(sprite.contents().height() / (sprite.getV1() - sprite.getV0()));
		int sourceWidth = Math.round((sprite.getU1() - sprite.getU0()) * width / textureWidth * atlasWidth);
		int sourceHeight = Math.round((sprite.getV1() - sprite.getV0()) * height / textureHeight * atlasHeight);
		graphics.blit(sprite.atlasLocation(), x, y, width, height, minU * atlasWidth, minV * atlasHeight, sourceWidth, sourceHeight, atlasWidth, atlasHeight);
		graphics.setColor(1, 1, 1, 1);
	}
	public static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, float u, float v, int uSize, int vSize, int textureWidth, int textureHeight) {
		graphics.blit(texture, x, y, width, height, u, v, uSize, vSize, textureWidth, textureHeight);
	}
	public static ResourceLocation registerIcon(NativeImage image) {
		return Minecraft.getInstance().getTextureManager().register("mezz_config_gui_mod_icon", new DynamicTexture(image));
	}
	@org.jetbrains.annotations.Nullable
	public static net.minecraft.network.chat.Style truncationStyle(Font font, net.minecraft.network.chat.FormattedText text, int width) {
		return font.getSplitter().componentStyleAtWidth(text, width);
	}

}
