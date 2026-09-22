package net.mezzdev.config.gui.neoforge;

import net.mezzdev.config.gui.screenlist.ConfigScreenOwnerMetadata;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IConfigurable;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;

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

	@Nullable
	private static Path getIconPath(IModInfo modInfo) {
		return getPreferredIconFile(modInfo.getConfig(), modInfo.getOwningFile().getConfig(), modInfo.getLogoFile().orElse(null))
			.map(NeoForgeConfigScreenOwnerMetadata::toPathParts)
			.flatMap(pathParts -> modInfo.getOwningFile().getFile().getContents().findFile(String.join("/", pathParts)))
			.map(Path::of)
			.filter(Files::exists)
			.orElse(null);
	}

	static Optional<String> getPreferredIconFile(
		IConfigurable modConfig,
		IConfigurable fileConfig,
		@Nullable String legacyLogoFile
	) {
		return modConfig.<String>getConfigElement("iconFile")
			.or(() -> fileConfig.getConfigElement("iconFile"))
			.or(() -> Optional.ofNullable(legacyLogoFile))
			.or(() -> fileConfig.getConfigElement("logoFile"))
			.or(() -> modConfig.getConfigElement("bannerFile"))
			.or(() -> fileConfig.getConfigElement("bannerFile"));
	}

	private static String[] toPathParts(String path) {
		return path.replace('\\', '/').split("/");
	}
}
