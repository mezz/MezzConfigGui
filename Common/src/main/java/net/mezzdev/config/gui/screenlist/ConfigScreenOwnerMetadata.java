package net.mezzdev.config.gui.screenlist;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Icon metadata for the mod that owns a config screen.
 */
public record ConfigScreenOwnerMetadata(
	Optional<Path> iconPath
) {
	public ConfigScreenOwnerMetadata {
		Objects.requireNonNull(iconPath, "iconPath");
	}

	public ConfigScreenOwnerMetadata() {
		this(Optional.empty());
	}
}
