package net.mezzdev.config.gui.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import de.siphalor.amecs.key_modifiers.api.AmecsKeyModifiersApi;
import de.siphalor.amecs.key_modifiers.api.AmecsKeyModifier;
import de.siphalor.amecs.key_modifiers.api.AmecsKeyModifiers;
import de.siphalor.amecs.key_modifiers.api.AmecsKeyModifierCombination;
import net.mezzdev.config.gui.keybindings.ConfigKeyBinding;
import net.mezzdev.config.gui.keybindings.ConfigKeyBindingUtil;
import net.mezzdev.config.gui.keybindings.ConfigKeyModifier;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class AmecsConfigKeyMappingHelper {
	private AmecsConfigKeyMappingHelper() {

	}

	public static ConfigKeyModifier getBoundModifier(KeyMapping keyMapping) {
		return getOnlyModifier(AmecsKeyModifiersApi.getBoundModifiers(keyMapping));
	}

	public static ConfigKeyModifier getDefaultModifier(KeyMapping keyMapping) {
		return getOnlyModifier(AmecsKeyModifiersApi.getDefaultModifiers(keyMapping));
	}

	public static void setModifier(KeyMapping keyMapping, ConfigKeyModifier modifier) {
		AmecsKeyModifierCombination modifiers = AmecsKeyModifiersApi.getBoundModifiers(keyMapping);
		modifiers.unset();
		if (modifier != ConfigKeyModifier.NONE)
			modifiers.set(toAmecs(modifier), true);
		modifiers.cleanup(keyMapping);
	}

	public static Component getDisplayName(ConfigKeyBinding value) {
		InputConstants.Key key = ConfigKeyBindingUtil.getKey(value.keyName());
		AmecsKeyModifierCombination modifiers = new AmecsKeyModifierCombination();
		if (value.modifier() != ConfigKeyModifier.NONE)
			modifiers.set(toAmecs(value.modifier()), true);
		Component component = ConfigKeyBindingUtil.getKeyDisplayName(key);
		for (ConfigKeyModifier modifier : fromAmecs(modifiers)) {
			component = ConfigKeyBindingUtil.getCombinedName(modifier, component);
		}
		return component;
	}

	public static ConfigKeyModifier getKeyModifier(InputConstants.Key key) {
		return ConfigKeyBindingUtil.getKeyModifier(key);
	}

	private static ConfigKeyModifier getOnlyModifier(AmecsKeyModifierCombination modifiers) {
		List<ConfigKeyModifier> configModifiers = fromAmecs(modifiers);
		for (ConfigKeyModifier modifier : configModifiers) {
			if (modifier != ConfigKeyModifier.NONE) {
				return modifier;
			}
		}
		return ConfigKeyModifier.NONE;
	}

	private static List<ConfigKeyModifier> fromAmecs(AmecsKeyModifierCombination modifiers) {
		if (modifiers.isUnset()) {
			return List.of(ConfigKeyModifier.NONE);
		}
		List<ConfigKeyModifier> modifiersList = new ArrayList<>();
		if (modifiers.getAlt()) {
			modifiersList.add(ConfigKeyModifier.ALT);
		}
		if (modifiers.getControl()) {
			modifiersList.add(ConfigKeyModifier.CONTROL_OR_COMMAND);
		}
		if (modifiers.getShift()) {
			modifiersList.add(ConfigKeyModifier.SHIFT);
		}
		return modifiersList;
	}

	private static AmecsKeyModifier toAmecs(ConfigKeyModifier modifier) {
		return switch (modifier) {
			case CONTROL_OR_COMMAND -> AmecsKeyModifiers.CONTROL;
			case SHIFT -> AmecsKeyModifiers.SHIFT;
			case ALT -> AmecsKeyModifiers.ALT;
			case NONE -> throw new IllegalArgumentException("NONE has no AMECS modifier");
		};
	}
}
