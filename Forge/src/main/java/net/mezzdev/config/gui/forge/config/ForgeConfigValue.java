package net.mezzdev.config.gui.forge.config;

import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.ConfigValueAccess;
import net.mezzdev.config.gui.info.ServerConfigAccess;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class ForgeConfigValue<T> implements IConfigScreenValue<T>, IConfigLocalizedValue, ConfigValueAccess {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Supplier<ServerConfigAccess> SERVER_ACCESS = () -> {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.getSingleplayerServer() != null) {
			return ServerConfigAccess.LOCAL;
		}
		if (minecraft.getConnection() == null) {
			return ServerConfigAccess.UNAVAILABLE;
		}
		return ServerConfigAccess.READ_ONLY;
	};

	private final String name;
	private final String localizationKey;
	private final Component localizedName;
	private final Component localizedDescription;
	private final ModConfig modConfig;
	private final ForgeConfigSpec modConfigSpec;
	private final ForgeConfigSpec.ConfigValue<T> configValue;
	private final T defaultValue;
	private final IConfigValueSerializer<T> serializer;
	private final ForgeConfigSpec.ValueSpec valueSpec;
	private final ConfigValueRestartRequirement restartRequirement;
	@Nullable
	private List<Consumer<T>> listeners;
	@Nullable
	private T lastNotifiedValue;

	public ForgeConfigValue(
		String modId,
		ModConfig modConfig,
		ForgeConfigSpec modConfigSpec,
		ForgeConfigSpec.ConfigValue<T> configValue,
		ForgeConfigSpec.ValueSpec valueSpec,
		IConfigValueSerializer<T> serializer
	) {
		List<String> path = configValue.getPath();
		this.name = String.join(".", path);
		this.localizationKey = ForgeConfigLocalization.getValueLocalizationKey(modId, path, valueSpec.getTranslationKey());
		this.localizedName = ForgeConfigLocalization.getValueName(localizationKey, path);
		this.restartRequirement = getRestartRequirement(valueSpec);
		this.localizedDescription = ForgeConfigLocalization.getValueDescription(localizationKey, valueSpec);
		this.modConfig = modConfig;
		this.modConfigSpec = modConfigSpec;
		this.configValue = configValue;
		this.defaultValue = snapshot(configValue.getDefault());
		this.serializer = serializer;
		this.valueSpec = valueSpec;
	}

	static ConfigValueRestartRequirement getRestartRequirement(ForgeConfigSpec.ValueSpec valueSpec) {
		if (valueSpec.needsWorldRestart()) {
			return ConfigValueRestartRequirement.WORLD_RESTART;
		}
		return ConfigValueRestartRequirement.NONE;
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
	public Optional<Supplier<ServerConfigAccess>> getServerAccess() {
		if (modConfig.getType() == ModConfig.Type.SERVER) {
			return Optional.of(SERVER_ACCESS);
		}
		return Optional.empty();
	}

	@Override
	public boolean isEditable() {
		if (modConfig.getType() != ModConfig.Type.SERVER) {
			return true;
		}
		return SERVER_ACCESS.get() == ServerConfigAccess.LOCAL;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T getValue() {
		T value = configValue.get();
		if (value instanceof List<?> list && serializer instanceof ForgeListSerializer<?> listSerializer) {
			return (T) listSerializer.normalize(list);
		}
		return snapshot(value);
	}

	@Override
	public T getDefaultValue() {
		return defaultValue;
	}

	@Override
	public boolean set(@Nullable T value) {
		if (!isEditable()) {
			throw new IllegalStateException("Native Forge multiplayer server configs cannot be edited from this client.");
		}
		if (value == null || !serializer.isValid(value) || !valueSpec.test(value)) {
			throw new IllegalArgumentException(
				"Invalid Forge config value '%s'. %s".formatted(value, serializer.getValidValuesDescription())
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
				ForgeConfigValueReloads.register(modConfig, this);
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
				LOGGER.error("Forge config value listener failed for {}.", name, exception);
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
			ForgeConfigValueReloads.unregister(modConfig, this);
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
