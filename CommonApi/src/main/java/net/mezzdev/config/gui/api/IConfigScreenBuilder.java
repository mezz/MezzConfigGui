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
	 * The config GUI shows customized categories in the order they are added here, followed by any values that were not
	 * explicitly added to a customized category.
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
	 * Configure an existing category without changing its order on the screen.
	 * <p>
	 * If there is no existing category with this name, the category is added after the existing categories.
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
