package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.api.value.IConfigValue;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

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

	public static List<IConfigValue<?>> createForModId(String modId) {
		String checkedModId = modId.toLowerCase(Locale.ROOT);
		KeyMapping[] keyMappings = Minecraft.getInstance().options.keyMappings;
		return Arrays.stream(keyMappings)
			.filter(keyMapping -> belongsToModId(keyMapping, checkedModId))
			.map(KeyMappingAdapters::create)
			.map(KeyMappingConfigValue::new)
			.<IConfigValue<?>>map(value -> value)
			.toList();
	}

	private static boolean belongsToModId(KeyMapping keyMapping, String modId) {
		String checkedModId = modId.toLowerCase(Locale.ROOT);
		String keyName = keyMapping.getName().toLowerCase(Locale.ROOT);
		if (keyName.startsWith("key." + checkedModId + ".")) {
			return true;
		}

		String category = keyMapping.getCategory().toLowerCase(Locale.ROOT);
		return category.equals("key.categories." + checkedModId) ||
			category.startsWith("key.categories." + checkedModId + ".") ||
			category.equals("key.category." + checkedModId) ||
			category.startsWith("key.category." + checkedModId + ".");
	}
}
