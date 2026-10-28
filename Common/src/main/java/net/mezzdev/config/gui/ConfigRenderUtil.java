package net.mezzdev.config.gui;

import net.mezzdev.config.gui.ConfigGuiColors.GuiColor;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Shared rendering helpers for config screens and quick menus. */
public final class ConfigRenderUtil {
	private ConfigRenderUtil() {}

	public static List<Component> getTooltipLines(ConfigInfo info) {
		List<Component> lines = new ArrayList<>();
		lines.add(withDefaultColor(info.title(), ConfigGuiColors.getColor(GuiColor.CONFIG_SCREEN_TOOLTIP_TITLE_TEXT)));
		for (Component line : info.lines()) {
			lines.add(withDefaultColor(line, ConfigGuiColors.getColor(GuiColor.CONFIG_SCREEN_TOOLTIP_TEXT)));
		}
		return lines;
	}

	private static Component withDefaultColor(Component component, int color) {
		var result = component.copy();
		if (result.getStyle().getColor() == null) {
			result.setStyle(result.getStyle().withColor(color & 0xFFFFFF));
		}
		return result;
	}
}
