package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

final class NeoForgeConfigValue<T> implements IConfigScreenValue<T>, IConfigLocalizedValue {
	private static final Logger LOGGER = LogManager.getLogger();

	private final String name;
	private final String localizationKey;
	private final Component localizedName;
	private final Component localizedDescription;
	private final ModConfig modConfig;
	private final ModConfigSpec modConfigSpec;
	private final ModConfigSpec.ConfigValue<T> configValue;
	private final T defaultValue;
	private final IConfigValueSerializer<T> serializer;
	private final ConfigValueRestartRequirement restartRequirement;
	@Nullable
	private List<Consumer<T>> listeners;
	@Nullable
	private T lastNotifiedValue;

	public NeoForgeConfigValue(
		String modId,
		ModConfig modConfig,
		ModConfigSpec modConfigSpec,
		ModConfigSpec.ConfigValue<T> configValue,
		ModConfigSpec.ValueSpec valueSpec,
		IConfigValueSerializer<T> serializer
	) {
		List<String> path = configValue.getPath();
		this.name = String.join(".", path);
		this.localizationKey = NeoForgeConfigLocalization.getValueLocalizationKey(modId, path, valueSpec.getTranslationKey());
		this.localizedName = NeoForgeConfigLocalization.getValueName(localizationKey, path);
		this.restartRequirement = getRestartRequirement(modConfig, valueSpec);
		this.localizedDescription = NeoForgeConfigLocalization.getValueDescription(localizationKey, valueSpec.getComment(), restartRequirement);
		this.modConfig = modConfig;
		this.modConfigSpec = modConfigSpec;
		this.configValue = configValue;
		this.defaultValue = snapshot(configValue.getDefault());
		this.serializer = serializer;
	}

	private static ConfigValueRestartRequirement getRestartRequirement(
		ModConfig modConfig,
		ModConfigSpec.ValueSpec valueSpec
	) {
		if (modConfig.getType() == ModConfig.Type.STARTUP) {
			return ConfigValueRestartRequirement.GAME_RESTART;
		}
		return switch (valueSpec.restartType()) {
			case NONE -> ConfigValueRestartRequirement.NONE;
			case WORLD -> ConfigValueRestartRequirement.WORLD_RESTART;
			case GAME -> ConfigValueRestartRequirement.GAME_RESTART;
		};
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getLocalizationKey() {
		return localizationKey;
	}

	@Override
	public Component getLocalizedName() {
		return localizedName;
	}

	@Override
	public Component getLocalizedDescription() {
		return localizedDescription;
	}

	@Override
	public T getValue() {
		return snapshot(configValue.getRaw());
	}

	@Override
	public T getDefaultValue() {
		return defaultValue;
	}

	@Override
	public boolean set(T value) {
		if (value == null || !serializer.isValid(value)) {
			throw new IllegalArgumentException(
				"Invalid NeoForge config value '%s'. %s".formatted(value, serializer.getValidValuesDescription())
			);
		}
		T valueSnapshot = snapshot(value);
		if (Objects.equals(getValue(), valueSnapshot)) {
			return false;
		}
		configValue.set(valueSnapshot);
		modConfigSpec.save();
		notifyListeners(valueSnapshot);
		return true;
	}

	@SuppressWarnings("unchecked")
	private static <T> T snapshot(T value) {
		if (value instanceof List<?> list) {
			return (T) List.copyOf(list);
		}
		return value;
	}

	@Override
	public Runnable addListener(Consumer<T> listener) {
		Consumer<T> checkedListener = Objects.requireNonNull(listener, "listener");
		synchronized (this) {
			if (listeners == null) {
				listeners = new ArrayList<>();
				lastNotifiedValue = getValue();
				NeoForgeConfigValueReloads.register(modConfig, this);
			}
			listeners.add(checkedListener);
		}
		return () -> removeListener(checkedListener);
	}

	private void notifyListeners(T value) {
		T valueSnapshot = snapshot(value);
		List<Consumer<T>> listeners;
		synchronized (this) {
			if (this.listeners == null || Objects.equals(lastNotifiedValue, valueSnapshot)) {
				return;
			}
			lastNotifiedValue = valueSnapshot;
			listeners = List.copyOf(this.listeners);
		}
		for (Consumer<T> listener : listeners) {
			try {
				listener.accept(valueSnapshot);
			} catch (RuntimeException exception) {
				LOGGER.error("NeoForge config value listener failed for {}.", name, exception);
			}
		}
	}

	private void removeListener(Consumer<T> listener) {
		boolean unregister = false;
		synchronized (this) {
			if (listeners != null) {
				listeners.remove(listener);
				if (listeners.isEmpty()) {
					listeners = null;
					lastNotifiedValue = null;
					unregister = true;
				}
			}
		}
		if (unregister) {
			NeoForgeConfigValueReloads.unregister(modConfig, this);
		}
	}

	void onConfigReloaded() {
		notifyListeners(getValue());
	}

	@Override
	public ConfigValueApplyMode getApplyMode() {
		return ConfigValueApplyMode.ON_APPLY;
	}

	@Override
	public ConfigValueRestartRequirement getRestartRequirement() {
		return restartRequirement;
	}

	@Override
	public IConfigValueSerializer<T> getSerializer() {
		return serializer;
	}
}
