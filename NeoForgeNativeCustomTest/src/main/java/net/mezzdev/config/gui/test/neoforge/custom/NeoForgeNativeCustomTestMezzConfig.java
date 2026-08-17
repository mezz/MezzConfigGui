package net.mezzdev.config.gui.test.neoforge.custom;

import net.mezzdev.config.api.Configs;
import net.mezzdev.config.api.IConfigRegistration;
import net.mezzdev.config.api.schema.IConfigCategoryBuilder;
import net.mezzdev.config.api.schema.IConfigSchemaBuilder;

/**
 * Registers a MezzConfig schema alongside this mod's native NeoForge configs.
 */
public final class NeoForgeNativeCustomTestMezzConfig {
	private static final String LOCALIZATION_PATH = NeoForgeNativeCustomTestMod.MOD_ID + ".config";

	private NeoForgeNativeCustomTestMezzConfig() {

	}

	public static void register() {
		IConfigRegistration registration = Configs.forMod(NeoForgeNativeCustomTestMod.MOD_ID);
		IConfigSchemaBuilder schema = registration.createClientSchemaBuilder(
			NeoForgeNativeCustomTestMod.MOD_ID + ".ini",
			LOCALIZATION_PATH
		);
		IConfigCategoryBuilder mezzConfig = schema.addCategory("mezzConfig");
		mezzConfig.addBoolean("mixedSourceEnabled", true).build();
		schema.build();
	}
}
