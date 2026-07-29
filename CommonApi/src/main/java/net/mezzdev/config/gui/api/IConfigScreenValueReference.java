package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.IConfigValue;

import java.util.Objects;

/**
 * References a config value that can be displayed on a config screen.
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface IConfigScreenValueReference {
	/**
	 * Reference a config value by object identity.
	 *
	 * @param configValue config value to reference
	 * @return a config screen value reference for the given config value
	 *
	 * @since 0.1.0
	 */
	static IConfigScreenValueReference configValue(IConfigValue<?> configValue) {
		IConfigValue<?> checkedConfigValue = Objects.requireNonNull(configValue, "configValue");
		return new IConfigScreenValueReference() {
			@Override
			public boolean matches(IConfigValue<?> value) {
				return checkedConfigValue == value;
			}

			@Override
			public String toString() {
				return checkedConfigValue.getName();
			}
		};
	}

	/**
	 * Returns true when this reference points to the given config value.
	 *
	 * @param value config value to test
	 * @return true when this reference points to the given config value
	 *
	 * @since 0.1.0
	 */
	boolean matches(IConfigValue<?> value);
}
