package net.mezzdev.config.gui.util;

import net.mezzdev.config.gui.internal.NumberFormatting;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumberFormattingTest {
	@Test
	void usesRegionalGroupingAndDecimalSeparators() {
		assertEquals("1,234,567.125", NumberFormatting.format(1_234_567.125, Locale.US));
		assertEquals("1.234.567,125", NumberFormatting.format(1_234_567.125, Locale.GERMANY));
		assertEquals("1\u202f234\u202f567,125", NumberFormatting.format(1_234_567.125, Locale.FRANCE));
	}

	@Test
	void preservesLargeIntegersAndSmallFractions() {
		assertEquals("9,223,372,036,854,775,807", NumberFormatting.format(Long.MAX_VALUE, Locale.US));
		assertEquals("-9,223,372,036,854,775,808", NumberFormatting.format(Long.MIN_VALUE, Locale.US));
		assertEquals("0.000000001", NumberFormatting.format(0.000000001, Locale.US));
		assertEquals("0.1", NumberFormatting.format(0.1f, Locale.US));
	}

	@Test
	void followsTheSystemFormatLocaleIndependentlyOfTheDisplayLocale() {
		Locale originalFormat = Locale.getDefault(Locale.Category.FORMAT);
		Locale originalDisplay = Locale.getDefault(Locale.Category.DISPLAY);
		try {
			Locale.setDefault(Locale.Category.DISPLAY, Locale.US);
			Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
			assertEquals("1.234.567", NumberFormatting.format(1_234_567));
		} finally {
			Locale.setDefault(Locale.Category.FORMAT, originalFormat);
			Locale.setDefault(Locale.Category.DISPLAY, originalDisplay);
		}
	}
}
