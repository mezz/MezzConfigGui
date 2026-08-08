package net.mezzdev.config.gui;

import net.mezzdev.config.api.files.ConfigManagers;
import net.mezzdev.config.api.files.IConfigManager;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Creates generated config GUI screens from schemas registered with MezzConfig.
 */
public final class MezzConfigScreenConfigs {
	private MezzConfigScreenConfigs() {

	}

	public static List<ConfigScreenConfig> getActiveConfigScreens() {
		return ConfigManagers.getConfigManager()
			.map(MezzConfigScreenConfigs::getConfigScreens)
			.orElseGet(List::of);
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

	private static String getDisplayNameFallback(String name) {
		String[] words = name
			.replace('-', '_')
			.replace('.', '_')
			.split("_+");
		StringBuilder result = new StringBuilder();
		for (String word : words) {
			if (word.isBlank()) {
				continue;
			}
			if (!result.isEmpty()) {
				result.append(' ');
			}
			String lowercaseWord = word.toLowerCase(Locale.ROOT);
			result.append(Character.toUpperCase(lowercaseWord.charAt(0)));
			if (lowercaseWord.length() > 1) {
				result.append(lowercaseWord.substring(1));
			}
		}
		if (result.isEmpty()) {
			return name;
		}
		return result.toString();
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
			String fallback = "%s Configuration".formatted(getDisplayNameFallback(modId));
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
				.filter(schema -> schema.getPath().isPresent())
				.map(ConfigScreenSchema::from)
				.toList();
			return new MergedConfigScreenSchema(activeSchemas)
				.getCategories();
		}
	}
}
