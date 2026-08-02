package net.mezzdev.config.gui.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.mezzdev.config.gui.ConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Optional JEI integration for the config screen.
 */
@JeiPlugin
public class ConfigGuiPlugin implements IModPlugin {
	private static final String CONFIG_GUI_MOD_ID = "mezz_config_gui";

	@Override
	public ResourceLocation getPluginUid() {
		return ResourceLocation.fromNamespaceAndPath(CONFIG_GUI_MOD_ID, "config_gui");
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addGuiScreenHandler(ConfigScreen.class, ConfigGuiPlugin::getProperties);
		registration.addGlobalGuiHandler(new ConfigScreenGuiHandler());
	}

	@Nullable
	private static IGuiProperties getProperties(Screen screen) {
		if (ConfigScreen.isCapturingKeyBinding(screen)) {
			return null;
		}
		@Nullable
		Rect2i area = getScreenArea(screen);
		if (area == null || screen.width <= 0 || screen.height <= 0) {
			return null;
		}
		return new ConfigGuiProperties(
			screen.getClass(),
			area.getX(),
			area.getY(),
			area.getWidth(),
			area.getHeight(),
			screen.width,
			screen.height
		);
	}

	@Nullable
	private static Rect2i getScreenArea(Screen screen) {
		if (screen instanceof ConfigScreen configScreen) {
			return configScreen.getScreenArea();
		}
		return null;
	}

	private record ConfigGuiProperties(
		Class<? extends Screen> screenClass,
		int guiLeft,
		int guiTop,
		int guiXSize,
		int guiYSize,
		int screenWidth,
		int screenHeight
	) implements IGuiProperties {

	}
}
