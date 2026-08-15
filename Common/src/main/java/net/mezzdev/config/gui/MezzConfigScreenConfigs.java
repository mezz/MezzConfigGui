package net.mezzdev.config.gui;

import net.mezzdev.config.api.files.IConfigManager;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.util.ConfigNameUtil;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Creates generated config GUI screens from schemas registered with MezzConfig.
 */
public final class MezzConfigScreenConfigs {
	private static volatile @Nullable IConfigManager configManager;

	private MezzConfigScreenConfigs() {

	}

	public static void setConfigManager(IConfigManager configManager) {
		MezzConfigScreenConfigs.configManager = Objects.requireNonNull(configManager, "configManager");
	}

	public static List<ConfigScreenConfig> getActiveConfigScreens() {
		IConfigManager configManager = MezzConfigScreenConfigs.configManager;
		if (configManager == null) {
			return List.of();
		}
		return getConfigScreens(configManager);
	}

	public static List<ConfigScreenConfig> getConfigScreens(IConfigManager configManager) {
		Objects.requireNonNull(configManager, "configManager");
		Map<String, List<IConfigSchema>> schemasByModId = new LinkedHashMap<>();
		configManager.getSchemas()
			.stream()
			.sorted(Comparator
				.comparing(IConfigSchema::getModId)
				.thenComparing(MezzConfigScreenConfigs::getSortablePath)
			)
			.forEach(schema -> schemasByModId.computeIfAbsent(schema.getModId(), ignored -> new ArrayList<>())
				.add(schema));

		return schemasByModId.entrySet()
			.stream()
			.map(entry -> new MezzConfigManagerScreenConfig(entry.getKey(), entry.getValue()))
			.map(ConfigScreenConfig.class::cast)
			.toList();
	}

	private static String getSortablePath(IConfigSchema schema) {
		return schema.getPath()
			.map(Path::toString)
			.orElse("");
	}

	private record MezzConfigManagerScreenConfig(
		String modId,
		List<IConfigSchema> schemas
	) implements ConfigScreenConfig {
		private MezzConfigManagerScreenConfig {
			Objects.requireNonNull(modId, "modId");
			schemas = List.copyOf(schemas);
		}

		@Override
		public String getModId() {
			return modId;
		}

		@Override
		public Component getTitle() {
			String localizationKey = modId + ".config.screen.title";
			String fallback = "%s Configuration".formatted(ConfigNameUtil.getDisplayNameFallback(modId));
			return Component.translatableWithFallback(localizationKey, fallback);
		}

		@Override
		public ConfigScreenSchema getSchema() {
			return new MergedMezzConfigScreenSchema(schemas);
		}
	}

	private record MergedMezzConfigScreenSchema(
		List<IConfigSchema> schemas
	) implements ConfigScreenSchema {
		private MergedMezzConfigScreenSchema {
			schemas = List.copyOf(schemas);
		}

		@Override
		public List<? extends ConfigScreenCategory> getCategories() {
			List<ConfigScreenSchema> activeSchemas = schemas.stream()
				.filter(IConfigSchema::isActive)
				.map(ConfigScreenSchema::from)
				.toList();
			return new MergedConfigScreenSchema(activeSchemas)
				.getCategories();
		}

		@Override
		public Optional<IConfigSchema> findBackingSchema(IConfigScreenValue<?> value) {
			return schemas.stream()
				.map(ConfigScreenSchema::from)
				.map(schema -> schema.findBackingSchema(value))
				.flatMap(Optional::stream)
				.findFirst();
		}
	}
}
