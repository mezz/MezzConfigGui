package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.api.value.ConfigValueRestartRequirement;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

class ConfigScreenValueWithApplyMode<T> implements IConfigScreenValue<T> {
	private final IConfigScreenValue<T> configValue;
	private final ConfigValueApplyMode applyMode;

	public ConfigScreenValueWithApplyMode(IConfigScreenValue<T> configValue, ConfigValueApplyMode applyMode) {
		this.configValue = Objects.requireNonNull(configValue, "configValue");
		this.applyMode = Objects.requireNonNull(applyMode, "applyMode");
	}

	@Override
	public String getName() {
		return configValue.getName();
	}

	@Override
	public String getLocalizationKey() {
		return configValue.getLocalizationKey();
	}

	@Override
	public T getValue() {
		return configValue.getValue();
	}

	@Override
	public T getDefaultValue() {
		return configValue.getDefaultValue();
	}

	@Override
	public boolean set(T value) {
		return configValue.set(value);
	}

	@Override
	public Runnable addListener(Consumer<T> listener) {
		return configValue.addListener(listener);
	}

	@Override
	public ConfigValueApplyMode getApplyMode() {
		return applyMode;
	}

	@Override
	public ConfigValueRestartRequirement getRestartRequirement() {
		return configValue.getRestartRequirement();
	}

	@Override
	public Object getIdentityKey() {
		return configValue.getIdentityKey();
	}

	@Override
	public IConfigValueSerializer<T> getSerializer() {
		return configValue.getSerializer();
	}

	@Override
	public Optional<IConfigValue<T>> getConfigValue() {
		return configValue.getConfigValue();
	}

	@Override
	public String toString() {
		return configValue.toString();
	}

	static final class Localized<T> extends ConfigScreenValueWithApplyMode<T> implements IConfigLocalizedValue {
		private final IConfigLocalizedValue localizedValue;

		public Localized(
			IConfigScreenValue<T> configValue,
			ConfigValueApplyMode applyMode,
			IConfigLocalizedValue localizedValue
		) {
			super(configValue, applyMode);
			this.localizedValue = Objects.requireNonNull(localizedValue, "localizedValue");
		}

		@Override
		public Component getLocalizedName() {
			return localizedValue.getLocalizedName();
		}

		@Override
		public Component getLocalizedDescription() {
			return localizedValue.getLocalizedDescription();
		}
	}
}
