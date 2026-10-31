package net.mezzdev.config.gui.test.fabric.defaults;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.mezzdev.config.gui.ConfigScreen;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingAdapter;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingBinding;
import net.mezzdev.config.gui.api.keybindings.KeyMappingModifier;
import net.mezzdev.config.gui.fabric.FabricConfigKeyMappingPlatformHelper;
import net.mezzdev.config.gui.keybindings.ConfigKeyBindingUtil;
import net.mezzdev.config.gui.keybindings.ConfigKeyMappingHelper;
import net.mezzdev.config.gui.keybindings.KeyMappingConfigValue;
import net.mezzdev.config.gui.keybindings.KeyMappingConfigValues;
import net.mezzdev.config.gui.keybindings.KeyMappingValue;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Exercises the binding editor with an installed JEI mapping and Fabric's real mixins. */
final class FabricJeiKeyMappingTest {

	private FabricJeiKeyMappingTest() {}

	static void run() {
		check(FabricLoader.getInstance().isModLoaded("jei"), "JEI must be installed for its key mapping regression test");
		Minecraft minecraft = Minecraft.getInstance();
		verifyConfigScreen(minecraft);
		boolean usesAmecs = FabricLoader.getInstance().isModLoaded("amecs_key_modifiers");
		String mappingClass = "mezz.jei.fabric.input.FabricKeyMapping";
		if (usesAmecs) {
			verifyAmecsModifiers();
			mappingClass = "mezz.jei.fabric.input.AmecsKeyMapping";
		}
		String expectedMappingClass = mappingClass;
		// Use the unmodified recipe shortcut so a reset exercises only the key storage.
		KeyMapping mapping = Arrays.stream(minecraft.options.keyMappings)
			.filter(candidate -> candidate.getClass().getName().equals(expectedMappingClass))
			.filter(candidate -> candidate.getName().equals("key.jei.showRecipe"))
			.filter(candidate -> !candidate.getDefaultKey().equals(InputConstants.UNKNOWN))
			.findFirst()
			.orElseThrow(() -> new AssertionError("JEI's Fabric recipe shortcut has no bound default"));
		IKeyMappingAdapter adapter = new FabricConfigKeyMappingPlatformHelper().createAdapter(mapping);
		var screenValue = KeyMappingConfigValues.create(List.of(mapping)).get(0);
		check(screenValue instanceof KeyMappingConfigValue, "JEI mapping did not create a key mapping screen value");
		KeyMappingConfigValue value = (KeyMappingConfigValue) screenValue;
		IKeyMappingBinding original = ConfigKeyMappingHelper.INSTANCE.createBinding(ConfigKeyBindingUtil.getKey(mapping), Set.of());
		List<KeyMappingValue> notifications = new ArrayList<>();
		Runnable removeListener = value.addListener(notifications::add);
		try {
			check(adapter.getValue().equals(original), "Fabric adapter did not read JEI's current binding");
			InputConstants.Key changedKey = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F8);
			if (changedKey.equals(original.getKey()) || changedKey.equals(mapping.getDefaultKey())) {
				changedKey = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F9);
			}
			IKeyMappingBinding changed = ConfigKeyMappingHelper.INSTANCE.createBinding(changedKey, Set.of());
			verifyChange(mapping, adapter, value, changed, notifications, usesAmecs, "rebind");
			verifyChange(mapping, adapter, value,
				ConfigKeyMappingHelper.INSTANCE.createBinding(InputConstants.UNKNOWN, Set.of()), notifications, usesAmecs, "unbind");
			verifyChange(mapping, adapter, value, adapter.getDefaultValue(), notifications, usesAmecs, "reset");
		} finally {
			removeListener.run();
			adapter.setValue(original);
			KeyMapping.resetMapping();
			Minecraft.getInstance().options.save();
		}
		LogManager.getLogger().info("MEZZ_CONFIG_GUI_JEI_KEY_MAPPING_TEST_PASSED");
	}

	private static void verifyAmecsModifiers() {
		var mapping = new KeyMapping("key.mezz_config_gui_test.amecs", GLFW.GLFW_KEY_F10, "key.categories.misc");
		var adapter = new FabricConfigKeyMappingPlatformHelper().createAdapter(mapping);
		var defaults = adapter.getDefaultValue();
		var binding = ConfigKeyMappingHelper.INSTANCE.createBinding(
			InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F11),
			Set.of(KeyMappingModifier.CONTROL, KeyMappingModifier.SHIFT)
		);
		adapter.setValue(binding);
		check(adapter.getValue().equals(binding), "AMECS must retain both physical modifiers");
		adapter.setValue(defaults);
		check(adapter.getValue().equals(defaults), "Reset must clear AMECS modifiers");
		LogManager.getLogger().info("MEZZ_CONFIG_GUI_AMECS_TEST_PASSED");
	}

	private static void verifyConfigScreen(Minecraft minecraft) {
		Screen parent = minecraft.screen;
		try {
			Class<?> plugin = Class.forName("mezz.jei.gui.config.JeiConfigGuiPlugin");
			var created = (Optional<?>) plugin.getMethod("createScreen", Screen.class).invoke(null, parent);
			Object screen = created.orElseThrow(() -> new AssertionError("JEI's config screen was not registered"));
			check(screen instanceof ConfigScreen, "JEI must provide its configured MezzConfig GUI screen");
			minecraft.setScreen((Screen) screen);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("JEI's published config screen could not be opened", e);
		} finally {
			minecraft.setScreen(parent);
		}
	}

	private static void verifyChange(
		KeyMapping mapping,
		IKeyMappingAdapter adapter,
		KeyMappingConfigValue value,
		IKeyMappingBinding binding,
		List<KeyMappingValue> notifications,
		boolean usesAmecs,
		String stage
	) {
		int notificationCount = notifications.size();
		check(value.set(value.getValue().withBinding(binding)), "JEI " + stage + " was not reported as a change");
		check(adapter.getValue().equals(binding), "Fabric adapter did not read the new JEI binding after " + stage);
		check(mapping.saveString().equals(binding.getKey().getName()), "JEI serialized binding is stale after " + stage);
		if (usesAmecs) {
			check(KeyBindingHelper.getBoundKeyOf(mapping).equals(binding.getKey()), "AMECS must retain the real JEI binding after " + stage);
		} else {
			check(KeyBindingHelper.getBoundKeyOf(mapping).equals(InputConstants.UNKNOWN), "JEI parent binding must remain hidden after " + stage);
		}
		check(notifications.size() == notificationCount + 1, "Binding listener was not notified exactly once after " + stage);
		KeyMappingValue notified = notifications.get(notifications.size() - 1);
		check(notified.binding().equals(binding), "Binding listener received a stale value after " + stage);
		String displayed = value.getLocalizedValueName(value.getLocalizationKey(), notified).getString();
		String expected = mapping.getTranslatedKeyMessage().getString();
		check(displayed.equals(expected), "Displayed JEI binding is stale after " + stage + " (" + displayed + ", expected " + expected + ")");
		check(!value.set(value.getValue().withBinding(binding)), "An unchanged JEI binding was reported as a change after " + stage);
		check(notifications.size() == notificationCount + 1, "An unchanged binding notified listeners after " + stage);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
