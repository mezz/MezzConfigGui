package net.mezzdev.config.gui.textures;

import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

/**
 * Atlas-backed icons shared by compact config GUI buttons.
 */
public enum ConfigButtonIcon {
	SCREEN_LIST("icons/screen_list", 8, 8),
	X("icons/x", 10, 10),
	CHECK("icons/check", 12, 10),
	RESET("icons/reset", 12, 10),
	ADD("icons/add", 10, 10),
	UP("icons/button_up", 8, 8),
	DOWN("icons/button_down", 8, 8);

	private final int width;
	private final int height;
	private final ConfigDrawableStatic drawable;

	ConfigButtonIcon(String name, int width, int height) {
		this.width = width;
		this.height = height;
		ResourceLocation location = ResourceLocation.fromNamespaceAndPath(ConfigGuiSpriteManager.TEXTURE_NAMESPACE, name);
		this.drawable = new ConfigDrawableStatic(
			() -> ConfigTextures.get().getGuiSpriteManager().getSprite(location),
			width,
			height
		);
	}

	public void draw(GuiGraphics guiGraphics, ImmutableRect2i area, boolean active) {
		draw(guiGraphics, area.getX(), area.getY(), area.getWidth(), area.getHeight(), active);
	}

	public void draw(GuiGraphics guiGraphics, Rect2i area, boolean active) {
		draw(guiGraphics, area.getX(), area.getY(), area.getWidth(), area.getHeight(), active);
	}

	void draw(GuiGraphics guiGraphics, int areaX, int areaY, int areaWidth, int areaHeight, boolean active) {
		int x = areaX + (areaWidth - width) / 2;
		int y = areaY + (areaHeight - height) / 2;
		if (active) {
			drawable.draw(guiGraphics, x, y);
			return;
		}

		guiGraphics.pose().pushPose();
		int color = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.DISABLED_BUTTON_ICON_TINT);
		guiGraphics.setColor(
			((color >>> 16) & 0xFF) / 255.0f,
			((color >>> 8) & 0xFF) / 255.0f,
			(color & 0xFF) / 255.0f,
			((color >>> 24) & 0xFF) / 255.0f
		);
		drawable.draw(guiGraphics, x, y);
		guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
		guiGraphics.pose().popPose();
	}
}
