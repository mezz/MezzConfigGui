package net.mezzdev.config.gui.textures;

import net.mezzdev.config.gui.ConfigRenderUtil;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import java.util.function.Supplier;

public final class ConfigScalableDrawable {
	private final Supplier<TextureAtlasSprite> spriteSupplier;
	public ConfigScalableDrawable(Supplier<TextureAtlasSprite> spriteSupplier) { this.spriteSupplier = spriteSupplier; }
	public void draw(GuiGraphics graphics, ImmutableRect2i area) { draw(graphics, area.getX(), area.getY(), area.getWidth(), area.getHeight()); }
	public void draw(GuiGraphics graphics, int x, int y, int width, int height) {
		if (width <= 0 || height <= 0)
			return;
		TextureAtlasSprite sprite = spriteSupplier.get();
		LegacySpriteScaling scaling = LegacySpriteScaling.get(sprite);
		if (scaling.type().equals("tile")) {
			tile(graphics, sprite, scaling.width(), scaling.height(), 0, 0, scaling.width(), scaling.height(), x, y, width, height);
		} else if (scaling.type().equals("nine_slice")) {
			int left = Math.min(scaling.left(), width / 2);
			int right = Math.min(scaling.right(), width / 2);
			int top = Math.min(scaling.top(), height / 2);
			int bottom = Math.min(scaling.bottom(), height / 2);
			int[] sourceX = {0, scaling.left(), scaling.width() - scaling.right(), scaling.width()};
			int[] sourceY = {0, scaling.top(), scaling.height() - scaling.bottom(), scaling.height()};
			int[] targetX = {x, x + left, x + width - right, x + width};
			int[] targetY = {y, y + top, y + height - bottom, y + height};
			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 3; column++) {
					tile(graphics, sprite, scaling.width(), scaling.height(), sourceX[column], sourceY[row], sourceX[column + 1] - sourceX[column], sourceY[row + 1] - sourceY[row], targetX[column], targetY[row], targetX[column + 1] - targetX[column], targetY[row + 1] - targetY[row]);
				}
			}
		} else {
			ConfigRenderUtil.blitSprite(graphics, sprite, width, height, 0, 0, x, y, 0, width, height);
		}
	}
	private static void tile(GuiGraphics graphics, TextureAtlasSprite sprite, int textureWidth, int textureHeight, int u, int v, int tileWidth, int tileHeight, int x, int y, int width, int height) {
		if (tileWidth <= 0 || tileHeight <= 0)
			return;
		for (int dy = 0; dy < height; dy += tileHeight) {
			for (int dx = 0; dx < width; dx += tileWidth) {
				ConfigRenderUtil.blitSprite(graphics, sprite, textureWidth, textureHeight, u, v, x + dx, y + dy, 0, Math.min(tileWidth, width - dx), Math.min(tileHeight, height - dy));
			}
		}
	}
}
