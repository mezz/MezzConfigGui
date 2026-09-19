package net.mezzdev.config.gui.model;

import net.mezzdev.config.gui.ConfigScreenCategory;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Groups the entry widgets for a config category and exposes category info for hover panels.
 */
public final class ConfigCategoryWidget {
	private final ConfigScreenCategory category;
	private final List<ConfigEntryWidget<?>> entryWidgets;
	private final ConfigSectionHeader categoryHeader;
	private final Map<Integer, ConfigSectionHeader> sectionHeaders = new LinkedHashMap<>();

	public ConfigCategoryWidget(
		ConfigScreenCategory category,
		List<ConfigEntryWidget<?>> entryWidgets
	) {
		this(category, entryWidgets, List.of(), () -> {});
	}

	public ConfigCategoryWidget(
		ConfigScreenCategory category,
		List<ConfigEntryWidget<?>> entryWidgets,
		List<Section> sections
	) {
		this(category, entryWidgets, sections, () -> {});
	}

	public ConfigCategoryWidget(
		ConfigScreenCategory category,
		List<ConfigEntryWidget<?>> entryWidgets,
		List<Section> sections,
		Runnable layoutUpdater
	) {
		this.category = category;
		this.entryWidgets = List.copyOf(entryWidgets);
		this.categoryHeader = new ConfigSectionHeader(category.getLocalizedName(), category.getLocalizedDescription(), layoutUpdater);
		for (Section section : sections) {
			sectionHeaders.put(section.firstEntryIndex(), new ConfigSectionHeader(section.title(), section.description(), layoutUpdater));
		}
	}

	public ConfigSectionHeader getCategoryHeader() {
		return categoryHeader;
	}

	public List<ConfigEntryWidget<?>> getEntryWidgets() {
		return entryWidgets;
	}

	@Nullable
	public ConfigSectionHeader getSectionHeader(int entryIndex) {
		return sectionHeaders.get(entryIndex);
	}

	public Collection<ConfigSectionHeader> getSectionHeaders() {
		return sectionHeaders.values();
	}

	public Stream<ConfigSectionHeader> getAllSectionHeaders() {
		return Stream.concat(Stream.of(categoryHeader), sectionHeaders.values().stream());
	}

	public void resetBounds() {
		categoryHeader.resetBounds();
		for (ConfigSectionHeader header : sectionHeaders.values()) {
			header.resetBounds();
		}
		for (ConfigEntryWidget<?> entryWidget : entryWidgets) {
			entryWidget.resetBounds();
		}
	}

	public ConfigInfo getInfo() {
		return new ConfigInfo(category.getLocalizedName(), category.getLocalizedDescription());
	}

	public record Section(int firstEntryIndex, Component title, Component description) {
	}
}
