package net.mezzdev.config.gui.textures;

import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;

/**
 * Draws the textured checkbox used for boolean config values.
 */
public final class ConfigCheckbox {
	private static final int CHECKBOX_SIZE = 18;

	private ConfigCheckbox() {

	}

	public static void draw(GuiGraphics guiGraphics, ImmutableRect2i area, boolean checked, boolean hovered) {
		draw(guiGraphics, area.getX(), area.getY(), area.getWidth(), area.getHeight(), checked, hovered);
	}

	public static void draw(GuiGraphics guiGraphics, Rect2i area, boolean checked, boolean hovered) {
		draw(guiGraphics, area.getX(), area.getY(), area.getWidth(), area.getHeight(), checked, hovered);
	}

	private static void draw(
		GuiGraphics guiGraphics,
		int areaX,
		int areaY,
		int areaWidth,
		int areaHeight,
		boolean checked,
		boolean hovered
	) {
		int x = areaX + (areaWidth - CHECKBOX_SIZE) / 2;
		int y = areaY + (areaHeight - CHECKBOX_SIZE) / 2;
		ConfigTextures.get().getCheckbox(hovered).draw(guiGraphics, x, y);
		if (checked) {
			ConfigButtonIcon.CHECK.draw(guiGraphics, areaX, areaY, areaWidth, areaHeight, true);
		}
	}
}
