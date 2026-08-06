package net.mezzdev.config.gui.screenlist;

import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.mezzdev.config.gui.ConfigScreenNavigation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Config screen factories discovered for the current platform.
 */
public final class ConfigScreenFactoryRegistry {
	private final Map<String, IConfigScreenFactory> factories;
	private final List<ConfigScreenFactoryEntry> entries;
	private final ConfigScreenNavigation navigation;

	public ConfigScreenFactoryRegistry(
		Map<String, IConfigScreenFactory> factories,
		List<ConfigScreenFactoryEntry> entries,
		ConfigScreenNavigation navigation
	) {
		this.factories = Collections.unmodifiableMap(new LinkedHashMap<>(factories));
		this.entries = List.copyOf(entries);
		this.navigation = navigation;
	}

	public Map<String, IConfigScreenFactory> getFactories() {
		return factories;
	}

	public List<ConfigScreenFactoryEntry> getEntries() {
		return entries;
	}

	public void setScreenListFactory(IConfigScreenFactory screenListFactory) {
		navigation.setScreenListFactory(screenListFactory);
	}
}
