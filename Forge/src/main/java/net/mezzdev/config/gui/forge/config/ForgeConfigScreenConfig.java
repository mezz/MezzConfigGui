package net.mezzdev.config.gui.forge.config;

import net.mezzdev.config.gui.ConfigScreenConfig;
import net.mezzdev.config.gui.ConfigScreenSchema;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModContainer;

final class ForgeConfigScreenConfig implements ConfigScreenConfig {
	private final ModContainer modContainer;
	private final Component title;

	public ForgeConfigScreenConfig(ModContainer modContainer) {
		this.modContainer = modContainer;
		this.title = ForgeConfigLocalization.getScreenTitle(modContainer);
	}

	@Override
	public String getModId() {
		return modContainer.getModId();
	}

	@Override
	public Component getTitle() {
		return title;
	}

	@Override
	public ConfigScreenSchema getSchema() {
		return new ForgeConfigSchema(ForgeConfigScreenConfigs.getCategories(modContainer));
	}
}
