package net.mezzdev.config.gui.api;

import net.mezzdev.config.gui.internal.NumberFormatting;

import net.mezzdev.config.api.schema.category.IConfigEditorCategory;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * Localizes config values and categories for GUI display from GUI-specific providers or MezzConfig localization keys.
 *
 * @since 0.1.0
 */
public final class ConfigValueLocalization {
	private static final String DESCRIPTION_SUFFIX = ".description";

	private ConfigValueLocalization() {

	}

	/**
	 * Get the localized category name.
	 *
	 * @since 0.1.0
	 */
	public static Component getName(IConfigEditorCategory category) {
		if (category instanceof IConfigLocalizedCategory localizedCategory) {
			return localizedCategory.getLocalizedName();
		}
		return Component.translatable(category.getLocalizationKey());
	}

	/**
	 * Get the localized category description.
	 *
	 * @since 0.1.0
	 */
	public static Component getDescription(IConfigEditorCategory category) {
		if (category instanceof IConfigLocalizedCategory localizedCategory) {
			return localizedCategory.getLocalizedDescription();
		}
		return Component.translatable(category.getLocalizationKey() + DESCRIPTION_SUFFIX);
	}

	/**
	 * Get the localized config value name.
	 *
	 * @since 0.1.0
	 */
	public static Component getName(IConfigValue<?> configValue) {
		return getName(IConfigScreenValue.configValue(configValue));
	}

	/**
	 * Get the localized config screen value name.
	 *
	 * @since 0.1.0
	 */
	public static Component getName(IConfigScreenValue<?> configValue) {
		if (configValue instanceof IConfigLocalizedValue localizedValue) {
			return localizedValue.getLocalizedName();
		}
		Optional<? extends IConfigValue<?>> backingConfigValue = configValue.getConfigValue();
		if (backingConfigValue.isPresent() && backingConfigValue.get() instanceof IConfigLocalizedValue localizedValue) {
			return localizedValue.getLocalizedName();
		}
		return Component.translatable(configValue.getLocalizationKey());
	}

	/**
	 * Get the localized config value description.
	 *
	 * @since 0.1.0
	 */
	public static Component getDescription(IConfigValue<?> configValue) {
		return getDescription(IConfigScreenValue.configValue(configValue));
	}

	/**
	 * Get the localized config screen value description.
	 *
	 * @since 0.1.0
	 */
	public static Component getDescription(IConfigScreenValue<?> configValue) {
		if (configValue instanceof IConfigLocalizedValue localizedValue) {
			return localizedValue.getLocalizedDescription();
		}
		Optional<? extends IConfigValue<?>> backingConfigValue = configValue.getConfigValue();
		if (backingConfigValue.isPresent() && backingConfigValue.get() instanceof IConfigLocalizedValue localizedValue) {
			return localizedValue.getLocalizedDescription();
		}
		return Component.translatable(configValue.getLocalizationKey() + DESCRIPTION_SUFFIX);
	}

	/**
	 * Get the localized display name for a config value option.
	 *
	 * @since 0.1.0
	 */
	public static <T> Component getValueName(IConfigValue<T> configValue, T value) {
		return getValueName(IConfigScreenValue.configValue(configValue), value);
	}

	/**
	 * Get the localized display name for a config screen value option.
	 *
	 * @since 0.1.0
	 */
	public static <T> Component getValueName(IConfigScreenValue<T> configValue, T value) {
		return getValueName(configValue.getSerializer(), configValue.getLocalizationKey(), value);
	}

	/**
	 * Get the localized display name for a config value option.
	 *
	 * @since 0.1.0
	 */
	public static <T> Component getValueName(IConfigValueSerializer<T> serializer, String configValueLocalizationKey, T value) {
		Optional<Component> providedName = getProvidedValueName(serializer, configValueLocalizationKey, value);
		if (providedName.isPresent()) {
			return providedName.get();
		}
		if (value instanceof Boolean booleanValue) {
			return Component.translatable(getBooleanValueNameTranslationKey(booleanValue));
		}
		if (value instanceof Enum<?> enumValue) {
			String key = getEnumValueLocalizationKey(configValueLocalizationKey, enumValue);
			return Component.translatableWithFallback(key, getDisplayNameFallback(enumValue.name()));
		}
		if (value instanceof Number number) {
			return Component.literal(NumberFormatting.format(number));
		}
		return Component.literal(serializer.serialize(value));
	}

	/**
	 * Get the localized description for a config value option.
	 *
	 * @since 0.1.0
	 */
	public static <T> Optional<Component> getValueDescription(IConfigValue<T> configValue, T value) {
		return getValueDescription(IConfigScreenValue.configValue(configValue), value);
	}

	/**
	 * Get the localized description for a config screen value option.
	 *
	 * @since 0.1.0
	 */
	public static <T> Optional<Component> getValueDescription(IConfigScreenValue<T> configValue, T value) {
		return getValueDescription(configValue.getSerializer(), configValue.getLocalizationKey(), value);
	}

	/**
	 * Get the localized description for a config value option.
	 *
	 * @since 0.1.0
	 */
	public static <T> Optional<Component> getValueDescription(IConfigValueSerializer<T> serializer, String configValueLocalizationKey, T value) {
		Optional<Component> providedDescription = getProvidedValueDescription(serializer, configValueLocalizationKey, value);
		if (providedDescription.isPresent()) {
			return providedDescription;
		}
		if (value instanceof Boolean booleanValue) {
			String key = configValueLocalizationKey + ".value." + booleanValue + DESCRIPTION_SUFFIX;
			if (Language.getInstance().has(key)) {
				return Optional.of(Component.translatable(key));
			}
			return Optional.of(Component.translatable(getBooleanValueDescriptionTranslationKey(booleanValue)));
		}
		if (value instanceof Enum<?> enumValue) {
			String key = getEnumValueLocalizationKey(configValueLocalizationKey, enumValue) + DESCRIPTION_SUFFIX;
			if (Language.getInstance().has(key)) {
				return Optional.of(Component.translatable(key));
			}
		}
		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	private static <T> Optional<Component> getProvidedValueName(IConfigValueSerializer<T> serializer, String configValueLocalizationKey, T value) {
		if (serializer instanceof IConfigValueLocalizationProvider<?> localizationProvider) {
			IConfigValueLocalizationProvider<T> typedProvider = (IConfigValueLocalizationProvider<T>) localizationProvider;
			return Optional.of(typedProvider.getLocalizedValueName(configValueLocalizationKey, value));
		}
		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	private static <T> Optional<Component> getProvidedValueDescription(IConfigValueSerializer<T> serializer, String configValueLocalizationKey, T value) {
		if (serializer instanceof IConfigValueLocalizationProvider<?> localizationProvider) {
			IConfigValueLocalizationProvider<T> typedProvider = (IConfigValueLocalizationProvider<T>) localizationProvider;
			return typedProvider.getLocalizedValueDescription(configValueLocalizationKey, value);
		}
		return Optional.empty();
	}

	private static String getEnumValueLocalizationKey(String configValueLocalizationKey, Enum<?> enumValue) {
		return "%s.%s".formatted(configValueLocalizationKey, enumValue.name().toLowerCase(Locale.ROOT));
	}

	private static String getBooleanValueNameTranslationKey(boolean value) {
		if (value) {
			return "mezz_config.config.value.boolean.true";
		}
		return "mezz_config.config.value.boolean.false";
	}

	private static String getBooleanValueDescriptionTranslationKey(boolean value) {
		if (value) {
			return "mezz_config.config.value.boolean.true.description";
		}
		return "mezz_config.config.value.boolean.false.description";
	}

	private static String getDisplayNameFallback(String name) {
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
