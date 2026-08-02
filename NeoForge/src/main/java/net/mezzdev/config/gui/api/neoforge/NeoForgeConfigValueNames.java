package net.mezzdev.config.gui.api.neoforge;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Objects;

/**
 * Helpers for getting stable config GUI value names from native NeoForge config values.
 *
 * @since 0.1.0
 */
public final class NeoForgeConfigValueNames {
	private NeoForgeConfigValueNames() {

	}

	/**
	 * Get the stable config GUI value name for a native NeoForge config value.
	 * <p>
	 * Use this with the {@code ByName} methods on
	 * {@link net.mezzdev.config.gui.api.IConfigScreenCategoryBuilder}.
	 *
	 * @param configValue config value to name
	 * @return the stable config GUI value name for the given config value
	 *
	 * @since 0.1.0
	 */
	public static String getName(ModConfigSpec.ConfigValue<?> configValue) {
		ModConfigSpec.ConfigValue<?> checkedConfigValue = Objects.requireNonNull(configValue, "configValue");
		List<String> path = List.copyOf(checkedConfigValue.getPath());
		return String.join(".", path);
	}
}
