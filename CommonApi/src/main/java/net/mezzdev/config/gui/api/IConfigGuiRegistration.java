package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.schema.IConfigEditableSchema;
import net.mezzdev.config.api.value.ConfigValueEditorType;
import net.mezzdev.config.api.value.ConfigValueUpdateType;
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
	 * Customize the config screen for this mod.
	 * <p>
	 * This can be used with a screen registered through {@link #registerScreen(Component, Supplier, IConfigRestartHandler)}
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
	 * The restart handler is called when applying saved changes requires the owner mod to restart or reload
	 * (see {@link ConfigValueUpdateType#RESTART}). Return {@link ConfigRestartResult#NEXT_GAME_START} when the mod
	 * cannot apply the saved changes until the game starts again.
	 *
	 * @param title the title shown at the top of the config screen
	 * @param schemaSupplier supplies the config schema each time the screen is opened
	 * @param restartHandler handles and reports saved changes that require the owner mod to restart or reload
	 *
	 * @since 0.1.0
	 */
	void registerScreen(
		Component title,
		Supplier<? extends IConfigEditableSchema> schemaSupplier,
		IConfigRestartHandler restartHandler
	);
}
