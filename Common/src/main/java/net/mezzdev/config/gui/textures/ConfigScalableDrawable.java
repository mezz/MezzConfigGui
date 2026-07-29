package net.mezzdev.config.gui.textures;

import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class ConfigScalableDrawable {
	private final Supplier<TextureAtlasSprite> spriteSupplier;

	@Nullable
	private TextureAtlasSprite sprite;

	public ConfigScalableDrawable(Supplier<TextureAtlasSprite> spriteSupplier) {
		this.spriteSupplier = spriteSupplier;
	}

	public void draw(GuiGraphics guiGraphics, ImmutableRect2i area) {
		draw(guiGraphics, area.getX(), area.getY(), area.getWidth(), area.getHeight());
	}

	public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset, int width, int height) {
		TextureAtlasSprite sprite = getSprite();
		GuiSpriteScaling scaling = getSpriteScaling(sprite);

		switch (scaling) {
			case GuiSpriteScaling.Tile tileScaling -> guiGraphics.blitTiledSprite(
				sprite,
				xOffset,
				yOffset,
				0,
				width,
				height,
				0,
				0,
				tileScaling.width(),
				tileScaling.height(),
				tileScaling.width(),
				tileScaling.height()
			);
			case GuiSpriteScaling.NineSlice nineSliceScaling -> guiGraphics.blitNineSlicedSprite(
				sprite,
				nineSliceScaling,
				xOffset,
				yOffset,
				0,
				width,
				height
			);
			default -> {
				SpriteContents contents = sprite.contents();
				guiGraphics.blitSprite(
					sprite,
					contents.width(),
					contents.height(),
					0,
					0,
					xOffset,
					yOffset,
					0,
					width,
					height
				);
			}
		}
	}

	private TextureAtlasSprite getSprite() {
		if (sprite == null) {
			sprite = spriteSupplier.get();
		}
		return sprite;
	}

	private static GuiSpriteScaling getSpriteScaling(TextureAtlasSprite sprite) {
		SpriteContents contents = sprite.contents();
		ResourceMetadata metadata = contents.metadata();
		return metadata.getSection(GuiMetadataSection.TYPE)
			.orElse(GuiMetadataSection.DEFAULT)
			.scaling();
	}
}
