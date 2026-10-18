package net.mezzdev.config.gui.internal;

import net.mezzdev.config.api.value.color.PackedColor;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import net.mezzdev.config.api.value.serializer.IConfigListValueSerializer;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigRangeValueEditorMetadata;
import net.mezzdev.config.gui.api.IConfigRangeValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigListValueEditorOptions;
import net.mezzdev.config.gui.api.IConfigValueEditorMetadata;
import net.mezzdev.config.gui.api.IConfigValueEditorMetadataFactory;
import net.mezzdev.config.gui.api.IConfigValueEditorSerializer;
import net.minecraft.network.chat.Component;

public final class ConfigValueEditorMetadataFactory implements IConfigValueEditorMetadataFactory {
	public static final ConfigValueEditorMetadataFactory INSTANCE = new ConfigValueEditorMetadataFactory();

	private ConfigValueEditorMetadataFactory() {
	}

	public <T> IConfigValueEditorMetadata<T> create(IConfigValue<T> value) {
		var editorInfo = value.getEditorInfo();
		return create(editorInfo.getSerializer(), editorInfo.getPendingValue());
	}

	@Override
	@SuppressWarnings({"unchecked", "deprecation"})
	public <T> IConfigValueEditorMetadata<T> create(IConfigValueSerializer<T> serializer, T value) {
		if (serializer instanceof IConfigRangeValueEditorSerializer<?> rangeSerializer) {
			return (IConfigValueEditorMetadata<T>) createLegacyRange(rangeSerializer);
		}
		if (serializer instanceof IConfigValueEditorSerializer<?> editorSerializer) {
			return create(serializer, (ConfigValueEditorType<T>) editorSerializer.getEditorType());
		}
		if (serializer instanceof IConfigListValueSerializer<?> listSerializer) {
			return (IConfigValueEditorMetadata<T>) createList(listSerializer);
		}
		return create(serializer, (ConfigValueEditorType<T>) getEditorType(serializer, value));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> IConfigValueEditorMetadata<T> create(IConfigValueSerializer<T> serializer, ConfigValueEditorType<T> editorType) {
		if (editorType == ConfigValueEditorTypes.getList()) {
			if (serializer instanceof IConfigListValueSerializer<?> listSerializer) {
				return (IConfigValueEditorMetadata<T>) createList(listSerializer);
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

	@SuppressWarnings("deprecation")
	private static <T> ListValueEditorMetadata<T> createList(IConfigListValueSerializer<T> serializer) {
		boolean allowsRemoving = !(serializer instanceof IConfigListValueEditorOptions options) || options.allowsRemovingValues();
		return new ListValueEditorMetadata<>(serializer, allowsRemoving);
	}

	@SuppressWarnings("deprecation")
	private static <T> IConfigRangeValueEditorMetadata<T> createLegacyRange(IConfigRangeValueEditorSerializer<T> serializer) {
		return new RangeMetadata<>(serializer, serializer.getBounds()) {
			@Override
			public Component getLocalizedValueName(String localizationKey, ConfigValueRange<T> value) {
				return serializer.getLocalizedValueName(localizationKey, value);
			}
		};
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

	private static class RangeMetadata<T> extends ConfigValueEditorMetadata<ConfigValueRange<T>> implements IConfigRangeValueEditorMetadata<T> {
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
