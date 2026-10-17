package net.mezzdev.config.gui.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingBinding;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * A key mapping that can be edited by a config screen.
 */
public interface IConfigKeyMapping {
	/**
	 * Get the stable name of this key mapping.
	 */
	String getName();

	/**
	 * Get the localized display name for this key mapping.
	 */
	Component getLocalizedName();

	/**
	 * Get a localized description of where this key mapping is active.
	 */
	Component getLocalizedContext();

	/**
	 * Get the localized description for this key mapping.
	 */
	Component getLocalizedDescription();

	/**
	 * Get the current binding value.
	 */
	IKeyMappingBinding getValue();

	/**
	 * Get the default binding value.
	 */
	IKeyMappingBinding getDefaultValue();

	/**
	 * Set the binding value.
	 */
	void set(IKeyMappingBinding value);

	/**
	 * Get the localized display name for a binding value.
	 */
	Component getValueName(IKeyMappingBinding value);

	/**
	 * Check whether the input can act as a modifier.
	 */
	boolean isModifier(InputConstants.Key key);

	/**
	 * Choose a supported binding from an input event and its held modifiers.
	 */
	IKeyMappingBinding createBinding(InputConstants.Key key, int eventModifiers);

	/**
	 * Get display information for key mappings that conflict with the given value.
	 */
	@Unmodifiable
	List<ConfigKeyMappingConflict> getConflicts(IKeyMappingBinding value);
}
