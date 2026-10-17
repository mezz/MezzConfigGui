package net.mezzdev.config.gui.api.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;

/**
 * An immutable configured shortcut and its modifier requirements, including portable aliases.
 * Create bindings with {@link IKeyMappingHelper#createBinding(InputConstants.Key, Set)}.
 * <p>
 * Bindings are equal when their keys and both modifier sets are equal.
 * Portable aliases remain distinct from physical modifiers.
 *
 * @since 0.6.0
 */
@ApiStatus.NonExtendable
public interface IKeyMappingBinding {
	/**
	 * Get the input assigned to this binding.
	 *
	 * @return the keyboard or mouse input, or {@link InputConstants#UNKNOWN} when unbound
	 *
	 * @since 0.6.0
	 */
	InputConstants.Key getKey();

	/**
	 * Get the standard modifiers required by this binding.
	 *
	 * @return an immutable set of standard modifiers, or an empty set when none are required
	 *
	 * @since 0.6.0
	 */
	Set<KeyMappingModifier> getModifiers();

	/**
	 * Check whether no keyboard key or mouse button is assigned.
	 *
	 * @return true when this binding is unbound
	 *
	 * @since 0.6.0
	 */
	boolean isUnbound();

	/**
	 * Resolve this binding's modifier requirements to physical inputs for the current platform.
	 * {@link KeyMappingModifier#CONTROL_OR_COMMAND} becomes {@link KeyInputModifier#COMMAND} on macOS
	 * and {@link KeyInputModifier#CONTROL} elsewhere.
	 * Custom modifiers are preserved. The result does not retain whether a modifier came from an alias.
	 *
	 * @return the input combination required by this binding on this platform
	 *
	 * @since 0.6.0
	 */
	IKeyMappingInput resolve();

	/**
	 * Get localized text for this binding, including its modifiers.
	 *
	 * @return the localized binding name
	 *
	 * @since 0.6.0
	 */
	Component getDisplayName();

	/**
	 * Get additional modifiers supplied by other mods.
	 *
	 * @return an immutable set of custom identifiers, or an empty set when none are required
	 *
	 * @since 0.6.0
	 */
	Set<String> getCustomModifiers();
}
