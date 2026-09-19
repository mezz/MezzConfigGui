package net.mezzdev.config.gui.textures;

import net.mezzdev.config.gui.ConfigRenderUtil;

import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;

import java.util.function.Supplier;

public final class ConfigScalableDrawable {
	private final Supplier<TextureAtlasSprite> spriteSupplier;

	public ConfigScalableDrawable(Supplier<TextureAtlasSprite> spriteSupplier) {
		this.spriteSupplier = spriteSupplier;
	}

	public void draw(GuiGraphicsExtractor guiGraphics, ImmutableRect2i area) {
		draw(guiGraphics, area.getX(), area.getY(), area.getWidth(), area.getHeight());
	}

	public void draw(GuiGraphicsExtractor guiGraphics, int xOffset, int yOffset, int width, int height) {
		TextureAtlasSprite sprite = getSprite();
		GuiSpriteScaling scaling = getSpriteScaling(sprite);

		if (scaling instanceof GuiSpriteScaling.Tile tile) {
			ConfigRenderUtil.blitTiledSprite(guiGraphics, sprite, xOffset, yOffset, 0, width, height, 0, 0, tile.width(), tile.height(), tile.width(), tile.height());
		} else if (scaling instanceof GuiSpriteScaling.NineSlice nineSlice) {
			ConfigRenderUtil.blitNineSlicedSprite(guiGraphics, sprite, nineSlice, xOffset, yOffset, 0, width, height);
		} else {
			SpriteContents contents = sprite.contents();
			ConfigRenderUtil.blitSprite(guiGraphics, sprite, contents.width(), contents.height(), 0, 0, xOffset, yOffset, 0, width, height);
		}
	}

	private TextureAtlasSprite getSprite() {
		return spriteSupplier.get();
	}

	private static GuiSpriteScaling getSpriteScaling(TextureAtlasSprite sprite) {
		SpriteContents contents = sprite.contents();
		return contents.getAdditionalMetadata(GuiMetadataSection.TYPE)
			.orElse(GuiMetadataSection.DEFAULT)
			.scaling();
	}
}
