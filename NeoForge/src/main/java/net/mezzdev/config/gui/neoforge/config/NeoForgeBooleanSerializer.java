package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigValueIcon;
import net.mezzdev.config.gui.api.IConfigValueIconProvider;
import net.mezzdev.config.gui.api.IConfigValueEditorSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

final class NeoForgeBooleanSerializer implements IConfigValueEditorSerializer<Boolean>, IConfigValueIconProvider<Boolean> {
	public static final NeoForgeBooleanSerializer INSTANCE = new NeoForgeBooleanSerializer();
	private static final Identifier ENABLED_ICON = Identifier.withDefaultNamespace("container/beacon/confirm");
	private static final Identifier DISABLED_ICON = Identifier.withDefaultNamespace("container/beacon/cancel");

	private NeoForgeBooleanSerializer() {

	}

	@Override
	public String serialize(Boolean value) {
		return value.toString();
	}

	@Override
	public IDeserializeResult<Boolean> deserialize(String string) {
		string = string.trim();
		if ("true".equalsIgnoreCase(string)) {
			return IDeserializeResult.success(true);
		}
		if ("false".equalsIgnoreCase(string)) {
			return IDeserializeResult.success(false);
		}
		return IDeserializeResult.failure("string must be 'true' or 'false'");
	}

	@Override
	public boolean isValid(Boolean value) {
		return value != null;
	}

	@Override
	public Optional<List<Boolean>> getAllValidValues() {
		return Optional.of(List.of(true, false));
	}

	@Override
	public Component getLocalizedValueName(String configValueLocalizationKey, Boolean value) {
		return Component.translatable(getValueNameTranslationKey(value));
	}

	@Override
	public Optional<Component> getLocalizedValueDescription(String configValueLocalizationKey, Boolean value) {
		String suffix = ".value." + value + ".description";
		String translationKey = configValueLocalizationKey + suffix;
		if (net.minecraft.locale.Language.getInstance().has(translationKey)) {
			return Optional.of(Component.translatable(translationKey));
		}
		return Optional.of(Component.translatable(getValueDescriptionTranslationKey(value)));
	}

	@Override
	public Optional<IConfigValueIcon> getIcon(Boolean value) {
		Identifier location = getValueIconLocation(value);
		return Optional.of((guiGraphics, area) -> net.mezzdev.config.gui.ConfigRenderUtil.blitSprite(guiGraphics, location, area.getX(), area.getY(), area.getWidth(), area.getHeight()));
	}

	@Override
	public String getValidValuesDescription() {
		return "[true, false]";
	}

	private static String getValueNameTranslationKey(boolean value) {
		if (value) {
			return "mezz_config.config.value.boolean.true";
		}
		return "mezz_config.config.value.boolean.false";
	}

	private static String getValueDescriptionTranslationKey(boolean value) {
		if (value) {
			return "mezz_config.config.value.boolean.true.description";
		}
		return "mezz_config.config.value.boolean.false.description";
	}

	private static Identifier getValueIconLocation(boolean value) {
		if (value) {
			return ENABLED_ICON;
		}
		return DISABLED_ICON;
	}

	@Override
	public ConfigValueEditorType<Boolean> getEditorType() {
		return ConfigValueEditorTypes.BOOLEAN;
	}

}
