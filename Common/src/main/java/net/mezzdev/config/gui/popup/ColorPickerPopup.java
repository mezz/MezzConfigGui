package net.mezzdev.config.gui.popup;

import net.mezzdev.config.api.value.ConfigColorFormat;
import net.mezzdev.config.api.value.PackedColor;
import net.mezzdev.config.gui.api.IConfigValuePopup;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.info.ColorSwatch;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Standard visual color picker with an HSV chart, HSV and RGB sliders, and hexadecimal input.
 */
public final class ColorPickerPopup implements IConfigValuePopup<PackedColor> {
	private static final int WIDTH = 256;
	private static final int PADDING = 7;
	private static final int CONTROL_GAP = 5;
	private static final int COLOR_PREVIEW_HEIGHT = 40;
	private static final int COLOR_PLANE_HEIGHT = 86;
	private static final int PLANE_VERTICAL_LABEL_WIDTH = 18;
	private static final int PLANE_HORIZONTAL_LABEL_HEIGHT = 10;
	private static final int CHANNEL_SLIDER_COUNT = 6;
	private static final int CHANNEL_SLIDER_ROW_HEIGHT = 14;
	private static final int CHANNEL_SLIDER_HEIGHT = 10;
	private static final int CHANNEL_SLIDER_LABEL_WIDTH = 14;
	private static final int CHANNEL_SLIDER_VALUE_WIDTH = 30;
	private static final int ALPHA_HEIGHT = 11;
	private static final int FIELD_HEIGHT = 18;
	private static final int FIELD_TEXT_PADDING = 2;
	private static final int MAX_EDIT_TEXT_LENGTH = 12;
	private static final float PRECISE_SLIDER_SCALE = 0.1f;
	private static final int BACKGROUND_COLOR = 0xF0101218;
	private static final int BORDER_DARK_COLOR = 0xFF050609;
	private static final int BORDER_LIGHT_COLOR = 0x667F8A9A;
	private static final int MARKER_DARK_COLOR = 0xFF000000;
	private static final int MARKER_LIGHT_COLOR = 0xFFFFFFFF;
	private static final int FIELD_BACKGROUND_COLOR = 0xFF171A20;
	private static final int FIELD_FOCUSED_COLOR = 0xFF1F2C3A;
	private static final int FIELD_BORDER_COLOR = 0x555E6877;
	private static final int FIELD_FOCUSED_BORDER_COLOR = 0xFF7DB6F2;
	private static final int FIELD_INVALID_BORDER_COLOR = 0xFFFF7070;

	private final ConfigColorFormat format;
	private final ColorPickerModel model;
	private ColorControl activeControl = ColorControl.NONE;
	private ColorControl precisionControl = ColorControl.NONE;
	private float precisionPointerAnchor;
	private float precisionValueAnchor;
	@Nullable
	private ColorField focusedField;
	private String editText = "";
	private boolean selectAll;

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
		int height = PADDING + COLOR_PREVIEW_HEIGHT + CONTROL_GAP;
		height += COLOR_PLANE_HEIGHT + PLANE_HORIZONTAL_LABEL_HEIGHT + CONTROL_GAP;
		height += CHANNEL_SLIDER_COUNT * CHANNEL_SLIDER_ROW_HEIGHT + CONTROL_GAP;
		if (hasAlpha()) {
			height += ALPHA_HEIGHT + CONTROL_GAP;
		}
		return height + FIELD_HEIGHT + PADDING;
	}

	@Override
	public Optional<PackedColor> getHoveredValue(Rect2i area, double mouseX, double mouseY) {
		return Optional.empty();
	}

	@Override
	public Optional<PackedColor> getClickedValue(Rect2i area, double mouseX, double mouseY, int button) {
		if (button != 0) {
			return Optional.empty();
		}
		PickerLayout layout = createLayout(area);
		@Nullable
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
	public Optional<PackedColor> getDraggedValue(Rect2i area, double mouseX, double mouseY, int button) {
		if (button != 0) {
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
		if (Screen.isSelectAll(keyCode)) {
			selectAll = true;
			return true;
		}
		if (Screen.isPaste(keyCode)) {
			appendText(Minecraft.getInstance().keyboardHandler.getClipboard());
			applyEditText(valueConsumer);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_TAB) {
			focusField(ColorField.HEX);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			applyEditText(valueConsumer);
			selectAll = true;
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			clearFocus();
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
			removeLastCharacter();
			applyEditText(valueConsumer);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_DELETE) {
			editText = "";
			selectAll = false;
			return true;
		}
		return true;
	}

	@Override
	public boolean closesAfterValueSelected() {
		return false;
	}

	@Override
	public void draw(GuiGraphics guiGraphics, Rect2i area, double mouseX, double mouseY) {
		PickerLayout layout = createLayout(area);
		drawBackground(guiGraphics, area);
		ColorSwatch.draw(guiGraphics, layout.colorPreviewArea(), model.getPackedColor());
		drawColorControls(guiGraphics, layout);
		if (hasAlpha()) {
			drawAlpha(guiGraphics, layout.alphaArea());
		}
		drawFields(guiGraphics, layout);
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
		model.setSaturationAndValue(x, y);
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
		@Nullable
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
			case HUE -> model.setHue(position);
			case SATURATION -> model.setSaturationAndValue(position, model.getValue());
			case VALUE -> model.setSaturationAndValue(model.getSaturation(), position);
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
		return Math.clamp(valueAnchor + (pointerPosition - pointerAnchor) * PRECISE_SLIDER_SCALE, 0.0f, 1.0f);
	}

	private static boolean isShiftDown() {
		return Minecraft.getInstance() != null && Screen.hasShiftDown();
	}

	private float getControlPosition(ColorControl control) {
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

	private void drawColorControls(GuiGraphics guiGraphics, PickerLayout layout) {
		drawHsvPlane(guiGraphics, layout.colorPlaneArea());
		drawPlaneAxisLabels(guiGraphics, layout);
		drawSliders(guiGraphics, layout.sliderAreas());
	}

	private void drawHsvPlane(GuiGraphics guiGraphics, Rect2i area) {
		for (int xOffset = 0; xOffset < area.getWidth(); xOffset++) {
			float saturation = getPosition(xOffset, 0, area.getWidth());
			int topColor = ColorPickerModel.hsvToArgb(model.getHue(), saturation, 1.0f);
			guiGraphics.fillGradient(
				area.getX() + xOffset,
				area.getY(),
				area.getX() + xOffset + 1,
				area.getY() + area.getHeight(),
				topColor,
				0xFF000000
			);
		}
		drawBorder(guiGraphics, area, FIELD_BORDER_COLOR);
		int markerX = area.getX() + Math.round(model.getSaturation() * (area.getWidth() - 1));
		int markerY = area.getY() + Math.round((1.0f - model.getValue()) * (area.getHeight() - 1));
		drawPointMarker(guiGraphics, markerX, markerY);
	}

	private void drawSliders(GuiGraphics guiGraphics, List<SliderArea> sliders) {
		for (SliderArea slider : sliders) {
			drawSlider(guiGraphics, slider);
		}
	}

	private void drawSlider(GuiGraphics guiGraphics, SliderArea slider) {
		Rect2i area = slider.area();
		int length = area.getWidth();
		for (int offset = 0; offset < length; offset++) {
			float position = getPosition(offset, 0, length);
			int color = getSliderColor(slider.field(), position);
			guiGraphics.fill(area.getX() + offset, area.getY(), area.getX() + offset + 1, area.getY() + area.getHeight(), color);
		}
		drawBorder(guiGraphics, area, FIELD_BORDER_COLOR);
		float markerPosition = getSliderPosition(slider.field());
		int markerX = area.getX() + Math.round(markerPosition * (area.getWidth() - 1));
		drawVerticalMarker(guiGraphics, area, markerX);
		drawSliderLabel(guiGraphics, slider);
	}

	private int getSliderColor(ColorField field, float position) {
		return switch (field) {
			case HUE -> ColorPickerModel.hsvToArgb(position, 1.0f, 1.0f);
			case SATURATION -> ColorPickerModel.hsvToArgb(model.getHue(), position, 1.0f);
			case VALUE -> ColorPickerModel.hsvToArgb(0.0f, 0.0f, position);
			case RED -> 0xFF000000 | Math.round(position * 255.0f) << 16;
			case GREEN -> 0xFF000000 | Math.round(position * 255.0f) << 8;
			case BLUE -> 0xFF000000 | Math.round(position * 255.0f);
			default -> throw new IllegalArgumentException("Unsupported color slider field: " + field);
		};
	}

	private float getSliderPosition(ColorField field) {
		return switch (field) {
			case HUE -> model.getHue();
			case SATURATION -> model.getSaturation();
			case VALUE -> model.getValue();
			case RED -> model.getRgb().red() / 255.0f;
			case GREEN -> model.getRgb().green() / 255.0f;
			case BLUE -> model.getRgb().blue() / 255.0f;
			default -> throw new IllegalArgumentException("Unsupported color slider field: " + field);
		};
	}

	private void drawSliderLabel(GuiGraphics guiGraphics, SliderArea slider) {
		Font font = Minecraft.getInstance().font;
		Rect2i area = slider.area();
		int textY = area.getY() + (area.getHeight() - font.lineHeight) / 2;
		String label = slider.field().label;
		int labelX = area.getX() - CHANNEL_SLIDER_LABEL_WIDTH +
			(CHANNEL_SLIDER_LABEL_WIDTH - font.width(label)) / 2;
		guiGraphics.drawString(font, label, labelX, textY, MARKER_LIGHT_COLOR, false);
		String value = getFieldText(slider.field());
		int valueX = area.getX() + area.getWidth() + 3;
		guiGraphics.drawString(font, value, valueX, textY, MARKER_LIGHT_COLOR, false);
	}

	private void drawPlaneAxisLabels(GuiGraphics guiGraphics, PickerLayout layout) {
		Font font = Minecraft.getInstance().font;
		String vertical = "V ↑";
		Rect2i verticalArea = layout.verticalAxisLabelArea();
		int verticalX = verticalArea.getX() + (verticalArea.getWidth() - font.width(vertical)) / 2;
		int verticalY = verticalArea.getY() + (verticalArea.getHeight() - font.lineHeight) / 2;
		guiGraphics.drawString(font, vertical, verticalX, verticalY, MARKER_LIGHT_COLOR, false);
		String horizontal = "S →";
		Rect2i horizontalArea = layout.horizontalAxisLabelArea();
		int horizontalX = horizontalArea.getX() + (horizontalArea.getWidth() - font.width(horizontal)) / 2;
		int horizontalY = horizontalArea.getY() + (horizontalArea.getHeight() - font.lineHeight) / 2;
		guiGraphics.drawString(font, horizontal, horizontalX, horizontalY, MARKER_LIGHT_COLOR, false);
	}

	private void drawAlpha(GuiGraphics guiGraphics, Rect2i area) {
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
		drawBorder(guiGraphics, area, FIELD_BORDER_COLOR);
		Font font = Minecraft.getInstance().font;
		int textY = area.getY() + (area.getHeight() - font.lineHeight) / 2;
		int labelX = area.getX() - CHANNEL_SLIDER_LABEL_WIDTH +
			(CHANNEL_SLIDER_LABEL_WIDTH - font.width(ColorField.ALPHA.label)) / 2;
		guiGraphics.drawString(font, ColorField.ALPHA.label, labelX, textY, MARKER_LIGHT_COLOR, false);
		guiGraphics.drawString(
			font,
			getFieldText(ColorField.ALPHA),
			area.getX() + area.getWidth() + 3,
			textY,
			MARKER_LIGHT_COLOR,
			false
		);
	}

	private void drawFields(GuiGraphics guiGraphics, PickerLayout layout) {
		Font font = Minecraft.getInstance().font;
		drawField(guiGraphics, font, layout.hexFieldArea());
	}

	private void drawField(GuiGraphics guiGraphics, Font font, FieldArea fieldArea) {
		ColorField field = fieldArea.field();
		boolean focused = field == focusedField;
		boolean valid = !focused || isEditTextValid();
		int backgroundColor = FIELD_BACKGROUND_COLOR;
		if (focused) {
			backgroundColor = FIELD_FOCUSED_COLOR;
		}
		int borderColor = FIELD_BORDER_COLOR;
		if (focused) {
			borderColor = FIELD_INVALID_BORDER_COLOR;
			if (valid) {
				borderColor = FIELD_FOCUSED_BORDER_COLOR;
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
		int textColor = FIELD_INVALID_BORDER_COLOR;
		if (valid) {
			textColor = ConfigEntryWidget.TEXT_COLOR;
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
			case HUE -> Integer.toString(Math.round(model.getHue() * 360.0f));
			case SATURATION -> Integer.toString(Math.round(model.getSaturation() * 100.0f));
			case VALUE -> Integer.toString(Math.round(model.getValue() * 100.0f));
			case RED -> Integer.toString(model.getRgb().red());
			case GREEN -> Integer.toString(model.getRgb().green());
			case BLUE -> Integer.toString(model.getRgb().blue());
			case ALPHA -> Integer.toString(model.getAlphaChannel());
			case HEX -> ColorSwatch.formatHex(model.getPackedColor());
		};
	}

	private static void drawBackground(GuiGraphics guiGraphics, Rect2i area) {
		int right = area.getX() + area.getWidth();
		int bottom = area.getY() + area.getHeight();
		guiGraphics.fill(area.getX(), area.getY(), right, bottom, BACKGROUND_COLOR);
		guiGraphics.fill(area.getX(), area.getY(), right, area.getY() + 1, BORDER_DARK_COLOR);
		guiGraphics.fill(area.getX(), area.getY(), area.getX() + 1, bottom, BORDER_DARK_COLOR);
		guiGraphics.fill(right - 1, area.getY(), right, bottom, BORDER_LIGHT_COLOR);
		guiGraphics.fill(area.getX(), bottom - 1, right, bottom, BORDER_LIGHT_COLOR);
	}

	private static void fillWithBorder(GuiGraphics guiGraphics, Rect2i area, int fillColor, int borderColor) {
		int right = area.getX() + area.getWidth();
		int bottom = area.getY() + area.getHeight();
		guiGraphics.fill(area.getX(), area.getY(), right, bottom, borderColor);
		guiGraphics.fill(area.getX() + 1, area.getY() + 1, right - 1, bottom - 1, fillColor);
	}

	private static void drawBorder(GuiGraphics guiGraphics, Rect2i area, int color) {
		int right = area.getX() + area.getWidth();
		int bottom = area.getY() + area.getHeight();
		guiGraphics.fill(area.getX(), area.getY(), right, area.getY() + 1, color);
		guiGraphics.fill(area.getX(), area.getY(), area.getX() + 1, bottom, color);
		guiGraphics.fill(right - 1, area.getY(), right, bottom, color);
		guiGraphics.fill(area.getX(), bottom - 1, right, bottom, color);
	}

	private static void drawPointMarker(GuiGraphics guiGraphics, int x, int y) {
		guiGraphics.fill(x - 3, y - 3, x + 4, y + 4, MARKER_DARK_COLOR);
		guiGraphics.fill(x - 2, y - 2, x + 3, y + 3, MARKER_LIGHT_COLOR);
		guiGraphics.fill(x - 1, y - 1, x + 2, y + 2, MARKER_DARK_COLOR);
	}

	private static void drawVerticalMarker(GuiGraphics guiGraphics, Rect2i area, int x) {
		guiGraphics.fill(x - 1, area.getY() - 1, x + 2, area.getY() + area.getHeight() + 1, MARKER_DARK_COLOR);
		guiGraphics.fill(x, area.getY(), x + 1, area.getY() + area.getHeight(), MARKER_LIGHT_COLOR);
	}

	private PickerLayout createLayout(Rect2i area) {
		int contentWidth = Math.max(1, area.getWidth() - PADDING * 2);
		int visualX = area.getX() + PADDING;
		int visualY = area.getY() + PADDING;
		Rect2i colorPreviewArea = new Rect2i(visualX, visualY, contentWidth, COLOR_PREVIEW_HEIGHT);
		visualY += COLOR_PREVIEW_HEIGHT + CONTROL_GAP;
		int planeX = visualX + PLANE_VERTICAL_LABEL_WIDTH;
		int planeWidth = Math.max(1, contentWidth - PLANE_VERTICAL_LABEL_WIDTH);
		Rect2i colorPlaneArea = new Rect2i(planeX, visualY, planeWidth, COLOR_PLANE_HEIGHT);
		Rect2i verticalAxisLabelArea = new Rect2i(visualX, visualY, PLANE_VERTICAL_LABEL_WIDTH, COLOR_PLANE_HEIGHT);
		Rect2i horizontalAxisLabelArea = new Rect2i(
			planeX,
			visualY + COLOR_PLANE_HEIGHT,
			planeWidth,
			PLANE_HORIZONTAL_LABEL_HEIGHT
		);
		int sliderY = visualY + COLOR_PLANE_HEIGHT + PLANE_HORIZONTAL_LABEL_HEIGHT + CONTROL_GAP;
		List<ColorField> sliderFields = List.of(
			ColorField.HUE,
			ColorField.SATURATION,
			ColorField.VALUE,
			ColorField.RED,
			ColorField.GREEN,
			ColorField.BLUE
		);
		List<SliderArea> sliderAreas = createChannelSliders(visualX, sliderY, contentWidth, sliderFields);
		int y = sliderY + CHANNEL_SLIDER_COUNT * CHANNEL_SLIDER_ROW_HEIGHT + CONTROL_GAP;
		int sliderX = area.getX() + PADDING + CHANNEL_SLIDER_LABEL_WIDTH;
		int sliderWidth = Math.max(1, contentWidth - CHANNEL_SLIDER_LABEL_WIDTH - CHANNEL_SLIDER_VALUE_WIDTH);
		Rect2i alphaArea = new Rect2i(sliderX, y, sliderWidth, 0);
		if (hasAlpha()) {
			alphaArea = new Rect2i(sliderX, y, sliderWidth, ALPHA_HEIGHT);
			y += ALPHA_HEIGHT + CONTROL_GAP;
		}
		FieldArea hexFieldArea = new FieldArea(
			ColorField.HEX,
			new Rect2i(area.getX() + PADDING, y, contentWidth, FIELD_HEIGHT)
		);
		return new PickerLayout(
			colorPreviewArea,
			colorPlaneArea,
			verticalAxisLabelArea,
			horizontalAxisLabelArea,
			sliderAreas,
			alphaArea,
			hexFieldArea
		);
	}

	private static List<SliderArea> createChannelSliders(int x, int y, int width, List<ColorField> fields) {
		List<SliderArea> sliders = new ArrayList<>(fields.size());
		for (int i = 0; i < fields.size(); i++) {
			int rowY = y + i * CHANNEL_SLIDER_ROW_HEIGHT;
			int sliderY = rowY + (CHANNEL_SLIDER_ROW_HEIGHT - CHANNEL_SLIDER_HEIGHT) / 2;
			int sliderX = x + CHANNEL_SLIDER_LABEL_WIDTH;
			int sliderWidth = Math.max(1, width - CHANNEL_SLIDER_LABEL_WIDTH - CHANNEL_SLIDER_VALUE_WIDTH);
			Rect2i sliderArea = new Rect2i(sliderX, sliderY, sliderWidth, CHANNEL_SLIDER_HEIGHT);
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
		return (float) Math.clamp((position - start) / (size - 1), 0.0, 1.0);
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
		SLIDER,
		ALPHA,
		NONE
	}

	private enum ColorField {
		HUE("H"),
		SATURATION("S"),
		VALUE("V"),
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

	private record PickerLayout(
		Rect2i colorPreviewArea,
		Rect2i colorPlaneArea,
		Rect2i verticalAxisLabelArea,
		Rect2i horizontalAxisLabelArea,
		List<SliderArea> sliderAreas,
		Rect2i alphaArea,
		FieldArea hexFieldArea
	) {
		ColorControl getControl(double mouseX, double mouseY) {
			if (colorPlaneArea.getWidth() > 0 && contains(colorPlaneArea, mouseX, mouseY)) {
				return ColorControl.PLANE;
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
		private static final ColorControl ALPHA = new ColorControl(ControlType.ALPHA, null);
		private static final ColorControl NONE = new ColorControl(ControlType.NONE, null);

		private static ColorControl slider(ColorField field) {
			return new ColorControl(ControlType.SLIDER, field);
		}
	}

	private record ParsedEdit(int value, int alpha) {
	}
}
