package net.mezzdev.config.gui.api.neoforge;

import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.gui.api.IConfigScreenValueReference;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Objects;

/**
 * Helpers for referencing native NeoForge config values in config GUI screen customizations.
 *
 * @since 0.1.0
 */
public final class NeoForgeConfigValueReferences {
	private NeoForgeConfigValueReferences() {

	}

	/**
	 * Reference a native NeoForge config value.
	 *
	 * @param configValue config value to reference
	 * @return a config screen value reference for the given config value
	 *
	 * @since 0.1.0
	 */
	public static IConfigScreenValueReference configValue(ModConfigSpec.ConfigValue<?> configValue) {
		ModConfigSpec.ConfigValue<?> checkedConfigValue = Objects.requireNonNull(configValue, "configValue");
		List<String> path = List.copyOf(checkedConfigValue.getPath());
		String name = String.join(".", path);
		return new IConfigScreenValueReference() {
			@Override
			public boolean matches(IConfigValue<?> value) {
				return value.getName().equals(name);
			}

			@Override
			public String toString() {
				return name;
			}
		};
	}
}
