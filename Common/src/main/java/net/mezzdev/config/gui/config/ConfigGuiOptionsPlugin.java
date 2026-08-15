package net.mezzdev.config.gui.config;

import net.mezzdev.config.api.plugin.client.ClientConfigPlugin;
import net.mezzdev.config.api.plugin.client.IClientConfigPlugin;
import net.mezzdev.config.api.plugin.client.IClientConfigRegistration;
import net.mezzdev.config.gui.api.ConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import net.mezzdev.config.gui.MezzConfigScreenConfigs;
import net.minecraft.network.chat.Component;

/**
 * Registers MezzConfig GUI's own user-facing options.
 */
@ClientConfigPlugin
@ConfigGuiPlugin
public final class ConfigGuiOptionsPlugin implements IClientConfigPlugin, IConfigGuiPlugin {
	@Override
	public String getModId() {
		return ConfigGuiOptions.MOD_ID;
	}

	@Override
	public void registerClientConfigFiles(IClientConfigRegistration registration) {
		MezzConfigScreenConfigs.setConfigManager(registration.getConfigManager());
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
