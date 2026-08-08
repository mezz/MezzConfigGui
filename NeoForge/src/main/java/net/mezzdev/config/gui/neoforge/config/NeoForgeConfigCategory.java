package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.gui.ConfigScreenCategory;
import net.mezzdev.config.gui.ConfigScreenCategoryGroup;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigLocalizedCategory;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Collection;
import java.util.List;

record NeoForgeConfigCategory(
	String name,
	String localizationKey,
	Component localizedName,
	Component localizedDescription,
	ModConfig modConfig,
	ModConfigSpec modConfigSpec,
	List<NeoForgeConfigValue<?>> configValues
) implements ConfigScreenCategory, IConfigLocalizedCategory {
	public NeoForgeConfigCategory {
		configValues = List.copyOf(configValues);
	}

	@Override
	public ConfigScreenCategoryGroup getGroup() {
		if (modConfig.getType() == ModConfig.Type.SERVER) {
			return ConfigScreenCategoryGroup.LOADER_NATIVE_SERVER;
		}
		return ConfigScreenCategoryGroup.LOADER_NATIVE;
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
