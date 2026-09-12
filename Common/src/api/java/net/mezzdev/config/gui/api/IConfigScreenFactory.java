package net.mezzdev.config.gui.api;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * Creates a config screen from a parent screen.
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface IConfigScreenFactory {
	/**
	 * Create the config screen.
	 *
	 * @param parent the screen that opened this config screen, or null when there is no parent
	 * @return the created config screen
	 *
	 * @since 0.1.0
	 */
	Screen create(@Nullable Screen parent);
}
