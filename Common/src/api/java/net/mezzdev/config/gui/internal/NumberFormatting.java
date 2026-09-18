package net.mezzdev.config.gui.internal;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Display-only number formatting using the operating system's regional format.
 * Serialized config values and editable source text keep their canonical syntax.
 */
public final class NumberFormatting {
	private NumberFormatting() {

	}

	public static String format(Number value) {
		return format(value, Locale.getDefault(Locale.Category.FORMAT));
	}

	public static String format(Number value, Locale locale) {
		NumberFormat format = NumberFormat.getNumberInstance(locale);
		format.setMaximumFractionDigits(340);
		if (value instanceof Float floatValue && Float.isFinite(floatValue)) {
			return format.format(new BigDecimal(Float.toString(floatValue)));
		}
		return format.format(value);
	}
}
