package net.mezzdev.config.gui;

import net.mezzdev.config.api.schema.IConfigCategory;
import net.mezzdev.config.api.schema.IConfigEditorCategory;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Internal screen schema adapter for MezzConfig schemas.
 */
final class MezzConfigScreenSchema implements ConfigScreenSchema {
	private final IConfigSchema schema;

	public MezzConfigScreenSchema(IConfigSchema schema) {
		this.schema = Objects.requireNonNull(schema, "schema");
	}

	@Override
	public List<? extends ConfigScreenCategory> getCategories() {
		return createCategories(schema.getCategories(), schema.getEditorCategories());
	}

	private static List<ConfigScreenCategory> createCategories(
		List<? extends IConfigCategory> categories,
		List<? extends IConfigEditorCategory> editorCategories
	) {
		Map<String, MutableConfigScreenCategory> screenCategories = new LinkedHashMap<>();
		for (IConfigEditorCategory editorCategory : editorCategories) {
			screenCategories.put(editorCategory.getName(), createCategory(editorCategory));
		}

		for (IConfigCategory category : categories) {
			MutableConfigScreenCategory storageCategory = Objects.requireNonNull(
				screenCategories.get(category.getName()),
				() -> "Storage category is not in this schema's editor categories: " + category.getName()
			);
			for (IConfigValue<?> value : category.getConfigValues()) {
				IConfigScreenValue<?> screenValue = IConfigScreenValue.configValue(value);
				List<? extends IConfigEditorCategory> valueEditorCategories = value.getEditorCategories();
				if (valueEditorCategories.isEmpty()) {
					storageCategory.addValue(screenValue);
					continue;
				}
				for (IConfigEditorCategory editorCategoryValue : valueEditorCategories) {
					MutableConfigScreenCategory editorCategory = Objects.requireNonNull(
						screenCategories.get(editorCategoryValue.getName()),
						() -> "Editor category is not in this schema: " + editorCategoryValue.getName()
					);
					editorCategory.addValue(screenValue);
				}
			}
		}
		return screenCategories.values()
			.stream()
			.filter(category -> !category.isEmpty())
			.map(MutableConfigScreenCategory::toConfigScreenCategory)
			.toList();
	}

	private static MutableConfigScreenCategory createCategory(IConfigEditorCategory category) {
		return new MutableConfigScreenCategory(
			category.getName(),
			category.getLocalizationKey(),
			ConfigValueLocalization.getName(category),
			ConfigValueLocalization.getDescription(category)
		);
	}

	private static final class MutableConfigScreenCategory {
		private final String name;
		private final String localizationKey;
		private final Component title;
		private final Component description;
		private final List<IConfigScreenValue<?>> values = new ArrayList<>();

		private MutableConfigScreenCategory(
			String name,
			String localizationKey,
			Component title,
			Component description
		) {
			this.name = name;
			this.localizationKey = localizationKey;
			this.title = title;
			this.description = description;
		}

		public void addValue(IConfigScreenValue<?> value) {
			values.add(value);
		}

		public boolean isEmpty() {
			return values.isEmpty();
		}

		public ConfigScreenCategory toConfigScreenCategory() {
			return new MezzConfigScreenCategory(name, localizationKey, title, description, values);
		}
	}

	private record MezzConfigScreenCategory(
		String name,
		String localizationKey,
		Component title,
		Component description,
		List<IConfigScreenValue<?>> values
	) implements ConfigScreenCategory {
		private MezzConfigScreenCategory {
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
