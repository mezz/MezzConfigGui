package net.mezzdev.config.gui.popup;

import net.mezzdev.config.api.value.ConfigColorFormat;
import net.mezzdev.config.api.value.PackedColor;
import net.minecraft.client.renderer.Rect2i;
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
	void convertsBetweenRgbAndCmyk() {
		ColorPickerModel model = new ColorPickerModel(PackedColor.rgb(0xFF0000));

		ColorPickerModel.Cmyk red = model.getCmyk();
		assertEquals(0.0, red.cyan(), 0.0001);
		assertEquals(1.0, red.magenta(), 0.0001);
		assertEquals(1.0, red.yellow(), 0.0001);
		assertEquals(0.0, red.black(), 0.0001);

		model.setCmyk(1.0, 0.0, 0.0, 0.0);
		assertEquals(PackedColor.rgb(0x00FFFF), model.getPackedColor());
	}

	@Test
	void convertsBetweenRgbAndD65Lab() {
		ColorPickerModel model = new ColorPickerModel(PackedColor.rgb(0xFF0000));

		ColorPickerModel.Lab red = model.getLab();
		assertEquals(53.24, red.lightness(), 0.02);
		assertEquals(80.09, red.a(), 0.02);
		assertEquals(67.20, red.b(), 0.02);

		model.setLab(red.lightness(), red.a(), red.b());
		ColorPickerModel.Rgb roundTrip = model.getRgb();
		assertEquals(255, roundTrip.red(), 1);
		assertEquals(0, roundTrip.green(), 1);
		assertEquals(0, roundTrip.blue(), 1);
	}

	@Test
	void popupKeepsInteractiveSelectionsOpen() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.argb(0xFFFF0000));
		Rect2i area = new Rect2i(0, 0, popup.getWidth(), popup.getHeight());

		PackedColor transparentRed = popup.getClickedValue(area, 7, 101, 0).orElseThrow();

		assertEquals(PackedColor.argb(0x00FF0000), transparentRed);
		assertFalse(popup.closesAfterValueSelected());
	}

	@Test
	void draggingKeepsAdjustingTheControlWhereTheDragStarted() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.rgb(0xFF0000));
		Rect2i area = new Rect2i(0, 0, popup.getWidth(), popup.getHeight());

		popup.getClickedValue(area, 160, 7, 0).orElseThrow();
		PackedColor cyan = popup.getDraggedValue(area, -20, 50.5, 0).orElseThrow();

		assertEquals(PackedColor.rgb(0x00FFFF), cyan);
	}

	@Test
	void rgbPickerDoesNotExposeAnAlphaRow() {
		ColorPickerPopup rgb = new ColorPickerPopup(new PackedColor(0x336699, ConfigColorFormat.RGB));
		ColorPickerPopup argb = new ColorPickerPopup(new PackedColor(0xFF336699, ConfigColorFormat.ARGB));

		assertEquals(17, argb.getHeight() - rgb.getHeight());
	}

	@Test
	void popupAcceptsArgbHexInput() {
		ColorPickerPopup popup = new ColorPickerPopup(PackedColor.argb(0xFFFF0000));
		Rect2i area = new Rect2i(0, 0, popup.getWidth(), popup.getHeight());
		AtomicReference<PackedColor> editedColor = new AtomicReference<>();

		popup.getClickedValue(area, 150, 120, 0);
		popup.getClickedValue(area, 20, 140, 0);
		for (char character : "#804477DD".toCharArray()) {
			assertTrue(popup.charTyped(character, 0, editedColor::set));
		}

		assertEquals(PackedColor.argb(0x804477DD), editedColor.get());
	}
}
