package net.mezzdev.config.gui.forge;

import net.mezzdev.config.gui.screenlist.ConfigScreenOwnerMetadata;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

final class ForgeConfigScreenOwnerMetadata {
	private ForgeConfigScreenOwnerMetadata() {

	}

	public static ConfigScreenOwnerMetadata get(String modId) {
		return ModList.get()
			.getModContainerById(modId)
			.map(ForgeConfigScreenOwnerMetadata::get)
			.orElseGet(() -> new ConfigScreenOwnerMetadata(Component.literal(modId)));
	}

	private static ConfigScreenOwnerMetadata get(ModContainer modContainer) {
		IModInfo modInfo = modContainer.getModInfo();
		return new ConfigScreenOwnerMetadata(
			Component.literal(modInfo.getDisplayName()),
			getIconPath(modInfo)
		);
	}

	private static Optional<Path> getIconPath(IModInfo modInfo) {
		return modInfo.getLogoFile()
			.map(ForgeConfigScreenOwnerMetadata::toPathParts)
			.map(pathParts -> modInfo.getOwningFile().getFile().findResource(pathParts))
			.filter(Files::exists);
	}

	private static String[] toPathParts(String path) {
		return path.replace('\\', '/').split("/");
	}
}
