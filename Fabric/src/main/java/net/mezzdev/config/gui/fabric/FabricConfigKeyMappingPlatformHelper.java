package net.mezzdev.config.gui.fabric;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.mezzdev.config.gui.keybindings.ConfigKeyBinding;
import net.mezzdev.config.gui.keybindings.ConfigKeyBindingUtil;
import net.mezzdev.config.gui.keybindings.ConfigKeyModifier;
import net.mezzdev.config.gui.keybindings.VanillaConfigKeyMappingPlatformHelper;
import net.minecraft.client.KeyMapping;
public final class FabricConfigKeyMappingPlatformHelper extends VanillaConfigKeyMappingPlatformHelper {
	@Override
	public ConfigKeyBinding getValue(KeyMapping mapping) { return ConfigKeyBindingUtil.create(KeyMappingHelper.getBoundKeyOf(mapping), ConfigKeyModifier.NONE); }
	@Override
	public String getModNameForModId(String id) { return FabricLoader.getInstance().getModContainer(id).map(c -> c.getMetadata().getName()).orElseGet(() -> super.getModNameForModId(id)); }
	@Override
	public boolean isInDev() { return FabricLoader.getInstance().isDevelopmentEnvironment(); }
}
