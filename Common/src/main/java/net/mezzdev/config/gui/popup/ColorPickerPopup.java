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
import net.minecraft.client.renderer.Rect2i;

import java.util.Optional;

/**
 * Standard hue, saturation, value, and alpha picker for packed integer colors.
 */
public final class ColorPickerPopup implements IConfigValuePopup<PackedColor> {
	private static final int WIDTH = 176;
	private static final int PADDING = 7;
	private static final int CONTROL_GAP = 6;
	private static final int SATURATION_VALUE_HEIGHT = 88;
	private static final int SLIDER_SIZE = 12;
	private static final int ALPHA_HEIGHT = 11;
	private static final int FOOTER_HEIGHT = 18;
	private static final int PREVIEW_SIZE = 18;
	private static final int TEXT_GAP = 5;
	private static final int BACKGROUND_COLOR = 0xF0101218;
	private static final int BORDER_DARK_COLOR = 0xFF050609;
	private static final int BORDER_LIGHT_COLOR = 0x667F8A9A;
	private static final int MARKER_DARK_COLOR = 0xFF000000;
	private static final int MARKER_LIGHT_COLOR = 0xFFFFFFFF;

	private final ConfigColorFormat format;
	private final ColorPickerModel model;
	private Control activeControl = Control.NONE;

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
		int height = PADDING + SATURATION_VALUE_HEIGHT + CONTROL_GAP + FOOTER_HEIGHT + PADDING;
		if (hasAlpha()) {
			height += ALPHA_HEIGHT + CONTROL_GAP;
		}
		return height;
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
		drawFooter(guiGraphics, layout.footerArea());
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

	private void drawFooter(GuiGraphics guiGraphics, Rect2i area) {
		Rect2i previewArea = new Rect2i(area.getX(), area.getY(), PREVIEW_SIZE, PREVIEW_SIZE);
		ColorSwatch.draw(guiGraphics, previewArea, model.getPackedColor());

		Font font = Minecraft.getInstance().font;
		String hex = formatColor(model.getPackedColor());
		ImmutableRect2i textArea = new ImmutableRect2i(
			area.getX() + PREVIEW_SIZE + TEXT_GAP,
			area.getY(),
			Math.max(0, area.getWidth() - PREVIEW_SIZE - TEXT_GAP),
			area.getHeight()
		);
		ConfigEntryWidget.drawFittedText(guiGraphics, font, net.minecraft.network.chat.Component.literal(hex), textArea, ConfigEntryWidget.TEXT_COLOR, false);
	}

	private static String formatColor(PackedColor color) {
		if (color.format() == ConfigColorFormat.RGB) {
			return "0x%06X".formatted(color.packedValue() & 0xFFFFFF);
		}
		return "0x%08X".formatted(color.packedValue());
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
		Rect2i footerArea = new Rect2i(area.getX() + PADDING, y, contentWidth, FOOTER_HEIGHT);
		return new PickerLayout(saturationValueArea, hueArea, alphaArea, footerArea);
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

	private enum Control {
		SATURATION_VALUE,
		HUE,
		ALPHA,
		NONE
	}

	private record PickerLayout(Rect2i saturationValueArea, Rect2i hueArea, Rect2i alphaArea, Rect2i footerArea) {
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

		private static boolean contains(Rect2i area, double mouseX, double mouseY) {
			return mouseX >= area.getX() &&
				mouseX < area.getX() + area.getWidth() &&
				mouseY >= area.getY() &&
				mouseY < area.getY() + area.getHeight();
		}
	}
}
