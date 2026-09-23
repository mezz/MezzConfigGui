package net.mezzdev.config.gui;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
	public static void renderEditBox(EditBox box, GuiGraphics graphics, int x, int y, float tick) { box.render(graphics, x, y, tick); }

	public static void renderTooltip(GuiGraphics graphics, Font font, List<FormattedCharSequence> lines, int x, int y) {
		renderTooltip(graphics, font, lines, DefaultTooltipPositioner.INSTANCE, x, y);
	}
	public static void blitSprite(GuiGraphics graphics, TextureAtlasSprite sprite, int textureWidth, int textureHeight, int u, int v, int x, int y, int z, int width, int height) {
		blitSprite(graphics, sprite, textureWidth, textureHeight, u, v, x, y, z, width, height, -1);
	}

	private static final AtomicLong NEXT_ICON_ID = new AtomicLong();
	public static void pushPose(GuiGraphics graphics) { graphics.pose().pushMatrix(); }
	public static void popPose(GuiGraphics graphics) { graphics.pose().popMatrix(); }
	public static void translate(GuiGraphics graphics, float x, float y, float z) {
		graphics.pose().translate(x, y);
		if (z != 0)
			graphics.nextStratum();
	}
	public static void scale(GuiGraphics graphics, float x, float y, float z) { graphics.pose().scale(x, y); }
	public static void flush(GuiGraphics graphics) { graphics.nextStratum(); }
	public static void renderTooltip(GuiGraphics graphics, Font font, List<FormattedCharSequence> lines, ClientTooltipPositioner positioner, int x, int y) {
		graphics.renderTooltip(font, lines.stream().map(ClientTooltipComponent::create).toList(), x, y, positioner, null);
	}
	public static void blitSprite(GuiGraphics graphics, Identifier sprite, int x, int y, int width, int height) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
	}
	public static void blitSprite(GuiGraphics graphics, TextureAtlasSprite sprite, int textureWidth, int textureHeight, int u, int v, int x, int y, int z, int width, int height, int color) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, textureWidth, textureHeight, u, v, x, y, width, height, color);
	}
	public static void blitNineSlicedSprite(GuiGraphics graphics, TextureAtlasSprite sprite, GuiSpriteScaling.NineSlice scaling, int x, int y, int z, int width, int height) {
		graphics.blitNineSlicedSprite(RenderPipelines.GUI_TEXTURED, sprite, scaling, x, y, width, height, -1);
	}
	public static void blitTiledSprite(GuiGraphics graphics, TextureAtlasSprite sprite, int x, int y, int z, int width, int height, int u, int v, int tileWidth, int tileHeight, int textureWidth, int textureHeight) {
		graphics.blitTiledSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, u, v, tileWidth, tileHeight, textureWidth, textureHeight, -1);
	}
	public static void blit(GuiGraphics graphics, Identifier texture, int x, int y, int width, int height, float u, float v, int uSize, int vSize, int textureWidth, int textureHeight) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, uSize, vSize, textureWidth, textureHeight);
	}
	public static Identifier registerIcon(NativeImage image) {
		Identifier location = Identifier.fromNamespaceAndPath("mezz_config_gui", "mod_icon/" + NEXT_ICON_ID.getAndIncrement());
		Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(location::toString, image));
		return location;
	}
	public static Style truncationStyle(Font font, FormattedText text, int width) {
		var lastStyle = new AtomicReference<>(Style.EMPTY);
		font.substrByWidth(text, width).visit((style, value) -> {
			if (!value.isEmpty())
				lastStyle.set(style);
			return Optional.empty();
		}, Style.EMPTY);
		return lastStyle.get();
	}

}
