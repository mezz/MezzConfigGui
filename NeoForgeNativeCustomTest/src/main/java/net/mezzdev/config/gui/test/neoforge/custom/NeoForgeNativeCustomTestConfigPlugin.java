package net.mezzdev.config.gui.test.neoforge.custom;

import net.mezzdev.config.api.plugin.ConfigPlugin;
import net.mezzdev.config.api.plugin.IConfigPlugin;
import net.mezzdev.config.api.plugin.IConfigRegistration;
import net.mezzdev.config.api.schema.IConfigCategoryBuilder;
import net.mezzdev.config.api.schema.IConfigSchemaBuilder;

/**
 * Registers a MezzConfig schema alongside this mod's native NeoForge configs.
 */
@ConfigPlugin
public final class NeoForgeNativeCustomTestConfigPlugin implements IConfigPlugin {
	private static final String LOCALIZATION_PATH = NeoForgeNativeCustomTestMod.MOD_ID + ".config";

	@Override
	public String getModId() {
		return NeoForgeNativeCustomTestMod.MOD_ID;
	}

	@Override
	public void registerConfigFiles(IConfigRegistration registration) {
		IConfigSchemaBuilder schema = registration.createSchemaBuilder(
			NeoForgeNativeCustomTestMod.MOD_ID + ".ini",
			LOCALIZATION_PATH
		);
		IConfigCategoryBuilder mezzConfig = schema.addCategory("mezzConfig");
		mezzConfig.addBoolean("mixedSourceEnabled", true).build();
		schema.build();
	}
}
