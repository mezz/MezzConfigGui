package net.mezzdev.config.gui.api;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * Creates customizable menus from this mod's registered config screen entries.
 * Menus retain the entries' configured editors and save behavior.
 * Create menus on the client thread after plugin registration has finished.
 * <p>
 * Saved choices use registered category and value names. Keep these names stable across launches.
 * Menus with the same ordered default and additional paths share saved choices.
 *
 * @see IConfigGuiRegistration#getQuickConfigMenuFactory()
 * @since 0.7.0
 */
@ApiStatus.NonExtendable
public interface IQuickConfigMenuFactory {
	/**
	 * Creates a menu from registered {@code category.setting} paths.
	 *
	 * @param title localized menu title
	 * @param defaults setting paths shown initially, in order
	 * @param additional setting paths available through Customize, initially hidden
	 * @return a closed menu
	 * @throws IllegalArgumentException if a setting path is unknown, ambiguous, duplicated, or server-owned
	 * @throws IllegalStateException if this mod has no registered config screen
	 *
	 * @since 0.7.0
	 */
	IQuickConfigMenu create(Component title, List<String> defaults, List<String> additional);

	/**
	 * Starts a menu definition that accepts paths, MezzConfig values, and screen values directly.
	 *
	 * @param title localized menu title
	 * @return an empty menu builder
	 *
	 * @since 0.7.0
	 */
	IQuickConfigMenuBuilder createBuilder(Component title);
}
