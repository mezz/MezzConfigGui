package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.gui.api.keybindings.IKeyMappingAdapter;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingBinding;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingInput;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.util.Set;

/** Edits vanilla mappings, which do not support modifiers. */
public class VanillaKeyMappingAdapter implements IKeyMappingAdapter {
	private final KeyMapping mapping;

	public VanillaKeyMappingAdapter(KeyMapping mapping) {
		this.mapping = mapping;
	}

	@Override
	public KeyMapping getKeyMapping() {
		return mapping;
	}

	@Override
	public IKeyMappingBinding getValue() {
		return ConfigKeyMappingHelper.INSTANCE.createBinding(ConfigKeyBindingUtil.getKey(mapping), Set.of());
	}

	@Override
	public void setValue(IKeyMappingBinding value) {
		mapping.setKey(value.getKey());
	}

	@Override
	public IKeyMappingBinding getDefaultValue() {
		return ConfigKeyMappingHelper.INSTANCE.createBinding(mapping.getDefaultKey(), Set.of());
	}

	@Override
	public IKeyMappingBinding createBinding(IKeyMappingInput input) {
		return ConfigKeyMappingHelper.INSTANCE.createBinding(input.getKey(), Set.of());
	}

	@Override
	public Component getDisplayName(IKeyMappingBinding value) {
		return ConfigKeyBindingUtil.getKeyDisplayName(value.getKey());
	}
}
