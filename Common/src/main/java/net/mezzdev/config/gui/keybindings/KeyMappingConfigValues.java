package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.api.value.IConfigValue;
import net.minecraft.client.KeyMapping;

import java.util.Collection;
import java.util.List;

/**
 * Collects configurable key mappings and attaches screen metadata to them.
 */
public final class KeyMappingConfigValues {
	private KeyMappingConfigValues() {

	}

	public static List<IConfigValue<?>> create(Collection<? extends KeyMapping> keyMappings) {
		return keyMappings.stream()
			.map(KeyMappingAdapters::create)
			.map(KeyMappingConfigValue::new)
			.<IConfigValue<?>>map(value -> value)
			.toList();
	}
}
