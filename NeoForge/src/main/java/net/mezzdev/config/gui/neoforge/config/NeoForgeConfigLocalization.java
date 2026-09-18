package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.gui.util.ConfigNameUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
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

	public static Component getCategoryName(String localizationKey, ModConfig.Type type, String fileName, boolean showFileName) {
		String fallback = getDisplayNameFallback(type.extension());
		Component title = Component.translatableWithFallback(localizationKey + ".title", fallback);
		if (type == ModConfig.Type.COMMON) {
			title = Component.translatableWithFallback("mezz_config.config.native.local.title", "%s (local)", title);
		}
		if (showFileName) {
			return title.copy().append("\n").append(fileName);
		}
		return title;
	}

	public static Component getCategoryDescription(String localizationKey, ModConfig modConfig) {
		String tooltipKey = localizationKey + ".tooltip";
		String fallbackKey = switch (modConfig.getType()) {
			case COMMON -> "mezz_config.config.native.description.common";
			case CLIENT -> "mezz_config.config.native.description.client";
			case SERVER -> "mezz_config.config.native.description.server";
			case STARTUP -> "mezz_config.config.native.description.startup";
		};
		Component scope = Component.translatable(fallbackKey, modConfig.getFileName());
		if (I18n.exists(tooltipKey)) {
			return Component.translatable(tooltipKey).append("\n\n").append(scope);
		}
		return scope;
	}

	public static String getValueLocalizationKey(String modId, List<String> path, @Nullable String configuredTranslationKey) {
		if (configuredTranslationKey != null && !configuredTranslationKey.isBlank()) {
			return configuredTranslationKey;
		}
		return modId + ".configuration." + String.join(".", path);
	}

	public static Component getValueName(String modId, ModConfigSpec modConfigSpec, String localizationKey, List<String> path) {
		MutableComponent result = Component.empty();
		for (int level = 1; level < path.size(); level++) {
			List<String> sectionPath = path.subList(0, level);
			String sectionKey = getValueLocalizationKey(modId, sectionPath, modConfigSpec.getLevelTranslationKey(sectionPath));
			result.append(Component.translatableWithFallback(sectionKey, getDisplayNameFallback(sectionPath.getLast())));
			result.append(" › ");
		}
		String name = localizationKey;
		if (!path.isEmpty()) {
			name = path.getLast();
		}
		return result.append(Component.translatableWithFallback(localizationKey, getDisplayNameFallback(name)));
	}

	public static Component getValueDescription(String localizationKey, @Nullable String comment, ConfigValueRestartRequirement restartRequirement) {
		String tooltipKey = localizationKey + ".tooltip";
		Component description = Component.empty();
		if (I18n.exists(tooltipKey) || !Strings.isBlank(comment)) {
			description = Component.translatableWithFallback(tooltipKey, getCommentFallback(comment));
		}
		if (restartRequirement == ConfigValueRestartRequirement.NONE) {
			Component restartNotice = Component.translatableWithFallback(
				"mezz_config.config.native.restart.unknown",
				"This mod does not declare a restart requirement. A game restart may be needed for changes to take effect."
			);
			if (description.getString().isBlank()) {
				return restartNotice;
			}
			return description.copy().append("\n\n").append(restartNotice);
		}
		return description;
	}

	private static String getCommentFallback(@Nullable String comment) {
		if (comment == null) {
			return "";
		}
		return comment;
	}

	public static String getDisplayNameFallback(String name) {
		return ConfigNameUtil.getDisplayNameFallback(name);
	}
}
