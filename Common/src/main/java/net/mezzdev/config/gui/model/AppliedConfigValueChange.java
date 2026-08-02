package net.mezzdev.config.gui.model;

import net.mezzdev.config.gui.api.IConfigScreenValue;

/**
 * A config value change that has already been applied during the current config screen session.
 */
public record AppliedConfigValueChange<T>(
	IConfigScreenValue<T> configValue,
	T oldValue,
	T newValue
) {
	public ConfigValueChange<T> toUndoChange() {
		return new ConfigValueChange<>(configValue, oldValue);
	}
}
