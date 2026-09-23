package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditorSerializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Adapts key mapping metadata into a config value for the config screen.
 */
public final class KeyMappingConfigValue implements IConfigScreenValue<KeyMappingValue>, IConfigLocalizedValue {
	private static final Logger LOGGER = LogManager.getLogger();

	private final IConfigKeyMapping configKeyMapping;
	private final KeyMappingSerializer serializer;
	private final Runnable saveHandler;
	private final List<Consumer<KeyMappingValue>> listeners = new ArrayList<>();

	KeyMappingConfigValue(IConfigKeyMapping configKeyMapping) {
		this(configKeyMapping, KeyMappingConfigValue::saveKeyMappings);
	}

	KeyMappingConfigValue(IConfigKeyMapping configKeyMapping, Runnable saveHandler) {
		this.configKeyMapping = Objects.requireNonNull(configKeyMapping, "configKeyMapping");
		this.serializer = new KeyMappingSerializer(createValue(configKeyMapping.getDefaultValue()));
		this.saveHandler = Objects.requireNonNull(saveHandler, "saveHandler");
	}

	@Override
	public String getName() {
		return configKeyMapping.getName();
	}

	@Override
	public String getLocalizationKey() {
		return configKeyMapping.getName();
	}

	@Override
	public Component getLocalizedName() {
		return configKeyMapping.getLocalizedName();
	}

	@Override
	public Component getLocalizedDescription() {
		return configKeyMapping.getLocalizedDescription();
	}

	@Override
	public KeyMappingValue getValue() {
		return createValue(configKeyMapping.getValue());
	}

	@Override
	public KeyMappingValue getDefaultValue() {
		return createValue(configKeyMapping.getDefaultValue());
	}

	@Override
	public boolean set(@Nullable KeyMappingValue value) {
		if (!serializer.isValid(value)) {
			throw new IllegalArgumentException("Invalid key mapping value: " + value);
		}
		KeyMappingValue checkedValue = Objects.requireNonNull(value);
		ConfigKeyBinding binding = configKeyMapping.normalize(checkedValue.binding());
		if (getValue().binding().equals(binding)) {
			return false;
		}
		configKeyMapping.set(binding);
		saveHandler.run();
		notifyListeners(createValue(configKeyMapping.getValue()));
		return true;
	}

	private static void saveKeyMappings() {
		KeyMapping.resetMapping();
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.options.save();
	}

	@Override
	public Runnable addListener(Consumer<KeyMappingValue> listener) {
		Consumer<KeyMappingValue> checkedListener = Objects.requireNonNull(listener, "listener");
		listeners.add(checkedListener);
		return () -> listeners.remove(checkedListener);
	}

	private void notifyListeners(KeyMappingValue value) {
		for (Consumer<KeyMappingValue> listener : List.copyOf(listeners)) {
			try {
				listener.accept(value);
			} catch (RuntimeException exception) {
				LOGGER.error("Key mapping config value listener failed for {}.", getName(), exception);
			}
		}
	}

	@Override
	public ConfigValueApplyMode getApplyMode() {
		return ConfigValueApplyMode.IMMEDIATE;
	}

	@Override
	public IConfigValueEditorSerializer<KeyMappingValue> getSerializer() {
		return serializer;
	}

	private KeyMappingValue createValue(ConfigKeyBinding binding) {
		return new KeyMappingValue(binding, configKeyMapping);
	}
}
