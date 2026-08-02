package net.mezzdev.config.gui.jei;

import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import net.mezzdev.config.gui.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Reports config screen popup bounds so JEI can keep other UI from drawing over them.
 */
final class ConfigScreenGuiHandler implements IGlobalGuiHandler {
	@Override
	public Collection<Rect2i> getGuiExtraAreas() {
		Minecraft minecraft = Minecraft.getInstance();
		@Nullable
		Rect2i selectorArea = getValueSelectorArea(minecraft.screen);
		if (selectorArea != null) {
			return List.of(selectorArea);
		}
		return List.of();
	}

	@Nullable
	private static Rect2i getValueSelectorArea(@Nullable Object screen) {
		if (screen instanceof ConfigScreen configScreen) {
			return configScreen.getValueSelectorArea();
		}
		return null;
	}
}
