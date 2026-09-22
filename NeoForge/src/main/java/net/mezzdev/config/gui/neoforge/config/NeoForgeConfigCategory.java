package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.gui.ConfigScreenCategory;
import net.mezzdev.config.gui.ConfigScreenCategoryGroup;
import net.mezzdev.config.gui.ConfigScreenCategoryNavigationGroup;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigLocalizedCategory;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

record NeoForgeConfigCategory(
	String name,
	String localizationKey,
	Component localizedName,
	Component localizedDescription,
	ConfigScreenCategoryGroup group,
	@Nullable ConfigScreenCategoryNavigationGroup navigationGroup,
	List<IConfigScreenValue<?>> configValues
) implements ConfigScreenCategory, IConfigLocalizedCategory {
	public NeoForgeConfigCategory {
		configValues = List.copyOf(configValues);
	}

	@Override
	public ConfigScreenCategoryGroup getGroup() {
		return group;
	}

	@Override
	@Nullable
	public ConfigScreenCategoryNavigationGroup getNavigationGroup() {
		return navigationGroup;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getLocalizationKey() {
		return localizationKey;
	}

	@Override
	public Component getLocalizedName() {
		return localizedName;
	}

	@Override
	public Component getLocalizedDescription() {
		return localizedDescription;
	}

	@Override
	public Collection<? extends IConfigScreenValue<?>> getConfigValues() {
		return configValues;
	}
}
