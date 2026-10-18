package net.mezzdev.config.gui;

import net.mezzdev.config.api.value.color.PackedColor;
import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import net.mezzdev.config.api.value.serializer.IConfigListValueSerializer;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigRangeValueEditorMetadata;
import net.mezzdev.config.gui.api.IConfigValueEditorMetadata;
import net.mezzdev.config.gui.api.IConfigValueEditorMetadataFactory;
import net.minecraft.network.chat.Component;

public final class ConfigValueEditorMetadataFactory implements IConfigValueEditorMetadataFactory {
	public static final ConfigValueEditorMetadataFactory INSTANCE = new ConfigValueEditorMetadataFactory();

	private ConfigValueEditorMetadataFactory() {
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> IConfigValueEditorMetadata<T> create(IConfigValueSerializer<T> serializer, T value) {
		if (serializer instanceof IConfigListValueSerializer<?> listSerializer) {
			return (IConfigValueEditorMetadata<T>) new ListValueEditorMetadata<>(listSerializer, true);
		}
		return create(serializer, (ConfigValueEditorType<T>) getEditorType(serializer, value));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> IConfigValueEditorMetadata<T> create(IConfigValueSerializer<T> serializer, ConfigValueEditorType<T> editorType) {
		if (editorType == ConfigValueEditorTypes.getList()) {
			if (serializer instanceof IConfigListValueSerializer<?> listSerializer) {
				return (IConfigValueEditorMetadata<T>) new ListValueEditorMetadata<>(listSerializer, true);
			}
			throw new IllegalArgumentException("List metadata requires an IConfigListValueSerializer.");
		}
		if (editorType == ConfigValueEditorTypes.getRange()) {
			throw new IllegalArgumentException("Range metadata requires bounds; use createRange.");
		}
		return new ConfigValueEditorMetadata<>(serializer, editorType);
	}

	@Override
	public <T> IConfigRangeValueEditorMetadata<T> createRange(IConfigValueSerializer<ConfigValueRange<T>> serializer, ConfigValueRange<T> bounds) {
		return new RangeMetadata<>(serializer, bounds);
	}

	private static ConfigValueEditorType<?> getEditorType(IConfigValueSerializer<?> serializer, Object value) {
		if (value instanceof PackedColor) {
			return ConfigValueEditorTypes.COLOR;
		}
		if (value instanceof Integer && serializer.getRange().isPresent()) {
			return ConfigValueEditorTypes.INTEGER;
		}
		if (value instanceof Boolean) {
			return ConfigValueEditorTypes.BOOLEAN;
		}
		if (serializer.getAllValidValues().isPresent()) {
			return ConfigValueEditorTypes.getSelection();
		}
		return ConfigValueEditorTypes.getText();
	}

	private static final class RangeMetadata<T> extends ConfigValueEditorMetadata<ConfigValueRange<T>> implements IConfigRangeValueEditorMetadata<T> {
		private final ConfigValueRange<T> bounds;

		private RangeMetadata(IConfigValueSerializer<ConfigValueRange<T>> serializer, ConfigValueRange<T> bounds) {
			super(serializer, ConfigValueEditorTypes.getRange());
			this.bounds = bounds;
		}

		@Override
		public ConfigValueRange<T> getBounds() {
			return bounds;
		}

		@Override
		public Component getLocalizedValueName(String localizationKey, ConfigValueRange<T> value) {
			return Component.translatableWithFallback("mezz_config_gui.config.screen.range.value", "%s – %s", value.min(), value.max());
		}
	}
}
