package net.mezzdev.config.gui;

import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Combines config screen sources owned by the same mod.
 */
final class MergedConfigScreenConfig implements ConfigScreenConfig {
	private final String modId;
	private final List<ConfigScreenConfig> configScreens;

	MergedConfigScreenConfig(String modId, Collection<? extends ConfigScreenConfig> configScreens) {
		this.modId = Objects.requireNonNull(modId, "modId");
		this.configScreens = List.copyOf(configScreens);
		if (this.configScreens.isEmpty()) {
			throw new IllegalArgumentException("configScreens must not be empty.");
		}
		for (ConfigScreenConfig configScreen : this.configScreens) {
			if (!modId.equals(configScreen.getModId())) {
				throw new IllegalArgumentException("Cannot merge a config screen owned by a different mod: " + configScreen.getModId());
			}
		}
	}

	@Override
	public String getModId() {
		return modId;
	}

	@Override
	public Component getTitle() {
		return configScreens.getFirst().getTitle();
	}

	@Override
	public ConfigScreenSchema getSchema() {
		List<ConfigScreenSchema> schemas = configScreens.stream()
			.map(ConfigScreenConfig::getSchema)
			.toList();
		return new MergedConfigScreenSchema(schemas);
	}
}
