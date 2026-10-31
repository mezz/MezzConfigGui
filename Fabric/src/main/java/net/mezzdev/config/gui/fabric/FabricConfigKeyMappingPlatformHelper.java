package net.mezzdev.config.gui.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingAdapter;
import net.mezzdev.config.gui.keybindings.VanillaConfigKeyMappingPlatformHelper;
import net.minecraft.client.KeyMapping;

public final class FabricConfigKeyMappingPlatformHelper extends VanillaConfigKeyMappingPlatformHelper {
	private static final String AMECS_MOD_ID = "amecs_key_modifiers";

	@Override
	public IKeyMappingAdapter createAdapter(KeyMapping mapping) {
		if (FabricLoader.getInstance().isModLoaded(AMECS_MOD_ID)) {
			return new AmecsKeyMappingAdapter(mapping);
		}
		return super.createAdapter(mapping);
	}

	@Override
	public String getModNameForModId(String modId) {
		return FabricLoader.getInstance().getModContainer(modId)
			.map(container -> container.getMetadata().getName())
			.orElseGet(() -> super.getModNameForModId(modId));
	}

	@Override
	public boolean isInDev() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}
}
