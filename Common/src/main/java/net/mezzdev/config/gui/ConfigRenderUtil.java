package net.mezzdev.config.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;

/** Minecraft rendering operations whose signatures change between supported targets. */
public final class ConfigRenderUtil {
	private ConfigRenderUtil() {}
	public static void renderEditBox(net.minecraft.client.gui.components.EditBox box, GuiGraphicsExtractor graphics, int x, int y, float tick) { box.extractRenderState(graphics, x, y, tick); }

	public static void tooltip(GuiGraphicsExtractor graphics, Font font, List<FormattedCharSequence> lines, int x, int y) {
		tooltip(graphics, font, lines, DefaultTooltipPositioner.INSTANCE, x, y);
	}
	public static void blitSprite(GuiGraphicsExtractor graphics, TextureAtlasSprite sprite, int textureWidth, int textureHeight, int u, int v, int x, int y, int z, int width, int height) {
		blitSprite(graphics, sprite, textureWidth, textureHeight, u, v, x, y, z, width, height, -1);
	}

	private static final java.util.concurrent.atomic.AtomicLong NEXT_ICON_ID = new java.util.concurrent.atomic.AtomicLong();
	public static void pushPose(GuiGraphicsExtractor graphics) { graphics.pose().pushMatrix(); }
	public static void popPose(GuiGraphicsExtractor graphics) { graphics.pose().popMatrix(); }
	public static void translate(GuiGraphicsExtractor graphics, float x, float y, float z) {
		graphics.pose().translate(x, y);
		if (z != 0)
			graphics.nextStratum();
	}
	public static void scale(GuiGraphicsExtractor graphics, float x, float y, float z) { graphics.pose().scale(x, y); }
	public static void flush(GuiGraphicsExtractor graphics) { graphics.nextStratum(); }
	public static void tooltip(GuiGraphicsExtractor graphics, Font font, List<FormattedCharSequence> lines, ClientTooltipPositioner positioner, int x, int y) {
		graphics.tooltip(font, lines.stream().map(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent::create).toList(), x, y, positioner, null, false);
	}
	public static void blitSprite(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, int width, int height) {
		graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
	}
	public static void blitSprite(GuiGraphicsExtractor graphics, TextureAtlasSprite sprite, int textureWidth, int textureHeight, int u, int v, int x, int y, int z, int width, int height, int color) {
		graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sprite, textureWidth, textureHeight, u, v, x, y, width, height, color);
	}
	public static void blitNineSlicedSprite(GuiGraphicsExtractor graphics, TextureAtlasSprite sprite, GuiSpriteScaling.NineSlice scaling, int x, int y, int z, int width, int height) {
		graphics.blitNineSlicedSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sprite, scaling, x, y, width, height, -1);
	}
	public static void blitTiledSprite(GuiGraphicsExtractor graphics, TextureAtlasSprite sprite, int x, int y, int z, int width, int height, int u, int v, int tileWidth, int tileHeight, int textureWidth, int textureHeight) {
		graphics.blitTiledSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, u, v, tileWidth, tileHeight, textureWidth, textureHeight, -1);
	}
	public static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height, float u, float v, int uSize, int vSize, int textureWidth, int textureHeight) {
		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, uSize, vSize, textureWidth, textureHeight);
	}
	public static Identifier registerIcon(NativeImage image) {
		Identifier location = Identifier.fromNamespaceAndPath("mezz_config_gui", "mod_icon/" + NEXT_ICON_ID.getAndIncrement());
		Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(location::toString, image));
		return location;
	}
	public static net.minecraft.network.chat.Style truncationStyle(Font font, net.minecraft.network.chat.FormattedText text, int width) {
		var lastStyle = new java.util.concurrent.atomic.AtomicReference<>(net.minecraft.network.chat.Style.EMPTY);
		font.substrByWidth(text, width).visit((style, value) -> {
			if (!value.isEmpty())
				lastStyle.set(style);
			return java.util.Optional.empty();
		}, net.minecraft.network.chat.Style.EMPTY);
		return lastStyle.get();
	}

}
