package net.mezzdev.config.gui.config;

import net.mezzdev.config.gui.api.ConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import net.minecraft.network.chat.Component;

/**
 * Registers MezzConfig GUI's own user-facing options.
 */
@ConfigGuiPlugin
public final class ConfigGuiOptionsPlugin implements IConfigGuiPlugin {
	@Override
	public String getModId() {
		return ConfigGuiOptions.MOD_ID;
	}

	@Override
	public void register(IConfigGuiRegistration registration) {
		registration.registerScreen(
			Component.translatable("mezz_config_gui.config.screen.title"),
			ConfigGuiOptions::getSchema
		);
	}
}
