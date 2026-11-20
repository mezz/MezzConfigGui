package net.mezzdev.config.gui.forge;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.ConfigInputUtil;
import net.mezzdev.config.gui.api.keybindings.KeyInputModifier;
import net.mezzdev.config.gui.keybindings.ConfigKeyMappingHelper;
import net.mezzdev.config.gui.keybindings.KeyMappingAdapters;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.Set;
import static net.mezzdev.config.gui.api.keybindings.KeyMappingModifier.CONTROL_OR_COMMAND;
import static net.mezzdev.config.gui.api.keybindings.KeyMappingModifier.SHIFT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeKeyMappingAdapterTest {
	private final ConfigKeyMappingHelper helper = ConfigKeyMappingHelper.INSTANCE;

	@Test
	void captureCanRoundTripAForgeModifierUsedAsTheMainKey() {
		var platformHelper = new ForgeConfigKeyMappingPlatformHelper();
		KeyMapping mapping = mapping("modifier_key", KeyConflictContext.UNIVERSAL, KeyModifier.NONE);
		var adapter = platformHelper.createAdapter(mapping);
		var configMapping = KeyMappingAdapters.create(mapping);
		int keyCode = GLFW.GLFW_KEY_LEFT_CONTROL;
		int modifiers = GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT;
		if (ConfigInputUtil.isMac()) {
			keyCode = GLFW.GLFW_KEY_LEFT_SUPER;
			modifiers = GLFW.GLFW_MOD_SUPER | GLFW.GLFW_MOD_SHIFT;
		}
		var key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);

		var binding = configMapping.createBinding(key, modifiers);
		adapter.setValue(binding);

		assertEquals(key, mapping.getKey());
		assertEquals(KeyModifier.NONE, mapping.getKeyModifier());
		assertEquals(binding, adapter.getValue());
	}

	@Test
	void capturedModifiersUseTheNativeBindingRepresentation() {
		var platformHelper = new ForgeConfigKeyMappingPlatformHelper();
		KeyMapping mapping = mapping("capture", KeyConflictContext.UNIVERSAL, KeyModifier.NONE);
		var adapter = platformHelper.createAdapter(mapping);
		KeyInputModifier platformModifier = KeyInputModifier.CONTROL;
		int eventModifiers = GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT;
		if (ConfigInputUtil.isMac()) {
			platformModifier = KeyInputModifier.COMMAND;
			eventModifiers = GLFW.GLFW_MOD_SUPER | GLFW.GLFW_MOD_SHIFT;
		}
		var configMapping = KeyMappingAdapters.create(mapping);
		var binding = configMapping.createBinding(InputConstants.getKey("key.keyboard.l"), eventModifiers);
		assertTrue(mapping.isDefault());
		adapter.setValue(binding);

		assertEquals(KeyModifier.CONTROL, mapping.getKeyModifier());
		assertEquals(binding, adapter.getValue());
		assertEquals(Set.of(CONTROL_OR_COMMAND), binding.getModifiers());
		assertEquals(Set.of(platformModifier), binding.resolve().getModifiers());
	}

	@Test
	void conflictPreviewPreservesNativeControlAndHonorsNativeContexts() {
		var platformHelper = new ForgeConfigKeyMappingPlatformHelper();
		KeyMapping first = mapping("first", KeyConflictContext.IN_GAME, KeyModifier.CONTROL);
		KeyMapping second = mapping("second", KeyConflictContext.IN_GAME, KeyModifier.SHIFT);
		var adapter = platformHelper.createAdapter(first);
		var original = adapter.getValue();
		assertEquals(Set.of(CONTROL_OR_COMMAND), original.getModifiers());
		var proposed = helper.createBinding(original.getKey(), Set.of(SHIFT));
		assertTrue(helper.conflicts(adapter, proposed, second));
		assertEquals(original, adapter.getValue());
		assertEquals(KeyModifier.CONTROL, first.getKeyModifier());
		second.setKeyConflictContext(KeyConflictContext.GUI);
		assertFalse(helper.conflicts(adapter, proposed, second));
		assertEquals(original, adapter.getValue());
	}

	@Test
	void resetRetainsTheNativeControlDefault() {
		var platformHelper = new ForgeConfigKeyMappingPlatformHelper();
		KeyMapping mapping = mapping("defaults", KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL);
		var adapter = platformHelper.createAdapter(mapping);
		var defaults = adapter.getDefaultValue();
		var configMapping = KeyMappingAdapters.create(mapping);
		var binding = configMapping.createBinding(InputConstants.getKey("key.keyboard.l"), GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT);
		adapter.setValue(binding);
		assertEquals(1, adapter.getValue().getModifiers().size());
		adapter.setValue(defaults);
		assertTrue(mapping.isDefault());
		assertEquals(KeyModifier.CONTROL, mapping.getKeyModifier());
	}

	private static KeyMapping mapping(String name, KeyConflictContext context, KeyModifier modifier) {
		return new KeyMapping("key.test." + name, context, modifier, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P,
			"key.categories.adapter_test");
	}
}
