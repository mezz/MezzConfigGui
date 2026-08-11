package net.mezzdev.config.gui.info;

import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.api.value.PackedColor;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueIcon;
import net.mezzdev.config.gui.api.IConfigValueIconProvider;
import net.mezzdev.config.gui.textures.ConfigCheckbox;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;

import java.util.Optional;

/**
 * Draws small icons for config values when a compact visual cue is available.
 */
public final class ConfigValueIcon {
	public static final int ICON_SIZE = 18;
	static final int TEXT_GAP = 3;
	private static final int BUTTON_ICON_SIZE = 16;

	private ConfigValueIcon() {

	}

	public static <T> int getTextOffset(IConfigScreenValue<T> configValue, T value) {
		return getTextOffset(configValue.getSerializer(), value);
	}

	public static <T> int getTextOffset(IConfigValueSerializer<T> serializer, T value) {
		if (getIcon(serializer, value).isEmpty()) {
			return 0;
		}
		return ICON_SIZE + TEXT_GAP;
	}

	public static <T> void draw(GuiGraphics guiGraphics, IConfigScreenValue<T> configValue, T value, int x, int y) {
		draw(guiGraphics, configValue.getSerializer(), value, x, y);
	}

	public static <T> void draw(GuiGraphics guiGraphics, IConfigValueSerializer<T> serializer, T value, int x, int y) {
		getIcon(serializer, value)
			.ifPresent(icon -> icon.draw(guiGraphics, new Rect2i(x, y, ICON_SIZE, ICON_SIZE)));
	}

	public static <T> void drawInButton(GuiGraphics guiGraphics, IConfigScreenValue<T> configValue, T value, ImmutableRect2i buttonArea) {
		getIcon(configValue.getSerializer(), value)
			.ifPresent(icon -> {
				int x = buttonArea.getX() + Math.round((buttonArea.getWidth() - BUTTON_ICON_SIZE) / 2.0f);
				int y = buttonArea.getY() + Math.round((buttonArea.getHeight() - BUTTON_ICON_SIZE) / 2.0f);
				icon.draw(guiGraphics, new Rect2i(x, y, BUTTON_ICON_SIZE, BUTTON_ICON_SIZE));
			});
	}

	private static <T> Optional<IConfigValueIcon> getIcon(IConfigValueSerializer<T> serializer, T value) {
		Optional<IConfigValueIcon> icon = getProvidedIcon(serializer, value);
		if (icon.isPresent()) {
			return icon;
		}
		if (value instanceof PackedColor color) {
			return Optional.of((guiGraphics, area) -> ColorSwatch.draw(guiGraphics, area, color));
		}
		if (value instanceof Boolean booleanValue) {
			return Optional.of(new CheckboxConfigValueIcon(booleanValue));
		}
		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	private static <T> Optional<IConfigValueIcon> getProvidedIcon(IConfigValueSerializer<T> serializer, T value) {
		if (serializer instanceof IConfigValueIconProvider<?> iconProvider) {
			IConfigValueIconProvider<T> typedIconProvider = (IConfigValueIconProvider<T>) iconProvider;
			return typedIconProvider.getIcon(value);
		}
		return Optional.empty();
	}

	private record CheckboxConfigValueIcon(boolean checked) implements IConfigValueIcon {
		@Override
		public void draw(GuiGraphics guiGraphics, Rect2i area) {
			ConfigCheckbox.draw(guiGraphics, area, checked, false);
		}
	}
}
