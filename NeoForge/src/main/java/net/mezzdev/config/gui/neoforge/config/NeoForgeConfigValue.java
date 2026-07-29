package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.ConfigValueChange;
import net.mezzdev.config.api.value.ConfigValueUpdateType;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.IConfigValueEditorSerializer;
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

final class NeoForgeConfigValue<T> implements IConfigValue<T> {
	private static final Logger LOGGER = LogManager.getLogger();

	private final String name;
	private final String localizationKey;
	private final Component localizedName;
	private final Component localizedDescription;
	private final ModConfigSpec modConfigSpec;
	private final ModConfigSpec.ConfigValue<T> configValue;
	private final T defaultValue;
	private final IConfigValueEditorSerializer<T> serializer;
	private final ConfigValueUpdateType updateType;
	@Nullable
	private List<Consumer<T>> listeners;

	public NeoForgeConfigValue(
		String modId,
		ModConfig modConfig,
		ModConfigSpec modConfigSpec,
		ModConfigSpec.ConfigValue<T> configValue,
		ModConfigSpec.ValueSpec valueSpec,
		IConfigValueEditorSerializer<T> serializer
	) {
		List<String> path = configValue.getPath();
		this.name = String.join(".", path);
		this.localizationKey = NeoForgeConfigLocalization.getValueLocalizationKey(modId, path, valueSpec.getTranslationKey());
		this.localizedName = NeoForgeConfigLocalization.getValueName(localizationKey, path);
		this.localizedDescription = NeoForgeConfigLocalization.getValueDescription(localizationKey, valueSpec.getComment());
		this.modConfigSpec = modConfigSpec;
		this.configValue = configValue;
		this.defaultValue = configValue.getDefault();
		this.serializer = serializer;
		this.updateType = getUpdateType(modConfig, valueSpec);
	}

	private static ConfigValueUpdateType getUpdateType(ModConfig modConfig, ModConfigSpec.ValueSpec valueSpec) {
		if (modConfig.getType() == ModConfig.Type.STARTUP || valueSpec.restartType() != ModConfigSpec.RestartType.NONE) {
			return ConfigValueUpdateType.RESTART;
		}
		return ConfigValueUpdateType.ON_APPLY;
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
		return configValue.getRaw();
	}

	@Override
	public T getDefaultValue() {
		return defaultValue;
	}

	@Override
	public boolean set(T value) {
		if (!serializer.isValid(value)) {
			LOGGER.error("Tried to set invalid NeoForge config value: {}\n{}", value, serializer.getValidValuesDescription());
			return false;
		}
		if (Objects.equals(getValue(), value)) {
			return false;
		}
		configValue.set(value);
		notifyListeners(value);
		return true;
	}

	public boolean apply(ConfigValueChange<?> change) {
		return apply(change.value());
	}

	@SuppressWarnings("unchecked")
	private boolean apply(Object value) {
		return set((T) value);
	}

	public ModConfigSpec getModConfigSpec() {
		return modConfigSpec;
	}

	boolean matches(ModConfigSpec.ConfigValue<?> configValue) {
		return this.configValue == configValue;
	}

	@Override
	public void addListener(Consumer<T> listener) {
		if (listeners == null) {
			listeners = new ArrayList<>();
		}
		listeners.add(listener);
	}

	private void notifyListeners(T value) {
		if (listeners != null) {
			listeners.forEach(listener -> listener.accept(value));
		}
	}

	@Override
	public ConfigValueUpdateType getUpdateType() {
		return updateType;
	}

	@Override
	public IConfigValueEditorSerializer<T> getSerializer() {
		return serializer;
	}
}
