package net.mezzdev.config.gui.screenlist;

/**
 * Resolves platform-specific icon metadata for mods with config screens.
 */
@FunctionalInterface
public interface ConfigScreenOwnerMetadataProvider {
	ConfigScreenOwnerMetadata getMetadata(String modId);
}
