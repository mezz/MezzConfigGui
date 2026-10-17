package net.mezzdev.config.gui;

import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.api.IConfigValueEditorMetadata;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/** Standard editor metadata derived from a value's storage constraints. */
public class ConfigValueEditorMetadata<T> implements IConfigValueEditorMetadata<T> {
	private final IConfigValueSerializer<T> serializer;
	private final ConfigValueEditorType<T> editorType;

	public ConfigValueEditorMetadata(IConfigValueSerializer<T> serializer, ConfigValueEditorType<T> editorType) {
		this.serializer = serializer;
		this.editorType = editorType;
	}

	@Override
	public ConfigValueEditorType<T> getEditorType() {
		return editorType;
	}

	@Override
	public boolean isValid(T value) {
		return serializer.isValid(value);
	}

	@Override
	public Optional<List<T>> getAllValidValues() {
		return serializer.getAllValidValues();
	}

	@Override
	public Component getLocalizedValueName(String localizationKey, T value) {
		return ConfigValueLocalization.getValueName(serializer, localizationKey, value);
	}

	@Override
	public Optional<Component> getLocalizedValueDescription(String localizationKey, T value) {
		return ConfigValueLocalization.getValueDescription(localizationKey, value);
	}
}
