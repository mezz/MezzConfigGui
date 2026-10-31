package net.mezzdev.config.gui.screenlist;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Icon metadata for the mod that owns a config screen.
 */
public record ConfigScreenOwnerMetadata(
	@Nullable Path iconPath,
	@Nullable IconSource iconSource
) {
	public ConfigScreenOwnerMetadata() {
		this(null);
	}

	public ConfigScreenOwnerMetadata(@Nullable Path iconPath) {
		this(iconPath, createIconSource(iconPath));
	}

	public static ConfigScreenOwnerMetadata fromStream(IconSource iconSource) {
		return new ConfigScreenOwnerMetadata(null, Objects.requireNonNull(iconSource));
	}

	@Nullable
	private static IconSource createIconSource(@Nullable Path iconPath) {
		if (iconPath == null) {
			return null;
		}
		return () -> Files.newInputStream(iconPath);
	}

	@FunctionalInterface
	public interface IconSource {
		InputStream open() throws IOException;
	}
}
