package net.mezzdev.config.gui.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import de.siphalor.amecs.key_modifiers.api.AmecsKeyModifier;
import de.siphalor.amecs.key_modifiers.api.AmecsKeyModifierCombination;
import de.siphalor.amecs.key_modifiers.api.AmecsKeyModifiers;
import de.siphalor.amecs.key_modifiers.api.AmecsKeyModifiersApi;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingAdapter;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingBinding;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingInput;
import net.mezzdev.config.gui.api.keybindings.KeyInputModifier;
import net.mezzdev.config.gui.api.keybindings.KeyMappingModifier;
import net.mezzdev.config.gui.keybindings.ConfigKeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/** Optional AMECS integration. Loaded only when its modifier module is installed. */
final class AmecsKeyMappingAdapter implements IKeyMappingAdapter {
	private final KeyMapping mapping;
	private final @Nullable AmecsKeyModifier commandModifier;

	AmecsKeyMappingAdapter(KeyMapping mapping) {
		this.mapping = mapping;
		this.commandModifier = AmecsKeyModifiers.fromKeyCode(InputConstants.KEY_LGUI);
	}

	@Override
	public KeyMapping getKeyMapping() {
		return mapping;
	}

	@Override
	public IKeyMappingBinding getValue() {
		return createBinding(KeyMappingHelper.getBoundKeyOf(mapping), AmecsKeyModifiersApi.getBoundModifiers(mapping));
	}

	@Override
	public IKeyMappingBinding getDefaultValue() {
		return createBinding(mapping.getDefaultKey(), AmecsKeyModifiersApi.getDefaultModifiers(mapping));
	}

	@Override
	public void setValue(IKeyMappingBinding value) {
		IKeyMappingInput input = value.resolve();
		var modifiers = toAmecsModifiers(input);
		mapping.setKey(input.getKey());
		AmecsKeyModifiersApi.getBoundModifiers(mapping).copyFrom(modifiers);
	}

	@Override
	public IKeyMappingBinding createBinding(IKeyMappingInput input) {
		return createBinding(input.getKey(), toAmecsModifiers(input));
	}

	@Override
	public boolean isCustomModifier(InputConstants.Key key) {
		return AmecsKeyModifiers.fromKey(key) != null;
	}

	@Override
	public IKeyMappingInput captureInput(IKeyMappingInput input) {
		// Standard modifiers describe the event itself; AMECS supplies any custom keys.
		Set<String> customModifiers = new HashSet<>();
		AmecsKeyModifier keyModifier = AmecsKeyModifiers.fromKey(input.getKey());
		for (AmecsKeyModifier modifier : AmecsKeyModifierCombination.getCurrentlyPressed().getAll()) {
			if (modifier != keyModifier && getStandardModifier(modifier) == null) {
				customModifiers.add(modifier.getName());
			}
		}
		return input.withCustomModifiers(customModifiers);
	}

	@Override
	public Component getDisplayName(IKeyMappingBinding value) {
		var modifiers = toAmecsModifiers(value.resolve()).getAll().stream()
			.sorted(Comparator.comparing(AmecsKeyModifier::getName))
			.toList();
		Component result = value.getKey().getDisplayName();
		for (AmecsKeyModifier modifier : modifiers) {
			result = Component.translatable("mezz_config.key.combo", Component.translatable(modifier.getTranslationKey()), result);
		}
		return result;
	}

	private IKeyMappingBinding createBinding(InputConstants.Key key, AmecsKeyModifierCombination modifiers) {
		Set<KeyMappingModifier> standardModifiers = EnumSet.noneOf(KeyMappingModifier.class);
		Set<String> customModifiers = new HashSet<>();
		for (AmecsKeyModifier modifier : modifiers.getAll()) {
			KeyMappingModifier standardModifier = getStandardModifier(modifier);
			if (standardModifier != null) {
				standardModifiers.add(standardModifier);
			} else {
				customModifiers.add(modifier.getName());
			}
		}
		return ConfigKeyMappingHelper.INSTANCE.createBinding(key, standardModifiers, customModifiers);
	}

	private AmecsKeyModifierCombination toAmecsModifiers(IKeyMappingInput input) {
		var modifiers = new AmecsKeyModifierCombination();
		for (KeyInputModifier modifier : input.getModifiers()) {
			AmecsKeyModifier amecsModifier = switch (modifier) {
				case SHIFT -> AmecsKeyModifiers.SHIFT;
				case CONTROL -> AmecsKeyModifiers.CONTROL;
				case ALT -> AmecsKeyModifiers.ALT;
				case COMMAND -> commandModifier;
			};
			if (amecsModifier != null) {
				modifiers.set(amecsModifier, true);
			}
		}
		for (String name : input.getCustomModifiers()) {
			AmecsKeyModifier modifier = AmecsKeyModifiers.getByName(name);
			if (modifier != null) {
				modifiers.set(modifier, true);
			}
		}
		return modifiers;
	}

	private @Nullable KeyMappingModifier getStandardModifier(AmecsKeyModifier modifier) {
		if (modifier == AmecsKeyModifiers.SHIFT) {
			return KeyMappingModifier.SHIFT;
		}
		if (modifier == AmecsKeyModifiers.CONTROL) {
			return KeyMappingModifier.CONTROL;
		}
		if (modifier == AmecsKeyModifiers.ALT) {
			return KeyMappingModifier.ALT;
		}
		if (modifier == commandModifier) {
			return KeyMappingModifier.COMMAND;
		}
		return null;
	}
}
