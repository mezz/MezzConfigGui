package net.mezzdev.config.gui;

import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

class ConfigScreenValueWithCategoryPath<T> implements IConfigScreenValue<T>, ConfigValueCategoryPath {
	private final IConfigScreenValue<T> configValue;
	private final String categoryName;
	private final List<Category> categories;

	static <T> IConfigScreenValue<T> create(
		IConfigScreenValue<T> configValue,
		String categoryName,
		List<Category> categories
	) {
		IConfigScreenValue<T> checkedValue = Objects.requireNonNull(configValue, "configValue");
		if (checkedValue instanceof IConfigLocalizedValue localizedValue) {
			return new Localized<>(checkedValue, categoryName, categories, localizedValue);
		}
		return new ConfigScreenValueWithCategoryPath<>(checkedValue, categoryName, categories);
	}

	private ConfigScreenValueWithCategoryPath(
		IConfigScreenValue<T> configValue,
		String categoryName,
		List<Category> categories
	) {
		this.configValue = Objects.requireNonNull(configValue, "configValue");
		this.categoryName = Objects.requireNonNull(categoryName, "categoryName");
		this.categories = List.copyOf(categories);
	}

	@Override
	public String getRootCategoryName() {
		return categoryName;
	}

	@Override
	public List<Category> getCategories() {
		return categories;
	}

	@Override
	public String getName() {
		return configValue.getName();
	}

	@Override
	public String getLocalizationKey() {
		return configValue.getLocalizationKey();
	}

	@Override
	public T getValue() {
		return configValue.getValue();
	}

	@Override
	public T getDefaultValue() {
		return configValue.getDefaultValue();
	}

	@Override
	public boolean set(T value) {
		return configValue.set(value);
	}

	@Override
	public Runnable addListener(Consumer<T> listener) {
		return configValue.addListener(listener);
	}

	@Override
	public ConfigValueApplyMode getApplyMode() {
		return configValue.getApplyMode();
	}

	@Override
	public ConfigValueRestartRequirement getRestartRequirement() {
		return configValue.getRestartRequirement();
	}

	@Override
	public Object getIdentityKey() {
		return configValue.getIdentityKey();
	}

	@Override
	public IConfigValueSerializer<T> getSerializer() {
		return configValue.getSerializer();
	}

	@Override
	public Optional<IConfigValue<T>> getConfigValue() {
		return configValue.getConfigValue();
	}

	@Override
	public String toString() {
		return configValue.toString();
	}

	private static final class Localized<T> extends ConfigScreenValueWithCategoryPath<T> implements IConfigLocalizedValue {
		private final IConfigLocalizedValue localizedValue;

		private Localized(
			IConfigScreenValue<T> configValue,
			String categoryName,
			List<Category> categories,
			IConfigLocalizedValue localizedValue
		) {
			super(configValue, categoryName, categories);
			this.localizedValue = Objects.requireNonNull(localizedValue, "localizedValue");
		}

		@Override
		public Component getLocalizedName() {
			return localizedValue.getLocalizedName();
		}

		@Override
		public Component getLocalizedDescription() {
			return localizedValue.getLocalizedDescription();
		}
	}
}
