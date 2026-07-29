package net.mezzdev.config.gui.forge;

import net.minecraftforge.eventbus.api.IEventBus;

public final class ConfigGuiForgeClientSafeRunner {
	private final IEventBus modEventBus;

	public ConfigGuiForgeClientSafeRunner(IEventBus modEventBus) {
		this.modEventBus = modEventBus;
	}

	public void registerClient() {
		ConfigGuiForgeClient.register(modEventBus);
	}
}
