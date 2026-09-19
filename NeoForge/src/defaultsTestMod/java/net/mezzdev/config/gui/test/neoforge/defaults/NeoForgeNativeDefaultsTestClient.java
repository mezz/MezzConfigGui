package net.mezzdev.config.gui.test.neoforge.defaults;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

final class NeoForgeNativeDefaultsTestClient {
	private static final KeyMapping OPEN_DEFAULTS_SCREEN_KEY = new KeyMapping(
		"key.%s.openDefaultsScreen".formatted(NeoForgeNativeDefaultsTestMod.MOD_ID),
		InputConstants.Type.KEYBOARD,
		com.mojang.blaze3d.platform.InputConstants.KEY_K,
		net.mezzdev.config.gui.ConfigInputUtil.keyCategory(NeoForgeNativeDefaultsTestMod.MOD_ID)
	);
	private static final KeyMapping TOGGLE_DEFAULTS_OVERLAY_KEY = new KeyMapping(
		"key.%s.toggleDefaultsOverlay".formatted(NeoForgeNativeDefaultsTestMod.MOD_ID),
		InputConstants.Type.KEYBOARD,
		com.mojang.blaze3d.platform.InputConstants.KEY_L,
		net.mezzdev.config.gui.ConfigInputUtil.keyCategory(NeoForgeNativeDefaultsTestMod.MOD_ID)
	);

	private NeoForgeNativeDefaultsTestClient() {

	}

	static void register(IEventBus modEventBus) {
		modEventBus.addListener(NeoForgeNativeDefaultsTestClient::registerKeyMappings);
	}

	private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(OPEN_DEFAULTS_SCREEN_KEY);
		event.register(TOGGLE_DEFAULTS_OVERLAY_KEY);
	}
}
