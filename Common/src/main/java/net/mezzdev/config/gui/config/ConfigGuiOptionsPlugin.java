package net.mezzdev.config.gui.config;

import net.mezzdev.config.api.plugin.IConfigPlugin;
import net.mezzdev.config.api.plugin.IConfigRegistration;
import net.mezzdev.config.gui.api.ConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import net.minecraft.network.chat.Component;

/**
 * Registers MezzConfigGui's own user-facing options.
 */
@net.mezzdev.config.api.plugin.ConfigPlugin
@ConfigGuiPlugin
public final class ConfigGuiOptionsPlugin implements IConfigPlugin, IConfigGuiPlugin {
	@Override
	public String getModId() {
		return ConfigGuiOptions.MOD_ID;
	}

	@Override
	public void registerConfigFiles(IConfigRegistration registration) {
		ConfigGuiOptions.register(registration);
	}

	@Override
	public void register(IConfigGuiRegistration registration) {
		registration.registerScreen(
			Component.translatable("mezz_config_gui.config.screen.title"),
			ConfigGuiOptions::getSchema
		);
	}
}
