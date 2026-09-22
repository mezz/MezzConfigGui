package net.mezzdev.config.gui;

import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
		List<? extends ConfigScreenCategory> sourceCategories = schemas.stream()
			.flatMap(schema -> schema.getCategories().stream())
			.sorted(Comparator.comparing(ConfigScreenCategory::getGroup))
			.toList();
		for (ConfigScreenCategory category : sourceCategories) {
			categories.computeIfAbsent(
					category.getName(),
					ignored -> new MutableMergedConfigScreenCategory(category)
				)
				.add(category);
		}
		return categories.values()
			.stream()
			.map(MutableMergedConfigScreenCategory::toConfigScreenCategory)
			.toList();
	}

	@Override
	public Optional<IConfigSchema> findBackingSchema(IConfigScreenValue<?> value) {
		return schemas.stream()
			.map(schema -> schema.findBackingSchema(value))
			.flatMap(Optional::stream)
			.findFirst();
	}

	private static final class MutableMergedConfigScreenCategory {
		private final String name;
		private final String localizationKey;
		private final Component title;
		private final Component description;
		private ConfigScreenCategoryGroup group;
		@Nullable
		private ConfigScreenCategoryNavigationGroup navigationGroup;
		private final List<IConfigScreenValue<?>> values = new ArrayList<>();

		private MutableMergedConfigScreenCategory(ConfigScreenCategory category) {
			this.name = category.getName();
			this.localizationKey = category.getLocalizationKey();
			this.title = category.getLocalizedName();
			this.description = category.getLocalizedDescription();
			this.group = category.getGroup();
			this.navigationGroup = category.getNavigationGroup();
		}

		public void add(ConfigScreenCategory category) {
			if (category.getGroup().compareTo(group) < 0) {
				group = category.getGroup();
			}
			if (!Objects.equals(navigationGroup, category.getNavigationGroup())) {
				navigationGroup = null;
			}
			this.values.addAll(category.getConfigValues());
		}

		public ConfigScreenCategory toConfigScreenCategory() {
			return new MergedConfigScreenCategory(
				name,
				localizationKey,
				title,
				description,
				group,
				navigationGroup,
				values
			);
		}
	}

	private record MergedConfigScreenCategory(
		String name,
		String localizationKey,
		Component title,
		Component description,
		ConfigScreenCategoryGroup group,
		@Nullable ConfigScreenCategoryNavigationGroup navigationGroup,
		List<IConfigScreenValue<?>> values
	) implements ConfigScreenCategory {
		private MergedConfigScreenCategory {
			Objects.requireNonNull(name, "name");
			Objects.requireNonNull(localizationKey, "localizationKey");
			Objects.requireNonNull(title, "title");
			Objects.requireNonNull(description, "description");
			Objects.requireNonNull(group, "group");
			values = List.copyOf(values);
		}

		@Override
		public ConfigScreenCategoryGroup getGroup() {
			return group;
		}

		@Override
		@Nullable
		public ConfigScreenCategoryNavigationGroup getNavigationGroup() {
			return navigationGroup;
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
