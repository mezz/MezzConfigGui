package net.mezzdev.config.gui.fabric;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingAdapter;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingBinding;
import net.mezzdev.config.gui.keybindings.ConfigKeyMappingHelper;
import net.mezzdev.config.gui.keybindings.VanillaConfigKeyMappingPlatformHelper;
import net.mezzdev.config.gui.keybindings.VanillaKeyMappingAdapter;
import net.minecraft.client.KeyMapping;

import java.util.Set;

public final class FabricConfigKeyMappingPlatformHelper extends VanillaConfigKeyMappingPlatformHelper {
	@Override
	public IKeyMappingAdapter createAdapter(KeyMapping mapping) {
		if (FabricAmecsSupport.isEnabled()) {
			return new AmecsKeyMappingAdapter(mapping);
		}
		return new VanillaAdapter(mapping);
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

	private static final class VanillaAdapter extends VanillaKeyMappingAdapter {
		private VanillaAdapter(KeyMapping mapping) {
			super(mapping);
		}

		@Override
		public IKeyMappingBinding getValue() {
			return ConfigKeyMappingHelper.INSTANCE.createBinding(KeyBindingHelper.getBoundKeyOf(getKeyMapping()), Set.of());
		}
	}
}
