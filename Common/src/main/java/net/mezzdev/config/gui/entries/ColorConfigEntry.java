package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.api.value.color.PackedColor;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.info.ColorSwatch;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.popup.ColorPickerPopup;
import net.mezzdev.config.gui.popup.ConfigPopupSelector;
import net.mezzdev.config.gui.popup.ConfigValuePopupSelector;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Config entry for packed integer colors with a standalone swatch and hexadecimal input control.
 */
final class ColorConfigEntry extends ConfigEntryWidget<PackedColor> {
	private static final int CONTROL_HEIGHT = 18;
	private static final int SWATCH_SIZE = 18;
	private static final int CONTROL_GAP = 3;
	private static final int HEX_TEXT_PADDING = 4;
	private static final int MIN_HEX_FIELD_WIDTH = 39;
	private static final int MIN_CONTROL_WIDTH = SWATCH_SIZE + CONTROL_GAP + MIN_HEX_FIELD_WIDTH;

	private final IConfigValueSerializer<PackedColor> serializer;
	private final Consumer<ConfigPopupSelector> valueSelectorOpener;
	private ImmutableRect2i swatchArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i hexArea = ImmutableRect2i.EMPTY;

	ColorConfigEntry(
		IConfigScreenValue<PackedColor> value,
		IConfigValueSerializer<PackedColor> serializer,
		Consumer<ConfigPopupSelector> valueSelectorOpener,
		ConfigTextures textures
	) {
		super(value, textures);
		this.serializer = serializer;
		this.valueSelectorOpener = valueSelectorOpener;
	}

	@Override
	public void updateBounds(ImmutableRect2i area) {
		super.updateBounds(area);
		int controlWidth = getValueColumnWidth(area, MIN_CONTROL_WIDTH);
		int hexWidth = controlWidth - SWATCH_SIZE - CONTROL_GAP;
		int y = area.getY() + (area.getHeight() - CONTROL_HEIGHT) / 2;
		int x = area.getX() + area.getWidth() - controlWidth - VALUE_CONTROL_RIGHT_RESERVE;
		swatchArea = new ImmutableRect2i(x, y, SWATCH_SIZE, SWATCH_SIZE);
		hexArea = new ImmutableRect2i(x + SWATCH_SIZE + CONTROL_GAP, y, hexWidth, CONTROL_HEIGHT);
		recomputeNameArea(area, getValueColumnNameRightReserve(area, MIN_CONTROL_WIDTH));
	}

	@Override
	protected void drawContent(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		Font font = Minecraft.getInstance().font;
		drawName(guiGraphics);

		ColorSwatch.draw(guiGraphics, toRect2i(swatchArea), getValue());
		drawButtonBackground(guiGraphics, getTextures(), hexArea, true, hexArea.contains(mouseX, mouseY));
		drawHexText(guiGraphics, font, ColorSwatch.formatHex(getValue()));
	}

	private void drawHexText(GuiGraphics guiGraphics, Font font, String text) {
		ImmutableRect2i textArea = hexArea.cropLeft(HEX_TEXT_PADDING).cropRight(HEX_TEXT_PADDING);
		int y = getCenteredTextY(font, textArea);
		String visibleText = text;
		if (font.width(visibleText) > textArea.getWidth()) {
			visibleText = font.plainSubstrByWidth(visibleText, textArea.getWidth(), true);
		}
		drawText(guiGraphics, font, visibleText, textArea.getX(), y, getConfiguredTextColor());
	}

	@Override
	public ConfigInfo getInfo() {
		ConfigInfo info = super.getInfo();
		List<Component> lines = new ArrayList<>(info.lines());
		lines.add(Component.translatable("mezz_config.config.screen.validValues", serializer.getValidValuesDescription()));
		return new ConfigInfo(info.title(), lines);
	}

	@Override
	@Nullable
	public ConfigInfo getTooltipInfo(double mouseX, double mouseY) {
		ConfigInfo resetInfo = super.getTooltipInfo(mouseX, mouseY);
		if (resetInfo != null) {
			return resetInfo;
		}
		if (swatchArea.contains(mouseX, mouseY)) {
			return new ConfigInfo(
				Component.translatable("mezz_config.config.screen.color.choose"),
				List.of(Component.literal(ColorSwatch.formatHex(getValue())))
			);
		}
		if (hexArea.contains(mouseX, mouseY)) {
			return ConfigValueInfoFactory.createUpdateInfo(configValue, getValue(), hasPendingChange());
		}
		return null;
	}

	@Override
	protected boolean onMouseClicked(UserInput input) {
		if (super.onMouseClicked(input)) {
			return true;
		}
		if (swatchArea.contains(input.getMouseX(), input.getMouseY())) {
			if (!input.isSimulate()) {
				openColorPicker(false);
			}
			return true;
		}
		if (hexArea.contains(input.getMouseX(), input.getMouseY())) {
			if (!input.isSimulate()) {
				openColorPicker(true);
			}
			return true;
		}
		return false;
	}

	private void openColorPicker(boolean focusHexInput) {
		ColorPickerPopup popup = new ColorPickerPopup(getValue(), focusHexInput);
		ConfigValuePopupSelector<PackedColor> selector = new ConfigValuePopupSelector<>(
			configValue,
			popup,
			() -> getColorPickerAnchorArea(focusHexInput),
			this::hasPendingChange,
			this::setValue
		);
		valueSelectorOpener.accept(selector);
	}

	private ImmutableRect2i getColorPickerAnchorArea(boolean focusHexInput) {
		if (focusHexInput) {
			return hexArea;
		}
		return swatchArea;
	}

	private static Rect2i toRect2i(ImmutableRect2i area) {
		return new Rect2i(area.getX(), area.getY(), area.getWidth(), area.getHeight());
	}
}
