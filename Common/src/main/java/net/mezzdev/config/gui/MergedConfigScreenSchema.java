package net.mezzdev.config.gui;

import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Combines config screen schemas while preserving source and category order.
 */
final class MergedConfigScreenSchema implements ConfigScreenSchema {
	private final List<ConfigScreenSchema> schemas;

	MergedConfigScreenSchema(Collection<? extends ConfigScreenSchema> schemas) {
		this.schemas = List.copyOf(schemas);
	}

	@Override
	public List<? extends ConfigScreenCategory> getCategories() {
		Map<String, MutableMergedConfigScreenCategory> categories = new LinkedHashMap<>();
		for (ConfigScreenSchema schema : schemas) {
			for (ConfigScreenCategory category : schema.getCategories()) {
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
