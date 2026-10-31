package net.mezzdev.config.gui.neoforge;

import net.mezzdev.config.gui.screenlist.ConfigScreenOwnerMetadata;
import net.neoforged.fml.jarcontents.JarContents;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NeoForgeConfigScreenOwnerMetadataTest {
	@TempDir
	Path tempDir;

	@Test
	void packagedIconsLoadWithoutAMountedZipFileSystem() throws IOException {
		byte[] icon = png();
		Path jar = writeJar("logo.png", icon);
		try (JarContents contents = JarContents.ofPath(jar)) {
			URI iconUri = contents.findFile("logo.png").orElseThrow();
			assertThrows(FileSystemNotFoundException.class, () -> Path.of(iconUri));
			assertIcon(icon, NeoForgeConfigScreenOwnerMetadata.getIcon(contents, "logo.png"));
		}
	}

	@Test
	void packagedIconsSupportSpacesAndWindowsSeparators() throws IOException {
		byte[] icon = png();
		Path jar = writeJar("assets/logo 160.png", icon);
		try (JarContents contents = JarContents.ofPath(jar)) {
			assertIcon(icon, NeoForgeConfigScreenOwnerMetadata.getIcon(contents, "assets\\logo 160.png"));
		}
	}

	@Test
	void developmentFoldersAndMissingIconsRetainTheirBehavior() throws IOException {
		byte[] icon = png();
		Files.write(tempDir.resolve("logo.png"), icon);
		Files.createDirectory(tempDir.resolve("directory.png"));
		try (JarContents contents = JarContents.ofPath(tempDir)) {
			assertIcon(icon, NeoForgeConfigScreenOwnerMetadata.getIcon(contents, "logo.png"));
			assertNull(NeoForgeConfigScreenOwnerMetadata.getIcon(contents, "missing.png").iconSource());
			assertNull(NeoForgeConfigScreenOwnerMetadata.getIcon(contents, "directory.png").iconSource());
		}
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
