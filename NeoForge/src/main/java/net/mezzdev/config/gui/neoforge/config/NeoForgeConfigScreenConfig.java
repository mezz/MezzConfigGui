package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.gui.ConfigScreenConfig;
import net.mezzdev.config.gui.ConfigScreenSchema;
import net.mezzdev.config.gui.api.ConfigRestartResult;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;

final class NeoForgeConfigScreenConfig implements ConfigScreenConfig {
	private final ModContainer modContainer;
	private final Component title;

	public NeoForgeConfigScreenConfig(ModContainer modContainer) {
		this.modContainer = modContainer;
		this.title = NeoForgeConfigLocalization.getScreenTitle(modContainer);
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
		return new NeoForgeConfigSchema(NeoForgeConfigScreenConfigs.getCategories(modContainer));
	}

	@Override
	public ConfigRestartResult onRestartRequired() {
		return ConfigRestartResult.NEXT_GAME_START;
	}
}
