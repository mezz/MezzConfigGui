package net.mezzdev.config.gui.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.mezzdev.config.gui.screenlist.ConfigScreenFactoryRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides config screens registered with the config GUI API to Mod Menu.
 */
public final class ConfigGuiModMenuPlugin implements ModMenuApi {
	@Override
	public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
		ConfigScreenFactoryRegistry registry = ConfigGuiFabricClient.getScreenFactoryRegistry();
		Map<String, ConfigScreenFactory<?>> factories = new LinkedHashMap<>();
		registry.getFactories()
			.forEach((modId, factory) -> factories.put(modId, factory::create));
		return Map.copyOf(factories);
	}
}
