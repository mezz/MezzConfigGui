package net.mezzdev.config.gui;

import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditorMetadata;

/** Creates screen values for MezzConfig values using their serializer's editor metadata. */
public final class ConfigScreenValues {
	private ConfigScreenValues() {
	}

	public static <T> IConfigScreenValue<T> create(IConfigValue<T> value) {
		return IConfigScreenValue.configValue(value, createMetadata(value));
	}

	public static <T> IConfigScreenValue<T> create(IConfigValue<T> value, ConfigValueApplyMode applyMode) {
		return IConfigScreenValue.configValue(value, createMetadata(value), applyMode);
	}

	public static <T> IConfigScreenValue<T> create(IConfigValue<T> value, ConfigValueApplyMode applyMode, ConfigValueRestartRequirement restartRequirement) {
		return IConfigScreenValue.configValue(value, createMetadata(value), applyMode, restartRequirement);
	}

	private static <T> IConfigValueEditorMetadata<T> createMetadata(IConfigValue<T> value) {
		var editorInfo = value.getEditorInfo();
		return ConfigValueEditorMetadataFactory.INSTANCE.create(editorInfo.getSerializer(), editorInfo.getPendingValue());
	}
}
