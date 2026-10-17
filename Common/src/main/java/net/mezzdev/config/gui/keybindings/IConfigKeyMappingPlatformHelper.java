package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.gui.api.keybindings.IKeyMappingAdapter;
import net.minecraft.client.KeyMapping;

/**
 * Platform service for adapting Minecraft key mappings to config key mappings.
 */
public interface IConfigKeyMappingPlatformHelper {
	IKeyMappingAdapter createAdapter(KeyMapping keyMapping);

	String getModNameForModId(String modId);

	boolean isInDev();
}
