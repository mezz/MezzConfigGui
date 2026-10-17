package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.gui.api.keybindings.IKeyMappingAdapter;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;

public class VanillaConfigKeyMappingPlatformHelper implements IConfigKeyMappingPlatformHelper {
	@Override
	public IKeyMappingAdapter createAdapter(KeyMapping keyMapping) {
		return new VanillaKeyMappingAdapter(keyMapping);
	}

	@Override
	public String getModNameForModId(String modId) {
		return modId;
	}

	@Override
	public boolean isInDev() {
		return SharedConstants.IS_RUNNING_IN_IDE;
	}
}
