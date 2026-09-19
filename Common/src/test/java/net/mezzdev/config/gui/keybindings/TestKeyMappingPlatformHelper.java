package net.mezzdev.config.gui.keybindings;
/** Unit tests run without a loader or development client. */
public final class TestKeyMappingPlatformHelper extends VanillaConfigKeyMappingPlatformHelper {
	@Override
	public boolean isInDev() { return false; }
}
