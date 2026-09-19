package net.mezzdev.config.gui.textures;

import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

/**
 * Atlas-backed icons shared by compact config GUI buttons.
 */
public enum ConfigButtonIcon {
	SCREEN_LIST("icons/screen_list", 8, 8),
	X("icons/x", 10, 10),
	CHECK("icons/check", 12, 10),
	RESET("icons/reset", 12, 10),
	ADD("icons/add", 10, 10),
	UP("icons/arrow_up", 9, 9),
	DOWN("icons/arrow_down", 9, 9);

	private final int width;
	private final int height;
	private final ConfigDrawableStatic drawable;

	ConfigButtonIcon(String name, int width, int height) {
		this.width = width;
		this.height = height;
		Identifier location = Identifier.fromNamespaceAndPath(ConfigGuiSpriteManager.TEXTURE_NAMESPACE, name);
		this.drawable = new ConfigDrawableStatic(
			() -> ConfigTextures.get().getGuiSpriteManager().getSprite(location),
			width,
			height
		);
	}

	public void draw(GuiGraphicsExtractor guiGraphics, ImmutableRect2i area, boolean active) {
		draw(guiGraphics, area.getX(), area.getY(), area.getWidth(), area.getHeight(), active);
	}

	public void draw(GuiGraphicsExtractor guiGraphics, Rect2i area, boolean active) {
		draw(guiGraphics, area.getX(), area.getY(), area.getWidth(), area.getHeight(), active);
	}

	void draw(GuiGraphicsExtractor guiGraphics, int areaX, int areaY, int areaWidth, int areaHeight, boolean active) {
		int x = areaX + (areaWidth - width) / 2;
		int y = areaY + (areaHeight - height) / 2;
		if (active) {
			drawable.draw(guiGraphics, x, y);
			return;
		}

		int color = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.DISABLED_BUTTON_ICON_TINT);
		drawable.drawTinted(guiGraphics, x, y, color);
	}
}
