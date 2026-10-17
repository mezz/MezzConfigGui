package net.mezzdev.config.gui.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingAdapter;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingBinding;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingInput;

import net.mezzdev.config.gui.ConfigInputUtil;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

record ConfigKeyMapping(
	KeyMapping keyMapping,
	IKeyMappingAdapter adapter
) implements IConfigKeyMapping {
	ConfigKeyMapping(KeyMapping mapping) {
		this(mapping, KeyMappingAdapters.resolve(mapping));
	}

	private static final Logger LOGGER = LogManager.getLogger();
	private static final Set<String> MISSING_LOCALIZATION_KEYS = ConcurrentHashMap.newKeySet();

	@Override
	public String getName() {
		return keyMapping.getName();
	}

	@Override
	public Component getLocalizedName() {
		return getLocalized(keyMapping.getName(), "name");
	}

	@Override
	public Component getLocalizedContext() {
		return Component.translatableWithFallback(keyMapping.getName() + ".context", "");
	}

	@Override
	public Component getLocalizedDescription() {
		return Component.translatableWithFallback(keyMapping.getName() + ".description", "");
	}

	@Override
	public IKeyMappingBinding getValue() {
		return adapter.getValue();
	}

	@Override
	public IKeyMappingBinding getDefaultValue() {
		return adapter.getDefaultValue();
	}

	@Override
	public void set(IKeyMappingBinding value) {
		adapter.setValue(value);
	}

	@Override
	public Component getValueName(IKeyMappingBinding value) {
		return adapter.getDisplayName(value);
	}

	@Override
	public boolean isModifier(InputConstants.Key key) {
		return ConfigKeyMappingHelper.INSTANCE.getModifier(key) != null || adapter.isCustomModifier(key);
	}

	@Override
	public IKeyMappingBinding createBinding(InputConstants.Key key, int eventModifiers) {
		IKeyMappingInput input = ConfigKeyMappingHelper.INSTANCE.createInputFromEvent(key, eventModifiers);
		IKeyMappingInput captured = adapter.captureInput(input);
		return adapter.createBinding(captured);
	}

	@Override
	@Unmodifiable
	public List<ConfigKeyMappingConflict> getConflicts(IKeyMappingBinding value) {
		IConfigKeyMappingPlatformHelper platformHelper = ConfigKeyMappingPlatformServices.PLATFORM_HELPER;
		if (value.isUnbound()) {
			return List.of();
		}
		KeyMapping[] keyMappings = Minecraft.getInstance().options.keyMappings;
		List<ConfigKeyMappingConflict> conflicts = new ArrayList<>();
		for (KeyMapping otherKey : keyMappings) {
			if (otherKey != keyMapping && ConfigKeyMappingHelper.INSTANCE.conflicts(adapter, value, otherKey)) {
				conflicts.add(createConflict(platformHelper, otherKey));
			}
		}
		return List.copyOf(conflicts);
	}

	private static ConfigKeyMappingConflict createConflict(IConfigKeyMappingPlatformHelper platformHelper, KeyMapping conflict) {
		var conflictAdapter = KeyMappingAdapters.resolve(conflict);
		Component conflictInput = conflictAdapter.getDisplayName(conflictAdapter.getValue());
		return new ConfigKeyMappingConflict(
			getLocalized(conflict, conflict.getName(), "conflict name"),
			conflictInput,
			Component.literal(getModName(platformHelper, conflict)),
			getLocalized(conflict, ConfigInputUtil.category(conflict), "conflict category")
		);
	}

	private Component getLocalized(String key, String description) {
		return getLocalized(keyMapping, key, description);
	}

	private static Component getLocalized(KeyMapping keyMapping, String key, String description) {
		logMissingLocalization(keyMapping, key, description);
		return Component.translatable(key);
	}

	private static void logMissingLocalization(KeyMapping keyMapping, String key, String description) {
		if (!ConfigKeyMappingPlatformServices.PLATFORM_HELPER.isInDev()) {
			return;
		}
		if (Language.getInstance().has(key)) {
			return;
		}
		if (MISSING_LOCALIZATION_KEYS.add(key)) {
			LOGGER.error("Missing {} localization for key mapping '{}': {}", description, keyMapping.getName(), key);
		}
	}

	private static String getModName(IConfigKeyMappingPlatformHelper platformHelper, KeyMapping keyMapping) {
		String modId = getModId(keyMapping);
		return platformHelper.getModNameForModId(modId);
	}

	private static String getModId(KeyMapping keyMapping) {
		String keyName = keyMapping.getName();
		if (keyName.startsWith("key.")) {
			String[] parts = keyName.split("\\.");
			if (parts.length > 2 && !parts[1].equals("category") && !parts[1].equals("categories")) {
				return parts[1].toLowerCase(Locale.ROOT);
			}
		}
		return getModIdFromCategory(ConfigInputUtil.category(keyMapping));
	}

	private static String getModIdFromCategory(String category) {
		if (category.startsWith("key.categories.")) {
			return "minecraft";
		}
		if (category.startsWith("key.category.")) {
			String[] parts = category.split("\\.");
			if (parts.length > 2) {
				return parts[2].toLowerCase(Locale.ROOT);
			}
		}
		return "minecraft";
	}
}
