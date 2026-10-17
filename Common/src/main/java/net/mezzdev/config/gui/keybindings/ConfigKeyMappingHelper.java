package net.mezzdev.config.gui.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingAdapter;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingBinding;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingHelper;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingInput;
import net.mezzdev.config.gui.api.keybindings.KeyInputModifier;
import net.mezzdev.config.gui.api.keybindings.KeyMappingModifier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static net.mezzdev.config.gui.api.keybindings.KeyInputModifier.ALT;
import static net.mezzdev.config.gui.api.keybindings.KeyInputModifier.COMMAND;
import static net.mezzdev.config.gui.api.keybindings.KeyInputModifier.CONTROL;
import static net.mezzdev.config.gui.api.keybindings.KeyInputModifier.SHIFT;

public final class ConfigKeyMappingHelper implements IKeyMappingHelper {
	public static final ConfigKeyMappingHelper INSTANCE = new ConfigKeyMappingHelper();
	private static final Set<String> RESERVED_MODIFIER_NAMES = Set.of("shift", "control", "alt", "command", "control_or_command", "none");
	private static final String CUSTOM_MODIFIER_PATTERN = "[a-z0-9_.:/-]+";

	private ConfigKeyMappingHelper() {
	}

	@Override
	public IKeyMappingBinding createBinding(InputConstants.Key key, Set<KeyMappingModifier> modifiers) {
		return new ConfigKeyBinding(key, modifiers);
	}

	IKeyMappingBinding createBinding(IKeyMappingInput input) {
		Set<KeyMappingModifier> modifiers = EnumSet.noneOf(KeyMappingModifier.class);
		for (KeyInputModifier modifier : input.getModifiers()) {
			modifiers.add(toBindingModifier(modifier));
		}
		return createBinding(input.getKey(), modifiers, input.getCustomModifiers());
	}

	IKeyMappingInput resolve(IKeyMappingBinding binding) {
		Set<KeyInputModifier> modifiers = EnumSet.noneOf(KeyInputModifier.class);
		for (KeyMappingModifier modifier : binding.getModifiers()) {
			modifiers.add(resolveModifier(modifier));
		}
		return createInput(binding.getKey(), modifiers, binding.getCustomModifiers());
	}

	Component getDisplayName(IKeyMappingBinding value) {
		SortedSet<String> translationKeys = new TreeSet<>();
		for (String name : value.getCustomModifiers()) {
			translationKeys.add("mezz_config.key.modifier." + name);
		}
		for (KeyInputModifier modifier : resolve(value).getModifiers()) {
			translationKeys.add(getModifierTranslationKey(modifier));
		}
		Component result = value.getKey().getDisplayName();
		for (String translationKey : translationKeys) {
			Component modifierName = Component.translatable(translationKey);
			result = Component.translatable("mezz_config.key.combo", modifierName, result);
		}
		return result;
	}

	public boolean conflicts(IKeyMappingAdapter adapter, IKeyMappingBinding value, KeyMapping other) {
		if (value.isUnbound() || other.isUnbound()) {
			return false;
		}
		IKeyMappingBinding original = adapter.getValue();
		try {
			adapter.setValue(value);
			return adapter.conflicts(other) || KeyMappingAdapters.resolve(other).conflicts(adapter.getKeyMapping());
		} finally {
			adapter.setValue(original);
		}
	}

	IKeyMappingInput createInputFromEvent(InputConstants.Key key, int eventModifiers) {
		Set<KeyInputModifier> modifiers = EnumSet.noneOf(KeyInputModifier.class);
		if ((eventModifiers & InputConstants.MOD_SHIFT) != 0) {
			modifiers.add(SHIFT);
		}
		if ((eventModifiers & InputConstants.MOD_CONTROL) != 0) {
			modifiers.add(CONTROL);
		}
		if ((eventModifiers & InputConstants.MOD_ALT) != 0) {
			modifiers.add(ALT);
		}
		if ((eventModifiers & InputConstants.MOD_SUPER) != 0) {
			modifiers.add(COMMAND);
		}
		modifiers.remove(getModifier(key));
		return createInput(key, modifiers, Set.of());
	}

	@Nullable
	KeyInputModifier getModifier(InputConstants.Key key) {
		if (key.getType() != InputConstants.Type.KEYBOARD) {
			return null;
		}
		return switch (key.getValue()) {
			case InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT -> SHIFT;
			case InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL -> CONTROL;
			case InputConstants.KEY_LALT, InputConstants.KEY_RALT -> ALT;
			case InputConstants.KEY_LGUI, InputConstants.KEY_RGUI -> COMMAND;
			default -> null;
		};
	}

	@Override
	public IKeyMappingBinding createBinding(InputConstants.Key key, Set<KeyMappingModifier> modifiers, Set<String> customModifiers) {
		return new ConfigKeyBinding(key, modifiers, customModifiers);
	}

	IKeyMappingInput createInput(InputConstants.Key key, Set<KeyInputModifier> modifiers, Set<String> customModifiers) {
		return new KeyMappingInput(key, modifiers, customModifiers);
	}

	static Set<String> copyCustomModifiers(Set<String> modifiers) {
		Set<String> copy = Set.copyOf(modifiers);
		for (String modifier : copy) {
			if (!modifier.matches(CUSTOM_MODIFIER_PATTERN) || RESERVED_MODIFIER_NAMES.contains(modifier)) {
				throw new IllegalArgumentException("Invalid custom modifier identifier: " + modifier);
			}
		}
		return copy;
	}

	private static String getModifierTranslationKey(KeyInputModifier modifier) {
		return switch (modifier) {
			case SHIFT -> "mezz_config.key.modifier.shift";
			case CONTROL -> "mezz_config.key.modifier.control";
			case ALT -> "mezz_config.key.modifier.alt";
			case COMMAND -> "mezz_config.key.modifier.command";
		};
	}

	private static KeyInputModifier resolveModifier(KeyMappingModifier modifier) {
		return switch (modifier) {
			case SHIFT -> SHIFT;
			case CONTROL -> CONTROL;
			case ALT -> ALT;
			case COMMAND -> COMMAND;
			case CONTROL_OR_COMMAND -> {
				if (InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY) {
					yield COMMAND;
				}
				yield CONTROL;
			}
		};
	}

	private static KeyMappingModifier toBindingModifier(KeyInputModifier modifier) {
		return switch (modifier) {
			case SHIFT -> KeyMappingModifier.SHIFT;
			case CONTROL -> KeyMappingModifier.CONTROL;
			case ALT -> KeyMappingModifier.ALT;
			case COMMAND -> KeyMappingModifier.COMMAND;
		};
	}
}
