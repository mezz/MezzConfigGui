package net.mezzdev.config.gui.api.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;

/**
 * An immutable combination of a keyboard or mouse input and physical modifiers.
 * Represents captured input or a binding resolved for the current platform.
 * <p>
 * Inputs are equal when their keys and both modifier sets are equal.
 *
 * @since 0.6.0
 */
@ApiStatus.NonExtendable
public interface IKeyMappingInput {
	/**
	 * Get the main keyboard or mouse input.
	 *
	 * @return the input, or {@link InputConstants#UNKNOWN} when resolving an unbound binding
	 *
	 * @since 0.6.0
	 */
	InputConstants.Key getKey();

	/**
	 * Get the physical standard modifiers in this combination.
	 *
	 * @return an immutable set of modifiers, or an empty set when none are present
	 *
	 * @since 0.6.0
	 */
	Set<KeyInputModifier> getModifiers();

	/**
	 * Create a binding requiring this input's physical modifiers.
	 * All standard and custom modifiers are preserved.
	 *
	 * @return a binding with physical modifier requirements
	 *
	 * @since 0.6.0
	 */
	IKeyMappingBinding toBinding();

	/**
	 * Get additional modifiers supplied by other mods.
	 *
	 * @return an immutable set of custom identifiers, or an empty set when none are present
	 *
	 * @since 0.6.0
	 */
	Set<String> getCustomModifiers();

	/**
	 * Create an input combination with the same key and standard modifiers and different custom modifiers.
	 * An unknown input has no modifiers.
	 *
	 * @param customModifiers replacement identifiers matching {@code [a-z0-9_.:/-]+}, excluding standard modifier names and {@code none}
	 * @return an immutable input snapshot
	 * @throws IllegalArgumentException if a custom identifier is invalid or reserved
	 *
	 * @since 0.6.0
	 */
	IKeyMappingInput withCustomModifiers(Set<String> customModifiers);
}
