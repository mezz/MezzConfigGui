package net.mezzdev.config.gui.test.fabric.defaults;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingAdapter;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingBinding;
import net.mezzdev.config.gui.fabric.FabricConfigKeyMappingPlatformHelper;
import net.mezzdev.config.gui.keybindings.ConfigKeyBindingUtil;
import net.mezzdev.config.gui.keybindings.ConfigKeyMappingHelper;
import net.mezzdev.config.gui.keybindings.KeyMappingConfigValue;
import net.mezzdev.config.gui.keybindings.KeyMappingConfigValues;
import net.mezzdev.config.gui.keybindings.KeyMappingValue;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Exercises the binding editor with an installed JEI mapping and Fabric's real mixins. */
final class FabricJeiKeyMappingTest {

	private FabricJeiKeyMappingTest() {}

	static void run() {
		check(FabricLoader.getInstance().isModLoaded("jei"), "JEI must be installed for its key mapping regression test");
		KeyMapping mapping = Arrays.stream(Minecraft.getInstance().options.keyMappings)
			.filter(candidate -> candidate.getClass().getName().equals("mezz.jei.fabric.input.FabricKeyMapping"))
			.filter(candidate -> !candidate.getDefaultKey().equals(InputConstants.UNKNOWN))
			.findFirst()
			.orElseThrow(() -> new AssertionError("No installed JEI Fabric key mapping with a bound default was found"));
		IKeyMappingAdapter adapter = new FabricConfigKeyMappingPlatformHelper().createAdapter(mapping);
		var screenValue = KeyMappingConfigValues.create(List.of(mapping)).getFirst();
		check(screenValue instanceof KeyMappingConfigValue, "JEI mapping did not create a key mapping screen value");
		KeyMappingConfigValue value = (KeyMappingConfigValue) screenValue;
		IKeyMappingBinding original = ConfigKeyMappingHelper.INSTANCE.createBinding(ConfigKeyBindingUtil.getKey(mapping), Set.of());
		List<KeyMappingValue> notifications = new ArrayList<>();
		Runnable removeListener = value.addListener(notifications::add);
		try {
			check(adapter.getValue().equals(original), "Fabric adapter read JEI's hidden parent key instead of its real binding");
			InputConstants.Key changedKey = InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_F8);
			if (changedKey.equals(original.getKey()) || changedKey.equals(mapping.getDefaultKey())) {
				changedKey = InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_F9);
			}
			IKeyMappingBinding changed = ConfigKeyMappingHelper.INSTANCE.createBinding(changedKey, Set.of());
			verifyChange(mapping, adapter, value, changed, notifications, "rebind");
			verifyChange(mapping, adapter, value,
				ConfigKeyMappingHelper.INSTANCE.createBinding(InputConstants.UNKNOWN, Set.of()), notifications, "unbind");
			verifyChange(mapping, adapter, value, adapter.getDefaultValue(), notifications, "reset");
		} finally {
			removeListener.run();
			adapter.setValue(original);
			KeyMapping.resetMapping();
			Minecraft.getInstance().options.save();
		}
		LogManager.getLogger().info("MEZZ_CONFIG_GUI_JEI_KEY_MAPPING_TEST_PASSED");
	}

	private static void verifyChange(
		KeyMapping mapping,
		IKeyMappingAdapter adapter,
		KeyMappingConfigValue value,
		IKeyMappingBinding binding,
		List<KeyMappingValue> notifications,
		String stage
	) {
		int notificationCount = notifications.size();
		check(value.set(value.getValue().withBinding(binding)), "JEI " + stage + " was not reported as a change");
		check(adapter.getValue().equals(binding), "Fabric adapter did not read the new JEI binding after " + stage);
		check(mapping.saveString().equals(binding.getKey().getName()), "JEI serialized binding is stale after " + stage);
		check(KeyMappingHelper.getBoundKeyOf(mapping).equals(InputConstants.UNKNOWN), "JEI parent binding must remain hidden after " + stage);
		check(notifications.size() == notificationCount + 1, "Binding listener was not notified exactly once after " + stage);
		KeyMappingValue notified = notifications.getLast();
		check(notified.binding().equals(binding), "Binding listener received a stale value after " + stage);
		String displayed = value.getLocalizedValueName(value.getLocalizationKey(), notified).getString();
		check(displayed.equals(mapping.getTranslatedKeyMessage().getString()), "Displayed JEI binding is stale after " + stage);
		check(!value.set(value.getValue().withBinding(binding)), "An unchanged JEI binding was reported as a change after " + stage);
		check(notifications.size() == notificationCount + 1, "An unchanged binding notified listeners after " + stage);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
