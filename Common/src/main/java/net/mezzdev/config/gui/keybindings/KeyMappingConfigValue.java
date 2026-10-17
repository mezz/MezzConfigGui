package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingBinding;
import net.mezzdev.config.gui.api.IConfigValueEditorMetadata;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Adapts key mapping metadata into a config value for the config screen.
 */
public final class KeyMappingConfigValue implements IConfigScreenValue<KeyMappingValue>, IConfigLocalizedValue, IConfigValueEditorMetadata<KeyMappingValue> {
	private static final Logger LOGGER = LogManager.getLogger();

	private final IConfigKeyMapping configKeyMapping;
	private final Runnable saveHandler;
	private final List<Consumer<KeyMappingValue>> listeners = new ArrayList<>();

	KeyMappingConfigValue(IConfigKeyMapping configKeyMapping) {
		this(configKeyMapping, KeyMappingConfigValue::saveKeyMappings);
	}

	KeyMappingConfigValue(IConfigKeyMapping configKeyMapping, Runnable saveHandler) {
		this.configKeyMapping = Objects.requireNonNull(configKeyMapping, "configKeyMapping");
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
		if (!isValid(value)) {
			throw new IllegalArgumentException("Invalid key mapping value: " + value);
		}
		KeyMappingValue checkedValue = Objects.requireNonNull(value);
		IKeyMappingBinding binding = checkedValue.binding();
		IKeyMappingBinding original = configKeyMapping.getValue();
		if (original.equals(binding)) {
			return false;
		}
		configKeyMapping.set(binding);
		IKeyMappingBinding applied = configKeyMapping.getValue();
		if (original.equals(applied)) {
			return false;
		}
		saveHandler.run();
		notifyListeners(createValue(applied));
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
	public IConfigValueEditorMetadata<KeyMappingValue> getEditorMetadata() {
		return this;
	}

	@Override
	public ConfigValueEditorType<KeyMappingValue> getEditorType() {
		return ConfigValueEditorTypes.getKeyMapping();
	}

	@Override
	public boolean isValid(@Nullable KeyMappingValue value) {
		return value != null;
	}

	@Override
	public Optional<List<KeyMappingValue>> getAllValidValues() {
		return Optional.empty();
	}

	@Override
	public Component getLocalizedValueName(String configValueLocalizationKey, KeyMappingValue value) {
		return configKeyMapping.getValueName(value.binding());
	}

	private KeyMappingValue createValue(IKeyMappingBinding binding) {
		return new KeyMappingValue(binding, configKeyMapping);
	}
}
