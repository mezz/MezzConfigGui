package net.mezzdev.config.gui.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.ConfigInputUtil;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingAdapter;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingBinding;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingInput;
import net.mezzdev.config.gui.api.keybindings.KeyInputModifier;
import net.mezzdev.config.gui.api.keybindings.KeyMappingModifier;
import net.mezzdev.config.gui.keybindings.ConfigKeyBindingUtil;
import net.mezzdev.config.gui.keybindings.ConfigKeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

import java.util.List;
import java.util.Set;

import static net.mezzdev.config.gui.api.keybindings.KeyMappingModifier.ALT;
import static net.mezzdev.config.gui.api.keybindings.KeyMappingModifier.COMMAND;
import static net.mezzdev.config.gui.api.keybindings.KeyMappingModifier.CONTROL;
import static net.mezzdev.config.gui.api.keybindings.KeyMappingModifier.CONTROL_OR_COMMAND;
import static net.mezzdev.config.gui.api.keybindings.KeyMappingModifier.SHIFT;

/** Edits NeoForge mappings with their native modifier and conflict context. */
final class NeoForgeKeyMappingAdapter implements IKeyMappingAdapter {
	private static final List<KeyInputModifier> INPUT_MODIFIER_PRIORITY = List.of(KeyInputModifier.CONTROL, KeyInputModifier.COMMAND, KeyInputModifier.SHIFT, KeyInputModifier.ALT);
	private static final List<KeyMappingModifier> MODIFIER_PRIORITY = List.of(CONTROL_OR_COMMAND, CONTROL, COMMAND, SHIFT, ALT);
	private final KeyMapping mapping;

	NeoForgeKeyMappingAdapter(KeyMapping mapping) {
		this.mapping = mapping;
	}

	@Override
	public KeyMapping getKeyMapping() {
		return mapping;
	}

	@Override
	public IKeyMappingBinding getValue() {
		return ConfigKeyMappingHelper.INSTANCE.createBinding(mapping.getKey(), fromNeoForge(mapping.getKeyModifier()));
	}

	@Override
	public void setValue(IKeyMappingBinding value) {
		InputConstants.Key key = value.getKey();
		mapping.setKeyModifierAndCode(selectModifier(value), key);
		mapping.setKey(key);
	}

	@Override
	public IKeyMappingBinding getDefaultValue() {
		return ConfigKeyMappingHelper.INSTANCE.createBinding(mapping.getDefaultKey(), fromNeoForge(mapping.getDefaultKeyModifier()));
	}

	@Override
	public IKeyMappingBinding createBinding(IKeyMappingInput input) {
		for (KeyInputModifier modifier : INPUT_MODIFIER_PRIORITY) {
			if (!input.getModifiers().contains(modifier) || (modifier == KeyInputModifier.COMMAND && !ConfigInputUtil.isMac())) {
				continue;
			}
			KeyModifier nativeModifier = switch (modifier) {
				case CONTROL -> KeyModifier.CONTROL;
				case COMMAND -> KeyModifier.CONTROL_OR_COMMAND;
				case SHIFT -> KeyModifier.SHIFT;
				case ALT -> KeyModifier.ALT;
			};
			return ConfigKeyMappingHelper.INSTANCE.createBinding(input.getKey(), fromNeoForge(nativeModifier));
		}
		return ConfigKeyMappingHelper.INSTANCE.createBinding(input.getKey(), Set.of());
	}

	@Override
	public Component getDisplayName(IKeyMappingBinding value) {
		InputConstants.Key key = value.getKey();
		return selectModifier(value).getCombinedName(key, () -> ConfigKeyBindingUtil.getKeyDisplayName(key));
	}

	@Override
	public boolean conflicts(KeyMapping other) {
		IKeyConflictContext context = mapping.getKeyConflictContext();
		IKeyConflictContext otherContext = other.getKeyConflictContext();
		if (!context.conflicts(otherContext) && !otherContext.conflicts(context)) {
			return false;
		}
		return mapping.same(other);
	}

	private static KeyModifier selectModifier(IKeyMappingBinding value) {
		for (KeyMappingModifier modifier : MODIFIER_PRIORITY) {
			if (!value.getModifiers().contains(modifier) || (modifier == COMMAND && !ConfigInputUtil.isMac())) {
				continue;
			}
			KeyModifier nativeModifier = toNeoForge(modifier);
			if (!nativeModifier.matches(value.getKey())) {
				return nativeModifier;
			}
		}
		return KeyModifier.NONE;
	}

	private static KeyModifier toNeoForge(KeyMappingModifier modifier) {
		return switch (modifier) {
			case CONTROL_OR_COMMAND, COMMAND -> KeyModifier.CONTROL_OR_COMMAND;
			case CONTROL -> KeyModifier.CONTROL;
			case SHIFT -> KeyModifier.SHIFT;
			case ALT -> KeyModifier.ALT;
		};
	}

	private static Set<KeyMappingModifier> fromNeoForge(KeyModifier modifier) {
		return switch (modifier) {
			case CONTROL_OR_COMMAND -> Set.of(CONTROL_OR_COMMAND);
			case CONTROL -> Set.of(CONTROL);
			case SHIFT -> Set.of(SHIFT);
			case ALT -> Set.of(ALT);
			case NONE -> Set.of();
		};
	}
}
