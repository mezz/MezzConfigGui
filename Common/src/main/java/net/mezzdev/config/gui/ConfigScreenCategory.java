package net.mezzdev.config.gui;

import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;

/**
 * Internal config category view used by config screens.
 */
public interface ConfigScreenCategory {
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
