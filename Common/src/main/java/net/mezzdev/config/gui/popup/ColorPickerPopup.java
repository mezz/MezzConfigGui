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
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Standard visual color picker with editable HSV, RGB, CMYK, Lab, and hexadecimal representations.
 */
public final class ColorPickerPopup implements IConfigValuePopup<PackedColor> {
	private static final int WIDTH = 176;
	private static final int PADDING = 7;
	private static final int CONTROL_GAP = 6;
	private static final int SATURATION_VALUE_HEIGHT = 88;
	private static final int SLIDER_SIZE = 12;
	private static final int ALPHA_HEIGHT = 11;
	private static final int MODE_HEIGHT = 16;
	private static final int FIELD_TOP_GAP = 3;
	private static final int FIELD_HEIGHT = 18;
	private static final int FIELD_TEXT_PADDING = 2;
	private static final int MAX_EDIT_TEXT_LENGTH = 12;
	private static final int BACKGROUND_COLOR = 0xF0101218;
	private static final int BORDER_DARK_COLOR = 0xFF050609;
	private static final int BORDER_LIGHT_COLOR = 0x667F8A9A;
	private static final int MARKER_DARK_COLOR = 0xFF000000;
	private static final int MARKER_LIGHT_COLOR = 0xFFFFFFFF;
	private static final int MODE_BACKGROUND_COLOR = 0xAA1A1D24;
	private static final int MODE_SELECTED_COLOR = 0xFF3A536E;
	private static final int MODE_HOVER_COLOR = 0xFF313A46;
	private static final int FIELD_BACKGROUND_COLOR = 0xFF171A20;
	private static final int FIELD_FOCUSED_COLOR = 0xFF1F2C3A;
	private static final int FIELD_BORDER_COLOR = 0x555E6877;
	private static final int FIELD_FOCUSED_BORDER_COLOR = 0xFF7DB6F2;
	private static final int FIELD_INVALID_BORDER_COLOR = 0xFFFF7070;

	private final ConfigColorFormat format;
	private final ColorPickerModel model;
	private Control activeControl = Control.NONE;
	private ColorSpace colorSpace = ColorSpace.HSV;
	@Nullable
	private ColorField focusedField;
	private String editText = "";
	private boolean selectAll;

	public ColorPickerPopup(PackedColor color) {
		this.format = color.format();
		this.model = new ColorPickerModel(color);
	}

	@Override
	public int getWidth() {
		return WIDTH;
	}

	@Override
	public int getHeight() {
		int height = PADDING + SATURATION_VALUE_HEIGHT + CONTROL_GAP;
		if (hasAlpha()) {
			height += ALPHA_HEIGHT + CONTROL_GAP;
		}
		return height + MODE_HEIGHT + FIELD_TOP_GAP + FIELD_HEIGHT + PADDING;
	}

	@Override
	public Optional<PackedColor> getHoveredValue(Rect2i area, double mouseX, double mouseY) {
		PickerLayout layout = createLayout(area);
		Control control = layout.getControl(mouseX, mouseY);
		if (control == Control.NONE) {
			return Optional.empty();
		}
		ColorPickerModel candidate = new ColorPickerModel(model.getPackedColor());
		updateModel(candidate, control, layout, mouseX, mouseY);
		return Optional.of(candidate.getPackedColor());
	}

	@Override
	public Optional<PackedColor> getClickedValue(Rect2i area, double mouseX, double mouseY, int button) {
		if (button != 0) {
			return Optional.empty();
		}
		PickerLayout layout = createLayout(area);
		@Nullable
		ColorSpace clickedColorSpace = layout.getColorSpace(mouseX, mouseY);
		if (clickedColorSpace != null) {
			colorSpace = clickedColorSpace;
			activeControl = Control.NONE;
			clearFocus();
			return Optional.empty();
		}
		@Nullable
		ColorField clickedField = layout.getField(mouseX, mouseY);
		if (clickedField != null) {
			activeControl = Control.NONE;
			focusField(clickedField);
			return Optional.empty();
		}

		clearFocus();
		if (activeControl == Control.NONE) {
			activeControl = layout.getControl(mouseX, mouseY);
			return updateValue(activeControl, layout, mouseX, mouseY);
		}
		Control releasedControl = activeControl;
		activeControl = Control.NONE;
		return updateValue(releasedControl, layout, mouseX, mouseY);
	}

	@Override
	public Optional<PackedColor> getDraggedValue(Rect2i area, double mouseX, double mouseY, int button) {
		if (button != 0) {
			return Optional.empty();
		}
		PickerLayout layout = createLayout(area);
		Control control = activeControl;
		if (control == Control.NONE) {
			control = layout.getControl(mouseX, mouseY);
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
			int direction = 1;
			if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
				direction = -1;
			}
			focusNextField(direction);
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
		drawSaturationValue(guiGraphics, layout.saturationValueArea());
		drawHue(guiGraphics, layout.hueArea());
		if (hasAlpha()) {
			drawAlpha(guiGraphics, layout.alphaArea());
		}
		drawColorSpaceModes(guiGraphics, layout, mouseX, mouseY);
		drawFields(guiGraphics, layout);
	}

	private Optional<PackedColor> updateValue(Control control, PickerLayout layout, double mouseX, double mouseY) {
		if (control == Control.NONE) {
			return Optional.empty();
		}
		updateModel(model, control, layout, mouseX, mouseY);
		return Optional.of(model.getPackedColor());
	}

	private static void updateModel(
		ColorPickerModel model,
		Control control,
		PickerLayout layout,
		double mouseX,
		double mouseY
	) {
		switch (control) {
			case SATURATION_VALUE -> {
				Rect2i area = layout.saturationValueArea();
				float saturation = getPosition(mouseX, area.getX(), area.getWidth());
				float value = 1.0f - getPosition(mouseY, area.getY(), area.getHeight());
				model.setSaturationAndValue(saturation, value);
			}
			case HUE -> {
				Rect2i area = layout.hueArea();
				model.setHue(getPosition(mouseY, area.getY(), area.getHeight()));
			}
			case ALPHA -> {
				Rect2i area = layout.alphaArea();
				model.setAlpha(getPosition(mouseX, area.getX(), area.getWidth()));
			}
			case NONE -> {}
		}
	}

	private void drawSaturationValue(GuiGraphics guiGraphics, Rect2i area) {
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
		int markerX = area.getX() + Math.round(model.getSaturation() * (area.getWidth() - 1));
		int markerY = area.getY() + Math.round((1.0f - model.getValue()) * (area.getHeight() - 1));
		drawPointMarker(guiGraphics, markerX, markerY);
	}

	private void drawHue(GuiGraphics guiGraphics, Rect2i area) {
		for (int yOffset = 0; yOffset < area.getHeight(); yOffset++) {
			float hue = getPosition(yOffset, 0, area.getHeight());
			int color = ColorPickerModel.hsvToArgb(hue, 1.0f, 1.0f);
			guiGraphics.fill(area.getX(), area.getY() + yOffset, area.getX() + area.getWidth(), area.getY() + yOffset + 1, color);
		}
		int markerY = area.getY() + Math.round(model.getHue() * (area.getHeight() - 1));
		drawHorizontalMarker(guiGraphics, area, markerY);
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
	}

	private void drawColorSpaceModes(GuiGraphics guiGraphics, PickerLayout layout, double mouseX, double mouseY) {
		Font font = Minecraft.getInstance().font;
		for (ModeArea modeArea : layout.modeAreas()) {
			boolean selected = modeArea.colorSpace() == colorSpace;
			boolean hovered = contains(modeArea.area(), mouseX, mouseY);
			int color = MODE_BACKGROUND_COLOR;
			if (selected) {
				color = MODE_SELECTED_COLOR;
			} else if (hovered) {
				color = MODE_HOVER_COLOR;
			}
			fillWithBorder(guiGraphics, modeArea.area(), color, FIELD_BORDER_COLOR);
			ConfigEntryWidget.drawCenteredButtonText(
				guiGraphics,
				font,
				modeArea.colorSpace().label,
				toImmutableRect2i(modeArea.area()),
				ConfigEntryWidget.TEXT_COLOR
			);
		}
	}

	private void drawFields(GuiGraphics guiGraphics, PickerLayout layout) {
		Font font = Minecraft.getInstance().font;
		for (FieldArea fieldArea : layout.fieldAreas()) {
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
	}

	private void focusField(ColorField field) {
		focusedField = field;
		editText = getFieldText(field);
		selectAll = true;
	}

	private void focusNextField(int direction) {
		List<ColorField> fields = getVisibleFields();
		if (fields.isEmpty()) {
			clearFocus();
			return;
		}
		int index = fields.indexOf(focusedField);
		if (index < 0) {
			index = 0;
		} else {
			index = Math.floorMod(index + direction, fields.size());
		}
		focusField(fields.get(index));
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
		ColorField field = focusedField;
		if (field == null) {
			return Optional.empty();
		}
		if (field == ColorField.HEX) {
			return parseHexEdit(editText);
		}
		try {
			int value = Integer.parseInt(editText.trim());
			if (value < field.min || value > field.max) {
				return Optional.empty();
			}
			return Optional.of(new ParsedEdit(field, value, -1));
		} catch (NumberFormatException ignored) {
			return Optional.empty();
		}
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
			return Optional.of(new ParsedEdit(ColorField.HEX, rgb, alpha));
		} catch (NumberFormatException ignored) {
			return Optional.empty();
		}
	}

	private void applyParsedEdit(ParsedEdit edit) {
		int value = edit.value();
		switch (edit.field()) {
			case HUE -> model.setHue(value / 360.0f);
			case SATURATION -> model.setSaturationAndValue(value / 100.0f, model.getValue());
			case VALUE -> model.setSaturationAndValue(model.getSaturation(), value / 100.0f);
			case RED, GREEN, BLUE -> applyRgbEdit(edit.field(), value);
			case CYAN, MAGENTA, YELLOW, BLACK -> applyCmykEdit(edit.field(), value);
			case LAB_LIGHTNESS, LAB_A, LAB_B -> applyLabEdit(edit.field(), value);
			case ALPHA -> model.setAlphaChannel(value);
			case HEX -> {
				model.setRgb((value >>> 16) & 0xFF, (value >>> 8) & 0xFF, value & 0xFF);
				if (edit.alpha() >= 0) {
					model.setAlphaChannel(edit.alpha());
				}
			}
		}
	}

	private void applyRgbEdit(ColorField field, int value) {
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

	private void applyCmykEdit(ColorField field, int value) {
		ColorPickerModel.Cmyk cmyk = model.getCmyk();
		double cyan = cmyk.cyan();
		double magenta = cmyk.magenta();
		double yellow = cmyk.yellow();
		double black = cmyk.black();
		double normalizedValue = value / 100.0;
		switch (field) {
			case CYAN -> cyan = normalizedValue;
			case MAGENTA -> magenta = normalizedValue;
			case YELLOW -> yellow = normalizedValue;
			case BLACK -> black = normalizedValue;
			default -> throw new IllegalArgumentException("Not a CMYK field: " + field);
		}
		model.setCmyk(cyan, magenta, yellow, black);
	}

	private void applyLabEdit(ColorField field, int value) {
		ColorPickerModel.Lab lab = model.getLab();
		double lightness = lab.lightness();
		double a = lab.a();
		double b = lab.b();
		switch (field) {
			case LAB_LIGHTNESS -> lightness = value;
			case LAB_A -> a = value;
			case LAB_B -> b = value;
			default -> throw new IllegalArgumentException("Not a Lab field: " + field);
		}
		model.setLab(lightness, a, b);
	}

	private String getFieldText(ColorField field) {
		return switch (field) {
			case HUE -> Integer.toString(Math.round(model.getHue() * 360.0f));
			case SATURATION -> Integer.toString(Math.round(model.getSaturation() * 100.0f));
			case VALUE -> Integer.toString(Math.round(model.getValue() * 100.0f));
			case RED -> Integer.toString(model.getRgb().red());
			case GREEN -> Integer.toString(model.getRgb().green());
			case BLUE -> Integer.toString(model.getRgb().blue());
			case CYAN -> formatPercent(model.getCmyk().cyan());
			case MAGENTA -> formatPercent(model.getCmyk().magenta());
			case YELLOW -> formatPercent(model.getCmyk().yellow());
			case BLACK -> formatPercent(model.getCmyk().black());
			case LAB_LIGHTNESS -> Long.toString(Math.round(model.getLab().lightness()));
			case LAB_A -> Long.toString(Math.round(model.getLab().a()));
			case LAB_B -> Long.toString(Math.round(model.getLab().b()));
			case ALPHA -> Integer.toString(model.getAlphaChannel());
			case HEX -> formatHex(model.getPackedColor());
		};
	}

	private static String formatPercent(double value) {
		return Long.toString(Math.round(value * 100.0));
	}

	private static String formatHex(PackedColor color) {
		if (color.format() == ConfigColorFormat.RGB) {
			return "#%06X".formatted(color.packedValue() & 0xFFFFFF);
		}
		return "#%08X".formatted(color.packedValue()).toUpperCase(Locale.ROOT);
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

	private static void drawPointMarker(GuiGraphics guiGraphics, int x, int y) {
		guiGraphics.fill(x - 3, y - 3, x + 4, y + 4, MARKER_DARK_COLOR);
		guiGraphics.fill(x - 2, y - 2, x + 3, y + 3, MARKER_LIGHT_COLOR);
		guiGraphics.fill(x - 1, y - 1, x + 2, y + 2, MARKER_DARK_COLOR);
	}

	private static void drawHorizontalMarker(GuiGraphics guiGraphics, Rect2i area, int y) {
		guiGraphics.fill(area.getX() - 1, y - 1, area.getX() + area.getWidth() + 1, y + 2, MARKER_DARK_COLOR);
		guiGraphics.fill(area.getX(), y, area.getX() + area.getWidth(), y + 1, MARKER_LIGHT_COLOR);
	}

	private static void drawVerticalMarker(GuiGraphics guiGraphics, Rect2i area, int x) {
		guiGraphics.fill(x - 1, area.getY() - 1, x + 2, area.getY() + area.getHeight() + 1, MARKER_DARK_COLOR);
		guiGraphics.fill(x, area.getY(), x + 1, area.getY() + area.getHeight(), MARKER_LIGHT_COLOR);
	}

	private PickerLayout createLayout(Rect2i area) {
		int contentWidth = Math.max(1, area.getWidth() - PADDING * 2);
		int saturationValueWidth = Math.max(1, contentWidth - SLIDER_SIZE - CONTROL_GAP);
		Rect2i saturationValueArea = new Rect2i(area.getX() + PADDING, area.getY() + PADDING, saturationValueWidth, SATURATION_VALUE_HEIGHT);
		Rect2i hueArea = new Rect2i(
			saturationValueArea.getX() + saturationValueArea.getWidth() + CONTROL_GAP,
			saturationValueArea.getY(),
			SLIDER_SIZE,
			SATURATION_VALUE_HEIGHT
		);
		int y = saturationValueArea.getY() + saturationValueArea.getHeight() + CONTROL_GAP;
		Rect2i alphaArea = new Rect2i(area.getX() + PADDING, y, contentWidth, 0);
		if (hasAlpha()) {
			alphaArea = new Rect2i(area.getX() + PADDING, y, contentWidth, ALPHA_HEIGHT);
			y += ALPHA_HEIGHT + CONTROL_GAP;
		}
		List<ModeArea> modeAreas = createModeAreas(area.getX() + PADDING, y, contentWidth);
		y += MODE_HEIGHT + FIELD_TOP_GAP;
		List<FieldArea> fieldAreas = createFieldAreas(area.getX() + PADDING, y, contentWidth);
		return new PickerLayout(saturationValueArea, hueArea, alphaArea, modeAreas, fieldAreas);
	}

	private static List<ModeArea> createModeAreas(int x, int y, int width) {
		ColorSpace[] colorSpaces = ColorSpace.values();
		List<ModeArea> areas = new ArrayList<>(colorSpaces.length);
		for (int i = 0; i < colorSpaces.length; i++) {
			int left = x + width * i / colorSpaces.length;
			int right = x + width * (i + 1) / colorSpaces.length;
			areas.add(new ModeArea(colorSpaces[i], new Rect2i(left, y, right - left, MODE_HEIGHT)));
		}
		return areas;
	}

	private List<FieldArea> createFieldAreas(int x, int y, int width) {
		List<ColorField> fields = getVisibleFields();
		List<FieldArea> areas = new ArrayList<>(fields.size());
		for (int i = 0; i < fields.size(); i++) {
			int left = x + width * i / fields.size();
			int right = x + width * (i + 1) / fields.size();
			areas.add(new FieldArea(fields.get(i), new Rect2i(left, y, right - left, FIELD_HEIGHT)));
		}
		return areas;
	}

	private List<ColorField> getVisibleFields() {
		List<ColorField> fields = new ArrayList<>(colorSpace.fields);
		if (hasAlpha() && colorSpace != ColorSpace.HEX) {
			fields.add(ColorField.ALPHA);
		}
		return fields;
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

	private enum Control {
		SATURATION_VALUE,
		HUE,
		ALPHA,
		NONE
	}

	private enum ColorSpace {
		HSV("HSV", List.of(ColorField.HUE, ColorField.SATURATION, ColorField.VALUE)),
		RGB("RGB", List.of(ColorField.RED, ColorField.GREEN, ColorField.BLUE)),
		CMYK("CMYK", List.of(ColorField.CYAN, ColorField.MAGENTA, ColorField.YELLOW, ColorField.BLACK)),
		LAB("Lab", List.of(ColorField.LAB_LIGHTNESS, ColorField.LAB_A, ColorField.LAB_B)),
		HEX("Hex", List.of(ColorField.HEX));

		private final Component label;
		private final List<ColorField> fields;

		ColorSpace(String label, List<ColorField> fields) {
			this.label = Component.literal(label);
			this.fields = fields;
		}
	}

	private enum ColorField {
		HUE("H", 0, 360),
		SATURATION("S", 0, 100),
		VALUE("V", 0, 100),
		RED("R", 0, 255),
		GREEN("G", 0, 255),
		BLUE("B", 0, 255),
		CYAN("C", 0, 100),
		MAGENTA("M", 0, 100),
		YELLOW("Y", 0, 100),
		BLACK("K", 0, 100),
		LAB_LIGHTNESS("L", 0, 100),
		LAB_A("a", -128, 127),
		LAB_B("b", -128, 127),
		ALPHA("A", 0, 255),
		HEX("", 0, 0);

		private final String label;
		private final int min;
		private final int max;

		ColorField(String label, int min, int max) {
			this.label = label;
			this.min = min;
			this.max = max;
		}
	}

	private record PickerLayout(
		Rect2i saturationValueArea,
		Rect2i hueArea,
		Rect2i alphaArea,
		List<ModeArea> modeAreas,
		List<FieldArea> fieldAreas
	) {
		Control getControl(double mouseX, double mouseY) {
			if (contains(saturationValueArea, mouseX, mouseY)) {
				return Control.SATURATION_VALUE;
			}
			if (contains(hueArea, mouseX, mouseY)) {
				return Control.HUE;
			}
			if (alphaArea.getHeight() > 0 && contains(alphaArea, mouseX, mouseY)) {
				return Control.ALPHA;
			}
			return Control.NONE;
		}

		@Nullable
		ColorSpace getColorSpace(double mouseX, double mouseY) {
			for (ModeArea modeArea : modeAreas) {
				if (contains(modeArea.area(), mouseX, mouseY)) {
					return modeArea.colorSpace();
				}
			}
			return null;
		}

		@Nullable
		ColorField getField(double mouseX, double mouseY) {
			for (FieldArea fieldArea : fieldAreas) {
				if (contains(fieldArea.area(), mouseX, mouseY)) {
					return fieldArea.field();
				}
			}
			return null;
		}
	}

	private record ModeArea(ColorSpace colorSpace, Rect2i area) {
	}

	private record FieldArea(ColorField field, Rect2i area) {
	}

	private record ParsedEdit(ColorField field, int value, int alpha) {
	}
}
