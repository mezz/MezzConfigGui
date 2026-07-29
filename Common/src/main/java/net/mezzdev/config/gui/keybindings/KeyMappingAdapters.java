package net.mezzdev.config.gui.keybindings;

import net.minecraft.client.KeyMapping;

/**
 * Creates config GUI key mapping adapters for Minecraft key mappings.
 */
public final class KeyMappingAdapters {
	private KeyMappingAdapters() {

	}

	public static IConfigKeyMapping create(KeyMapping keyMapping) {
		return new ConfigKeyMapping(keyMapping);
	}
}
