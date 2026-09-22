package net.mezzdev.config.gui;

import net.mezzdev.config.api.schema.category.IConfigCategory;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Objects;

/**
 * Internal config category view used by config screens.
 */
public interface ConfigScreenCategory {
	/**
	 * Adapt a MezzConfig-managed category for display on a config screen.
	 */
	static ConfigScreenCategory configCategory(IConfigCategory category) {
		IConfigCategory checkedCategory = Objects.requireNonNull(category, "category");
		return new ConfigScreenCategory() {
			@Override
			public ConfigScreenCategoryGroup getGroup() {
				return ConfigScreenCategoryGroup.MOD_OWNED;
			}

			@Override
			@Nullable
			public ConfigScreenCategoryNavigationGroup getNavigationGroup() {
				return null;
			}

			@Override
			public String getName() {
				return checkedCategory.getName();
			}

			@Override
			public String getLocalizationKey() {
				return checkedCategory.getLocalizationKey();
			}

			@Override
			public Component getLocalizedName() {
				return ConfigValueLocalization.getName(checkedCategory);
			}

			@Override
			public Component getLocalizedDescription() {
				return ConfigValueLocalization.getDescription(checkedCategory);
			}

			@Override
			public Collection<? extends IConfigScreenValue<?>> getConfigValues() {
				return checkedCategory.getConfigValues()
					.stream()
					.map(IConfigScreenValue::configValue)
					.toList();
			}
		};
	}

	/**
	 * Get the broad group that controls this category's position in a merged config screen.
	 */
	ConfigScreenCategoryGroup getGroup();

	/**
	 * Get the shared navigation root for this category, when it can be grouped with compatible categories.
	 *
	 * @return the navigation group, or {@code null} when this category is not grouped
	 */
	@Nullable
	ConfigScreenCategoryNavigationGroup getNavigationGroup();

	/**
	 * The name of the category.
	 */
	String getName();

	/**
	 * Get the translation key used for this config category's name.
	 */
	String getLocalizationKey();

	/**
	 * Get the localized category name.
	 */
	Component getLocalizedName();

	/**
	 * Get the localized category description.
	 */
	Component getLocalizedDescription();

	/**
	 * The config screen values in the category.
	 */
	@Unmodifiable
	Collection<? extends IConfigScreenValue<?>> getConfigValues();
}
