package net.mezzdev.config.gui.fabric;

import net.fabricmc.loader.api.FabricLoader;

final class FabricAmecsSupport {
	private static final String AMECS_MOD_ID = "amecs_key_modifiers";
	private static final String DISABLE_AMECS_SUPPORT_PROPERTY = "mezz_config_gui.fabric.disableAmecsSupport";
	private static final boolean ENABLED = FabricLoader.getInstance().isModLoaded(AMECS_MOD_ID) &&
		!Boolean.getBoolean(DISABLE_AMECS_SUPPORT_PROPERTY);

	private FabricAmecsSupport() {

	}

	public static boolean isEnabled() {
		return ENABLED;
	}
}
