package net.mezzdev.config.gui.jei;

import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import net.mezzdev.config.gui.ConfigScreen;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reports config screen tab and popup bounds so JEI can keep other UI from drawing over them.
 */
final class ConfigScreenGuiHandler implements IGlobalGuiHandler {
	@Override
	public Collection<Rect2i> getGuiExtraAreas() {
		if (net.mezzdev.config.gui.ConfigClientUtil.screen() instanceof ConfigScreen configScreen) {
			List<Rect2i> areas = new ArrayList<>(2);
			@Nullable
			Rect2i modTabsArea = configScreen.getModTabsArea();
			if (modTabsArea != null) {
				areas.add(modTabsArea);
			}
			@Nullable
			Rect2i selectorArea = configScreen.getValueSelectorArea();
			if (selectorArea != null) {
				areas.add(selectorArea);
			}
			return areas;
		}
		return List.of();
	}
}
