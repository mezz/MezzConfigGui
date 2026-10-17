package net.mezzdev.config.gui.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.ConfigInputUtil;
import net.mezzdev.config.gui.api.keybindings.KeyInputModifier;
import net.mezzdev.config.gui.keybindings.ConfigKeyMappingHelper;
import net.mezzdev.config.gui.keybindings.KeyMappingAdapters;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static net.mezzdev.config.gui.api.keybindings.KeyMappingModifier.CONTROL;
import static net.mezzdev.config.gui.api.keybindings.KeyMappingModifier.SHIFT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeKeyMappingAdapterTest {
	private final ConfigKeyMappingHelper helper = ConfigKeyMappingHelper.INSTANCE;

	@Test
	void aModifierUsedAsTheMainKeyDoesNotDiscardOtherHeldModifiers() {
		var platformHelper = new NeoForgeConfigKeyMappingPlatformHelper();
		KeyMapping mapping = mapping("modifier_key", KeyConflictContext.UNIVERSAL, KeyModifier.NONE);
		var adapter = platformHelper.createAdapter(mapping);
		var configMapping = KeyMappingAdapters.create(mapping);
		var binding = configMapping.createBinding(InputConstants.getKey("key.keyboard.left.control"), InputConstants.MOD_CONTROL | InputConstants.MOD_SHIFT);

		adapter.setValue(binding);

		assertEquals(binding.getKey(), mapping.getKey());
		assertEquals(KeyModifier.SHIFT, mapping.getKeyModifier());
		assertEquals(binding, adapter.getValue());
	}

	@Test
	void capturedModifiersUseTheNativeBindingRepresentation() {
		var platformHelper = new NeoForgeConfigKeyMappingPlatformHelper();
		KeyMapping mapping = mapping("capture", KeyConflictContext.UNIVERSAL, KeyModifier.NONE);
		var adapter = platformHelper.createAdapter(mapping);
		KeyInputModifier platformModifier = KeyInputModifier.CONTROL;
		int eventModifiers = InputConstants.MOD_CONTROL | InputConstants.MOD_SHIFT;
		KeyModifier expectedModifier = KeyModifier.CONTROL;
		if (ConfigInputUtil.isMac()) {
			platformModifier = KeyInputModifier.COMMAND;
			eventModifiers = InputConstants.MOD_SUPER | InputConstants.MOD_SHIFT;
			expectedModifier = KeyModifier.CONTROL_OR_COMMAND;
		}
		var configMapping = KeyMappingAdapters.create(mapping);
		var binding = configMapping.createBinding(InputConstants.getKey("key.keyboard.l"), eventModifiers);
		assertTrue(mapping.isDefault());
		adapter.setValue(binding);

		assertEquals(expectedModifier, mapping.getKeyModifier());
		assertEquals(binding, adapter.getValue());
		assertEquals(Set.of(platformModifier), binding.resolve().getModifiers());
	}

	@Test
	void conflictPreviewPreservesPhysicalControlAndHonorsNativeContexts() {
		var platformHelper = new NeoForgeConfigKeyMappingPlatformHelper();
		KeyMapping first = mapping("first", KeyConflictContext.IN_GAME, KeyModifier.CONTROL);
		KeyMapping second = mapping("second", KeyConflictContext.IN_GAME, KeyModifier.SHIFT);
		var adapter = platformHelper.createAdapter(first);
		var original = adapter.getValue();
		assertEquals(Set.of(CONTROL), original.getModifiers());
		var proposed = helper.createBinding(original.getKey(), Set.of(SHIFT));
		assertTrue(helper.conflicts(adapter, proposed, second));
		assertEquals(original, adapter.getValue());
		assertEquals(KeyModifier.CONTROL, first.getKeyModifier());
		second.setKeyConflictContext(KeyConflictContext.GUI);
		assertFalse(helper.conflicts(adapter, proposed, second));
		assertEquals(original, adapter.getValue());
	}

	@Test
	void resetRetainsTheNativePlatformDependentDefault() {
		var platformHelper = new NeoForgeConfigKeyMappingPlatformHelper();
		KeyMapping mapping = mapping("defaults", KeyConflictContext.UNIVERSAL, KeyModifier.CONTROL_OR_COMMAND);
		var adapter = platformHelper.createAdapter(mapping);
		var defaults = adapter.getDefaultValue();
		var configMapping = KeyMappingAdapters.create(mapping);
		var binding = configMapping.createBinding(InputConstants.getKey("key.keyboard.l"), InputConstants.MOD_SHIFT | InputConstants.MOD_ALT);
		adapter.setValue(binding);
		assertEquals(1, adapter.getValue().getModifiers().size());
		adapter.setValue(defaults);
		assertTrue(mapping.isDefault());
		assertEquals(KeyModifier.CONTROL_OR_COMMAND, mapping.getKeyModifier());
	}

	private static KeyMapping mapping(String name, KeyConflictContext context, KeyModifier modifier) {
		return new KeyMapping("key.test." + name, context, modifier, InputConstants.Type.KEYBOARD, InputConstants.KEY_P,
			new KeyMapping.Category(Identifier.withDefaultNamespace("adapter_test")));
	}
}
