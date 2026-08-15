package net.mezzdev.config.gui.test.neoforge.custom;

import net.mezzdev.config.api.plugin.client.ClientConfigPlugin;
import net.mezzdev.config.api.plugin.client.IClientConfigPlugin;
import net.mezzdev.config.api.plugin.client.IClientConfigRegistration;
import net.mezzdev.config.api.schema.IConfigCategoryBuilder;
import net.mezzdev.config.api.schema.IConfigSchemaBuilder;

/**
 * Registers a MezzConfig schema alongside this mod's native NeoForge configs.
 */
@ClientConfigPlugin
public final class NeoForgeNativeCustomTestConfigPlugin implements IClientConfigPlugin {
	private static final String LOCALIZATION_PATH = NeoForgeNativeCustomTestMod.MOD_ID + ".config";

	@Override
	public String getModId() {
		return NeoForgeNativeCustomTestMod.MOD_ID;
	}

	@Override
	public void registerClientConfigFiles(IClientConfigRegistration registration) {
		IConfigSchemaBuilder schema = registration.createClientSchemaBuilder(
			NeoForgeNativeCustomTestMod.MOD_ID + ".ini",
			LOCALIZATION_PATH
		);
		IConfigCategoryBuilder mezzConfig = schema.addCategory("mezzConfig");
		mezzConfig.addBoolean("mixedSourceEnabled", true).build();
		schema.build();
	}
}
