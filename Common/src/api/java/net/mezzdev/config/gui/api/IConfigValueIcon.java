package net.mezzdev.config.gui.api;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;

/**
 * Draws a visual icon for one displayed config value option.
 *
 * @since 0.1.0
 */
public interface IConfigValueIcon {
	/**
	 * Draw this icon inside the given area.
	 *
	 * @param guiGraphics the draw context
	 * @param area the icon bounds
	 *
	 * @since 0.1.0
	 */
	void draw(GuiGraphicsExtractor guiGraphics, Rect2i area);
}
