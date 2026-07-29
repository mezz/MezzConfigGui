package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.schema.IConfigEditableSchema;
import net.mezzdev.config.gui.api.ConfigRestartResult;
import net.mezzdev.config.gui.api.IConfigScreenConfig;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;

final class NeoForgeConfigScreenConfig implements IConfigScreenConfig {
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
	public IConfigEditableSchema getSchema() {
		return new NeoForgeConfigSchema(getModId(), NeoForgeConfigScreenConfigs.getCategories(modContainer));
	}

	@Override
	public ConfigRestartResult onRestartRequired() {
		return ConfigRestartResult.NEXT_GAME_START;
	}
}
