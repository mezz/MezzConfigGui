package net.mezzdev.config.gui.popup;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.ConfigInputUtil;

import net.mezzdev.config.gui.util.ConfigMath;

import net.mezzdev.config.api.value.color.ConfigColorFormat;
import net.mezzdev.config.api.value.color.PackedColor;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.api.IConfigValuePopup;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.info.ColorSwatch;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Standard visual color picker with a hue-saturation chart, value and RGB sliders, and hexadecimal input.
 */
public final class ColorPickerPopup implements IConfigValuePopup<PackedColor> {
	private static final int WIDTH = 256;
	private static final int CHANNEL_SLIDER_COUNT = 3;
	private static final int MIN_COMPACT_PREVIEW_HEIGHT = 8;
	private static final int MIN_COMPACT_PLANE_HEIGHT = 44;
	private static final LayoutMetrics STANDARD_LAYOUT = new LayoutMetrics(
		7,
		5,
		28,
		86,
		12,
		3,
		18,
		18,
		2,
		14,
		10,
		14,
		30,
		11,
		18
	);
	private static final LayoutMetrics COMPACT_LAYOUT = new LayoutMetrics(
		4,
		3,
		14,
		64,
		10,
		2,
		14,
		14,
		1,
		12,
		8,
		10,
		24,
		9,
		16
	);
	private static final int FIELD_TEXT_PADDING = 2;
	private static final int DONE_BUTTON_WIDTH = 44;
	private static final int MAX_EDIT_TEXT_LENGTH = 12;
	private static final float PRECISE_SLIDER_SCALE = 0.1f;
	private final ConfigColorFormat format;
	private final ColorPickerModel model;
	private ColorAxis verticalAxis = ColorAxis.VALUE;
	private ColorControl activeControl = ColorControl.NONE;
	private ColorControl precisionControl = ColorControl.NONE;
	private float precisionPointerAnchor;
	private float precisionValueAnchor;
	@Nullable
	private ColorField focusedField;
	private String editText = "";
	private boolean selectAll;
	private boolean doneSelected;

	public ColorPickerPopup(PackedColor color) {
		this(color, false);
	}

	public ColorPickerPopup(PackedColor color, boolean focusHexInput) {
		this.format = color.format();
		this.model = new ColorPickerModel(color);
		if (focusHexInput) {
			focusField(ColorField.HEX);
		}
	}

	@Override
	public int getWidth() {
		return WIDTH;
	}

	@Override
	public int getHeight() {
		return getLayoutHeight(STANDARD_LAYOUT, hasAlpha());
	}

	@Override
	public Size getPreferredSize(int availableWidth, int availableHeight) {
		LayoutMetrics metrics = STANDARD_LAYOUT;
		if (availableWidth < WIDTH || availableHeight < getHeight()) {
			metrics = COMPACT_LAYOUT;
		}
		int width = Math.min(WIDTH, availableWidth);
		int height = Math.min(getLayoutHeight(metrics, hasAlpha()), availableHeight);
		return new Size(width, height);
	}

	@Override
	public Optional<PackedColor> getHoveredValue(Rect2i area, double mouseX, double mouseY) {
		return Optional.empty();
	}

	@Override
	public Optional<PackedColor> getClickedValue(Rect2i area, double mouseX, double mouseY, int button) {
		doneSelected = false;
		if (button != InputConstants.MOUSE_BUTTON_LEFT) {
			return Optional.empty();
		}
		PickerLayout layout = createLayout(area);
		if (contains(layout.doneButtonArea(), mouseX, mouseY)) {
			if (!canFinish()) {
				return Optional.empty();
			}
			PackedColor color = getEditedValue().orElseGet(model::getPackedColor);
			activeControl = ColorControl.NONE;
			clearPrecisionControl();
			clearFocus();
			doneSelected = true;
			return Optional.of(color);
		}
		ColorAxis clickedAxis = layout.getAxisSelector(mouseX, mouseY);
		if (clickedAxis != null) {
			verticalAxis = clickedAxis;
			activeControl = ColorControl.NONE;
			clearPrecisionControl();
			clearFocus();
			return Optional.empty();
		}
		ColorField clickedField = layout.getField(mouseX, mouseY);
		if (clickedField != null) {
			activeControl = ColorControl.NONE;
			clearPrecisionControl();
			focusField(clickedField);
			return Optional.empty();
		}

		clearFocus();
		if (activeControl == ColorControl.NONE) {
			activeControl = layout.getControl(mouseX, mouseY);
			clearPrecisionControl();
			return updateValue(activeControl, layout, mouseX, mouseY);
		}
		ColorControl releasedControl = activeControl;
		Optional<PackedColor> value = updateValue(releasedControl, layout, mouseX, mouseY);
		activeControl = ColorControl.NONE;
		clearPrecisionControl();
		return value;
	}

	@Override
	public void mouseReleased(Rect2i area, double mouseX, double mouseY, int button) {
		if (button == InputConstants.MOUSE_BUTTON_LEFT) {
			activeControl = ColorControl.NONE;
			clearPrecisionControl();
		}
	}

	@Override
	public Optional<PackedColor> getDraggedValue(Rect2i area, double mouseX, double mouseY, int button) {
		doneSelected = false;
		if (button != InputConstants.MOUSE_BUTTON_LEFT) {
			return Optional.empty();
		}
		PickerLayout layout = createLayout(area);
		ColorControl control = activeControl;
		if (control == ColorControl.NONE) {
			control = layout.getControl(mouseX, mouseY);
			activeControl = control;
			clearPrecisionControl();
		}
		return updateValue(control, layout, mouseX, mouseY);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers, Consumer<PackedColor> valueConsumer) {
		if (focusedField == null) {
			return false;
		}
		appendCharacter(codePoint);
		applyEditText(valueConsumer);
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers, Consumer<PackedColor> valueConsumer) {
		if (focusedField == null) {
			return false;
		}
		if (ConfigInputUtil.isSelectAll(keyCode)) {
			selectAll = true;
			return true;
		}
		if (ConfigInputUtil.isPaste(keyCode)) {
			appendText(Minecraft.getInstance().keyboardHandler.getClipboard());
			applyEditText(valueConsumer);
			return true;
		}
		if (keyCode == InputConstants.KEY_TAB) {
			focusField(ColorField.HEX);
			return true;
		}
		if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
			applyEditText(valueConsumer);
			selectAll = true;
			return true;
		}
		if (keyCode == InputConstants.KEY_ESCAPE) {
			clearFocus();
			return true;
		}
		if (keyCode == InputConstants.KEY_BACKSPACE) {
			removeLastCharacter();
			applyEditText(valueConsumer);
			return true;
		}
		if (keyCode == InputConstants.KEY_DELETE) {
			editText = "";
			selectAll = false;
			return true;
		}
		return true;
	}

	@Override
	public boolean closesAfterValueSelected() {
		return doneSelected;
	}

	@Override
	public void draw(GuiGraphicsExtractor guiGraphics, Rect2i area, double mouseX, double mouseY) {
		PickerLayout layout = createLayout(area);
		drawBackground(guiGraphics, area);
		ColorSwatch.draw(guiGraphics, layout.colorPreviewArea(), model.getPackedColor());
		drawColorControls(guiGraphics, layout, mouseX, mouseY);
		if (hasAlpha()) {
			drawAlpha(guiGraphics, layout.alphaArea(), layout.channelSliderLabelWidth());
		}
		drawFields(guiGraphics, layout);
		drawDoneButton(guiGraphics, layout.doneButtonArea(), mouseX, mouseY);
	}

	private boolean canFinish() {
		return focusedField == null || isEditTextValid();
	}

	private void drawDoneButton(GuiGraphicsExtractor guiGraphics, Rect2i area, double mouseX, double mouseY) {
		boolean enabled = canFinish();
		int backgroundColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_BACKGROUND);
		int borderColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_BORDER);
		if (enabled && contains(area, mouseX, mouseY)) {
			backgroundColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_FOCUSED_BACKGROUND);
			borderColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_FOCUSED_BORDER);
		}
		fillWithBorder(guiGraphics, area, backgroundColor, borderColor);
		int textColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_DISABLED_TEXT);
		if (enabled) {
			textColor = ConfigEntryWidget.getConfiguredTextColor();
		}
		ConfigEntryWidget.drawCenteredButtonText(
			guiGraphics, Minecraft.getInstance().font, Component.translatable("gui.done"), toImmutableRect2i(area), textColor
		);
	}

	private Optional<PackedColor> updateValue(ColorControl control, PickerLayout layout, double mouseX, double mouseY) {
		if (control == ColorControl.NONE) {
			return Optional.empty();
		}
		updateModel(model, control, layout, mouseX, mouseY);
		return Optional.of(model.getPackedColor());
	}

	private void updateModel(
		ColorPickerModel model,
		ColorControl control,
		PickerLayout layout,
		double mouseX,
		double mouseY
	) {
		switch (control.type()) {
			case PLANE -> updatePlane(model, layout.colorPlaneArea(), mouseX, mouseY);
			case VERTICAL_AXIS -> updateVerticalAxisSlider(model, layout.verticalSliderArea(), mouseY);
			case SLIDER -> updateSlider(model, control.field(), layout, mouseX, mouseY);
			case ALPHA -> {
				Rect2i area = layout.alphaArea();
				float pointerPosition = getPosition(mouseX, area.getX(), area.getWidth());
				model.setAlpha(getAdjustedSliderPosition(ColorControl.ALPHA, pointerPosition));
			}
			case NONE -> {}
		}
	}

	private void updatePlane(ColorPickerModel model, Rect2i area, double mouseX, double mouseY) {
		float x = getPosition(mouseX, area.getX(), area.getWidth());
		float y = 1.0f - getPosition(mouseY, area.getY(), area.getHeight());
		switch (verticalAxis) {
			case HUE -> model.setSaturationAndValue(x, y);
			case SATURATION -> {
				model.setHue(x);
				model.setSaturationAndValue(model.getSaturation(), y);
			}
			case VALUE -> {
				model.setHue(x);
				model.setSaturationAndValue(y, model.getValue());
			}
		}
	}

	private void updateVerticalAxisSlider(ColorPickerModel model, Rect2i area, double mouseY) {
		float pointerPosition = getVerticalAxisPosition(mouseY, area);
		float value = getAdjustedSliderPosition(ColorControl.VERTICAL_AXIS, pointerPosition);
		switch (verticalAxis) {
			case HUE -> model.setHue(value);
			case SATURATION -> model.setSaturationAndValue(value, model.getValue());
			case VALUE -> model.setSaturationAndValue(model.getSaturation(), value);
		}
	}

	private float getVerticalAxisPosition(double mouseY, Rect2i area) {
		float position = getPosition(mouseY, area.getY(), area.getHeight());
		if (verticalAxis == ColorAxis.HUE) {
			return position;
		}
		return 1.0f - position;
	}

	private void updateSlider(
		ColorPickerModel model,
		@Nullable ColorField field,
		PickerLayout layout,
		double mouseX,
		double mouseY
	) {
		if (field == null) {
			return;
		}
		SliderArea slider = layout.getSlider(field);
		if (slider == null) {
			return;
		}
		float pointerPosition = slider.getPosition(mouseX, mouseY);
		float position = getAdjustedSliderPosition(ColorControl.slider(field), pointerPosition);
		setSliderValue(model, field, position);
	}

	private void setSliderValue(ColorPickerModel model, ColorField field, float position) {
		switch (field) {
			case RED, GREEN, BLUE -> applyRgbEdit(model, field, Math.round(position * 255.0f));
			default -> throw new IllegalArgumentException("Unsupported color slider field: " + field);
		}
	}

	private float getAdjustedSliderPosition(ColorControl control, float pointerPosition) {
		if (!isShiftDown()) {
			clearPrecisionControl();
			return pointerPosition;
		}
		if (!control.equals(precisionControl)) {
			precisionControl = control;
			precisionPointerAnchor = pointerPosition;
			precisionValueAnchor = getControlPosition(control);
		}
		return getPreciseSliderPosition(precisionValueAnchor, precisionPointerAnchor, pointerPosition);
	}

	static float getPreciseSliderPosition(float valueAnchor, float pointerAnchor, float pointerPosition) {
		return ConfigMath.clamp(valueAnchor + (pointerPosition - pointerAnchor) * PRECISE_SLIDER_SCALE, 0.0f, 1.0f);
	}

	private static boolean isShiftDown() {
		return Minecraft.getInstance() != null && ConfigInputUtil.hasShiftDown();
	}

	private float getControlPosition(ColorControl control) {
		if (control.type() == ControlType.VERTICAL_AXIS) {
			return switch (verticalAxis) {
				case HUE -> model.getHue();
				case SATURATION -> model.getSaturation();
				case VALUE -> model.getValue();
			};
		}
		if (control.type() == ControlType.ALPHA) {
			return model.getAlpha();
		}
		ColorField field = control.field();
		if (field == null) {
			return 0.0f;
		}
		return getSliderPosition(field);
	}

	private void clearPrecisionControl() {
		precisionControl = ColorControl.NONE;
	}

	private void drawColorControls(GuiGraphicsExtractor guiGraphics, PickerLayout layout, double mouseX, double mouseY) {
		drawColorPlane(guiGraphics, layout.colorPlaneArea());
		drawVerticalAxisSlider(guiGraphics, layout.verticalSliderArea());
		drawAxisSelectors(guiGraphics, layout.axisSelectorAreas(), mouseX, mouseY);
		drawSliders(guiGraphics, layout.sliderAreas(), layout.channelSliderLabelWidth());
	}

	private void drawColorPlane(GuiGraphicsExtractor guiGraphics, Rect2i area) {
		for (int xOffset = 0; xOffset < area.getWidth(); xOffset++) {
			float x = getPosition(xOffset, 0, area.getWidth());
			int topColor = getPlaneColor(x, 1.0f);
			int bottomColor = getPlaneColor(x, 0.0f);
			guiGraphics.fillGradient(
				area.getX() + xOffset,
				area.getY(),
				area.getX() + xOffset + 1,
				area.getY() + area.getHeight(),
				topColor,
				bottomColor
			);
		}
		drawBorder(guiGraphics, area, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_BORDER));
		float markerXPosition = switch (verticalAxis) {
			case HUE -> model.getSaturation();
			case SATURATION, VALUE -> model.getHue();
		};
		float markerYPosition = switch (verticalAxis) {
			case HUE, SATURATION -> 1.0f - model.getValue();
			case VALUE -> 1.0f - model.getSaturation();
		};
		int markerX = area.getX() + Math.round(markerXPosition * (area.getWidth() - 1));
		int markerY = area.getY() + Math.round(markerYPosition * (area.getHeight() - 1));
		drawPointMarker(guiGraphics, markerX, markerY);
	}

	private int getPlaneColor(float x, float y) {
		return switch (verticalAxis) {
			case HUE -> ColorPickerModel.hsvToArgb(model.getHue(), x, y);
			case SATURATION -> ColorPickerModel.hsvToArgb(x, model.getSaturation(), y);
			case VALUE -> ColorPickerModel.hsvToArgb(x, y, model.getValue());
		};
	}

	private void drawVerticalAxisSlider(GuiGraphicsExtractor guiGraphics, Rect2i area) {
		for (int yOffset = 0; yOffset < area.getHeight(); yOffset++) {
			float position = getVerticalAxisPosition(area.getY() + yOffset, area);
			int color = getVerticalAxisColor(position);
			guiGraphics.fill(area.getX(), area.getY() + yOffset, area.getX() + area.getWidth(), area.getY() + yOffset + 1, color);
		}
		drawBorder(guiGraphics, area, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_BORDER));
		float markerPosition = switch (verticalAxis) {
			case HUE -> model.getHue();
			case SATURATION -> 1.0f - model.getSaturation();
			case VALUE -> 1.0f - model.getValue();
		};
		int markerY = area.getY() + Math.round(markerPosition * (area.getHeight() - 1));
		drawHorizontalMarker(guiGraphics, area, markerY);
	}

	private int getVerticalAxisColor(float position) {
		return switch (verticalAxis) {
			case HUE -> ColorPickerModel.hsvToArgb(position, 1.0f, 1.0f);
			case SATURATION -> ColorPickerModel.hsvToArgb(model.getHue(), position, 1.0f);
			case VALUE -> ColorPickerModel.hsvToArgb(model.getHue(), model.getSaturation(), position);
		};
	}

	private void drawAxisSelectors(
		GuiGraphicsExtractor guiGraphics,
		List<AxisSelectorArea> selectorAreas,
		double mouseX,
		double mouseY
	) {
		Font font = Minecraft.getInstance().font;
		for (AxisSelectorArea selectorArea : selectorAreas) {
			Rect2i area = selectorArea.area();
			boolean selected = selectorArea.axis() == verticalAxis;
			boolean hovered = contains(area, mouseX, mouseY);
			int backgroundColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_BACKGROUND);
			if (selected) {
				backgroundColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_FOCUSED_BACKGROUND);
			}
			int borderColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_BORDER);
			if (selected || hovered) {
				borderColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_FOCUSED_BORDER);
			}
			fillWithBorder(guiGraphics, area, backgroundColor, borderColor);
			String label = selectorArea.axis().label;
			int x = area.getX() + (area.getWidth() - font.width(label)) / 2 + 1;
			int y = area.getY() + (area.getHeight() - font.lineHeight) / 2 + 1;
			guiGraphics.text(font, label, x, y, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_MARKER_LIGHT), false);
		}
	}

	private void drawSliders(GuiGraphicsExtractor guiGraphics, List<SliderArea> sliders, int labelWidth) {
		for (SliderArea slider : sliders) {
			drawSlider(guiGraphics, slider, labelWidth);
		}
	}

	private void drawSlider(GuiGraphicsExtractor guiGraphics, SliderArea slider, int labelWidth) {
		Rect2i area = slider.area();
		int length = area.getWidth();
		for (int offset = 0; offset < length; offset++) {
			float position = getPosition(offset, 0, length);
			int color = getSliderColor(slider.field(), position);
			guiGraphics.fill(area.getX() + offset, area.getY(), area.getX() + offset + 1, area.getY() + area.getHeight(), color);
		}
		drawBorder(guiGraphics, area, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_BORDER));
		float markerPosition = getSliderPosition(slider.field());
		int markerX = area.getX() + Math.round(markerPosition * (area.getWidth() - 1));
		drawVerticalMarker(guiGraphics, area, markerX);
		drawSliderLabel(guiGraphics, slider, labelWidth);
	}

	private int getSliderColor(ColorField field, float position) {
		return switch (field) {
			case RED -> 0xFF000000 | Math.round(position * 255.0f) << 16;
			case GREEN -> 0xFF000000 | Math.round(position * 255.0f) << 8;
			case BLUE -> 0xFF000000 | Math.round(position * 255.0f);
			default -> throw new IllegalArgumentException("Unsupported color slider field: " + field);
		};
	}

	private float getSliderPosition(ColorField field) {
		return switch (field) {
			case RED -> model.getRgb().red() / 255.0f;
			case GREEN -> model.getRgb().green() / 255.0f;
			case BLUE -> model.getRgb().blue() / 255.0f;
			default -> throw new IllegalArgumentException("Unsupported color slider field: " + field);
		};
	}

	private void drawSliderLabel(GuiGraphicsExtractor guiGraphics, SliderArea slider, int labelWidth) {
		Font font = Minecraft.getInstance().font;
		Rect2i area = slider.area();
		int textY = area.getY() + (area.getHeight() - font.lineHeight) / 2;
		String label = slider.field().label;
		int labelX = area.getX() - labelWidth + (labelWidth - font.width(label)) / 2;
		guiGraphics.text(font, label, labelX, textY, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_MARKER_LIGHT), false);
		String value = getFieldText(slider.field());
		int valueX = area.getX() + area.getWidth() + 3;
		guiGraphics.text(font, value, valueX, textY, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_MARKER_LIGHT), false);
	}

	private void drawAlpha(GuiGraphicsExtractor guiGraphics, Rect2i area, int labelWidth) {
		ColorSwatch.drawCheckerboard(
			guiGraphics,
			area.getX(),
			area.getY(),
			area.getX() + area.getWidth(),
			area.getY() + area.getHeight()
		);
		int rgb = model.getOpaqueRgbColor() & 0x00FFFFFF;
		for (int xOffset = 0; xOffset < area.getWidth(); xOffset++) {
			int alpha = Math.round(getPosition(xOffset, 0, area.getWidth()) * 255.0f);
			int color = alpha << 24 | rgb;
			guiGraphics.fill(area.getX() + xOffset, area.getY(), area.getX() + xOffset + 1, area.getY() + area.getHeight(), color);
		}
		int markerX = area.getX() + Math.round(model.getAlpha() * (area.getWidth() - 1));
		drawVerticalMarker(guiGraphics, area, markerX);
		drawBorder(guiGraphics, area, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_BORDER));
		Font font = Minecraft.getInstance().font;
		int textY = area.getY() + (area.getHeight() - font.lineHeight) / 2;
		int labelX = area.getX() - labelWidth + (labelWidth - font.width(ColorField.ALPHA.label)) / 2;
		guiGraphics.text(font, ColorField.ALPHA.label, labelX, textY, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_MARKER_LIGHT), false);
		guiGraphics.text(
			font,
			getFieldText(ColorField.ALPHA),
			area.getX() + area.getWidth() + 3,
			textY,
			ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_MARKER_LIGHT),
			false
		);
	}

	private void drawFields(GuiGraphicsExtractor guiGraphics, PickerLayout layout) {
		Font font = Minecraft.getInstance().font;
		drawField(guiGraphics, font, layout.hexFieldArea());
	}

	private void drawField(GuiGraphicsExtractor guiGraphics, Font font, FieldArea fieldArea) {
		ColorField field = fieldArea.field();
		boolean focused = field == focusedField;
		boolean valid = !focused || isEditTextValid();
		int backgroundColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_BACKGROUND);
		if (focused) {
			backgroundColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_FOCUSED_BACKGROUND);
		}
		int borderColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_BORDER);
		if (focused) {
			borderColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_INVALID_BORDER);
			if (valid) {
				borderColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_FOCUSED_BORDER);
			}
		}
		fillWithBorder(guiGraphics, fieldArea.area(), backgroundColor, borderColor);

		String valueText = getFieldText(field);
		if (focused) {
			valueText = editText + "_";
		}
		String text = field.label + valueText;
		ImmutableRect2i textArea = toImmutableRect2i(fieldArea.area())
			.cropLeft(FIELD_TEXT_PADDING)
			.cropRight(FIELD_TEXT_PADDING);
		String visibleText = text;
		if (font.width(visibleText) > textArea.getWidth()) {
			visibleText = font.plainSubstrByWidth(visibleText, textArea.getWidth(), true);
		}
		int textY = ConfigEntryWidget.getCenteredTextY(font, textArea);
		int textColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_FIELD_INVALID_BORDER);
		if (valid) {
			textColor = ConfigEntryWidget.getConfiguredTextColor();
		}
		ConfigEntryWidget.drawText(guiGraphics, font, visibleText, textArea.getX(), textY, textColor);
	}

	private void focusField(ColorField field) {
		focusedField = field;
		editText = getFieldText(field);
		selectAll = true;
	}

	private void clearFocus() {
		focusedField = null;
		editText = "";
		selectAll = false;
	}

	private void appendText(String text) {
		for (int i = 0; i < text.length(); i++) {
			appendCharacter(text.charAt(i));
		}
	}

	private void appendCharacter(char codePoint) {
		if (!StringUtil.isAllowedChatCharacter(codePoint)) {
			return;
		}
		if (selectAll) {
			editText = "";
			selectAll = false;
		}
		if (editText.length() < MAX_EDIT_TEXT_LENGTH) {
			editText += codePoint;
		}
	}

	private void removeLastCharacter() {
		if (selectAll) {
			editText = "";
			selectAll = false;
		} else if (!editText.isEmpty()) {
			editText = editText.substring(0, editText.length() - 1);
		}
	}

	private void applyEditText(Consumer<PackedColor> valueConsumer) {
		getEditedValue().ifPresent(valueConsumer);
	}

	private boolean isEditTextValid() {
		return getParsedEdit().isPresent();
	}

	private Optional<PackedColor> getEditedValue() {
		Optional<ParsedEdit> parsedEdit = getParsedEdit();
		if (parsedEdit.isEmpty()) {
			return Optional.empty();
		}
		applyParsedEdit(parsedEdit.get());
		return Optional.of(model.getPackedColor());
	}

	private Optional<ParsedEdit> getParsedEdit() {
		if (focusedField != ColorField.HEX) {
			return Optional.empty();
		}
		return parseHexEdit(editText);
	}

	private Optional<ParsedEdit> parseHexEdit(String text) {
		String hex = text.trim();
		if (hex.startsWith("#")) {
			hex = hex.substring(1);
		} else if (hex.regionMatches(true, 0, "0x", 0, 2)) {
			hex = hex.substring(2);
		}
		boolean hasHexAlpha = hasAlpha() && hex.length() == 8;
		if (hex.length() != 6 && !hasHexAlpha) {
			return Optional.empty();
		}
		try {
			long value = Long.parseUnsignedLong(hex, 16);
			int rgb = (int) value & 0x00FFFFFF;
			int alpha = -1;
			if (hasHexAlpha) {
				alpha = (int) (value >>> 24) & 0xFF;
			}
			return Optional.of(new ParsedEdit(rgb, alpha));
		} catch (NumberFormatException ignored) {
			return Optional.empty();
		}
	}

	private void applyParsedEdit(ParsedEdit edit) {
		int value = edit.value();
		model.setRgb((value >>> 16) & 0xFF, (value >>> 8) & 0xFF, value & 0xFF);
		if (edit.alpha() >= 0) {
			model.setAlphaChannel(edit.alpha());
		}
	}

	private static void applyRgbEdit(ColorPickerModel model, ColorField field, int value) {
		ColorPickerModel.Rgb rgb = model.getRgb();
		int red = rgb.red();
		int green = rgb.green();
		int blue = rgb.blue();
		switch (field) {
			case RED -> red = value;
			case GREEN -> green = value;
			case BLUE -> blue = value;
			default -> throw new IllegalArgumentException("Not an RGB field: " + field);
		}
		model.setRgb(red, green, blue);
	}

	private String getFieldText(ColorField field) {
		return switch (field) {
			case RED -> Integer.toString(model.getRgb().red());
			case GREEN -> Integer.toString(model.getRgb().green());
			case BLUE -> Integer.toString(model.getRgb().blue());
			case ALPHA -> Integer.toString(model.getAlphaChannel());
			case HEX -> ColorSwatch.formatHex(model.getPackedColor());
		};
	}

	private static void drawBackground(GuiGraphicsExtractor guiGraphics, Rect2i area) {
		int right = area.getX() + area.getWidth();
		int bottom = area.getY() + area.getHeight();
		guiGraphics.fill(area.getX(), area.getY(), right, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_BACKGROUND));
		guiGraphics.fill(area.getX(), area.getY(), right, area.getY() + 1, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_BORDER_DARK));
		guiGraphics.fill(area.getX(), area.getY(), area.getX() + 1, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_BORDER_DARK));
		guiGraphics.fill(right - 1, area.getY(), right, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_BORDER_LIGHT));
		guiGraphics.fill(area.getX(), bottom - 1, right, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_BORDER_LIGHT));
	}

	private static void fillWithBorder(GuiGraphicsExtractor guiGraphics, Rect2i area, int fillColor, int borderColor) {
		int right = area.getX() + area.getWidth();
		int bottom = area.getY() + area.getHeight();
		guiGraphics.fill(area.getX(), area.getY(), right, bottom, borderColor);
		guiGraphics.fill(area.getX() + 1, area.getY() + 1, right - 1, bottom - 1, fillColor);
	}

	private static void drawBorder(GuiGraphicsExtractor guiGraphics, Rect2i area, int color) {
		int right = area.getX() + area.getWidth();
		int bottom = area.getY() + area.getHeight();
		guiGraphics.fill(area.getX(), area.getY(), right, area.getY() + 1, color);
		guiGraphics.fill(area.getX(), area.getY(), area.getX() + 1, bottom, color);
		guiGraphics.fill(right - 1, area.getY(), right, bottom, color);
		guiGraphics.fill(area.getX(), bottom - 1, right, bottom, color);
	}

	private static void drawPointMarker(GuiGraphicsExtractor guiGraphics, int x, int y) {
		guiGraphics.fill(x - 3, y - 3, x + 4, y + 4, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_MARKER_DARK));
		guiGraphics.fill(x - 2, y - 2, x + 3, y + 3, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_MARKER_LIGHT));
		guiGraphics.fill(x - 1, y - 1, x + 2, y + 2, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_MARKER_DARK));
	}

	private static void drawHorizontalMarker(GuiGraphicsExtractor guiGraphics, Rect2i area, int y) {
		guiGraphics.fill(area.getX() - 1, y - 1, area.getX() + area.getWidth() + 1, y + 2, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_MARKER_DARK));
		guiGraphics.fill(area.getX(), y, area.getX() + area.getWidth(), y + 1, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_MARKER_LIGHT));
	}

	private static void drawVerticalMarker(GuiGraphicsExtractor guiGraphics, Rect2i area, int x) {
		guiGraphics.fill(x - 1, area.getY() - 1, x + 2, area.getY() + area.getHeight() + 1, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_MARKER_DARK));
		guiGraphics.fill(x, area.getY(), x + 1, area.getY() + area.getHeight(), ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.COLOR_PICKER_MARKER_LIGHT));
	}

	private PickerLayout createLayout(Rect2i area) {
		LayoutMetrics metrics = getLayoutMetrics(area);
		int contentWidth = Math.max(1, area.getWidth() - metrics.padding() * 2);
		int visualX = area.getX() + metrics.padding();
		int visualY = area.getY() + metrics.padding();
		int leftGutterWidth = metrics.channelSliderLabelWidth();
		int sideControlsWidth = metrics.controlGap() + metrics.valueSliderWidth() + metrics.axisSelectorGap() +
			metrics.axisSelectorWidth();
		int rightGutterWidth = Math.max(metrics.channelSliderValueWidth(), sideControlsWidth);
		int controlX = visualX + leftGutterWidth;
		int controlWidth = Math.max(
			1,
			contentWidth - leftGutterWidth - rightGutterWidth
		);
		Rect2i colorPreviewArea = new Rect2i(controlX, visualY, controlWidth, metrics.colorPreviewHeight());
		visualY += metrics.colorPreviewHeight() + metrics.controlGap();
		Rect2i colorPlaneArea = new Rect2i(controlX, visualY, controlWidth, metrics.colorPlaneHeight());
		Rect2i verticalSliderArea = new Rect2i(
			controlX + controlWidth + metrics.controlGap(),
			visualY,
			metrics.valueSliderWidth(),
			metrics.colorPlaneHeight()
		);
		List<AxisSelectorArea> axisSelectorAreas = createAxisSelectorAreas(
			verticalSliderArea.getX() + verticalSliderArea.getWidth() + metrics.axisSelectorGap(),
			visualY,
			metrics
		);
		int sliderY = visualY + metrics.colorPlaneHeight() + metrics.controlGap();
		List<ColorField> sliderFields = List.of(
			ColorField.RED,
			ColorField.GREEN,
			ColorField.BLUE
		);
		List<SliderArea> sliderAreas = createChannelSliders(controlX, sliderY, controlWidth, sliderFields, metrics);
		int y = sliderY + CHANNEL_SLIDER_COUNT * metrics.channelSliderRowHeight() + metrics.controlGap();
		Rect2i alphaArea = new Rect2i(controlX, y, controlWidth, 0);
		if (hasAlpha()) {
			alphaArea = new Rect2i(controlX, y, controlWidth, metrics.alphaHeight());
			y += metrics.alphaHeight() + metrics.controlGap();
		}
		int doneWidth = Math.min(DONE_BUTTON_WIDTH, contentWidth / 3);
		int hexWidth = Math.max(1, contentWidth - doneWidth - metrics.controlGap());
		FieldArea hexFieldArea = new FieldArea(
			ColorField.HEX,
			new Rect2i(visualX, y, hexWidth, metrics.fieldHeight())
		);
		Rect2i doneButtonArea = new Rect2i(
			visualX + contentWidth - doneWidth, y, doneWidth, metrics.fieldHeight()
		);
		return new PickerLayout(
			colorPreviewArea,
			colorPlaneArea,
			verticalSliderArea,
			axisSelectorAreas,
			sliderAreas,
			metrics.channelSliderLabelWidth(),
			alphaArea,
			hexFieldArea,
			doneButtonArea
		);
	}

	private LayoutMetrics getLayoutMetrics(Rect2i area) {
		if (area.getWidth() >= WIDTH && area.getHeight() >= getHeight()) {
			return STANDARD_LAYOUT;
		}

		LayoutMetrics metrics = COMPACT_LAYOUT;
		int previewHeight = metrics.colorPreviewHeight();
		int heightWithoutPlane = getLayoutHeight(metrics, hasAlpha()) - metrics.colorPlaneHeight();
		int availablePlaneHeight = area.getHeight() - heightWithoutPlane;
		if (availablePlaneHeight < MIN_COMPACT_PLANE_HEIGHT) {
			int previewReduction = Math.min(
				previewHeight - MIN_COMPACT_PREVIEW_HEIGHT,
				MIN_COMPACT_PLANE_HEIGHT - availablePlaneHeight
			);
			previewHeight -= previewReduction;
			availablePlaneHeight += previewReduction;
		}
		int planeHeight = Math.max(1, Math.min(metrics.colorPlaneHeight(), availablePlaneHeight));
		return metrics.withDynamicHeights(previewHeight, planeHeight);
	}

	private static int getLayoutHeight(LayoutMetrics metrics, boolean hasAlpha) {
		int height = metrics.padding() + metrics.colorPreviewHeight() + metrics.controlGap();
		height += metrics.colorPlaneHeight() + metrics.controlGap();
		height += CHANNEL_SLIDER_COUNT * metrics.channelSliderRowHeight() + metrics.controlGap();
		if (hasAlpha) {
			height += metrics.alphaHeight() + metrics.controlGap();
		}
		return height + metrics.fieldHeight() + metrics.padding();
	}

	private static List<AxisSelectorArea> createAxisSelectorAreas(int x, int chartY, LayoutMetrics metrics) {
		ColorAxis[] axes = ColorAxis.values();
		int totalHeight = axes.length * metrics.axisSelectorHeight() +
			(axes.length - 1) * metrics.axisSelectorRowGap();
		int y = chartY + (metrics.colorPlaneHeight() - totalHeight) / 2;
		List<AxisSelectorArea> areas = new ArrayList<>(axes.length);
		for (int i = 0; i < axes.length; i++) {
			int buttonY = y + i * (metrics.axisSelectorHeight() + metrics.axisSelectorRowGap());
			Rect2i area = new Rect2i(x, buttonY, metrics.axisSelectorWidth(), metrics.axisSelectorHeight());
			areas.add(new AxisSelectorArea(axes[i], area));
		}
		return List.copyOf(areas);
	}

	private static List<SliderArea> createChannelSliders(
		int sliderX,
		int y,
		int sliderWidth,
		List<ColorField> fields,
		LayoutMetrics metrics
	) {
		List<SliderArea> sliders = new ArrayList<>(fields.size());
		for (int i = 0; i < fields.size(); i++) {
			int rowY = y + i * metrics.channelSliderRowHeight();
			int sliderY = rowY + (metrics.channelSliderRowHeight() - metrics.channelSliderHeight()) / 2;
			Rect2i sliderArea = new Rect2i(sliderX, sliderY, sliderWidth, metrics.channelSliderHeight());
			sliders.add(new SliderArea(fields.get(i), sliderArea));
		}
		return List.copyOf(sliders);
	}

	private boolean hasAlpha() {
		return format == ConfigColorFormat.ARGB;
	}

	private static float getPosition(double position, int start, int size) {
		if (size <= 1) {
			return 0.0f;
		}
		return (float) ConfigMath.clamp((position - start) / (size - 1), 0.0, 1.0);
	}

	private static boolean contains(Rect2i area, double mouseX, double mouseY) {
		return mouseX >= area.getX() &&
			mouseX < area.getX() + area.getWidth() &&
			mouseY >= area.getY() &&
			mouseY < area.getY() + area.getHeight();
	}

	private static ImmutableRect2i toImmutableRect2i(Rect2i area) {
		return new ImmutableRect2i(area.getX(), area.getY(), area.getWidth(), area.getHeight());
	}

	private enum ControlType {
		PLANE,
		VERTICAL_AXIS,
		SLIDER,
		ALPHA,
		NONE
	}

	private enum ColorAxis {
		HUE("H"),
		SATURATION("S"),
		VALUE("V");

		private final String label;

		ColorAxis(String label) {
			this.label = label;
		}
	}

	private enum ColorField {
		RED("R"),
		GREEN("G"),
		BLUE("B"),
		ALPHA("A"),
		HEX("");

		private final String label;

		ColorField(String label) {
			this.label = label;
		}
	}

	private record LayoutMetrics(
		int padding,
		int controlGap,
		int colorPreviewHeight,
		int colorPlaneHeight,
		int valueSliderWidth,
		int axisSelectorGap,
		int axisSelectorWidth,
		int axisSelectorHeight,
		int axisSelectorRowGap,
		int channelSliderRowHeight,
		int channelSliderHeight,
		int channelSliderLabelWidth,
		int channelSliderValueWidth,
		int alphaHeight,
		int fieldHeight
	) {
		LayoutMetrics withDynamicHeights(int previewHeight, int planeHeight) {
			return new LayoutMetrics(
				padding,
				controlGap,
				previewHeight,
				planeHeight,
				valueSliderWidth,
				axisSelectorGap,
				axisSelectorWidth,
				axisSelectorHeight,
				axisSelectorRowGap,
				channelSliderRowHeight,
				channelSliderHeight,
				channelSliderLabelWidth,
				channelSliderValueWidth,
				alphaHeight,
				fieldHeight
			);
		}
	}

	private record PickerLayout(
		Rect2i colorPreviewArea,
		Rect2i colorPlaneArea,
		Rect2i verticalSliderArea,
		List<AxisSelectorArea> axisSelectorAreas,
		List<SliderArea> sliderAreas,
		int channelSliderLabelWidth,
		Rect2i alphaArea,
		FieldArea hexFieldArea,
		Rect2i doneButtonArea
	) {
		ColorControl getControl(double mouseX, double mouseY) {
			if (colorPlaneArea.getWidth() > 0 && contains(colorPlaneArea, mouseX, mouseY)) {
				return ColorControl.PLANE;
			}
			if (contains(verticalSliderArea, mouseX, mouseY)) {
				return ColorControl.VERTICAL_AXIS;
			}
			for (SliderArea sliderArea : sliderAreas) {
				if (contains(sliderArea.area(), mouseX, mouseY)) {
					return ColorControl.slider(sliderArea.field());
				}
			}
			if (alphaArea.getHeight() > 0 && contains(alphaArea, mouseX, mouseY)) {
				return ColorControl.ALPHA;
			}
			return ColorControl.NONE;
		}

		@Nullable
		SliderArea getSlider(ColorField field) {
			for (SliderArea sliderArea : sliderAreas) {
				if (sliderArea.field() == field) {
					return sliderArea;
				}
			}
			return null;
		}

		@Nullable
		ColorField getField(double mouseX, double mouseY) {
			if (contains(hexFieldArea.area(), mouseX, mouseY)) {
				return hexFieldArea.field();
			}
			return null;
		}

		@Nullable
		ColorAxis getAxisSelector(double mouseX, double mouseY) {
			for (AxisSelectorArea selectorArea : axisSelectorAreas) {
				if (contains(selectorArea.area(), mouseX, mouseY)) {
					return selectorArea.axis();
				}
			}
			return null;
		}
	}

	private record AxisSelectorArea(ColorAxis axis, Rect2i area) {
	}

	private record FieldArea(ColorField field, Rect2i area) {
	}

	private record SliderArea(ColorField field, Rect2i area) {
		float getPosition(double mouseX, double mouseY) {
			return ColorPickerPopup.getPosition(mouseX, area.getX(), area.getWidth());
		}
	}

	private record ColorControl(ControlType type, @Nullable ColorField field) {
		private static final ColorControl PLANE = new ColorControl(ControlType.PLANE, null);
		private static final ColorControl VERTICAL_AXIS = new ColorControl(ControlType.VERTICAL_AXIS, null);
		private static final ColorControl ALPHA = new ColorControl(ControlType.ALPHA, null);
		private static final ColorControl NONE = new ColorControl(ControlType.NONE, null);

		private static ColorControl slider(ColorField field) {
			return new ColorControl(ControlType.SLIDER, field);
		}
	}

	private record ParsedEdit(int value, int alpha) {
	}
}
