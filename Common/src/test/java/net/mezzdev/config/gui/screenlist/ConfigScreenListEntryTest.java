package net.mezzdev.config.gui.screenlist;

import net.mezzdev.config.gui.ConfigScreenNavigation;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigScreenListEntryTest {
	@Test
	void unconfiguredNavigationDefaultsToAlphabeticalOrder() {
		List<ConfigScreenListEntry> entries = List.of(entry("zebra"), entry("beta"), entry("alpha"), entry("gamma"));
		assertEquals(List.of("alpha", "beta", "gamma", "zebra"), ids(ConfigScreenListEntry.applyPreferences(entries)));
	}

	@Test
	void navigationUsesAllRegisteredEntries() {
		ConfigScreenNavigation navigation = new ConfigScreenNavigation();
		navigation.setScreenListEntries(List.of(entry("zebra"), entry("alpha"), entry("beta")));
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
