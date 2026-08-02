package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditorSerializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Adapts key mapping metadata into a config value for the config screen.
 */
public final class KeyMappingConfigValue implements IConfigScreenValue<KeyMappingValue>, IConfigLocalizedValue {
	private final IConfigKeyMapping configKeyMapping;
	private final KeyMappingSerializer serializer;

	KeyMappingConfigValue(IConfigKeyMapping configKeyMapping) {
		this.configKeyMapping = configKeyMapping;
		this.serializer = new KeyMappingSerializer(createValue(configKeyMapping.getDefaultValue()));
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
	public boolean set(KeyMappingValue value) {
		ConfigKeyBinding binding = configKeyMapping.normalize(value.binding());
		if (getValue().binding().equals(binding)) {
			return false;
		}
		configKeyMapping.set(binding);
		KeyMapping.resetMapping();
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.options.save();
		return true;
	}

	@Override
	public void addListener(Consumer<KeyMappingValue> listener) {

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
