package net.mezzdev.config.gui.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.api.keybindings.KeyInputModifier;
import net.mezzdev.config.gui.api.keybindings.KeyMappingModifier;
import net.minecraft.client.input.InputQuirks;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeyMappingInputTest {
	private final ConfigKeyMappingHelper helper = ConfigKeyMappingHelper.INSTANCE;

	@Test
	void resolvingAnAliasDoesNotChangeTheConfiguredBinding() {
		var key = InputConstants.getKey("key.keyboard.p");
		var portable = helper.createBinding(key, Set.of(KeyMappingModifier.CONTROL_OR_COMMAND));

		var input = portable.resolve();
		var physical = input.toBinding();

		KeyInputModifier expected = KeyInputModifier.CONTROL;
		if (InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY) {
			expected = KeyInputModifier.COMMAND;
		}
		assertEquals(Set.of(expected), input.getModifiers());
		assertEquals(input, physical.resolve());
		assertNotEquals(portable, physical);
		assertEquals(Set.of(KeyMappingModifier.CONTROL_OR_COMMAND), portable.getModifiers());
	}

	@Test
	void capturedModifiersRoundTripThroughBindings() {
		var key = InputConstants.getKey("key.mouse.right");
		int mask = InputConstants.MOD_CONTROL | InputConstants.MOD_SUPER | InputConstants.MOD_SHIFT | InputConstants.MOD_ALT;
		var input = helper.createInputFromEvent(key, mask);

		var binding = input.toBinding();
		var resolved = binding.resolve();

		assertEquals(Set.of(KeyMappingModifier.CONTROL, KeyMappingModifier.COMMAND, KeyMappingModifier.SHIFT, KeyMappingModifier.ALT), binding.getModifiers());
		assertEquals(input, resolved);
	}

	@Test
	void capturingAModifierKeyExcludesItselfAndKeepsOtherHeldModifiers() {
		var key = InputConstants.getKey("key.keyboard.left.control");
		int mask = InputConstants.MOD_CONTROL | InputConstants.MOD_SUPER | InputConstants.MOD_SHIFT;

		var input = helper.createInputFromEvent(key, mask);

		assertEquals(key, input.getKey());
		assertEquals(Set.of(KeyInputModifier.COMMAND, KeyInputModifier.SHIFT), input.getModifiers());
	}

	@Test
	void convertingExistingInputPreservesAllStandardAndCustomRequirements() {
		var key = InputConstants.getKey("key.keyboard.left.control");
		var modifiers = new HashSet<>(Set.of(KeyInputModifier.CONTROL, KeyInputModifier.SHIFT));
		var customModifiers = new HashSet<>(Set.of("test:extra"));
		var input = helper.createInput(key, modifiers, customModifiers);
		modifiers.clear();
		customModifiers.clear();

		var binding = input.toBinding();
		var resolved = binding.resolve();

		assertEquals(key, binding.getKey());
		assertEquals(Set.of(KeyMappingModifier.CONTROL, KeyMappingModifier.SHIFT), binding.getModifiers());
		assertEquals(Set.of("test:extra"), binding.getCustomModifiers());
		assertEquals(input, resolved);
	}

	@Test
	void customModifierIdentifiersExcludeReservedNamesAndInvalidCharacters() {
		var key = InputConstants.getKey("key.keyboard.p");
		for (String invalid : Set.of("shift", "none", "bad modifier", "")) {
			assertThrows(IllegalArgumentException.class, () -> helper.createBinding(key, Set.of(), Set.of(invalid)));
		}
	}
}
