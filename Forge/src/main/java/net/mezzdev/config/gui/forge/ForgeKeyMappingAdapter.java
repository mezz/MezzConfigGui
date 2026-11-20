package net.mezzdev.config.gui.forge;

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
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

import java.util.List;
import java.util.Set;

import static net.mezzdev.config.gui.api.keybindings.KeyMappingModifier.ALT;
import static net.mezzdev.config.gui.api.keybindings.KeyMappingModifier.COMMAND;
import static net.mezzdev.config.gui.api.keybindings.KeyMappingModifier.CONTROL;
import static net.mezzdev.config.gui.api.keybindings.KeyMappingModifier.CONTROL_OR_COMMAND;
import static net.mezzdev.config.gui.api.keybindings.KeyMappingModifier.SHIFT;

/** Edits Forge mappings with their native modifier and conflict context. */
final class ForgeKeyMappingAdapter implements IKeyMappingAdapter {
	private static final List<KeyInputModifier> INPUT_MODIFIER_PRIORITY = List.of(getNativeControlInput(), KeyInputModifier.SHIFT, KeyInputModifier.ALT);
	private static final List<KeyMappingModifier> MODIFIER_PRIORITY = List.of(CONTROL_OR_COMMAND, CONTROL, COMMAND, SHIFT, ALT);
	private final KeyMapping mapping;

	ForgeKeyMappingAdapter(KeyMapping mapping) {
		this.mapping = mapping;
	}

	@Override
	public KeyMapping getKeyMapping() {
		return mapping;
	}

	@Override
	public IKeyMappingBinding getValue() {
		return ConfigKeyMappingHelper.INSTANCE.createBinding(mapping.getKey(), fromForge(mapping.getKeyModifier()));
	}

	@Override
	public void setValue(IKeyMappingBinding value) {
		InputConstants.Key key = value.getKey();
		mapping.setKeyModifierAndCode(selectModifier(value), key);
		mapping.setKey(key);
	}

	@Override
	public IKeyMappingBinding getDefaultValue() {
		return ConfigKeyMappingHelper.INSTANCE.createBinding(mapping.getDefaultKey(), fromForge(mapping.getDefaultKeyModifier()));
	}

	@Override
	public IKeyMappingBinding createBinding(IKeyMappingInput input) {
		InputConstants.Key key = input.getKey();
		// Capture only the main key so the binding round-trips on Forge versions that clear held modifiers.
		if (KeyModifier.isKeyCodeModifier(key)) {
			return ConfigKeyMappingHelper.INSTANCE.createBinding(key, Set.of());
		}
		for (KeyInputModifier modifier : INPUT_MODIFIER_PRIORITY) {
			if (!input.getModifiers().contains(modifier)) {
				continue;
			}
			KeyModifier nativeModifier = switch (modifier) {
				case CONTROL, COMMAND -> KeyModifier.CONTROL;
				case SHIFT -> KeyModifier.SHIFT;
				case ALT -> KeyModifier.ALT;
			};
			return ConfigKeyMappingHelper.INSTANCE.createBinding(key, fromForge(nativeModifier));
		}
		return ConfigKeyMappingHelper.INSTANCE.createBinding(key, Set.of());
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

	private static KeyInputModifier getNativeControlInput() {
		// Forge's Control modifier matches Command on macOS.
		if (ConfigInputUtil.isMac()) {
			return KeyInputModifier.COMMAND;
		}
		return KeyInputModifier.CONTROL;
	}

	private static KeyModifier selectModifier(IKeyMappingBinding value) {
		for (KeyMappingModifier modifier : MODIFIER_PRIORITY) {
			if (!value.getModifiers().contains(modifier)) {
				continue;
			}
			KeyModifier nativeModifier = toForge(modifier);
			if (nativeModifier != KeyModifier.NONE && !nativeModifier.matches(value.getKey())) {
				return nativeModifier;
			}
		}
		return KeyModifier.NONE;
	}

	private static KeyModifier toForge(KeyMappingModifier modifier) {
		return switch (modifier) {
			case CONTROL_OR_COMMAND -> KeyModifier.CONTROL;
			case CONTROL -> {
				if (!ConfigInputUtil.isMac()) {
					yield KeyModifier.CONTROL;
				}
				yield KeyModifier.NONE;
			}
			case COMMAND -> {
				if (ConfigInputUtil.isMac()) {
					yield KeyModifier.CONTROL;
				}
				yield KeyModifier.NONE;
			}
			case SHIFT -> KeyModifier.SHIFT;
			case ALT -> KeyModifier.ALT;
		};
	}

	private static Set<KeyMappingModifier> fromForge(KeyModifier modifier) {
		return switch (modifier) {
			case CONTROL -> Set.of(CONTROL_OR_COMMAND);
			case SHIFT -> Set.of(SHIFT);
			case ALT -> Set.of(ALT);
			case NONE -> Set.of();
		};
	}
}
