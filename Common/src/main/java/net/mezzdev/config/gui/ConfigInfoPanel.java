package net.mezzdev.config.gui;

import net.mezzdev.config.gui.ConfigGuiColors.GuiColor;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.Font;
import net.mezzdev.config.gui.api.LegacyGuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Wraps info text once for both measuring and drawing the bottom panel.
 */
final class ConfigInfoPanel {
	private static final int PADDING = 5;
	private static final int TITLE_GAP = 2;
	@Nullable
	private ConfigInfo info;
	private Supplier<@Nullable ConfigInfo> infoSource = () -> null;
	@Nullable
	private Font font;
	@Nullable
	private Language language;
	private int textWidth;
	private int height;
	private List<FormattedCharSequence> titleLines = List.of();
	private List<FormattedCharSequence> bodyLines = List.of();

	void updateSource(Supplier<@Nullable ConfigInfo> source, Font font, int width, boolean readingPanel) {
		// Keep the hovered source when the panel grows under the pointer or the user
		// moves into it to read. Resolve it again so server access changes stay live.
		if (!readingPanel || info == null) {
			infoSource = source;
		}
		update(infoSource.get(), font, width);
	}

	private void update(@Nullable ConfigInfo info, Font font, int width) {
		int textWidth = Math.max(1, width - PADDING * 2);
		Language language = Language.getInstance();
		if (Objects.equals(this.info, info) && this.font == font && this.language == language && this.textWidth == textWidth) {
			return;
		}
		this.info = info;
		this.font = font;
		this.language = language;
		this.textWidth = textWidth;
		titleLines = List.of();
		bodyLines = List.of();
		height = PADDING * 2;
		if (info != null) {
			titleLines = font.split(info.title(), textWidth);
			List<FormattedCharSequence> bodyLines = new ArrayList<>();
			for (Component line : info.lines()) {
				bodyLines.addAll(font.split(line, textWidth));
			}
			this.bodyLines = List.copyOf(bodyLines);
			height += (titleLines.size() + bodyLines.size()) * font.lineHeight;
			if (!titleLines.isEmpty()) {
				height += TITLE_GAP;
			}
		}
	}

	int getHeight() {
		return height;
	}

	void draw(LegacyGuiGraphics guiGraphics, Font font, ImmutableRect2i area) {
		guiGraphics.fill(area.getX(), area.getY(), area.getX() + area.getWidth(), area.getY() + area.getHeight(),
			ConfigGuiColors.getColor(GuiColor.CONFIG_SCREEN_INFO_BACKGROUND));
		guiGraphics.fill(area.getX(), area.getY(), area.getX() + area.getWidth(), area.getY() + 1,
			ConfigGuiColors.getColor(GuiColor.CONFIG_SCREEN_INFO_BORDER));
		int x = area.getX() + PADDING;
		int y = area.getY() + PADDING;
		int bottom = area.getY() + area.getHeight() - PADDING;
		guiGraphics.enableScissor(area.getX(), area.getY(), area.getX() + area.getWidth(), area.getY() + area.getHeight());
		y = drawLines(guiGraphics, font, titleLines, x, y, bottom, GuiColor.CONFIG_SCREEN_INFO_TITLE_TEXT);
		if (!titleLines.isEmpty()) {
			y += TITLE_GAP;
		}
		drawLines(guiGraphics, font, bodyLines, x, y, bottom, GuiColor.CONFIG_SCREEN_INFO_TEXT);
		guiGraphics.disableScissor();
	}

	private static int drawLines(LegacyGuiGraphics guiGraphics, Font font, List<FormattedCharSequence> lines, int x, int y, int bottom, GuiColor color) {
		for (FormattedCharSequence line : lines) {
			if (y + font.lineHeight > bottom) {
				break;
			}
			guiGraphics.drawString(font, line, x, y, ConfigGuiColors.getColor(color), false);
			y += font.lineHeight;
		}
		return y;
	}
}
