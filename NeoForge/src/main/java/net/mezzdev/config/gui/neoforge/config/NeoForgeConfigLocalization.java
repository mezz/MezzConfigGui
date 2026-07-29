package net.mezzdev.config.gui.neoforge.config;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

final class NeoForgeConfigLocalization {
	private NeoForgeConfigLocalization() {

	}

	public static Component getScreenTitle(ModContainer modContainer) {
		String modId = modContainer.getModId();
		String displayName = modContainer.getModInfo().getDisplayName();
		return Component.translatableWithFallback(modId + ".configuration.title", "%s Configuration", displayName);
	}

	public static String getCategoryLocalizationKey(String modId, ModConfig modConfig) {
		String configFileName = modConfig.getFileName()
			.replaceAll("[^a-zA-Z0-9]+", ".")
			.replaceFirst("^\\.", "")
			.replaceFirst("\\.$", "")
			.toLowerCase(Locale.ENGLISH);
		return modId + ".configuration.section." + configFileName;
	}

	public static Component getCategoryName(String localizationKey, ModConfig modConfig) {
		String fallback = getDisplayNameFallback(modConfig.getType().extension());
		return Component.translatableWithFallback(localizationKey + ".title", fallback);
	}

	public static Component getCategoryDescription(String localizationKey, ModConfig modConfig) {
		return Component.translatableWithFallback(localizationKey + ".tooltip", modConfig.getFileName());
	}

	public static String getValueLocalizationKey(String modId, List<String> path, @Nullable String configuredTranslationKey) {
		if (configuredTranslationKey != null && !configuredTranslationKey.isBlank()) {
			return configuredTranslationKey;
		}
		return modId + ".configuration." + String.join(".", path);
	}

	public static Component getValueName(String localizationKey, List<String> path) {
		String name = path.isEmpty() ? localizationKey : path.getLast();
		return Component.translatableWithFallback(localizationKey, getDisplayNameFallback(name));
	}

	public static Component getValueDescription(String localizationKey, @Nullable String comment) {
		String tooltipKey = localizationKey + ".tooltip";
		if (I18n.exists(tooltipKey) || !Strings.isBlank(comment)) {
			return Component.translatableWithFallback(tooltipKey, comment == null ? "" : comment);
		}
		return Component.empty();
	}

	public static String getDisplayNameFallback(String name) {
		String[] words = name
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
