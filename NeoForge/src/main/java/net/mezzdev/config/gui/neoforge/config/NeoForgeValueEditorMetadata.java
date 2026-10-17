package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.api.value.serializer.IConfigListValueSerializer;
import net.mezzdev.config.gui.ListValueEditorMetadata;
import net.mezzdev.config.gui.api.IConfigValueEditorMetadata;
import net.mezzdev.config.gui.ConfigRenderUtil;
import net.mezzdev.config.gui.ConfigValueEditorMetadata;
import net.mezzdev.config.gui.ConfigValueEditorMetadataFactory;
import net.mezzdev.config.gui.api.IConfigValueIcon;
import net.mezzdev.config.gui.api.IConfigValueIconProvider;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.TranslatableEnum;

import java.util.Optional;
import java.util.List;

final class NeoForgeValueEditorMetadata<T> extends ConfigValueEditorMetadata<T> implements IConfigValueIconProvider<T> {
	private static final Identifier ENABLED_ICON = Identifier.withDefaultNamespace("container/beacon/confirm");
	private static final Identifier DISABLED_ICON = Identifier.withDefaultNamespace("container/beacon/cancel");

	@SuppressWarnings("unchecked")
	static <T> IConfigValueEditorMetadata<T> create(IConfigValueSerializer<T> serializer, T value) {
		if (serializer instanceof IConfigListValueSerializer<?> listSerializer) {
			return (IConfigValueEditorMetadata<T>) createList(listSerializer);
		}
		return new NeoForgeValueEditorMetadata<>(serializer, value);
	}

	private static <T> IConfigValueEditorMetadata<List<T>> createList(IConfigListValueSerializer<T> serializer) {
		return new ListValueEditorMetadata<>(serializer, true) {
			@Override
			public IConfigValueEditorMetadata<T> getElementMetadata(T value) {
				return create(serializer.getElementSerializer(), value);
			}
		};
	}

	private NeoForgeValueEditorMetadata(IConfigValueSerializer<T> serializer, T value) {
		super(serializer, ConfigValueEditorMetadataFactory.INSTANCE.create(serializer, value).getEditorType());
	}

	@Override
	public Component getLocalizedValueName(String localizationKey, T value) {
		if (value instanceof Enum<?> enumValue) {
			return getTranslatedEnumValue(localizationKey, enumValue, ".name")
				.orElseGet(() -> {
					if (value instanceof TranslatableEnum translatableEnum) {
						return translatableEnum.getTranslatedName();
					}
					return Component.literal(NeoForgeConfigLocalization.getDisplayNameFallback(enumValue.name()));
				});
		}
		return super.getLocalizedValueName(localizationKey, value);
	}

	@Override
	public Optional<Component> getLocalizedValueDescription(String localizationKey, T value) {
		if (value instanceof Enum<?> enumValue) {
			return getTranslatedEnumValue(localizationKey, enumValue, ".description");
		}
		return super.getLocalizedValueDescription(localizationKey, value);
	}

	private static Optional<Component> getTranslatedEnumValue(String localizationKey, Enum<?> value, String suffix) {
		String translationKey = localizationKey + ".value." + value.name() + suffix;
		if (Language.getInstance().has(translationKey)) {
			return Optional.of(Component.translatable(translationKey));
		}
		return Optional.empty();
	}

	@Override
	public Optional<IConfigValueIcon> getIcon(T value) {
		if (value instanceof Boolean enabled) {
			Identifier location;
			if (enabled) {
				location = ENABLED_ICON;
			} else {
				location = DISABLED_ICON;
			}
			return Optional.of((guiGraphics, area) -> ConfigRenderUtil.blitSprite(guiGraphics, location, area.getX(), area.getY(), area.getWidth(), area.getHeight()));
		}
		return Optional.empty();
	}
}
