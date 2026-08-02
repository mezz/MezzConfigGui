package net.mezzdev.config.gui.model;

import net.mezzdev.config.gui.ConfigScreenCategory;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.api.ConfigInfo;

import java.util.List;

/**
 * Groups the entry widgets for a config category and exposes category info for hover panels.
 */
public final class ConfigCategoryWidget {
	private final ConfigScreenCategory category;
	private final List<ConfigEntryWidget<?>> entryWidgets;

	public ConfigCategoryWidget(
		ConfigScreenCategory category,
		List<ConfigEntryWidget<?>> entryWidgets
	) {
		this.category = category;
		this.entryWidgets = List.copyOf(entryWidgets);
	}

	public List<ConfigEntryWidget<?>> getEntryWidgets() {
		return entryWidgets;
	}

	public void resetBounds() {
		for (ConfigEntryWidget<?> entryWidget : entryWidgets) {
			entryWidget.resetBounds();
		}
	}

	public ConfigInfo getInfo() {
		return new ConfigInfo(category.getLocalizedName(), category.getLocalizedDescription());
	}
}
