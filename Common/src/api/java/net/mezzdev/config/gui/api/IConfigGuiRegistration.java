package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingAdapter;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registration for config GUI customizations provided by one config GUI plugin.
 *
 * @since 0.1.0
 */
@ApiStatus.NonExtendable
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
	 * Get the factory for adapting text serializers to screen-value editor metadata.
	 *
	 * @since 0.6.0
	 */
	IConfigValueEditorMetadataFactory getEditorMetadataFactory();

	/**
	 * Registers adapters for a key mapping class and its subclasses across config screens.
	 * The most specific registered class wins.
	 *
	 * @param mappingClass the key mapping class to adapt
	 * @param factory creates an adapter for each supplied mapping
	 * @param <T> the key mapping type
	 * @throws IllegalArgumentException if this class already has an adapter factory
	 *
	 * @since 0.6.0
	 */
	<T extends KeyMapping> void registerKeyMappingAdapter(Class<T> mappingClass, Function<T, IKeyMappingAdapter> factory);

	/**
	 * Get helpers for creating binding snapshots and implementing custom key mapping adapters.
	 *
	 * @return shared key mapping helpers
	 *
	 * @since 0.6.0
	 */
	IKeyMappingHelper getKeyMappingHelper();

	/**
	 * Get helper factories for config values backed by sortable runtime lists.
	 * <p>
	 * Prefer adding sorting configs directly with
	 * {@link IConfigScreenCategoryBuilder#addSortingConfig(String, String, net.mezzdev.config.api.sorting.ISortingConfig, java.util.Collection, net.mezzdev.config.api.value.serializer.IConfigValueSerializer)}
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
