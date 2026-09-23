package net.mezzdev.config.gui.textures;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public final class ConfigDrawableStatic {
	private final Supplier<TextureAtlasSprite> spriteSupplier;
	private final int width;
	private final int height;

	@Nullable
	private TextureAtlasSprite sprite;

	public ConfigDrawableStatic(Supplier<TextureAtlasSprite> spriteSupplier, int width, int height) {
		if (width < 0 || height < 0 || (width == 0) != (height == 0)) {
			throw new IllegalArgumentException("Config drawable size must be positive, or both dimensions must be 0 to use the sprite size");
		}
		this.spriteSupplier = spriteSupplier;
		this.width = width;
		this.height = height;
	}

	public int getWidth() {
		TextureAtlasSprite sprite = getSprite();
		return getWidth(sprite);
	}

	public int getHeight() {
		TextureAtlasSprite sprite = getSprite();
		return getHeight(sprite);
	}

	public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
		draw(guiGraphics, xOffset, yOffset, 0, 0, 0, 0);
	}

	public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset, int maskTop, int maskBottom, int maskLeft, int maskRight) {
		TextureAtlasSprite sprite = getSprite();
		int width = getWidth(sprite);
		int height = getHeight(sprite);

		int uWidth = width - (maskRight + maskLeft);
		int vHeight = height - (maskBottom + maskTop);

		guiGraphics.blitSprite(
			sprite,
			width,
			height,
			maskLeft,
			maskTop,
			xOffset + maskLeft,
			yOffset + maskTop,
			0,
			uWidth,
			vHeight
		);
	}

	private TextureAtlasSprite getSprite() {
		if (sprite == null) {
			sprite = spriteSupplier.get();
		}
		return sprite;
	}

	private int getWidth(TextureAtlasSprite sprite) {
		if (width > 0) {
			return width;
		}
		return sprite.contents().width();
	}

	private int getHeight(TextureAtlasSprite sprite) {
		if (height > 0) {
			return height;
		}
		return sprite.contents().height();
	}
}
