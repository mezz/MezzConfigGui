package net.mezzdev.config.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.mezzdev.config.gui.api.LegacyGuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Minecraft rendering operations whose signatures change between supported targets. */
public final class ConfigRenderUtil {
	private ConfigRenderUtil() {}
	public static void renderEditBox(net.mezzdev.config.gui.LegacyEditBox box, LegacyGuiGraphics graphics, int x, int y, float tick) { box.render(graphics.pose(), x, y, tick); }

	public static void renderTooltip(LegacyGuiGraphics graphics, Font font, List<FormattedCharSequence> lines, int x, int y) {
		renderTooltip(graphics, font, lines, LegacyTooltipPositioner.INSTANCE, x, y);
	}
	public static void blitSprite(LegacyGuiGraphics graphics, TextureAtlasSprite sprite, int textureWidth, int textureHeight, int u, int v, int x, int y, int z, int width, int height) {
		blitSprite(graphics, sprite, textureWidth, textureHeight, u, v, x, y, z, width, height, -1);
	}

	public static void pushPose(LegacyGuiGraphics graphics) { graphics.pose().pushPose(); }
	public static void popPose(LegacyGuiGraphics graphics) { graphics.pose().popPose(); }
	public static void translate(LegacyGuiGraphics graphics, float x, float y, float z) { graphics.pose().translate(x, y, z); }
	public static void scale(LegacyGuiGraphics graphics, float x, float y, float z) { graphics.pose().scale(x, y, z); }
	public static void flush(LegacyGuiGraphics graphics) { graphics.flush(); }
	public static void renderTooltip(LegacyGuiGraphics graphics, Font font, List<FormattedCharSequence> lines, LegacyTooltipPositioner positioner, int x, int y) {
		var screen = Minecraft.getInstance().screen;
		if (screen != null)
			screen.renderTooltip(graphics.pose(), lines, x, y);
	}
	public static void blitSprite(LegacyGuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height) {
		String path = sprite.getPath();
		ResourceLocation texture = new ResourceLocation("minecraft", "textures/gui/widgets.png");
		// Minecraft 1.19.2 stores the dark slider track at y=46; the handle states use y=66 and y=86.
		int sourceY = 46;
		if (path.contains("handle")) {
			sourceY = 66;
			if (path.contains("highlighted")) {
				sourceY = 86;
			}
		}
		int left = width / 2;
		graphics.blit(texture, x, y, left, height, 0, sourceY, left, 20, 256, 256);
		graphics.blit(texture, x + left, y, width - left, height, 200 - (width - left), sourceY, width - left, 20, 256, 256);
	}
	public static void blitSprite(LegacyGuiGraphics graphics, TextureAtlasSprite sprite, int textureWidth, int textureHeight, int u, int v, int x, int y, int z, int width, int height, int color) {
		graphics.setColor(((color >>> 16) & 255) / 255f, ((color >>> 8) & 255) / 255f, (color & 255) / 255f, ((color >>> 24) & 255) / 255f);
		float minU = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * u / textureWidth;
		float minV = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * v / textureHeight;
		int atlasWidth = Math.round(sprite.getWidth() / (sprite.getU1() - sprite.getU0()));
		int atlasHeight = Math.round(sprite.getHeight() / (sprite.getV1() - sprite.getV0()));
		int sourceWidth = Math.round((sprite.getU1() - sprite.getU0()) * width / textureWidth * atlasWidth);
		int sourceHeight = Math.round((sprite.getV1() - sprite.getV0()) * height / textureHeight * atlasHeight);
		graphics.blit(sprite.atlas().location(), x, y, width, height, minU * atlasWidth, minV * atlasHeight, sourceWidth, sourceHeight, atlasWidth, atlasHeight);
		graphics.setColor(1, 1, 1, 1);
	}
	public static void blit(LegacyGuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, float u, float v, int uSize, int vSize, int textureWidth, int textureHeight) {
		graphics.blit(texture, x, y, width, height, u, v, uSize, vSize, textureWidth, textureHeight);
	}
	public static ResourceLocation registerIcon(NativeImage image) {
		return Minecraft.getInstance().getTextureManager().register("mezz_config_gui_mod_icon", new DynamicTexture(image));
	}
	@Nullable
	public static Style truncationStyle(Font font, FormattedText text, int width) {
		return font.getSplitter().componentStyleAtWidth(text, width);
	}

}
