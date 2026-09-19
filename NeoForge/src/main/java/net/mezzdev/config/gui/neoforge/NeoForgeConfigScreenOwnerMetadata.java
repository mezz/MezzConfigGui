package net.mezzdev.config.gui.neoforge;

import net.mezzdev.config.gui.screenlist.ConfigScreenOwnerMetadata;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

final class NeoForgeConfigScreenOwnerMetadata {
	private NeoForgeConfigScreenOwnerMetadata() {

	}

	public static ConfigScreenOwnerMetadata get(String modId) {
		return ModList.get()
			.getModContainerById(modId)
			.map(NeoForgeConfigScreenOwnerMetadata::get)
			.orElseGet(ConfigScreenOwnerMetadata::new);
	}

	private static ConfigScreenOwnerMetadata get(ModContainer modContainer) {
		IModInfo modInfo = modContainer.getModInfo();
		return new ConfigScreenOwnerMetadata(getIconPath(modInfo));
	}

	private static Optional<Path> getIconPath(IModInfo modInfo) {
		return modInfo.getLogoFile()
			.map(NeoForgeConfigScreenOwnerMetadata::toPathParts)
			.flatMap(pathParts -> modInfo.getOwningFile().getFile().getContents().findFile(String.join("/", pathParts)))
			.map(Path::of)
			.filter(Files::exists);
	}

	private static String[] toPathParts(String path) {
		return path.replace('\\', '/').split("/");
	}
}
