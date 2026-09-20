package net.mezzdev.config.gui.forge;

import net.minecraftforge.eventbus.api.IEventBus;

public final class ConfigGuiForgeClientSafeRunner {
	private final IEventBus modEventBus;
	private final ConfigGuiForgeNetwork network;

	public ConfigGuiForgeClientSafeRunner(IEventBus modEventBus, ConfigGuiForgeNetwork network) {
		this.modEventBus = modEventBus;
		this.network = network;
	}

	public void registerClient() {
		ConfigGuiForgeClient.register(modEventBus, network);
	}
}
