package net.mezzdev.config.gui.forge.config;

import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ForgeConfigValueReloads {
	private static final Set<ModContainer> REGISTERED_MOD_CONTAINERS = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final Map<ModConfig, Set<ForgeConfigValue<?>>> VALUES_BY_CONFIG = new IdentityHashMap<>();

	private ForgeConfigValueReloads() {

	}

	public static void register(ModContainer modContainer) {
		if (!(modContainer instanceof FMLModContainer javaModContainer)) {
			return;
		}
		synchronized (REGISTERED_MOD_CONTAINERS) {
			if (REGISTERED_MOD_CONTAINERS.contains(modContainer)) {
				return;
			}
			javaModContainer.getEventBus().addListener(ForgeConfigValueReloads::onConfigReloaded);
			REGISTERED_MOD_CONTAINERS.add(modContainer);
		}
	}

	public static void register(ModConfig modConfig, ForgeConfigValue<?> value) {
		synchronized (VALUES_BY_CONFIG) {
			VALUES_BY_CONFIG.computeIfAbsent(
					modConfig,
					ignored -> Collections.newSetFromMap(new IdentityHashMap<>())
				)
				.add(value);
		}
	}

	public static void unregister(ModConfig modConfig, ForgeConfigValue<?> value) {
		synchronized (VALUES_BY_CONFIG) {
			Set<ForgeConfigValue<?>> values = VALUES_BY_CONFIG.get(modConfig);
			if (values != null) {
				values.remove(value);
				if (values.isEmpty()) {
					VALUES_BY_CONFIG.remove(modConfig);
				}
			}
		}
	}

	private static void onConfigReloaded(ModConfigEvent.Reloading event) {
		List<ForgeConfigValue<?>> values;
		synchronized (VALUES_BY_CONFIG) {
			values = new ArrayList<>(VALUES_BY_CONFIG.getOrDefault(event.getConfig(), Set.of()));
		}
		values.forEach(ForgeConfigValue::onConfigReloaded);
	}
}
