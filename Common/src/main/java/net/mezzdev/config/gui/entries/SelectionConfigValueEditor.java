package net.mezzdev.config.gui.entries;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditor;
import net.mezzdev.config.gui.api.IConfigValuePopup;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.popup.ConfigValueSelector;
import net.mezzdev.config.gui.textures.ConfigButtonIcon;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * Compact dropdown editor for values with a finite set of valid options.
 */
final class SelectionConfigValueEditor<T> implements IConfigValueEditor<T> {
	private static final int ARROW_SIZE = 9;
	private static final int ARROW_PADDING = 3;
	private static final int VALUE_TEXT_PADDING = 4;
	private static final int VALUE_BUTTON_HEIGHT = 18;

	SelectionConfigValueEditor() {
	}

	@Override
	public int getControlWidth(IConfigScreenValue<T> configValue, T value) {
		return ConfigEntryWidget.PREFERRED_VALUE_CONTROL_WIDTH;
	}

	@Override
	public int getControlHeight(IConfigScreenValue<T> configValue, T value) {
		return VALUE_BUTTON_HEIGHT;
	}

	@Override
	public void draw(
		GuiGraphics guiGraphics,
		Rect2i area,
		IConfigScreenValue<T> configValue,
		T value,
		boolean hovered,
		boolean hasPendingChange
	) {
		Font font = Minecraft.getInstance().font;
		Component valueName = getValueName(configValue, value);
		ImmutableRect2i textArea = toImmutableRect2i(area)
			.cropLeft(VALUE_TEXT_PADDING)
			.cropRight(ARROW_SIZE + ARROW_PADDING * 2);
		ConfigEntryWidget.drawFittedText(guiGraphics, font, valueName, textArea, ConfigEntryWidget.getConfiguredTextColor(), false);

		Rect2i arrowArea = new Rect2i(
			area.getX() + area.getWidth() - ARROW_SIZE - ARROW_PADDING,
			area.getY(),
			ARROW_SIZE,
			area.getHeight()
		);
		ConfigButtonIcon.DOWN.draw(guiGraphics, arrowArea, true);
	}

	@Override
	public Optional<ConfigInfo> getTooltipInfo(
		Rect2i area,
		IConfigScreenValue<T> configValue,
		T value,
		boolean hasPendingChange,
		double mouseX,
		double mouseY
	) {
		return Optional.empty();
	}

	@Override
	public Optional<IConfigValuePopup<T>> createPopup(
		Rect2i area,
		IConfigScreenValue<T> configValue,
		T value,
		double mouseX,
		double mouseY,
		int button
	) {
		if (button != InputConstants.MOUSE_BUTTON_LEFT) {
			return Optional.empty();
		}
		List<T> validValues = getValidValues(configValue);
		ConfigValueSelector<T> popup = new ConfigValueSelector<>(configValue, validValues, value);
		if (popup.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(popup);
	}

	private static <T> Component getValueName(IConfigScreenValue<T> configValue, T value) {
		return ConfigValueLocalization.getValueName(configValue, value);
	}

	private static <T> List<T> getValidValues(IConfigScreenValue<T> configValue) {
		return configValue.getSerializer()
			.getAllValidValues()
			.map(List::copyOf)
			.orElseThrow(() -> new UnsupportedOperationException("Selection config value has no valid values: " + configValue.getName()));
	}

	private static ImmutableRect2i toImmutableRect2i(Rect2i area) {
		return new ImmutableRect2i(area.getX(), area.getY(), area.getWidth(), area.getHeight());
	}
}
