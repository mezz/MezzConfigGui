package net.mezzdev.config.gui.entries;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditor;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.mezzdev.config.gui.textures.ConfigCheckbox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;

import java.util.Optional;

/**
 * Checkbox editor for boolean values.
 */
final class BooleanConfigValueEditor implements IConfigValueEditor<Boolean> {
	private static final int CONTROL_SIZE = 18;

	@Override
	public int getControlWidth(IConfigScreenValue<Boolean> configValue, Boolean value) {
		return CONTROL_SIZE;
	}

	@Override
	public int getControlHeight(IConfigScreenValue<Boolean> configValue, Boolean value) {
		return CONTROL_SIZE;
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
		ConfigCheckbox.draw(guiGraphics, area, value, hovered);
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
		if (button != InputConstants.MOUSE_BUTTON_LEFT) {
			return Optional.empty();
		}
		return Optional.of(!value);
	}
}
