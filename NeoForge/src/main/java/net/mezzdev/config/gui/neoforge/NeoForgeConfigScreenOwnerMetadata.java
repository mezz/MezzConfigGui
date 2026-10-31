package net.mezzdev.config.gui.neoforge;

import net.mezzdev.config.gui.screenlist.ConfigScreenOwnerMetadata;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.jarcontents.JarResource;
import net.neoforged.neoforgespi.language.IConfigurable;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jspecify.annotations.Nullable;

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
		return getPreferredIconFile(modInfo.getConfig(), modInfo.getOwningFile().getConfig(), modInfo.getLogoFile().orElse(null))
			.map(iconFile -> getIcon(modInfo.getOwningFile().getFile().getContents(), iconFile))
			.orElseGet(ConfigScreenOwnerMetadata::new);
	}

	static ConfigScreenOwnerMetadata getIcon(JarContents contents, String iconFile) {
		JarResource resource = contents.get(iconFile);
		if (resource == null) {
			return new ConfigScreenOwnerMetadata();
		}
		return ConfigScreenOwnerMetadata.fromStream(resource::open);
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
}
