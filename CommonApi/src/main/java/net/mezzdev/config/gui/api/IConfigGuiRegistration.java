package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.schema.IConfigSchema;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Registration for config GUI customizations provided by one config GUI plugin.
 *
 * @since 0.1.0
 */
public interface IConfigGuiRegistration {
	/**
	 * Register a custom value editor for config values that return the matching editor type.
	 *
	 * @param editorType the config value editor type to handle
	 * @param editorFactory creates custom value editors
	 * @param <T> the config value type edited by this control
	 *
	 * @since 0.1.0
	 */
	<T> void registerValueEditor(ConfigValueEditorType<T> editorType, IConfigValueEditorFactory<T> editorFactory);

	/**
	 * Get helper factories for config values backed by sortable runtime lists.
	 * <p>
	 * Prefer adding sorting configs directly with
	 * {@link IConfigScreenCategoryBuilder#addSortingConfig(String, String, net.mezzdev.config.api.sorting.ISortingConfig, java.util.Collection, net.mezzdev.config.api.value.IConfigValueSerializer)}
	 * or
	 * {@link IConfigScreenCategoryBuilder#addStringSortingConfig(String, String, net.mezzdev.config.api.sorting.ISortingConfig, java.util.Collection)}
	 * when customizing a screen category. This helper remains for callers that still need to create a screen value
	 * explicitly.
	 *
	 * @return helper factories for sortable runtime list values
	 *
	 * @since 0.1.0
	 */
	ISortableConfigValueFactory getSortableConfigValueFactory();

	/**
	 * Customize the config screen for this mod.
	 * <p>
	 * This can be used with a screen registered through {@link #registerScreen(Component, Supplier)}
	 * or with a screen automatically detected by the config GUI from a platform-native config system. This method does
	 * not create a screen on its own.
	 *
	 * @param screenBuilderConsumer receives the screen builder
	 *
	 * @since 0.1.0
	 */
	void configureScreen(Consumer<IConfigScreenBuilder> screenBuilderConsumer);

	/**
	 * Register a config screen.
	 *
	 * @param title the title shown at the top of the config screen
	 * @param schemaSupplier supplies the config schema each time the screen is opened
	 *
	 * @since 0.1.0
	 */
	void registerScreen(
		Component title,
		Supplier<? extends IConfigSchema> schemaSupplier
	);
}
