package net.mezzdev.config.gui.api.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

/**
 * Adapts one key mapping for the shared binding editor.
 * <p>
 * Register adapters with {@link IConfigGuiRegistration#registerKeyMappingAdapter} for custom key mapping implementations.
 *
 * @since 0.6.0
 */
public interface IKeyMappingAdapter {
	/**
	 * Get the key mapping wrapped by this adapter.
	 *
	 * @return the same mapping instance supplied to the registered adapter factory
	 *
	 * @since 0.6.0
	 */
	KeyMapping getKeyMapping();

	/**
	 * Get the current binding, including its modifiers.
	 *
	 * @return a snapshot of the assigned input and its modifiers, or an unbound binding if no input is assigned
	 *
	 * @since 0.6.0
	 */
	IKeyMappingBinding getValue();

	/**
	 * Replace the current input and its entire modifier set.
	 *
	 * @param value the complete binding to apply
	 *
	 * @since 0.6.0
	 */
	void setValue(IKeyMappingBinding value);

	/**
	 * Read the binding used when the user resets this key mapping.
	 *
	 * @return the complete default binding, which may be unbound
	 *
	 * @since 0.6.0
	 */
	IKeyMappingBinding getDefaultValue();

	/**
	 * Get localized text for a binding, including any modifiers.
	 *
	 * @param value the binding to display, including pending edits
	 * @return the localized binding name
	 *
	 * @since 0.6.0
	 */
	default Component getDisplayName(IKeyMappingBinding value) {
		return value.getDisplayName();
	}

	/**
	 * Check whether this mapping's current binding conflicts with another mapping, accounting for modifiers
	 * and activation context. The editor checks both directions.
	 *
	 * @param other the mapping to compare against
	 * @return true if the bindings conflict
	 *
	 * @since 0.6.0
	 */
	default boolean conflicts(KeyMapping other) {
		return getKeyMapping().same(other);
	}

	/**
	 * Choose a supported binding for captured input using this mapping's modifier representation.
	 * Excludes unsupported modifiers and unsupported combinations.
	 * Does not change the current binding. Assigning the result with {@link #setValue} must make
	 * {@link #getValue} return the same key and modifiers.
	 *
	 * @param input the keyboard or mouse input captured by {@link #captureInput}
	 * @return a binding that this mapping can store unchanged
	 *
	 * @since 0.6.0
	 */
	default IKeyMappingBinding createBinding(IKeyMappingInput input) {
		return input.toBinding();
	}

	/**
	 * Capture any additional modifier state for a keyboard or mouse input event.
	 * The supplied input has no custom modifiers.
	 * The result excludes custom modifiers represented by the main key.
	 *
	 * @param input the input and physical standard modifiers from the event, excluding the main key's modifier
	 * @return a snapshot of the input and its held physical standard and custom modifiers
	 *
	 * @since 0.6.0
	 */
	default IKeyMappingInput captureInput(IKeyMappingInput input) {
		return input;
	}

	/**
	 * Identify an additional modifier key supplied by this integration.
	 * The editor already recognizes Shift, Control, Alt, and Command/Super.
	 *
	 * @param key the keyboard or mouse input to identify
	 * @return true if this input can act as a custom modifier for the mapping
	 *
	 * @since 0.6.0
	 */
	default boolean isCustomModifier(InputConstants.Key key) {
		return false;
	}
}
