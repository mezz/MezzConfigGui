package net.mezzdev.config.gui.model;

import net.mezzdev.config.gui.api.IConfigScreenValue;

import java.util.List;

/**
 * A pending GUI edit for one config value.
 */
public record ConfigValueChange<T>(
	IConfigScreenValue<T> configValue,
	T value
) {
	public static boolean requiresRestart(List<ConfigValueChange<?>> changes) {
		return changes.stream()
			.map(ConfigValueChange::configValue)
			.anyMatch(IConfigScreenValue::requiresRestart);
	}

	public boolean apply() {
		return configValue.set(value);
	}
}
