package net.mezzdev.config.gui.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.api.value.ConfigValueEditorType;
import net.mezzdev.config.api.value.ConfigValueEditorTypes;
import net.mezzdev.config.api.value.IConfigValueEditorSerializer;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.Optional;

/**
 * Serializes key binding values for the config value interface used by the screen.
 */
final class KeyMappingSerializer implements IConfigValueEditorSerializer<KeyMappingValue> {
	private final KeyMappingValue defaultValue;

	public KeyMappingSerializer(KeyMappingValue defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public String serialize(KeyMappingValue value) {
		ConfigKeyBinding binding = value.binding();
		if (binding.modifier() == ConfigKeyModifier.NONE) {
			return binding.keyName();
		}
		return binding.keyName() + ":" + binding.modifier().name();
	}

	@Override
	public IDeserializeResult<KeyMappingValue> deserialize(String string) {
		try {
			String[] parts = string.trim().split(":", 2);
			InputConstants.Key key = InputConstants.getKey(parts[0]);
			ConfigKeyModifier modifier = parts.length > 1 ? getModifier(parts[1]) : ConfigKeyModifier.NONE;
			return new KeyMappingDeserializeResult<>(defaultValue.withBinding(new ConfigKeyBinding(key.getName(), modifier)));
		} catch (IllegalArgumentException e) {
			return new KeyMappingDeserializeResult<>(null, e.getMessage());
		}
	}

	@Override
	public boolean isValid(KeyMappingValue value) {
		return value != null;
	}

	@Override
	public Optional<Collection<KeyMappingValue>> getAllValidValues() {
		return Optional.empty();
	}

	@Override
	public ConfigValueEditorType<KeyMappingValue> getEditorType() {
		return ConfigValueEditorTypes.getKeyMapping();
	}

	@Override
	public Component getLocalizedValueName(String configValueLocalizationKey, KeyMappingValue value) {
		return value.configKeyMapping().getValueName(value.binding());
	}

	@Override
	public String getValidValuesDescription() {
		return "Any keyboard key, mouse button, or unbound.";
	}

	private static ConfigKeyModifier getModifier(String value) {
		return switch (value.toUpperCase()) {
			case "CONTROL", "COMMAND", "CONTROL_OR_COMMAND" -> ConfigKeyModifier.CONTROL_OR_COMMAND;
			case "SHIFT" -> ConfigKeyModifier.SHIFT;
			case "ALT" -> ConfigKeyModifier.ALT;
			default -> ConfigKeyModifier.NONE;
		};
	}
}
