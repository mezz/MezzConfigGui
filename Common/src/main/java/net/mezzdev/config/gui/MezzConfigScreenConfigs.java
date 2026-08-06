package net.mezzdev.config.gui;

import net.mezzdev.config.api.files.ConfigManagers;
import net.mezzdev.config.api.files.IConfigManager;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
			return Component.translatable("mezz_config.config.screen.title");
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
			Map<String, MutableMergedConfigScreenCategory> categories = new LinkedHashMap<>();
			for (IConfigSchema schema : schemas) {
				if (schema.getPath().isEmpty()) {
					continue;
				}
				for (ConfigScreenCategory category : ConfigScreenSchema.from(schema).getCategories()) {
					categories.computeIfAbsent(
							category.getName(),
							ignored -> new MutableMergedConfigScreenCategory(category)
						)
						.addValues(category.getConfigValues());
				}
			}
			return categories.values()
				.stream()
				.map(MutableMergedConfigScreenCategory::toConfigScreenCategory)
				.toList();
		}
	}

	private static final class MutableMergedConfigScreenCategory {
		private final String name;
		private final String localizationKey;
		private final Component title;
		private final Component description;
		private final List<IConfigScreenValue<?>> values = new ArrayList<>();

		private MutableMergedConfigScreenCategory(ConfigScreenCategory category) {
			this.name = category.getName();
			this.localizationKey = category.getLocalizationKey();
			this.title = category.getLocalizedName();
			this.description = category.getLocalizedDescription();
		}

		public void addValues(Collection<? extends IConfigScreenValue<?>> values) {
			this.values.addAll(values);
		}

		public ConfigScreenCategory toConfigScreenCategory() {
			return new MergedConfigScreenCategory(
				name,
				localizationKey,
				title,
				description,
				values
			);
		}
	}

	private record MergedConfigScreenCategory(
		String name,
		String localizationKey,
		Component title,
		Component description,
		List<IConfigScreenValue<?>> values
	) implements ConfigScreenCategory {
		private MergedConfigScreenCategory {
			Objects.requireNonNull(name, "name");
			Objects.requireNonNull(localizationKey, "localizationKey");
			Objects.requireNonNull(title, "title");
			Objects.requireNonNull(description, "description");
			values = List.copyOf(values);
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getLocalizationKey() {
			return localizationKey;
		}

		@Override
		public Component getLocalizedName() {
			return title;
		}

		@Override
		public Component getLocalizedDescription() {
			return description;
		}

		@Override
		public Collection<? extends IConfigScreenValue<?>> getConfigValues() {
			return values;
		}
	}
}
