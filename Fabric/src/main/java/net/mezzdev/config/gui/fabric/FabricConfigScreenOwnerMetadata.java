package net.mezzdev.config.gui.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.mezzdev.config.gui.screenlist.ConfigScreenOwnerMetadata;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.Optional;

final class FabricConfigScreenOwnerMetadata {
	private static final int ICON_SIZE = 64;

	private FabricConfigScreenOwnerMetadata() {

	}

	public static ConfigScreenOwnerMetadata get(String modId) {
		return FabricLoader.getInstance()
			.getModContainer(modId)
			.map(FabricConfigScreenOwnerMetadata::get)
			.orElseGet(() -> new ConfigScreenOwnerMetadata(Component.literal(modId)));
	}

	private static ConfigScreenOwnerMetadata get(ModContainer modContainer) {
		ModMetadata metadata = modContainer.getMetadata();
		Optional<Path> iconPath = metadata.getIconPath(ICON_SIZE)
			.flatMap(modContainer::findPath);
		return new ConfigScreenOwnerMetadata(
			Component.literal(metadata.getName()),
			iconPath
		);
	}
}
