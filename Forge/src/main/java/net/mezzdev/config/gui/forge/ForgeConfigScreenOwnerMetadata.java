package net.mezzdev.config.gui.forge;

import net.mezzdev.config.gui.screenlist.ConfigScreenOwnerMetadata;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

final class ForgeConfigScreenOwnerMetadata {
	private ForgeConfigScreenOwnerMetadata() {

	}

	public static ConfigScreenOwnerMetadata get(String modId) {
		return ModList.get()
			.getModContainerById(modId)
			.map(ForgeConfigScreenOwnerMetadata::get)
			.orElseGet(ConfigScreenOwnerMetadata::new);
	}

	private static ConfigScreenOwnerMetadata get(ModContainer modContainer) {
		IModInfo modInfo = modContainer.getModInfo();
		return modInfo.getLogoFile()
			.map(iconFile -> getIcon(modInfo.getOwningFile().getFile()::findResource, iconFile))
			.orElseGet(ConfigScreenOwnerMetadata::new);
	}

	static ConfigScreenOwnerMetadata getIcon(Function<String[], Path> findResource, String iconFile) {
		Path iconPath = findResource.apply(toPathParts(iconFile));
		if (Files.isRegularFile(iconPath)) {
			return new ConfigScreenOwnerMetadata(iconPath);
		}
		return new ConfigScreenOwnerMetadata();
	}

	private static String[] toPathParts(String path) {
		return path.replace('\\', '/').split("/");
	}
}
