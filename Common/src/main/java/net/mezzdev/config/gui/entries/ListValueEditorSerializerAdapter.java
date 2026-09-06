package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.ConfigListOrdering;
import net.mezzdev.config.api.value.serializer.IConfigListValueSerializer;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigValueLocalizationProvider;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

final class ListValueEditorSerializerAdapter<T> implements IConfigListValueEditorSerializer<T> {
	private final IConfigValueSerializer<List<T>> listSerializer;
	private final IConfigValueSerializer<T> elementSerializer;

	public ListValueEditorSerializerAdapter(
		IConfigValueSerializer<List<T>> listSerializer,
		IConfigValueSerializer<T> elementSerializer
	) {
		this.listSerializer = listSerializer;
		this.elementSerializer = elementSerializer;
	}

	@Override
	public IConfigValueSerializer<T> getElementSerializer() {
		return elementSerializer;
	}

	@Override
	public ConfigListOrdering getOrdering() {
		if (listSerializer instanceof IConfigListValueSerializer<?> typedListSerializer) {
			return typedListSerializer.getOrdering();
		}
		return ConfigListOrdering.ORDERED;
	}

	@Override
	public String serialize(List<T> value) {
		return listSerializer.serialize(value);
	}

	@Override
	public IDeserializeResult<List<T>> deserialize(String string) {
		return listSerializer.deserialize(string);
	}

	@Override
	public boolean isValid(List<T> value) {
		return listSerializer.isValid(value);
	}

	@Override
	public Optional<List<List<T>>> getAllValidValues() {
		return listSerializer.getAllValidValues();
	}

	@Override
	public Component getLocalizedValueName(String configValueLocalizationKey, List<T> value) {
		if (listSerializer instanceof IConfigValueLocalizationProvider<?> localizationProvider) {
			@SuppressWarnings("unchecked")
			IConfigValueLocalizationProvider<List<T>> typedLocalizationProvider = (IConfigValueLocalizationProvider<List<T>>) localizationProvider;
			return typedLocalizationProvider.getLocalizedValueName(configValueLocalizationKey, value);
		}
		if (value.isEmpty()) {
			return Component.translatable("mezz_config.config.value.list.empty");
		}
		return Component.literal(listSerializer.serialize(value));
	}

	@Override
	public Optional<Component> getLocalizedValueDescription(String configValueLocalizationKey, List<T> value) {
		if (listSerializer instanceof IConfigValueLocalizationProvider<?> localizationProvider) {
			@SuppressWarnings("unchecked")
			IConfigValueLocalizationProvider<List<T>> typedLocalizationProvider = (IConfigValueLocalizationProvider<List<T>>) localizationProvider;
			return typedLocalizationProvider.getLocalizedValueDescription(configValueLocalizationKey, value);
		}
		return Optional.empty();
	}

	@Override
	public String getValidValuesDescription() {
		return listSerializer.getValidValuesDescription();
	}
}
