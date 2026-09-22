package net.mezzdev.config.gui.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.mezzdev.config.gui.screenlist.ConfigScreenOwnerMetadata;

final class FabricConfigScreenOwnerMetadata {
	private static final int ICON_SIZE = 64;

	private FabricConfigScreenOwnerMetadata() {

	}

	public static ConfigScreenOwnerMetadata get(String modId) {
		return FabricLoader.getInstance()
			.getModContainer(modId)
			.map(FabricConfigScreenOwnerMetadata::get)
			.orElseGet(ConfigScreenOwnerMetadata::new);
	}

	private static ConfigScreenOwnerMetadata get(ModContainer modContainer) {
		ModMetadata metadata = modContainer.getMetadata();
		return new ConfigScreenOwnerMetadata(
			metadata.getIconPath(ICON_SIZE)
				.flatMap(modContainer::findPath)
				.orElse(null)
		);
	}
}
