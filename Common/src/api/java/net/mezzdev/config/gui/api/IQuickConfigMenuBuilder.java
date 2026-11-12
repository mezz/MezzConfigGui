package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.IConfigValue;
import org.jetbrains.annotations.ApiStatus;

/**
 * Defines a quick menu's default and additional settings in insertion order.
 * Paths, MezzConfig values, and screen values can be mixed in either group.
 * Screen values match registered entries by {@link IConfigScreenValue#getIdentityKey()}.
 * When a value appears more than once, its first entry in screen order supplies
 * the editor and persistent path.
 *
 * @since 0.7.0
 */
@ApiStatus.NonExtendable
public interface IQuickConfigMenuBuilder {
	/**
	 * Adds a setting shown initially.
	 *
	 * @param path registered {@code category.setting} path
	 * @return this builder
	 * @since 0.7.0
	 */
	IQuickConfigMenuBuilder addDefault(String path);

	/**
	 * Adds a MezzConfig value shown initially.
	 *
	 * @param value a value registered on the config screen
	 * @return this builder
	 * @since 0.7.0
	 */
	IQuickConfigMenuBuilder addDefault(IConfigValue<?> value);

	/**
	 * Adds a screen value shown initially.
	 *
	 * @param value a registered screen value
	 * @return this builder
	 * @since 0.7.0
	 */
	IQuickConfigMenuBuilder addDefault(IConfigScreenValue<?> value);

	/**
	 * Adds a setting available through Customize, initially hidden.
	 *
	 * @param path registered {@code category.setting} path
	 * @return this builder
	 * @since 0.7.0
	 */
	IQuickConfigMenuBuilder addAdditional(String path);

	/**
	 * Adds a MezzConfig value available through Customize, initially hidden.
	 *
	 * @param value a value registered on the config screen
	 * @return this builder
	 * @since 0.7.0
	 */
	IQuickConfigMenuBuilder addAdditional(IConfigValue<?> value);

	/**
	 * Adds a screen value available through Customize, initially hidden.
	 *
	 * @param value a registered screen value
	 * @return this builder
	 * @since 0.7.0
	 */
	IQuickConfigMenuBuilder addAdditional(IConfigScreenValue<?> value);

	/**
	 * Creates the menu from the configured settings.
	 * Call on the client thread after plugin registration has finished.
	 *
	 * @return a closed menu
	 * @throws IllegalArgumentException if a setting is unknown, ambiguous, duplicated, or server-owned
	 * @throws IllegalStateException if this mod has no registered config screen
	 * @since 0.7.0
	 */
	IQuickConfigMenu build();
}
