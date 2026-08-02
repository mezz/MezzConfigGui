package net.mezzdev.config.gui.entries;

import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditor;
import net.mezzdev.config.gui.info.ConfigValueIcon;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;

import java.util.Optional;

/**
 * Compact toggle editor for boolean values.
 */
final class BooleanConfigValueEditor implements IConfigValueEditor<Boolean> {
	private static final int BUTTON_SIZE = 18;

	@Override
	public int getControlWidth(IConfigScreenValue<Boolean> configValue, Boolean value) {
		return BUTTON_SIZE;
	}

	@Override
	public int getControlHeight(IConfigScreenValue<Boolean> configValue, Boolean value) {
		return BUTTON_SIZE;
	}

	@Override
	public void draw(
		GuiGraphics guiGraphics,
		Rect2i area,
		IConfigScreenValue<Boolean> configValue,
		Boolean value,
		boolean hovered,
		boolean hasPendingChange
	) {
		ConfigValueIcon.drawInButton(guiGraphics, configValue, value, toImmutableRect2i(area));
	}

	@Override
	public Optional<ConfigInfo> getTooltipInfo(
		Rect2i area,
		IConfigScreenValue<Boolean> configValue,
		Boolean value,
		boolean hasPendingChange,
		double mouseX,
		double mouseY
	) {
		return Optional.of(ConfigValueInfoFactory.createBooleanToggle(configValue, value, hasPendingChange));
	}

	@Override
	public Optional<Boolean> getClickedValue(
		Rect2i area,
		IConfigScreenValue<Boolean> configValue,
		Boolean value,
		double mouseX,
		double mouseY,
		int button
	) {
		if (button != 0) {
			return Optional.empty();
		}
		return Optional.of(!value);
	}

	private static ImmutableRect2i toImmutableRect2i(Rect2i area) {
		return new ImmutableRect2i(area.getX(), area.getY(), area.getWidth(), area.getHeight());
	}
}
