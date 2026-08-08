package net.mezzdev.config.gui.util;

import java.util.Locale;

/**
 * Formatting helpers for config names that do not have translations.
 */
public final class ConfigNameUtil {
	private ConfigNameUtil() {

	}

	public static String getDisplayNameFallback(String name) {
		String[] words = name
			.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
			.replace('-', '_')
			.replace('.', '_')
			.split("_+");
		StringBuilder result = new StringBuilder();
		for (String word : words) {
			if (word.isBlank()) {
				continue;
			}
			if (!result.isEmpty()) {
				result.append(' ');
			}
			String lowercaseWord = word.toLowerCase(Locale.ROOT);
			result.append(Character.toUpperCase(lowercaseWord.charAt(0)));
			if (lowercaseWord.length() > 1) {
				result.append(lowercaseWord.substring(1));
			}
		}
		if (result.isEmpty()) {
			return name;
		}
		return result.toString();
	}
}
