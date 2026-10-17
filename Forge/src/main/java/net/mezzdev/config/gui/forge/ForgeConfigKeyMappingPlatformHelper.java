package net.mezzdev.config.gui.forge;

import net.mezzdev.config.gui.api.keybindings.IKeyMappingAdapter;
import net.mezzdev.config.gui.keybindings.VanillaConfigKeyMappingPlatformHelper;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

public final class ForgeConfigKeyMappingPlatformHelper extends VanillaConfigKeyMappingPlatformHelper {
	@Override
	public IKeyMappingAdapter createAdapter(KeyMapping mapping) {
		return new ForgeKeyMappingAdapter(mapping);
	}

	@Override
	public String getModNameForModId(String modId) {
		return ModList.get()
			.getModContainerById(modId)
			.map(modContainer -> modContainer.getModInfo().getDisplayName())
			.orElse(super.getModNameForModId(modId));
	}

	@Override
	public boolean isInDev() {
		return !FMLLoader.isProduction();
	}

}
