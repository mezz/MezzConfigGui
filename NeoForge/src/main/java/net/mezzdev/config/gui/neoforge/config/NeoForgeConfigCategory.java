package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.schema.IConfigCategory;
import net.mezzdev.config.api.value.IConfigValue;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Collection;
import java.util.List;

record NeoForgeConfigCategory(
	String name,
	Component localizedName,
	Component localizedDescription,
	ModConfig modConfig,
	ModConfigSpec modConfigSpec,
	List<NeoForgeConfigValue<?>> configValues
) implements IConfigCategory {
	public NeoForgeConfigCategory {
		configValues = List.copyOf(configValues);
	}

	@Override
	public String getName() {
		return name;
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
	public Collection<? extends IConfigValue<?>> getConfigValues() {
		return configValues;
	}
}
