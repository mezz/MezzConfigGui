package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.gui.api.keybindings.IKeyMappingBinding;
import net.minecraft.network.chat.Component;

/**
 * Editable key mapping config value with the metadata needed by the config screen.
 */
public record KeyMappingValue(IKeyMappingBinding binding, IConfigKeyMapping configKeyMapping) {
	public KeyMappingValue withBinding(IKeyMappingBinding binding) {
		return new KeyMappingValue(binding, configKeyMapping);
	}

	public Component context() {
		return configKeyMapping.getLocalizedContext();
	}
}
