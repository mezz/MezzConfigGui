package net.mezzdev.config.gui.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingBinding;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingInput;
import net.mezzdev.config.gui.api.keybindings.KeyInputModifier;

import java.util.Objects;
import java.util.Set;

/** An immutable keyboard or mouse input combination with physical modifiers. */
record KeyMappingInput(InputConstants.Key key, Set<KeyInputModifier> modifiers, Set<String> customModifiers) implements IKeyMappingInput {
	KeyMappingInput {
		Objects.requireNonNull(key);
		modifiers = Set.copyOf(modifiers);
		customModifiers = ConfigKeyMappingHelper.copyCustomModifiers(customModifiers);
		if (key.equals(InputConstants.UNKNOWN)) {
			modifiers = Set.of();
			customModifiers = Set.of();
		}
	}

	@Override
	public InputConstants.Key getKey() {
		return key;
	}

	@Override
	public Set<KeyInputModifier> getModifiers() {
		return modifiers;
	}

	@Override
	public IKeyMappingBinding toBinding() {
		return ConfigKeyMappingHelper.INSTANCE.createBinding(this);
	}

	@Override
	public Set<String> getCustomModifiers() {
		return customModifiers;
	}

	@Override
	public IKeyMappingInput withCustomModifiers(Set<String> customModifiers) {
		return new KeyMappingInput(key, modifiers, customModifiers);
	}
}
