package net.mezzdev.config.gui;

import net.mezzdev.config.api.Configs;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.util.ConfigNameUtil;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Creates generated config GUI screens from schemas registered with MezzConfig.
 */
public final class MezzConfigScreenConfigs {
	private MezzConfigScreenConfigs() {

	}

	public static List<ConfigScreenConfig> getActiveConfigScreens() {
		return getConfigScreens(Configs.getSchemas());
	}

	public static List<ConfigScreenConfig> getConfigScreens(Collection<? extends IConfigSchema> schemas) {
		Objects.requireNonNull(schemas, "schemas");
		Map<String, List<IConfigSchema>> schemasByModId = new LinkedHashMap<>();
		schemas
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
			return net.mezzdev.config.gui.internal.LegacyTranslations.translatableWithFallback(localizationKey, fallback);
		}

		@Override
		public ConfigScreenSchema getSchema() {
			return new MergedMezzConfigScreenSchema(schemas);
		}
	}

	private static final class MergedMezzConfigScreenSchema implements ConfigScreenSchema {
		private final List<IConfigSchema> schemas;
		private final Map<IConfigSchema, ConfigScreenSchema> screenSchemas = new IdentityHashMap<>();

		private MergedMezzConfigScreenSchema(List<IConfigSchema> schemas) {
			this.schemas = List.copyOf(schemas);
		}

		private ConfigScreenSchema getScreenSchema(IConfigSchema schema) {
			return screenSchemas.computeIfAbsent(schema, ConfigScreenSchema::from);
		}

		@Override
		public List<? extends ConfigScreenCategory> getCategories() {
			List<ConfigScreenSchema> activeSchemas = schemas.stream()
				.filter(IConfigSchema::isActive)
				.map(this::getScreenSchema)
				.toList();
			return new MergedConfigScreenSchema(activeSchemas)
				.getCategories();
		}

		@Override
		public Optional<IConfigSchema> findBackingSchema(IConfigScreenValue<?> value) {
			return schemas.stream()
				.map(this::getScreenSchema)
				.map(schema -> schema.findBackingSchema(value))
				.flatMap(Optional::stream)
				.findFirst();
		}
	}
}
