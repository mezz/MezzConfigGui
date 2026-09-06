package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.editor.ConfigValueEditMode;
import net.mezzdev.config.api.value.editor.IConfigValueEditorInfo;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;

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
 * <p>
 * Values returned by this interface, passed to listeners, and accepted by {@link #set(Object)} must be effectively
 * immutable snapshots with stable equality. Mutable native config collections must never be exposed directly because
 * the GUI keeps values while edits are staged and while applied changes remain undoable.
 *
 * @param <T> the value type
 *
 * @since 0.1.0
 */
public interface IConfigScreenValue<T> {
	/**
	 * Adapt a MezzConfig-managed config value for display on a config screen.
	 * Restart-required config values display their pending saved value while their effective value remains fixed until the
	 * applicable lifecycle boundary.
	 *
	 * @since 0.1.0
	 */
	static <T> IConfigScreenValue<T> configValue(IConfigValue<T> configValue) {
		IConfigValue<T> checkedConfigValue = Objects.requireNonNull(configValue, "configValue");
		IConfigValueEditorInfo<T> editorInfo = Objects.requireNonNull(checkedConfigValue.getEditorInfo(), "configValue editorInfo");
		ConfigValueEditMode editMode = Objects.requireNonNull(editorInfo.getEditMode(), "configValue editMode");
		ConfigValueRestartRequirement restartRequirement = Objects.requireNonNull(editorInfo.getRestartRequirement(), "configValue restartRequirement");
		return configValue(checkedConfigValue, getApplyMode(editMode), restartRequirement);
	}

	/**
	 * Adapt a MezzConfig-managed config value for display on a config screen.
	 *
	 * @since 0.1.0
	 */
	static <T> IConfigScreenValue<T> configValue(IConfigValue<T> configValue, ConfigValueApplyMode applyMode) {
		IConfigValue<T> checkedConfigValue = Objects.requireNonNull(configValue, "configValue");
		IConfigValueEditorInfo<T> editorInfo = Objects.requireNonNull(checkedConfigValue.getEditorInfo(), "configValue editorInfo");
		ConfigValueRestartRequirement restartRequirement = Objects.requireNonNull(editorInfo.getRestartRequirement(), "configValue restartRequirement");
		return configValue(checkedConfigValue, applyMode, restartRequirement);
	}

	private static ConfigValueApplyMode getApplyMode(ConfigValueEditMode editMode) {
		return switch (editMode) {
			case IMMEDIATE -> ConfigValueApplyMode.IMMEDIATE;
			case BATCH -> ConfigValueApplyMode.ON_APPLY;
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
		ConfigValueRestartRequirement restartRequirement
	) {
		IConfigValue<T> checkedConfigValue = Objects.requireNonNull(configValue, "configValue");
		IConfigValueEditorInfo<T> editorInfo = Objects.requireNonNull(checkedConfigValue.getEditorInfo(), "configValue editorInfo");
		ConfigValueApplyMode checkedApplyMode = Objects.requireNonNull(applyMode, "applyMode");
		ConfigValueRestartRequirement checkedRestartRequirement = Objects.requireNonNull(restartRequirement, "restartRequirement");
		return new IConfigScreenValue<>() {
			@Override
			public String getName() {
				return editorInfo.getName();
			}

			@Override
			public String getLocalizationKey() {
				return editorInfo.getLocalizationKey();
			}

			@Override
			public T getValue() {
				return editorInfo.getPendingValue();
			}

			@Override
			public T getDefaultValue() {
				return editorInfo.getDefaultValue();
			}

			@Override
			public boolean set(T value) {
				return checkedConfigValue.set(value);
			}

			@Override
			public Runnable addListener(Consumer<T> listener) {
				return editorInfo.addPendingListener(change -> listener.accept(change.newValue()));
			}

			@Override
			public ConfigValueApplyMode getApplyMode() {
				return checkedApplyMode;
			}

			@Override
			public ConfigValueRestartRequirement getRestartRequirement() {
				return checkedRestartRequirement;
			}

			@Override
			public IConfigValueSerializer<T> getSerializer() {
				return editorInfo.getSerializer();
			}

			@Override
			public Optional<IConfigValue<T>> getConfigValue() {
				return Optional.of(checkedConfigValue);
			}

			@Override
			public Object getIdentityKey() {
				return checkedConfigValue;
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
	 * Override the restart or reload required after this config screen value is saved.
	 *
	 * @since 0.1.0
	 */
	static <T> IConfigScreenValue<T> withRestartRequirement(
		IConfigScreenValue<T> configValue,
		ConfigValueRestartRequirement restartRequirement
	) {
		IConfigScreenValue<T> checkedConfigValue = Objects.requireNonNull(configValue, "configValue");
		ConfigValueRestartRequirement checkedRestartRequirement = Objects.requireNonNull(restartRequirement, "restartRequirement");
		if (checkedConfigValue instanceof IConfigLocalizedValue localizedValue) {
			return new ConfigScreenValueWithRestartRequirement.Localized<>(checkedConfigValue, checkedRestartRequirement, localizedValue);
		}
		return new ConfigScreenValueWithRestartRequirement<>(checkedConfigValue, checkedRestartRequirement);
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
	 * <p>
	 * Returned values must be effectively immutable. In particular, collection-backed implementations must return a
	 * snapshot instead of exposing mutable native config storage.
	 *
	 * @since 0.1.0
	 */
	T getValue();

	/**
	 * Get the default value.
	 * <p>
	 * Returned values must be effectively immutable and have stable {@link Object#equals(Object)} behavior.
	 *
	 * @since 0.1.0
	 */
	T getDefaultValue();

	/**
	 * Set the value.
	 *
	 * @param value the new value
	 * @return {@code true} when the stored value changed, or {@code false} when the value was valid but unchanged
	 * @throws IllegalArgumentException when the value is invalid
	 * @throws RuntimeException when the value cannot be stored or saved
	 *
	 * @since 0.1.0
	 */
	boolean set(T value);

	/**
	 * Add a listener that is called with the new value when this config screen value changes.
	 * <p>
	 * Callbacks run synchronously on the thread applying or loading the change; implementations must not dispatch them
	 * to another thread. Implementations must notify listeners for successful changes made through {@link #set(Object)}
	 * and for external reload or change events exposed by their backing storage. The GUI subscribes while its screen is
	 * active and dispatches callbacks to the Minecraft client thread before updating widgets.
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
	 * Get the restart or reload required after this value is saved.
	 *
	 * @since 0.1.0
	 */
	default ConfigValueRestartRequirement getRestartRequirement() {
		return ConfigValueRestartRequirement.NONE;
	}

	/**
	 * Get the reference-stable identity key used to match and deduplicate this value.
	 * <p>
	 * Keys are compared by reference, not with {@link Object#equals(Object)}. Decorators must return the wrapped
	 * value's key. Values that share a key must represent the same logical value and type. The default gives direct
	 * implementations identity for the lifetime of the object.
	 *
	 * @since 0.1.0
	 */
	default Object getIdentityKey() {
		return this;
	}

	/**
	 * Get the helper for serializing values to and from Strings, and validating values.
	 *
	 * @since 0.1.0
	 */
	IConfigValueSerializer<T> getSerializer();

	/**
	 * Get the backing MezzConfig value, if this screen value adapts one.
	 * Platform-native and screen-only values should leave this empty. Value matching uses {@link #getIdentityKey()}.
	 *
	 * @since 0.1.0
	 */
	default Optional<IConfigValue<T>> getConfigValue() {
		return Optional.empty();
	}
}
