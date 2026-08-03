package net.mezzdev.config.gui.api;

import net.minecraft.network.chat.Component;

/**
 * Customizes a config screen provided by the config GUI.
 *
 * @since 0.1.0
 */
public interface IConfigScreenBuilder {
	/**
	 * Set the title shown at the top of the config screen.
	 *
	 * @param title the screen title
	 *
	 * @since 0.1.0
	 */
	void setTitle(Component title);

	/**
	 * Do not add automatically detected categories to this screen unless they are also configured here.
	 * <p>
	 * Configured categories still keep their automatically detected values unless
	 * {@link IConfigScreenCategoryBuilder#clearDefaultValues()} is called for that category.
	 *
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	default IConfigScreenBuilder clearDefaultCategories() {
		return this;
	}

	/**
	 * Add a category in the order it should appear on the screen.
	 * <p>
	 * Adding a category does not remove automatically detected categories. Call {@link #clearDefaultCategories()} when
	 * the screen should only show configured categories. Values added to this category are appended to its automatically
	 * detected values unless {@link IConfigScreenCategoryBuilder#clearDefaultValues()} is called.
	 * <p>
	 * The category title and description default to existing schema localization when the category already exists, or to
	 * the screen's inferred category localization path followed by {@code .} and {@code name}.
	 *
	 * @param name the stable category name
	 * @return the category builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addCategory(String name);

	/**
	 * Configure a category without forcing it into the ordered category list.
	 * <p>
	 * If this matches an automatically detected category, the category keeps its automatically detected position and
	 * values. Values added to this category are appended to its automatically detected values unless
	 * {@link IConfigScreenCategoryBuilder#clearDefaultValues()} is called.
	 * <p>
	 * The category title and description default to existing schema localization when the category already exists, or to
	 * the screen's inferred category localization path followed by {@code .} and {@code name}.
	 *
	 * @param name the stable category name
	 * @return the category builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder configureCategory(String name);
}
