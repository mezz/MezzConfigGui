package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.api.value.ConfigValueRestartRequirement;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

class ConfigScreenValueWithRestartRequirement<T> implements IConfigScreenValue<T> {
	private final IConfigScreenValue<T> configValue;
	private final ConfigValueRestartRequirement restartRequirement;

	public ConfigScreenValueWithRestartRequirement(
		IConfigScreenValue<T> configValue,
		ConfigValueRestartRequirement restartRequirement
	) {
		this.configValue = Objects.requireNonNull(configValue, "configValue");
		this.restartRequirement = Objects.requireNonNull(restartRequirement, "restartRequirement");
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
		return configValue.getApplyMode();
	}

	@Override
	public ConfigValueRestartRequirement getRestartRequirement() {
		return restartRequirement;
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
	public boolean equals(Object obj) {
		return configValue.equals(obj);
	}

	@Override
	public int hashCode() {
		return configValue.hashCode();
	}

	@Override
	public String toString() {
		return configValue.toString();
	}

	static final class Localized<T> extends ConfigScreenValueWithRestartRequirement<T> implements IConfigLocalizedValue {
		private final IConfigLocalizedValue localizedValue;

		public Localized(
			IConfigScreenValue<T> configValue,
			ConfigValueRestartRequirement restartRequirement,
			IConfigLocalizedValue localizedValue
		) {
			super(configValue, restartRequirement);
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
