package net.mezzdev.config.gui.screenlist;

import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Display metadata for the mod that owns a config screen.
 */
public record ConfigScreenOwnerMetadata(
	Component displayName,
	Optional<Path> iconPath
) {
	public ConfigScreenOwnerMetadata {
		Objects.requireNonNull(displayName, "displayName");
		Objects.requireNonNull(iconPath, "iconPath");
	}

	public ConfigScreenOwnerMetadata(Component displayName) {
		this(displayName, Optional.empty());
	}
}
