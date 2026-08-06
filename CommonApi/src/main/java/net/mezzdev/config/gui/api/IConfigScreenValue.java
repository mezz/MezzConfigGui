package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.ConfigValueEditMode;
import net.mezzdev.config.api.value.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.IConfigValueSerializer;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Represents a value shown on a config screen.
 * <p>
 * MezzConfig-managed values can be adapted with {@link #configValue(IConfigValue)}. Screen-only values can implement
 * this interface directly without implementing MezzConfig's non-extendable {@link IConfigValue}. Platform-native
 * config adapters, such as the native NeoForge config adapter, should also implement this interface directly and use
 * the {@code ByName} methods on {@link IConfigScreenCategoryBuilder} for GUI customization.
 *
 * @param <T> the value type
 *
 * @since 0.1.0
 */
public interface IConfigScreenValue<T> {
	/**
	 * Adapt a MezzConfig-managed config value for display on a config screen.
	 *
	 * @since 0.1.0
	 */
	static <T> IConfigScreenValue<T> configValue(IConfigValue<T> configValue) {
		IConfigValue<T> checkedConfigValue = Objects.requireNonNull(configValue, "configValue");
		ConfigValueEditMode editMode = Objects.requireNonNull(checkedConfigValue.getEditMode(), "configValue editMode");
		ConfigValueRestartRequirement restartRequirement = Objects.requireNonNull(checkedConfigValue.getRestartRequirement(), "configValue restartRequirement");
		return configValue(checkedConfigValue, getApplyMode(editMode), requiresRestart(restartRequirement));
	}

	/**
	 * Adapt a MezzConfig-managed config value for display on a config screen.
	 *
	 * @since 0.1.0
	 */
	static <T> IConfigScreenValue<T> configValue(IConfigValue<T> configValue, ConfigValueApplyMode applyMode) {
		IConfigValue<T> checkedConfigValue = Objects.requireNonNull(configValue, "configValue");
		ConfigValueRestartRequirement restartRequirement = Objects.requireNonNull(checkedConfigValue.getRestartRequirement(), "configValue restartRequirement");
		return configValue(checkedConfigValue, applyMode, requiresRestart(restartRequirement));
	}

	private static ConfigValueApplyMode getApplyMode(ConfigValueEditMode editMode) {
		return switch (editMode) {
			case IMMEDIATE -> ConfigValueApplyMode.IMMEDIATE;
			case BATCH -> ConfigValueApplyMode.ON_APPLY;
		};
	}

	private static boolean requiresRestart(ConfigValueRestartRequirement restartRequirement) {
		return switch (restartRequirement) {
			case NONE -> false;
			case WORLD_RESTART, GAME_RESTART -> true;
		};
	}

	/**
	 * Adapt a MezzConfig-managed config value for display on a config screen.
	 *
	 * @since 0.1.0
	 */
	static <T> IConfigScreenValue<T> configValue(
		IConfigValue<T> configValue,
		ConfigValueApplyMode applyMode,
		boolean requiresRestart
	) {
		IConfigValue<T> checkedConfigValue = Objects.requireNonNull(configValue, "configValue");
		ConfigValueApplyMode checkedApplyMode = Objects.requireNonNull(applyMode, "applyMode");
		return new IConfigScreenValue<>() {
			@Override
			public String getName() {
				return checkedConfigValue.getName();
			}

			@Override
			public String getLocalizationKey() {
				return checkedConfigValue.getLocalizationKey();
			}

			@Override
			public T getValue() {
				return checkedConfigValue.getValue();
			}

			@Override
			public T getDefaultValue() {
				return checkedConfigValue.getDefaultValue();
			}

			@Override
			public boolean set(T value) {
				return checkedConfigValue.set(value);
			}

			@Override
			public Runnable addListener(Consumer<T> listener) {
				return checkedConfigValue.addListener(change -> listener.accept(change.newValue()));
			}

			@Override
			public ConfigValueApplyMode getApplyMode() {
				return checkedApplyMode;
			}

			@Override
			public boolean requiresRestart() {
				return requiresRestart;
			}

			@Override
			public IConfigValueSerializer<T> getSerializer() {
				return checkedConfigValue.getSerializer();
			}

			@Override
			public Optional<IConfigValue<T>> getConfigValue() {
				return Optional.of(checkedConfigValue);
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj) {
					return true;
				}
				if (obj instanceof IConfigScreenValue<?> other) {
					return other.getConfigValue()
						.filter(configValue -> configValue == checkedConfigValue)
						.isPresent();
				}
				return false;
			}

			@Override
			public int hashCode() {
				return System.identityHashCode(checkedConfigValue);
			}

			@Override
			public String toString() {
				return checkedConfigValue.toString();
			}
		};
	}

	/**
	 * Override when the config GUI saves edits for one config screen value.
	 *
	 * @since 0.1.0
	 */
	static <T> IConfigScreenValue<T> withApplyMode(IConfigScreenValue<T> configValue, ConfigValueApplyMode applyMode) {
		IConfigScreenValue<T> checkedConfigValue = Objects.requireNonNull(configValue, "configValue");
		ConfigValueApplyMode checkedApplyMode = Objects.requireNonNull(applyMode, "applyMode");
		if (checkedConfigValue instanceof IConfigLocalizedValue localizedValue) {
			return new ConfigScreenValueWithApplyMode.Localized<>(checkedConfigValue, checkedApplyMode, localizedValue);
		}
		return new ConfigScreenValueWithApplyMode<>(checkedConfigValue, checkedApplyMode);
	}

	/**
	 * Override whether this config screen value needs a restart or larger reload after it is saved.
	 *
	 * @since 0.1.0
	 */
	static <T> IConfigScreenValue<T> withRestartRequirement(IConfigScreenValue<T> configValue, boolean requiresRestart) {
		IConfigScreenValue<T> checkedConfigValue = Objects.requireNonNull(configValue, "configValue");
		if (checkedConfigValue instanceof IConfigLocalizedValue localizedValue) {
			return new ConfigScreenValueWithRestartRequirement.Localized<>(checkedConfigValue, requiresRestart, localizedValue);
		}
		return new ConfigScreenValueWithRestartRequirement<>(checkedConfigValue, requiresRestart);
	}

	/**
	 * Get the stable name of this config screen value.
	 *
	 * @since 0.1.0
	 */
	String getName();

	/**
	 * Get the translation key used for this config screen value's name.
	 *
	 * @since 0.1.0
	 */
	String getLocalizationKey();

	/**
	 * Get the current value.
	 *
	 * @since 0.1.0
	 */
	T getValue();

	/**
	 * Get the default value.
	 *
	 * @since 0.1.0
	 */
	T getDefaultValue();

	/**
	 * Set the value.
	 *
	 * @since 0.1.0
	 */
	boolean set(T value);

	/**
	 * Add a listener that is called with the new value when this config screen value changes.
	 *
	 * @return a callback that removes this listener
	 *
	 * @since 0.1.0
	 */
	Runnable addListener(Consumer<T> listener);

	/**
	 * Get when the config GUI should save edits for this value.
	 *
	 * @since 0.1.0
	 */
	default ConfigValueApplyMode getApplyMode() {
		return ConfigValueApplyMode.ON_APPLY;
	}

	/**
	 * Return true if changes to this value need a restart or larger reload before they fully take effect.
	 *
	 * @since 0.1.0
	 */
	default boolean requiresRestart() {
		return false;
	}

	/**
	 * Get the helper for serializing values to and from Strings, and validating values.
	 *
	 * @since 0.1.0
	 */
	IConfigValueSerializer<T> getSerializer();

	/**
	 * Get the backing MezzConfig value, if this screen value adapts one.
	 * <p>
	 * Platform-native values should leave this empty and provide their own identity through stable names or
	 * platform-specific reference helpers.
	 *
	 * @since 0.1.0
	 */
	default Optional<IConfigValue<T>> getConfigValue() {
		return Optional.empty();
	}
}
