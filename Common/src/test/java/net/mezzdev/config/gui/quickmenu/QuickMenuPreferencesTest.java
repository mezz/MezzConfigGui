package net.mezzdev.config.gui.quickmenu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.UncheckedIOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuickMenuPreferencesTest {
	@Test
	void customizationIsDeferredAndSurvivesReloadIncludingEmptySelection(@TempDir Path directory) throws Exception {
		var preferences = preferences(directory);
		var menu = new QuickMenuSelection<>(List.of("display.labels", "display.columns"), preferences, preferences::set);
		assertEquals(List.of("display.labels"), menu.selected());
		menu.toggleMembership("display.columns");
		menu.move("display.columns", -1);
		assertEquals(List.of("display.labels"), preferences.getValue());
		menu.getPendingChange().orElseThrow().apply();
		menu.saved();
		assertEquals(List.of("display.columns", "display.labels"), reload(directory).getValue());
		menu.toggleMembership("display.labels");
		menu.toggleMembership("display.columns");
		menu.getPendingChange().orElseThrow().apply();
		assertEquals(List.of(), reload(directory).getValue());
	}

	@Test
	void sameDefinitionSharesChoicesButOtherMenusAndModsRemainIndependent(@TempDir Path directory) {
		var first = preferences(directory);
		var same = preferences(directory);
		var otherMenu = new QuickMenuPreferences(directory, "example", List.of("display.columns"), List.of("display.labels"));
		var otherMod = new QuickMenuPreferences(directory, "another_mod", List.of("display.labels"), List.of("display.columns"));
		first.set(List.of());
		assertEquals(List.of(), same.getValue());
		assertEquals(List.of("display.columns"), otherMenu.getValue());
		assertEquals(List.of("display.labels"), otherMod.getValue());
	}

	@Test
	void failedSaveRetainsPendingSelectionForRetry(@TempDir Path directory) throws Exception {
		Path blocked = directory.resolve("blocked");
		var preferences = preferences(blocked);
		var menu = new QuickMenuSelection<>(List.of("display.labels", "display.columns"), preferences, preferences::set);
		menu.toggleMembership("display.columns");
		Files.writeString(blocked, "not a directory");
		assertThrows(UncheckedIOException.class, () -> menu.getPendingChange().orElseThrow().apply());
		assertEquals(List.of("display.labels"), preferences.getValue());
		assertEquals(List.of("display.labels", "display.columns"), menu.selected());
		Files.delete(blocked);
		menu.getPendingChange().orElseThrow().apply();
		menu.saved();
		assertEquals(List.of("display.labels", "display.columns"), reload(blocked).getValue());
	}

	private static QuickMenuPreferences preferences(Path directory) {
		return new QuickMenuPreferences(directory, "example", List.of("display.labels"), List.of("display.columns"));
	}

	private static QuickMenuPreferences reload(Path directory) throws Exception {
		// Load the actual saved file at a fresh location, bypassing the shared in-memory preferences.
		Path fresh = Files.createTempDirectory(directory.getParent(), "reload-");
		try (var files = Files.list(directory)) {
			for (Path file : files.toList()) {
				Files.copy(file, fresh.resolve(file.getFileName()));
			}
		}
		return preferences(fresh);
	}
}
