package net.mezzdev.config.gui.config;

import net.mezzdev.config.api.sorting.ISortingConfig;
import net.mezzdev.config.gui.api.IConfigScreenCategoryBuilder;
import net.mezzdev.config.gui.screenlist.ConfigScreenListEntry;
import net.mezzdev.config.gui.util.ConfigLocale;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Owns the complete mod list, including hidden entries that can be restored in the sorting editor. */
public final class ModNavigationConfig {
	private final ISortingConfig<String> sortingConfig;
	private Map<String, ConfigScreenListEntry> entries = Map.of();

	public ModNavigationConfig(Function<Comparator<String>, ISortingConfig<String>> factory) {
		Comparator<String> comparator = Comparator.comparing(this::sortName).thenComparing(Comparator.naturalOrder());
		this.sortingConfig = factory.apply(comparator);
	}

	public void setEntries(List<ConfigScreenListEntry> entries) {
		Map<String, ConfigScreenListEntry> byId = new LinkedHashMap<>();
		for (ConfigScreenListEntry entry : entries) {
			byId.put(entry.modId(), entry);
		}
		this.entries = byId;
	}

	public List<ConfigScreenListEntry> applyPreferences(List<ConfigScreenListEntry> entries) {
		setEntries(entries);
		return sortingConfig.getSortedValues(this.entries.keySet()).stream()
			.map(this.entries::get)
			.toList();
	}

	public void configureCategory(IConfigScreenCategoryBuilder category) {
		Map<String, ConfigScreenListEntry> entries = new LinkedHashMap<>(this.entries);
		category.clearDefaultValues()
			.addStringSortingConfig("modOrder", "mezz_config_gui.config.modList.modOrder", sortingConfig, entries.keySet())
			.setValueName(modId -> entries.get(modId).title())
			.setValueDescription(modId -> Optional.of(Component.literal(modId)))
			.setValueIcon(modId -> Optional.of((graphics, area) -> entries.get(modId).icon().draw(
				graphics,
				Minecraft.getInstance().font,
				new ImmutableRect2i(area.getX(), area.getY(), area.getWidth(), area.getHeight())
			)));
	}

	public void migrateLegacyPreferences(Path path, List<String> order, List<String> hidden) {
		if (order.isEmpty() && hidden.isEmpty()) {
			return;
		}
		List<String> orderedIds = normalizeIds(order);
		List<String> hiddenIds = normalizeIds(hidden);
		LinkedHashSet<String> allIds = new LinkedHashSet<>(orderedIds);
		allIds.addAll(hiddenIds);
		List<String> visibleIds = orderedIds.stream().filter(id -> !hiddenIds.contains(id)).toList();
		sortingConfig.setLegacyMigration(List.of(path), (source, context) -> context.setSortedValues(allIds, visibleIds));
	}

	private String sortName(String modId) {
		ConfigScreenListEntry entry = entries.get(modId);
		if (entry == null) {
			return ConfigLocale.toLowercase(modId);
		}
		return entry.sortName();
	}

	private static List<String> normalizeIds(List<String> ids) {
		return ids.stream()
			.map(id -> id.strip().toLowerCase(Locale.ROOT))
			.filter(id -> !id.isEmpty())
			.distinct()
			.toList();
	}
}
