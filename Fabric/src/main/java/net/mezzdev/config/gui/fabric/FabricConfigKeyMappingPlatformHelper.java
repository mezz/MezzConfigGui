package net.mezzdev.config.gui.fabric;
import net.fabricmc.loader.api.FabricLoader;
import net.mezzdev.config.gui.keybindings.VanillaConfigKeyMappingPlatformHelper;

public final class FabricConfigKeyMappingPlatformHelper extends VanillaConfigKeyMappingPlatformHelper {
	@Override
	public String getModNameForModId(String id) { return FabricLoader.getInstance().getModContainer(id).map(c -> c.getMetadata().getName()).orElseGet(() -> super.getModNameForModId(id)); }
	@Override
	public boolean isInDev() { return FabricLoader.getInstance().isDevelopmentEnvironment(); }
}
