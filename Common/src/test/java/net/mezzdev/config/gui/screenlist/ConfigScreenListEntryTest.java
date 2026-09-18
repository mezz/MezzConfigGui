package net.mezzdev.config.gui.screenlist;

import net.mezzdev.config.gui.ConfigScreenNavigation;
import net.mezzdev.config.gui.config.ConfigGuiOptionsTestUtil;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigScreenListEntryTest {
	@Test
	void packOrderComesFirstAndUnlistedModsRemainAlphabetical() {
		List<ConfigScreenListEntry> entries = List.of(entry("zebra"), entry("beta"), entry("alpha"), entry("gamma"));
		try (var order = ConfigGuiOptionsTestUtil.setValue("modOrder", List.of("missing", " GAMMA ", "beta", "gamma"))) {
			assertEquals(List.of("gamma", "beta", "alpha", "zebra"), ids(ConfigScreenListEntry.applyPreferences(entries)));
		}
		assertEquals(List.of("alpha", "beta", "gamma", "zebra"), ids(ConfigScreenListEntry.applyPreferences(entries)));
	}

	@Test
	void hidingWinsOverOrderingAndPreferencesDoNotDiscardRegisteredEntries() {
		ConfigScreenNavigation navigation = new ConfigScreenNavigation();
		navigation.setScreenListEntries(List.of(entry("zebra"), entry("alpha"), entry("beta")));
		try (var order = ConfigGuiOptionsTestUtil.setValue("modOrder", List.of("beta", "zebra"));
			var hidden = ConfigGuiOptionsTestUtil.setValue("hiddenMods", List.of(" BETA ", "missing"))
		) {
			assertEquals(List.of("zebra", "alpha"), ids(navigation.getScreenListEntries()));
		}
		assertEquals(List.of("alpha", "beta", "zebra"), ids(navigation.getScreenListEntries()));
	}

	private static List<String> ids(List<ConfigScreenListEntry> entries) {
		return entries.stream().map(ConfigScreenListEntry::modId).toList();
	}

	private static ConfigScreenListEntry entry(String modId) {
		Component title = Component.literal(modId);
		return new ConfigScreenListEntry(modId, title, parent -> {
			throw new AssertionError("Navigation sorting must not open screens");
		}, new ConfigScreenOwnerIcon(modId, title, Optional.empty()));
	}
}
