package net.mezzdev.config.gui.screenlist;

/**
 * Resolves platform-specific display metadata for mods with config screens.
 */
@FunctionalInterface
public interface ConfigScreenOwnerMetadataProvider {
	ConfigScreenOwnerMetadata getMetadata(String modId);
}
