package net.mezzdev.config.gui.screenlist;

import java.nio.file.Path;

import org.jetbrains.annotations.Nullable;

/**
 * Icon metadata for the mod that owns a config screen.
 */
public record ConfigScreenOwnerMetadata(
	@Nullable Path iconPath
) {
	public ConfigScreenOwnerMetadata() {
		this(null);
	}
}
