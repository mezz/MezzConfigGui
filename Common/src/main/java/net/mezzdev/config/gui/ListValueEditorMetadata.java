package net.mezzdev.config.gui;

import net.mezzdev.config.api.value.serializer.IConfigListValueSerializer;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigListValueEditorMetadata;
import net.mezzdev.config.gui.api.IConfigValueEditorMetadata;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/** Standard list presentation with metadata for each element. */
public class ListValueEditorMetadata<T> extends ConfigValueEditorMetadata<List<T>> implements IConfigListValueEditorMetadata<T> {
	private final IConfigListValueSerializer<T> serializer;
	private final boolean allowsRemovingValues;

	public ListValueEditorMetadata(IConfigListValueSerializer<T> serializer, boolean allowsRemovingValues) {
		super(serializer, ConfigValueEditorTypes.getList());
		this.serializer = serializer;
		this.allowsRemovingValues = allowsRemovingValues;
	}

	@Override
	public IConfigValueEditorMetadata<T> getElementMetadata(T value) {
		return ConfigValueEditorMetadataFactory.INSTANCE.create(serializer.getElementSerializer(), value);
	}

	@Override
	public boolean allowsRemovingValues() {
		return allowsRemovingValues;
	}

	@Override
	public Component getLocalizedValueName(String localizationKey, List<T> values) {
		if (values.isEmpty()) {
			return Component.translatable("mezz_config_gui.config.value.list.empty");
		}
		MutableComponent result = Component.empty();
		for (int i = 0; i < values.size(); i++) {
			T value = values.get(i);
			if (i > 0) {
				result.append(", ");
			}
			result.append(getElementMetadata(value).getLocalizedValueName(localizationKey, value));
		}
		return result;
	}
}
