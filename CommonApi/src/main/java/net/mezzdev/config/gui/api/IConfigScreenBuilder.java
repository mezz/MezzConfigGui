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
	 * Set the handler for saved changes that require the owner mod to restart or reload.
	 *
	 * @param restartHandler handles and reports saved changes that require the owner mod to restart or reload
	 *
	 * @since 0.1.0
	 */
	void setRestartHandler(IConfigRestartHandler restartHandler);

	/**
	 * Add a category in the order it should appear on the screen.
	 * <p>
	 * Adding a category switches the screen to a manually configured layout. Automatically detected categories are no
	 * longer added unless they are also configured here. If values are added to this category, they replace the
	 * category's automatically detected values. If no values are added, this category keeps its automatically detected
	 * values.
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
	 * If this matches an automatically detected category and no values are added, the category keeps its automatically
	 * detected position and values. Adding values switches the screen to a manually configured layout. Automatically
	 * detected categories are no longer added unless they are also configured here, and these values replace the
	 * category's automatically detected values.
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
