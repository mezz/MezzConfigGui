package net.mezzdev.config.gui.model;

import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.util.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * A heading above a small subsection's values, with its description available on hover.
 */
public final class ConfigSectionHeader {
	private static final int HORIZONTAL_PADDING = 6;
	private static final int TOP_PADDING = 8;
	private static final int BOTTOM_PADDING = 5;

	private final Component title;
	private final ConfigInfo info;
	private ImmutableRect2i area = ImmutableRect2i.EMPTY;
	private List<FormattedCharSequence> lines = List.of();

	public ConfigSectionHeader(Component title, Component description) {
		this.title = StringUtil.stripStyling(title);
		this.info = new ConfigInfo(title, description);
	}

	public int updateBounds(int x, int y, int width) {
		Font font = Minecraft.getInstance().font;
		lines = font.split(title, Math.max(1, width - 2 * HORIZONTAL_PADDING));
		int height = Math.max(1, lines.size()) * font.lineHeight + TOP_PADDING + BOTTOM_PADDING;
		area = new ImmutableRect2i(x, y, width, height);
		return height;
	}

	public void resetBounds() {
		area = ImmutableRect2i.EMPTY;
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return area.contains(mouseX, mouseY);
	}

	public ConfigInfo getInfo() {
		return info;
	}

	public void draw(GuiGraphics guiGraphics) {
		if (area.equals(ImmutableRect2i.EMPTY)) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		int x = area.getX();
		int y = area.getY();
		int right = x + area.getWidth();
		int bottom = y + area.getHeight();
		guiGraphics.fill(x + 1, y + TOP_PADDING / 2, right - 1, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SECTION_HEADER_BACKGROUND));
		guiGraphics.fill(x + 1, bottom - 1, right - 1, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SECTION_HEADER_DIVIDER));
		int textY = y + TOP_PADDING;
		for (FormattedCharSequence line : lines) {
			guiGraphics.drawString(font, line, x + HORIZONTAL_PADDING, textY, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SECTION_HEADER_TEXT), false);
			textY += font.lineHeight;
		}
	}
}
