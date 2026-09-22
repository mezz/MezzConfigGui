package net.mezzdev.config.gui.forge;

import net.mezzdev.config.gui.screenlist.ConfigScreenOwnerMetadata;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;

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
		return new ConfigScreenOwnerMetadata(getIconPath(modInfo));
	}

	@Nullable
	private static Path getIconPath(IModInfo modInfo) {
		return modInfo.getLogoFile()
			.map(ForgeConfigScreenOwnerMetadata::toPathParts)
			.map(pathParts -> modInfo.getOwningFile().getFile().findResource(pathParts))
			.filter(Files::exists)
			.orElse(null);
	}

	private static String[] toPathParts(String path) {
		return path.replace('\\', '/').split("/");
	}
}
