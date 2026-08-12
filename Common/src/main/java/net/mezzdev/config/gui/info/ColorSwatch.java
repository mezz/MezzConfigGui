package net.mezzdev.config.gui.info;

import net.mezzdev.config.api.value.ConfigColorFormat;
import net.mezzdev.config.api.value.PackedColor;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;

/**
 * Draws and formats packed RGB and ARGB colors.
 */
public final class ColorSwatch {
	private static final int CHECKER_SIZE = 3;

	private ColorSwatch() {

	}

	public static void draw(GuiGraphics guiGraphics, Rect2i area, PackedColor color) {
		draw(guiGraphics, area, color.packedValue(), color.format());
	}

	public static String formatHex(PackedColor color) {
		return switch (color.format()) {
			case RGB -> "#%06X".formatted(color.packedValue() & 0xFFFFFF);
			case ARGB -> "#%08X".formatted(color.packedValue());
		};
	}

	public static void draw(GuiGraphics guiGraphics, Rect2i area, int color, ConfigColorFormat format) {
		if (area.getWidth() <= 0 || area.getHeight() <= 0) {
			return;
		}
		int right = area.getX() + area.getWidth();
		int bottom = area.getY() + area.getHeight();
		guiGraphics.fill(area.getX(), area.getY(), right, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_SWATCH_BORDER));

		int x = area.getX() + 1;
		int y = area.getY() + 1;
		int innerRight = right - 1;
		int innerBottom = bottom - 1;
		drawCheckerboard(guiGraphics, x, y, innerRight, innerBottom);
		guiGraphics.fill(x, y, innerRight, innerBottom, toArgb(color, format));
	}

	public static void drawCheckerboard(GuiGraphics guiGraphics, int x, int y, int right, int bottom) {
		for (int tileY = y; tileY < bottom; tileY += CHECKER_SIZE) {
			for (int tileX = x; tileX < right; tileX += CHECKER_SIZE) {
				int column = (tileX - x) / CHECKER_SIZE;
				int row = (tileY - y) / CHECKER_SIZE;
				int checkerColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_SWATCH_CHECKER_DARK);
				if ((column + row) % 2 == 0) {
					checkerColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_SWATCH_CHECKER_LIGHT);
				}
				guiGraphics.fill(
					tileX,
					tileY,
					Math.min(tileX + CHECKER_SIZE, right),
					Math.min(tileY + CHECKER_SIZE, bottom),
					checkerColor
				);
			}
		}
	}

	public static int toArgb(int color, ConfigColorFormat format) {
		if (format == ConfigColorFormat.RGB) {
			return 0xFF000000 | color;
		}
		return color;
	}
}
