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
			public boolean matches(IConfigScreenValue<?> value) {
				return value.getConfigValue()
					.filter(configValue -> configValue == checkedConfigValue)
					.isPresent();
			}

			@Override
			public String toString() {
				return checkedConfigValue.getName();
			}
		};
	}

	/**
	 * Reference a config screen value by object identity.
	 *
	 * @param configValue config screen value to reference
	 * @return a config screen value reference for the given config screen value
	 *
	 * @since 0.1.0
	 */
	static IConfigScreenValueReference screenValue(IConfigScreenValue<?> configValue) {
		IConfigScreenValue<?> checkedConfigValue = Objects.requireNonNull(configValue, "configValue");
		return new IConfigScreenValueReference() {
			@Override
			public boolean matches(IConfigScreenValue<?> value) {
				return checkedConfigValue == value;
			}

			@Override
			public String toString() {
				return checkedConfigValue.getName();
			}
		};
	}

	/**
	 * Reference a config screen value by stable name.
	 * <p>
	 * This is useful for screen values that do not have a MezzConfig {@link IConfigValue} backing object, such as
	 * platform-native config values adapted for the config GUI.
	 *
	 * @param name config screen value name
	 * @return a config screen value reference for the given name
	 *
	 * @since 0.1.0
	 */
	static IConfigScreenValueReference named(String name) {
		String checkedName = Objects.requireNonNull(name, "name");
		if (checkedName.isBlank()) {
			throw new IllegalArgumentException("name must not be blank.");
		}
		return new IConfigScreenValueReference() {
			@Override
			public boolean matches(IConfigScreenValue<?> value) {
				return value.getName().equals(checkedName);
			}

			@Override
			public String toString() {
				return checkedName;
			}
		};
	}

	/**
	 * Returns true when this reference points to the given config value.
	 *
	 * @param value config screen value to test
	 * @return true when this reference points to the given config screen value
	 *
	 * @since 0.1.0
	 */
	boolean matches(IConfigScreenValue<?> value);
}
