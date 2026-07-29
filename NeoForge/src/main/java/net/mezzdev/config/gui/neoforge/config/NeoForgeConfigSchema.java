package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.schema.IConfigCategory;
import net.mezzdev.config.api.schema.IConfigEditableSchema;
import net.mezzdev.config.api.value.ConfigValueChange;
import net.mezzdev.config.api.value.ConfigValueUpdateType;
import net.mezzdev.config.api.value.IConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class NeoForgeConfigSchema implements IConfigEditableSchema {
	private static final Logger LOGGER = LogManager.getLogger();

	private final Path path;
	private final List<NeoForgeConfigCategory> categories;
	private final Map<IConfigValue<?>, NeoForgeConfigValue<?>> values;

	public NeoForgeConfigSchema(String modId, Collection<NeoForgeConfigCategory> categories) {
		this.path = Path.of(modId + "-neoforge-configs");
		this.categories = List.copyOf(categories);
		this.values = createValueMap(categories);
	}

	private static Map<IConfigValue<?>, NeoForgeConfigValue<?>> createValueMap(Collection<NeoForgeConfigCategory> categories) {
		Map<IConfigValue<?>, NeoForgeConfigValue<?>> values = new IdentityHashMap<>();
		for (NeoForgeConfigCategory category : categories) {
			for (NeoForgeConfigValue<?> value : category.configValues()) {
				values.put(value, value);
			}
		}
		return values;
	}

	@Override
	public Path getPath() {
		return path;
	}

	@Override
	public List<? extends IConfigCategory> getCategories() {
		return categories;
	}

	@Override
	public void clearListeners() {

	}

	@Override
	public ConfigValueUpdateType getUpdateType(List<ConfigValueChange<?>> changes) {
		ConfigValueUpdateType updateType = ConfigValueUpdateType.IMMEDIATE;
		for (ConfigValueChange<?> change : changes) {
			updateType = max(updateType, change.configValue().getUpdateType());
		}
		return updateType;
	}

	@Override
	public ConfigValueUpdateType applyChanges(List<ConfigValueChange<?>> changes) {
		ConfigValueUpdateType updateType = ConfigValueUpdateType.IMMEDIATE;
		Set<ModConfigSpec> changedSpecs = new LinkedHashSet<>();
		for (ConfigValueChange<?> change : changes) {
			NeoForgeConfigValue<?> value = values.get(change.configValue());
			if (value == null) {
				LOGGER.error("Tried to apply a change for a NeoForge config value from another schema: {}", change.configValue().getName());
				continue;
			}
			if (value.apply(change)) {
				updateType = max(updateType, value.getUpdateType());
				changedSpecs.add(value.getModConfigSpec());
			}
		}
		for (ModConfigSpec modConfigSpec : changedSpecs) {
			modConfigSpec.save();
		}
		return updateType;
	}

	private static ConfigValueUpdateType max(ConfigValueUpdateType first, ConfigValueUpdateType second) {
		if (first == ConfigValueUpdateType.RESTART || second == ConfigValueUpdateType.RESTART) {
			return ConfigValueUpdateType.RESTART;
		}
		if (first == ConfigValueUpdateType.ON_APPLY || second == ConfigValueUpdateType.ON_APPLY) {
			return ConfigValueUpdateType.ON_APPLY;
		}
		return ConfigValueUpdateType.IMMEDIATE;
	}
}
