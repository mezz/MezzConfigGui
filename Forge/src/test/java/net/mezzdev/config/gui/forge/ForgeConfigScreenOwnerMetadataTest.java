package net.mezzdev.config.gui.forge;

import net.mezzdev.config.gui.screenlist.ConfigScreenOwnerMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ForgeConfigScreenOwnerMetadataTest {
	@TempDir
	Path tempDir;

	@Test
	void packagedIconsLoadThroughTheLoaderFileSystem() throws IOException {
		byte[] icon = png();
		Path jar = writeJar("logo.png", icon);
		try (var fileSystem = FileSystems.newFileSystem(jar)) {
			Path root = fileSystem.getPath("/");
			assertIcon(icon, ForgeConfigScreenOwnerMetadata.getIcon(parts -> root.resolve(String.join("/", parts)), "logo.png"));
		}
	}

	@Test
	void packagedIconsSupportSpacesAndWindowsSeparators() throws IOException {
		byte[] icon = png();
		Path jar = writeJar("assets/logo 160.png", icon);
		try (var fileSystem = FileSystems.newFileSystem(jar)) {
			Path root = fileSystem.getPath("/");
			assertIcon(icon, ForgeConfigScreenOwnerMetadata.getIcon(parts -> root.resolve(String.join("/", parts)), "assets\\logo 160.png"));
		}
	}

	@Test
	void developmentFoldersAndMissingIconsRetainTheirBehavior() throws IOException {
		byte[] icon = png();
		Files.write(tempDir.resolve("logo.png"), icon);
		Files.createDirectory(tempDir.resolve("directory.png"));
		assertIcon(icon, ForgeConfigScreenOwnerMetadata.getIcon(parts -> tempDir.resolve(String.join("/", parts)), "logo.png"));
		assertNull(ForgeConfigScreenOwnerMetadata.getIcon(parts -> tempDir.resolve(String.join("/", parts)), "missing.png").iconSource());
		assertNull(ForgeConfigScreenOwnerMetadata.getIcon(parts -> tempDir.resolve(String.join("/", parts)), "directory.png").iconSource());
	}

	private Path writeJar(String name, byte[] icon) throws IOException {
		Path jar = tempDir.resolve("mod.jar");
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
			output.putNextEntry(new JarEntry(name));
			output.write(icon);
			output.closeEntry();
		}
		return jar;
	}

	private static byte[] png() throws IOException {
		BufferedImage image = new BufferedImage(2, 3, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(1, 1, 0xFF336699);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ImageIO.write(image, "png", bytes);
		return bytes.toByteArray();
	}

	private static void assertIcon(byte[] expected, ConfigScreenOwnerMetadata metadata) throws IOException {
		ConfigScreenOwnerMetadata.IconSource source = metadata.iconSource();
		assertNotNull(source);
		try (InputStream stream = source.open()) {
			byte[] actual = stream.readAllBytes();
			assertArrayEquals(expected, actual);
			BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(actual));
			assertNotNull(image);
			assertEquals(2, image.getWidth());
			assertEquals(3, image.getHeight());
		}
	}
}
