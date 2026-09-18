package net.mezzdev.config.gui.screenlist;

import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.util.ConfigLocale;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One resolved config screen entry shared by direct navigation and the browse-all screen.
 */
public record ConfigScreenListEntry(
	String modId,
	Component title,
	IConfigScreenFactory factory,
	ConfigScreenOwnerIcon icon
) {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Comparator<ConfigScreenListEntry> ALPHABETICAL_ORDER = Comparator.comparing(ConfigScreenListEntry::sortName)
		.thenComparing(ConfigScreenListEntry::modId);

	public ConfigScreenListEntry {
		Objects.requireNonNull(modId, "modId");
		Objects.requireNonNull(title, "title");
		Objects.requireNonNull(factory, "factory");
		Objects.requireNonNull(icon, "icon");
	}

	public static List<ConfigScreenListEntry> create(
		List<ConfigScreenFactoryEntry> factoryEntries,
		ConfigScreenOwnerMetadataProvider metadataProvider
	) {
		return factoryEntries.stream()
			.map(factoryEntry -> create(factoryEntry, metadataProvider))
			.sorted(ALPHABETICAL_ORDER)
			.toList();
	}

	public static List<ConfigScreenListEntry> applyPreferences(List<ConfigScreenListEntry> entries) {
		Map<String, Integer> order = new HashMap<>();
		for (String modId : ConfigGuiOptions.getModOrder()) {
			order.putIfAbsent(normalizeModId(modId), order.size());
		}
		Set<String> hidden = ConfigGuiOptions.getHiddenMods().stream()
			.map(ConfigScreenListEntry::normalizeModId)
			.collect(Collectors.toSet());
		return entries.stream()
			.filter(entry -> !hidden.contains(entry.modId()))
			.sorted(Comparator.<ConfigScreenListEntry>comparingInt(entry -> order.getOrDefault(entry.modId(), Integer.MAX_VALUE))
				.thenComparing(ALPHABETICAL_ORDER))
			.toList();
	}

	private static String normalizeModId(String modId) {
		return modId.strip().toLowerCase(Locale.ROOT);
	}

	private static ConfigScreenListEntry create(
		ConfigScreenFactoryEntry factoryEntry,
		ConfigScreenOwnerMetadataProvider metadataProvider
	) {
		String modId = factoryEntry.modId();
		Component title = factoryEntry.title();
		ConfigScreenOwnerMetadata metadata = getMetadata(modId, metadataProvider);
		return new ConfigScreenListEntry(
			modId,
			title,
			factoryEntry.factory(),
			new ConfigScreenOwnerIcon(modId, title, metadata.iconPath())
		);
	}

	private static ConfigScreenOwnerMetadata getMetadata(
		String modId,
		ConfigScreenOwnerMetadataProvider metadataProvider
	) {
		try {
			ConfigScreenOwnerMetadata metadata = metadataProvider.getMetadata(modId);
			if (metadata != null) {
				return metadata;
			}
		} catch (RuntimeException | LinkageError e) {
			LOGGER.warn("Failed to load config screen metadata for mod id: {}", modId, e);
		}
		return new ConfigScreenOwnerMetadata();
	}

	public String sortName() {
		return ConfigLocale.toLowercase(title.getString());
	}

	public boolean matches(String searchText) {
		return ConfigLocale.toLowercase(title.getString()).contains(searchText);
	}
}
