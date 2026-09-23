package net.mezzdev.config.gui;

import net.mezzdev.config.gui.util.Pair;
import net.mezzdev.config.gui.util.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

final class ConfigTooltip {
	private static final int MAX_WIDTH = 280;

	private final List<Component> lines = new ArrayList<>();

	public void add(Component component) {
		lines.add(component);
	}

	public void draw(GuiGraphicsExtractor guiGraphics, int x, int y) {
		if (!lines.isEmpty()) {
			Font font = Minecraft.getInstance().font;
			List<FormattedText> formattedLines = lines.stream()
				.map(component -> (FormattedText) component)
				.toList();
			Pair<List<FormattedText>, Boolean> splitLines = StringUtil.splitLines(
				font,
				formattedLines,
				MAX_WIDTH,
				Integer.MAX_VALUE
			);
			List<FormattedCharSequence> visibleLines = Language.getInstance().getVisualOrder(splitLines.first());
			if (ConfigClientUtil.screen() instanceof MezzConfigScreen screen) {
				screen.setTooltipForNextRenderPass(visibleLines);
			} else {
				ConfigRenderUtil.tooltip(guiGraphics, font, visibleLines, x, y);
			}
		}
	}
}
