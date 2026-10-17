package net.mezzdev.config.gui.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingBinding;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingInput;
import net.mezzdev.config.gui.api.keybindings.KeyMappingModifier;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.Set;

/** An immutable keyboard or mouse binding with standard and custom modifiers. */
public record ConfigKeyBinding(InputConstants.Key key, Set<KeyMappingModifier> modifiers, Set<String> customModifiers) implements IKeyMappingBinding {
	public static final ConfigKeyBinding UNKNOWN = new ConfigKeyBinding(InputConstants.UNKNOWN, Set.of());

	public ConfigKeyBinding {
		Objects.requireNonNull(key);
		modifiers = Set.copyOf(modifiers);
		customModifiers = ConfigKeyMappingHelper.copyCustomModifiers(customModifiers);
		if (key.equals(InputConstants.UNKNOWN)) {
			modifiers = Set.of();
			customModifiers = Set.of();
		}
	}

	public ConfigKeyBinding(InputConstants.Key key, Set<KeyMappingModifier> modifiers) {
		this(key, modifiers, Set.of());
	}

	@Override
	public InputConstants.Key getKey() {
		return key;
	}

	@Override
	public Set<KeyMappingModifier> getModifiers() {
		return modifiers;
	}

	@Override
	public boolean isUnbound() {
		return key.equals(InputConstants.UNKNOWN);
	}

	@Override
	public IKeyMappingInput resolve() {
		return ConfigKeyMappingHelper.INSTANCE.resolve(this);
	}

	@Override
	public Component getDisplayName() {
		return ConfigKeyMappingHelper.INSTANCE.getDisplayName(this);
	}

	@Override
	public Set<String> getCustomModifiers() {
		return customModifiers;
	}
}
