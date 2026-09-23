package net.mezzdev.config.gui.popup;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.api.value.color.ConfigColorFormat;
import net.mezzdev.config.api.value.color.PackedColor;
import net.mezzdev.config.gui.api.IConfigValuePopup;
import net.mezzdev.config.gui.info.ColorSwatch;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorPickerModelTest {
	@Test
	void preservesArgbColorWhenConvertingThroughHsv() {
		PackedColor color = PackedColor.argb(0x7F33AA55);

		ColorPickerModel model = new ColorPickerModel(color);

		assertEquals(color, model.getPackedColor());
	}

	@Test
	void preservesRgbFormatWithoutAddingAlpha() {
		PackedColor color = PackedColor.rgb(0x4477DD);
		ColorPickerModel model = new ColorPickerModel(color);

		model.setAlpha(0.0f);

		assertEquals(color, model.getPackedColor());
		assertEquals(0xFF4477DD, model.getArgbColor());
	}

	@Test
	void convertsPrimaryHues() {
		assertEquals(0xFFFF0000, ColorPickerModel.hsvToArgb(0.0f, 1.0f, 1.0f));
		assertEquals(0xFF00FF00, ColorPickerModel.hsvToArgb(1.0f / 3.0f, 1.0f, 1.0f));
		assertEquals(0xFF0000FF, ColorPickerModel.hsvToArgb(2.0f / 3.0f, 1.0f, 1.0f));
	}

	@Test
	void alphaPickerUsesTheFullByteRange() {
		ColorPickerModel model = new ColorPickerModel(PackedColor.argb(0xFFFF0000));

		model.setAlpha(0.5f);

		assertEquals(PackedColor.argb(0x80FF0000), model.getPackedColor());
	}

	@Test
	void formatsCompactRgbAndArgbHexValues() {
		assertEquals("#336699", ColorSwatch.formatHex(PackedColor.rgb(0x336699)));
		assertEquals("#80336699", ColorSwatch.formatHex(PackedColor.argb(0x80336699)));
	}

	@Test
	void hueSaturationPlaneUsesHueOnXAndSaturationOnY() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.rgb(0xFF0000));
		Rect2i area = new Rect2i(0, 0, popup.getWidth(), popup.getHeight());

		double greenHueX = 21.0 + 189.0 / 3.0;
		PackedColor green = clickControl(popup, area, greenHueX, 40);
		PackedColor white = clickControl(popup, area, greenHueX, 125);

		assertEquals(PackedColor.rgb(0x00FF00), green);
		assertEquals(PackedColor.rgb(0xFFFFFF), white);
	}

	@Test
	void verticalSliderCanControlHue() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.rgb(0xFF0000));
		Rect2i area = new Rect2i(0, 0, popup.getWidth(), popup.getHeight());

		assertTrue(popup.getClickedValue(area, 240, 63, InputConstants.MOUSE_BUTTON_LEFT).isEmpty());
		double greenHueY = 40.0 + 85.0 / 3.0;
		PackedColor green = clickControl(popup, area, 221, greenHueY);

		assertEquals(PackedColor.rgb(0x00FF00), green);
	}

	@Test
	void verticalSliderCanControlSaturation() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.rgb(0xFF0000));
		Rect2i area = new Rect2i(0, 0, popup.getWidth(), popup.getHeight());

		assertTrue(popup.getClickedValue(area, 240, 83, InputConstants.MOUSE_BUTTON_LEFT).isEmpty());
		PackedColor white = clickControl(popup, area, 221, 125);

		assertEquals(PackedColor.rgb(0xFFFFFF), white);
	}

	@Test
	void hueSelectionChangesThePlaneToSaturationAndValue() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.rgb(0xFF0000));
		Rect2i area = new Rect2i(0, 0, popup.getWidth(), popup.getHeight());

		popup.getClickedValue(area, 240, 63, InputConstants.MOUSE_BUTTON_LEFT);
		PackedColor red = clickControl(popup, area, 210, 40);
		PackedColor black = clickControl(popup, area, 210, 125);

		assertEquals(PackedColor.rgb(0xFF0000), red);
		assertEquals(PackedColor.rgb(0x000000), black);
	}

	@Test
	void rgbSlidersEditChannelsBelowHueSaturationControls() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.rgb(0x336699));
		Rect2i area = new Rect2i(0, 0, popup.getWidth(), popup.getHeight());

		PackedColor noRed = clickControl(popup, area, 21, 133);
		PackedColor fullGreen = clickControl(popup, area, 210, 147);
		PackedColor noBlue = clickControl(popup, area, 21, 161);

		assertEquals(PackedColor.rgb(0x006699), noRed);
		assertEquals(PackedColor.rgb(0x00FF99), fullGreen);
		assertEquals(PackedColor.rgb(0x00FF00), noBlue);
	}

	@Test
	void compactPickerKeepsRgbSlidersAndHexInputInsideTheAvailableArea() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.rgb(0x336699));
		IConfigValuePopup.Size size = popup.getPreferredSize(180, 300);
		Rect2i area = new Rect2i(0, 0, size.width(), size.height());
		AtomicReference<@Nullable PackedColor> editedColor = new AtomicReference<>();

		PackedColor noRed = clickControl(popup, area, 14, 90);
		popup.getClickedValue(area, 90, 135, InputConstants.MOUSE_BUTTON_LEFT);
		for (char character : "#112233".toCharArray()) {
			assertTrue(popup.charTyped(character, 0, editedColor::set));
		}

		assertEquals(PackedColor.rgb(0x006699), noRed);
		assertEquals(PackedColor.rgb(0x112233), editedColor.get());
	}

	@Test
	void pickerRequestsCompactBoundsInsteadOfLeavingUnusedPopupSpace() {
		ColorPickerPopup rgb = new ColorPickerPopup(PackedColor.rgb(0x336699));
		ColorPickerPopup argb = new ColorPickerPopup(PackedColor.argb(0xFF336699));

		IConfigValuePopup.Size rgbSize = rgb.getPreferredSize(180, 300);
		IConfigValuePopup.Size argbSize = argb.getPreferredSize(180, 300);

		assertEquals(new IConfigValuePopup.Size(180, 147), rgbSize);
		assertEquals(new IConfigValuePopup.Size(180, 159), argbSize);
	}

	@Test
	void compactArgbPickerKeepsAlphaAndHexInputInsideTheAvailableArea() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.argb(0xFF336699));
		IConfigValuePopup.Size size = popup.getPreferredSize(180, 300);
		Rect2i area = new Rect2i(0, 0, size.width(), size.height());
		AtomicReference<@Nullable PackedColor> editedColor = new AtomicReference<>();

		PackedColor transparent = popup.getClickedValue(area, 14, 130, InputConstants.MOUSE_BUTTON_LEFT).orElseThrow();
		popup.getClickedValue(area, 90, 147, InputConstants.MOUSE_BUTTON_LEFT);
		for (char character : "#80112233".toCharArray()) {
			assertTrue(popup.charTyped(character, 0, editedColor::set));
		}

		assertEquals(PackedColor.argb(0x00336699), transparent);
		assertEquals(PackedColor.argb(0x80112233), editedColor.get());
	}

	@Test
	void popupKeepsInteractiveSelectionsOpen() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.argb(0xFFFF0000));
		Rect2i area = new Rect2i(0, 0, popup.getWidth(), popup.getHeight());

		PackedColor transparentRed = popup.getClickedValue(area, 21, 180, InputConstants.MOUSE_BUTTON_LEFT).orElseThrow();

		assertEquals(PackedColor.argb(0x00FF0000), transparentRed);
		assertFalse(popup.closesAfterValueSelected());
	}

	@Test
	void draggingKeepsAdjustingTheControlWhereTheDragStarted() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.rgb(0xFF0000));
		Rect2i area = new Rect2i(0, 0, popup.getWidth(), popup.getHeight());

		popup.getClickedValue(area, 221, 40, InputConstants.MOUSE_BUTTON_LEFT).orElseThrow();
		PackedColor halfValueRed = popup.getDraggedValue(area, -20, 82.5, InputConstants.MOUSE_BUTTON_LEFT).orElseThrow();

		assertEquals(PackedColor.rgb(0x800000), halfValueRed);
	}

	@Test
	void rgbPickerDoesNotExposeAnAlphaRow() {
		ColorPickerPopup rgb = new ColorPickerPopup(new PackedColor(0x336699, ConfigColorFormat.RGB));
		ColorPickerPopup argb = new ColorPickerPopup(new PackedColor(0xFF336699, ConfigColorFormat.ARGB));

		assertEquals(16, argb.getHeight() - rgb.getHeight());
	}

	@Test
	void popupAcceptsArgbHexInput() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.argb(0xFFFF0000));
		Rect2i area = new Rect2i(0, 0, popup.getWidth(), popup.getHeight());
		AtomicReference<@Nullable PackedColor> editedColor = new AtomicReference<>();

		popup.getClickedValue(area, 180, 200, InputConstants.MOUSE_BUTTON_LEFT);
		for (char character : "#804477DD".toCharArray()) {
			assertTrue(popup.charTyped(character, 0, editedColor::set));
		}

		assertEquals(PackedColor.argb(0x804477DD), editedColor.get());
	}

	@Test
	void hexInputIsAlwaysAvailable() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.rgb(0xFF0000));
		Rect2i area = new Rect2i(0, 0, popup.getWidth(), popup.getHeight());
		AtomicReference<@Nullable PackedColor> editedColor = new AtomicReference<>();

		popup.getClickedValue(area, 180, 185, InputConstants.MOUSE_BUTTON_LEFT);
		for (char character : "#336699".toCharArray()) {
			assertTrue(popup.charTyped(character, 0, editedColor::set));
		}

		assertEquals(PackedColor.rgb(0x336699), editedColor.get());
	}

	@Test
	void focusedHexInputAcceptsTypingAsSoonAsPickerOpens() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.rgb(0xFF0000), true);
		AtomicReference<@Nullable PackedColor> editedColor = new AtomicReference<>();

		for (char character : "#123456".toCharArray()) {
			assertTrue(popup.charTyped(character, 0, editedColor::set));
		}

		assertEquals(PackedColor.rgb(0x123456), editedColor.get());
	}

	@Test
	void chartAndSlidersDoNotExposeHoveredValues() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.rgb(0x336699));
		Rect2i area = new Rect2i(0, 0, popup.getWidth(), popup.getHeight());

		assertTrue(popup.getHoveredValue(area, 100, 90).isEmpty());
		assertTrue(popup.getHoveredValue(area, 100, 155).isEmpty());
	}

	@Test
	void shiftPrecisionScalesSliderMovementFromItsAnchor() {
		assertEquals(0.51f, ColorPickerPopup.getPreciseSliderPosition(0.5f, 0.4f, 0.5f), 0.0001f);
		assertEquals(0.0f, ColorPickerPopup.getPreciseSliderPosition(0.02f, 0.8f, -0.8f), 0.0001f);
		assertEquals(1.0f, ColorPickerPopup.getPreciseSliderPosition(0.98f, 0.2f, 0.8f), 0.0001f);
	}

	private static PackedColor clickControl(ColorPickerPopup popup, Rect2i area, double mouseX, double mouseY) {
		PackedColor value = popup.getClickedValue(area, mouseX, mouseY, InputConstants.MOUSE_BUTTON_LEFT).orElseThrow();
		popup.getClickedValue(area, mouseX, mouseY, InputConstants.MOUSE_BUTTON_LEFT).orElseThrow();
		return value;
	}
}
