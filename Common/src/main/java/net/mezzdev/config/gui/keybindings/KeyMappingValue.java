package net.mezzdev.config.gui.keybindings;

import net.minecraft.network.chat.Component;

/**
 * Editable key mapping config value with the metadata needed by the config screen.
 */
public record KeyMappingValue(ConfigKeyBinding binding, IConfigKeyMapping configKeyMapping) {
	public KeyMappingValue withBinding(ConfigKeyBinding binding) {
		return new KeyMappingValue(binding, configKeyMapping);
	}

	public Component context() {
		return configKeyMapping.getLocalizedContext();
	}
}
