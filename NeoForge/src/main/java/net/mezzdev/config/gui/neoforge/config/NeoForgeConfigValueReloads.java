package net.mezzdev.config.gui.neoforge.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class NeoForgeConfigValueReloads {
	private static final Set<ModContainer> REGISTERED_MOD_CONTAINERS = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final Map<ModConfig, Set<NeoForgeConfigValue<?>>> VALUES_BY_CONFIG = new IdentityHashMap<>();

	private NeoForgeConfigValueReloads() {

	}

	public static void register(ModContainer modContainer) {
		synchronized (REGISTERED_MOD_CONTAINERS) {
			if (REGISTERED_MOD_CONTAINERS.contains(modContainer)) {
				return;
			}
			modContainer.getEventBus().addListener(NeoForgeConfigValueReloads::onConfigReloaded);
			REGISTERED_MOD_CONTAINERS.add(modContainer);
		}
	}

	public static void register(ModConfig modConfig, NeoForgeConfigValue<?> value) {
		synchronized (VALUES_BY_CONFIG) {
			VALUES_BY_CONFIG.computeIfAbsent(
				modConfig,
				ignored -> Collections.newSetFromMap(new IdentityHashMap<>())
			).add(value);
		}
	}

	public static void unregister(ModConfig modConfig, NeoForgeConfigValue<?> value) {
		synchronized (VALUES_BY_CONFIG) {
			Set<NeoForgeConfigValue<?>> values = VALUES_BY_CONFIG.get(modConfig);
			if (values != null) {
				values.remove(value);
				if (values.isEmpty()) {
					VALUES_BY_CONFIG.remove(modConfig);
				}
			}
		}
	}

	private static void onConfigReloaded(ModConfigEvent.Reloading event) {
		List<NeoForgeConfigValue<?>> values;
		synchronized (VALUES_BY_CONFIG) {
			values = new ArrayList<>(VALUES_BY_CONFIG.getOrDefault(event.getConfig(), Set.of()));
		}
		values.forEach(NeoForgeConfigValue::onConfigReloaded);
	}
}
