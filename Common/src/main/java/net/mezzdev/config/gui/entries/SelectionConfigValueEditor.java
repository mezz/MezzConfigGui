package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.IConfigValueEditor;
import net.mezzdev.config.gui.api.IConfigValuePopup;
import net.mezzdev.config.gui.popup.ConfigValueSelector;
import net.mezzdev.config.gui.textures.ConfigDrawableStatic;
import net.mezzdev.config.gui.textures.ConfigTextures;
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
	private static final int MAX_VALUE_BUTTON_WIDTH = 140;

	private final ConfigTextures textures;

	SelectionConfigValueEditor(ConfigTextures textures) {
		this.textures = textures;
	}

	@Override
	public int getControlWidth(IConfigValue<T> configValue, T value) {
		Font font = Minecraft.getInstance().font;
		Component valueName = getValueName(configValue, value);
		int textWidth = (int) (font.width(valueName) * ConfigEntryWidget.TEXT_SCALE);
		int preferredWidth = textWidth + ARROW_SIZE + ARROW_PADDING * 2 + VALUE_TEXT_PADDING * 2;
		return Math.min(preferredWidth, MAX_VALUE_BUTTON_WIDTH);
	}

	@Override
	public int getControlHeight(IConfigValue<T> configValue, T value) {
		return VALUE_BUTTON_HEIGHT;
	}

	@Override
	public void draw(
		GuiGraphics guiGraphics,
		Rect2i area,
		IConfigValue<T> configValue,
		T value,
		boolean hovered,
		boolean hasPendingChange
	) {
		Font font = Minecraft.getInstance().font;
		Component valueName = getValueName(configValue, value);
		ImmutableRect2i textArea = toImmutableRect2i(area)
			.cropLeft(VALUE_TEXT_PADDING)
			.cropRight(ARROW_SIZE + ARROW_PADDING * 2);
		ConfigEntryWidget.drawFittedText(guiGraphics, font, valueName, textArea, ConfigEntryWidget.TEXT_COLOR, false);

		ConfigDrawableStatic arrowDown = textures.getArrowDown();
		int arrowX = area.getX() + area.getWidth() - ARROW_SIZE - ARROW_PADDING;
		int arrowY = area.getY() + (area.getHeight() - ARROW_SIZE) / 2;
		arrowDown.draw(guiGraphics, arrowX, arrowY);
	}

	@Override
	public Optional<ConfigInfo> getTooltipInfo(
		Rect2i area,
		IConfigValue<T> configValue,
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
		IConfigValue<T> configValue,
		T value,
		double mouseX,
		double mouseY,
		int button
	) {
		if (button != 0) {
			return Optional.empty();
		}
		List<T> validValues = getValidValues(configValue);
		ConfigValueSelector<T> popup = new ConfigValueSelector<>(configValue, validValues, value);
		if (popup.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(popup);
	}

	private static <T> Component getValueName(IConfigValue<T> configValue, T value) {
		return configValue.getSerializer()
			.getLocalizedValueName(configValue.getLocalizationKey(), value);
	}

	private static <T> List<T> getValidValues(IConfigValue<T> configValue) {
		return configValue.getSerializer()
			.getAllValidValues()
			.map(List::copyOf)
			.orElseThrow(() -> new UnsupportedOperationException("Selection config value has no valid values: " + configValue.getName()));
	}

	private static ImmutableRect2i toImmutableRect2i(Rect2i area) {
		return new ImmutableRect2i(area.getX(), area.getY(), area.getWidth(), area.getHeight());
	}
}
