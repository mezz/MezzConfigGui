package net.mezzdev.config.gui;

import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * Presentation metadata that locates a config screen value in a nested category path.
 */
public interface ConfigValueCategoryPath {
	String getRootCategoryName();

	List<Category> getCategories();

	static <T> IConfigScreenValue<T> withCategoryPath(
		IConfigScreenValue<T> value,
		String categoryName,
		List<Category> categories
	) {
		return ConfigScreenValueWithCategoryPath.create(value, categoryName, categories);
	}

	static <T> IConfigScreenValue<T> copyTo(IConfigScreenValue<?> source, IConfigScreenValue<T> target) {
		if (source == target) {
			return target;
		}
		return getMetadata(source)
			.map(metadata -> withCategoryPath(
				target,
				metadata.getRootCategoryName(),
				metadata.getCategories()
			))
			.orElse(target);
	}

	static Optional<ConfigValueCategoryPath> getMetadata(IConfigScreenValue<?> value) {
		if (value instanceof ConfigValueCategoryPath categoryPath) {
			return Optional.of(categoryPath);
		}
		if (value.getIdentityKey() instanceof ConfigValueCategoryPath categoryPath) {
			return Optional.of(categoryPath);
		}
		return Optional.empty();
	}

	static List<Category> getCategories(IConfigScreenValue<?> value) {
		return getMetadata(value)
			.map(ConfigValueCategoryPath::getCategories)
			.orElseGet(List::of);
	}

	static Component getContextualName(IConfigScreenValue<?> value, Component name) {
		var result = Component.empty();
		for (Category category : getCategories(value)) {
			result.append(category.title()).append(" › ");
		}
		return result.append(name);
	}

	record Category(String name, Component title, Component description) {
	}
}
