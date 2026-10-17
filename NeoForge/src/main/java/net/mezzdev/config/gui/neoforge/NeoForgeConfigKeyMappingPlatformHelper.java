package net.mezzdev.config.gui.neoforge;

import net.mezzdev.config.gui.api.keybindings.IKeyMappingAdapter;
import net.mezzdev.config.gui.keybindings.VanillaConfigKeyMappingPlatformHelper;
import net.minecraft.client.KeyMapping;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

public final class NeoForgeConfigKeyMappingPlatformHelper extends VanillaConfigKeyMappingPlatformHelper {
	@Override
	public IKeyMappingAdapter createAdapter(KeyMapping mapping) {
		return new NeoForgeKeyMappingAdapter(mapping);
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
		return !FMLLoader.getCurrent().isProduction();
	}

}
