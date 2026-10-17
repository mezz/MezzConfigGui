package net.mezzdev.config.gui.forge.config;

import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.api.value.serializer.IConfigListValueSerializer;
import net.mezzdev.config.gui.internal.ListValueEditorMetadata;
import net.mezzdev.config.gui.api.IConfigValueEditorMetadata;
import net.mezzdev.config.gui.internal.ConfigValueEditorMetadata;
import net.mezzdev.config.gui.internal.ConfigValueEditorMetadataFactory;
import net.mezzdev.config.gui.api.IConfigValueIcon;
import net.mezzdev.config.gui.api.IConfigValueIconProvider;
import net.minecraft.client.resources.language.I18n;
import net.mezzdev.config.gui.ConfigRenderUtil;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.List;

final class ForgeValueEditorMetadata<T> extends ConfigValueEditorMetadata<T> implements IConfigValueIconProvider<T> {
	private static final ResourceLocation ENABLED_ICON = new ResourceLocation("mezz_config", "icons/check");
	private static final ResourceLocation DISABLED_ICON = new ResourceLocation("mezz_config", "icons/x");

	@SuppressWarnings("unchecked")
	static <T> IConfigValueEditorMetadata<T> create(IConfigValueSerializer<T> serializer, T value) {
		if (serializer instanceof IConfigListValueSerializer<?> listSerializer) {
			return (IConfigValueEditorMetadata<T>) createList(listSerializer);
		}
		return new ForgeValueEditorMetadata<>(serializer, value);
	}

	private static <T> IConfigValueEditorMetadata<List<T>> createList(IConfigListValueSerializer<T> serializer) {
		return new ListValueEditorMetadata<>(serializer, true) {
			@Override
			public IConfigValueEditorMetadata<T> getElementMetadata(T value) {
				return create(serializer.getElementSerializer(), value);
			}
		};
	}

	private ForgeValueEditorMetadata(IConfigValueSerializer<T> serializer, T value) {
		super(serializer, ConfigValueEditorMetadataFactory.INSTANCE.create(serializer, value).getEditorType());
	}

	@Override
	public Component getLocalizedValueName(String localizationKey, T value) {
		if (value instanceof Enum<?> enumValue) {
			return getTranslatedEnumValue(localizationKey, enumValue, ".name")
				.orElseGet(() -> Component.literal(ForgeConfigLocalization.getDisplayNameFallback(enumValue.name())));
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
		if (I18n.exists(translationKey)) {
			return Optional.of(Component.translatable(translationKey));
		}
		return Optional.empty();
	}

	@Override
	public Optional<IConfigValueIcon> getIcon(T value) {
		if (value instanceof Boolean enabled) {
			ResourceLocation location;
			if (enabled) {
				location = ENABLED_ICON;
			} else {
				location = DISABLED_ICON;
			}
			return Optional.of((guiGraphics, area) -> ConfigRenderUtil.blitSprite(guiGraphics,
				ConfigTextures.get().getGuiSpriteManager().getSprite(location),
				16, 16, 0, 0, area.getX(), area.getY(), 0, area.getWidth(), area.getHeight()));
		}
		return Optional.empty();
	}
}
