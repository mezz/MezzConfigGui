package net.mezzdev.config.gui.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.mezzdev.config.gui.ConfigGui;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides config screens registered with the config GUI API to Mod Menu.
 */
public final class ConfigGuiModMenuPlugin implements ModMenuApi {
	@Override
	public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
		Map<String, ConfigScreenFactory<?>> factories = new LinkedHashMap<>();
		ConfigGui.createScreenFactories(ConfigGuiFabricPluginFinder.getPlugins())
			.forEach((modId, factory) -> factories.put(modId, factory::create));
		return Map.copyOf(factories);
	}
}
