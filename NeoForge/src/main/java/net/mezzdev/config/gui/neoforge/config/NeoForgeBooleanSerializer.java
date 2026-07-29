package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.ConfigValueEditorType;
import net.mezzdev.config.api.value.ConfigValueEditorTypes;
import net.mezzdev.config.api.value.IConfigValueEditorSerializer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

final class NeoForgeBooleanSerializer implements IConfigValueEditorSerializer<Boolean> {
	public static final NeoForgeBooleanSerializer INSTANCE = new NeoForgeBooleanSerializer();
	private static final ResourceLocation ENABLED_ICON = ResourceLocation.withDefaultNamespace("container/beacon/confirm");
	private static final ResourceLocation DISABLED_ICON = ResourceLocation.withDefaultNamespace("container/beacon/cancel");

	private NeoForgeBooleanSerializer() {

	}

	@Override
	public String serialize(Boolean value) {
		return value.toString();
	}

	@Override
	public NeoForgeDeserializeResult<Boolean> deserialize(String string) {
		string = string.trim();
		if ("true".equalsIgnoreCase(string)) {
			return new NeoForgeDeserializeResult<>(true);
		}
		if ("false".equalsIgnoreCase(string)) {
			return new NeoForgeDeserializeResult<>(false);
		}
		return new NeoForgeDeserializeResult<>(null, "string must be 'true' or 'false'");
	}

	@Override
	public boolean isValid(Boolean value) {
		return value != null;
	}

	@Override
	public Optional<Collection<Boolean>> getAllValidValues() {
		return Optional.of(List.of(true, false));
	}

	@Override
	public Component getLocalizedValueName(String configValueLocalizationKey, Boolean value) {
		return Component.translatable(value ? "mezz_config.config.value.boolean.true" : "mezz_config.config.value.boolean.false");
	}

	@Override
	public Optional<Component> getLocalizedValueDescription(String configValueLocalizationKey, Boolean value) {
		String suffix = ".value." + value + ".description";
		String translationKey = configValueLocalizationKey + suffix;
		if (I18n.exists(translationKey)) {
			return Optional.of(Component.translatable(translationKey));
		}
		return Optional.of(Component.translatable(value ? "mezz_config.config.value.boolean.true.description" : "mezz_config.config.value.boolean.false.description"));
	}

	@Override
	public Optional<ResourceLocation> getValueIcon(Boolean value) {
		return Optional.of(value ? ENABLED_ICON : DISABLED_ICON);
	}

	@Override
	public String getValidValuesDescription() {
		return "[true, false]";
	}

	@Override
	public ConfigValueEditorType<Boolean> getEditorType() {
		return ConfigValueEditorTypes.BOOLEAN;
	}

}
