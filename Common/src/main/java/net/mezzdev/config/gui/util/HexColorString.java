package net.mezzdev.config.gui.util;

import net.mezzdev.config.api.value.color.PackedColor;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;

import java.util.Locale;
import java.util.Optional;

/**
 * Explicit RGB/ARGB strings, retaining their spelling when edited as colors.
 */
public record HexColorString(String text, String prefix, PackedColor color) {

	public static Optional<HexColorString> parse(Object value) {
		if (!(value instanceof String text)) {
			return Optional.empty();
		}
		String prefix;
		if (text.startsWith("#")) {
			prefix = "#";
		} else if (text.startsWith("0x") || text.startsWith("0X")) {
			prefix = text.substring(0, 2);
		} else {
			return Optional.empty();
		}
		String digits = text.substring(prefix.length());
		if (digits.length() != 6 && digits.length() != 8) {
			return Optional.empty();
		}
		for (int i = 0; i < digits.length(); i++) {
			char c = digits.charAt(i);
			if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F')) {
				return Optional.empty();
			}
		}
		int packed = Integer.parseUnsignedInt(digits, 16);
		PackedColor color = PackedColor.argb(packed);
		if (digits.length() == 6) {
			color = PackedColor.rgb(packed);
		}
		return Optional.of(new HexColorString(text, prefix, color));
	}

	public String format(PackedColor updated) {
		if (updated.format() != color.format()) {
			throw new IllegalArgumentException("Cannot change the color string's format");
		}
		if (updated.equals(color)) {
			return text;
		}
		int width = text.length() - prefix.length();
		String digits = Integer.toHexString(updated.packedValue());
		digits = "0".repeat(width - digits.length()) + digits;
		String originalDigits = text.substring(prefix.length());
		if (originalDigits.equals(originalDigits.toUpperCase(Locale.ROOT))) {
			digits = digits.toUpperCase(Locale.ROOT);
		}
		return prefix + digits;
	}

	@SuppressWarnings("unchecked")
	public <T> Optional<T> update(T original, PackedColor updated, IConfigValueSerializer<T> serializer) {
		if (!(original instanceof String)) {
			return Optional.empty();
		}
		T value = (T) format(updated);
		return Optional.of(value).filter(serializer::isValid);
	}
}
