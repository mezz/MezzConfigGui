package net.mezzdev.config.gui.neoforge.config;

import net.minecraft.locale.Language;
import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigValueEditorSerializer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

final class NeoForgeEnumSerializer<T extends Enum<T>> implements IConfigValueEditorSerializer<T> {
	private final Class<T> enumClass;
	private final List<T> validValues;

	public NeoForgeEnumSerializer(Class<T> enumClass, Collection<T> validValues) {
		this.enumClass = enumClass;
		this.validValues = List.copyOf(validValues);
	}

	@Override
	public String serialize(T value) {
		return value.name();
	}

	@Override
	public IDeserializeResult<T> deserialize(String string) {
		string = string.trim();
		if (string.startsWith("\"") && string.endsWith("\"")) {
			string = string.substring(1, string.length() - 1);
		}
		try {
			T value = Enum.valueOf(enumClass, string);
			if (!isValid(value)) {
				return IDeserializeResult.failure("Invalid enum name: %s".formatted(string));
			}
			return IDeserializeResult.success(value);
		} catch (IllegalArgumentException e) {
			return IDeserializeResult.failure("Invalid enum name: %s".formatted(e.getMessage()));
		}
	}

	@Override
	public boolean isValid(T value) {
		return validValues.contains(value);
	}

	@Override
	public Optional<List<T>> getAllValidValues() {
		return Optional.of(validValues);
	}

	@Override
	public Component getLocalizedValueName(String configValueLocalizationKey, T value) {
		return getTranslatedEnumValue(configValueLocalizationKey, value, ".name")
			.orElseGet(() -> {
				if (value instanceof TranslatableEnum translatableEnum) {
					return translatableEnum.getTranslatedName();
				}
				return Component.literal(NeoForgeConfigLocalization.getDisplayNameFallback(value.name()));
			});
	}

	@Override
	public Optional<Component> getLocalizedValueDescription(String configValueLocalizationKey, T value) {
		return getTranslatedEnumValue(configValueLocalizationKey, value, ".description");
	}

	private Optional<Component> getTranslatedEnumValue(String configValueLocalizationKey, T value, String suffix) {
		String translationKey = configValueLocalizationKey + ".value." + value.name() + suffix;
		if (Language.getInstance().has(translationKey)) {
			return Optional.of(Component.translatable(translationKey));
		}
		return Optional.empty();
	}

	@Override
	public String getValidValuesDescription() {
		String names = validValues.stream()
			.map(Enum::name)
			.collect(Collectors.joining(", "));
		return "[%s]".formatted(names);
	}

	@Override
	public ConfigValueEditorType<T> getEditorType() {
		return ConfigValueEditorTypes.getSelection();
	}

}
