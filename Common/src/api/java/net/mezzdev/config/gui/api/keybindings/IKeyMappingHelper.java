package net.mezzdev.config.gui.api.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;

/**
 * Creates immutable binding snapshots.
 * Obtain this helper from {@link IConfigGuiRegistration#getKeyMappingHelper()}.
 *
 * @since 0.6.0
 */
@ApiStatus.NonExtendable
public interface IKeyMappingHelper {
	/**
	 * Create a binding with an immutable copy of its modifiers.
	 * An unbound binding has no modifiers.
	 *
	 * @param key the keyboard or mouse input, or {@link InputConstants#UNKNOWN} for an unbound binding
	 * @param modifiers the standard modifiers required by the binding
	 * @return an immutable binding snapshot
	 *
	 * @since 0.6.0
	 */
	IKeyMappingBinding createBinding(InputConstants.Key key, Set<KeyMappingModifier> modifiers);

	/**
	 * Create a binding that also requires custom modifiers supplied by other mods.
	 * An unbound binding has no modifiers.
	 *
	 * @param key the keyboard or mouse input, or {@link InputConstants#UNKNOWN} for an unbound binding
	 * @param modifiers the standard modifiers required by the binding
	 * @param customModifiers additional identifiers matching {@code [a-z0-9_.:/-]+}, excluding standard modifier names and {@code none}
	 * @return an immutable binding snapshot
	 * @throws IllegalArgumentException if a custom identifier is invalid or reserved
	 *
	 * @since 0.6.0
	 */
	IKeyMappingBinding createBinding(InputConstants.Key key, Set<KeyMappingModifier> modifiers, Set<String> customModifiers);
}
