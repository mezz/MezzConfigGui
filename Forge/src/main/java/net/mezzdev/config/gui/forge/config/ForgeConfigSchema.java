package net.mezzdev.config.gui.forge.config;

import net.mezzdev.config.gui.ConfigScreenSchema;

import java.util.Collection;
import java.util.List;

final class ForgeConfigSchema implements ConfigScreenSchema {
	private final List<ForgeConfigCategory> categories;

	public ForgeConfigSchema(Collection<ForgeConfigCategory> categories) {
		this.categories = List.copyOf(categories);
	}

	@Override
	public List<ForgeConfigCategory> getCategories() {
		return categories;
	}
}
