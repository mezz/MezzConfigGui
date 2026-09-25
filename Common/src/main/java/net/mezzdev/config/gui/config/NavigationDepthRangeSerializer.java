package net.mezzdev.config.gui.config;

import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.gui.api.IConfigRangeValueEditorSerializer;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Serializes the finite minimum and maximum sidebar depths. */
final class NavigationDepthRangeSerializer implements IConfigRangeValueEditorSerializer<Integer> {
	static final NavigationDepthRangeSerializer INSTANCE = new NavigationDepthRangeSerializer();
	static final int MAX_DEPTH = 10;

	private NavigationDepthRangeSerializer() {
	}

	@Override
	public ConfigValueRange<Integer> getBounds() {
		return new ConfigValueRange<>(0, MAX_DEPTH);
	}

	@Override
	public boolean isValid(@Nullable ConfigValueRange<Integer> value) {
		return value != null && value.min() >= 0 && value.min() <= MAX_DEPTH &&
			value.max() >= value.min() && value.max() <= MAX_DEPTH;
	}

	@Override
	public String serialize(ConfigValueRange<Integer> value) {
		return value.min() + ".." + value.max();
	}

	@Override
	public IDeserializeResult<ConfigValueRange<Integer>> deserialize(String string) {
		String[] parts = string.trim().split("\\.\\.", -1);
		if (parts.length == 2) {
			try {
				int min = Integer.parseInt(parts[0].trim());
				int max = Integer.parseInt(parts[1].trim());
				ConfigValueRange<Integer> value = new ConfigValueRange<>(min, max);
				if (isValid(value)) {
					return IDeserializeResult.success(value);
				}
			} catch (NumberFormatException ignored) {
				// Return the same useful format hint for malformed numbers and ranges.
			}
		}
		return IDeserializeResult.failure(getValidValuesDescription());
	}

	@Override
	public String getValidValuesDescription() {
		return "Enter min..max (0 to 10).";
	}

	@Override
	public Component getLocalizedValueName(String localizationKey, ConfigValueRange<Integer> value) {
		return Component.translatableWithFallback("mezz_config.config.screen.range.value", "%s – %s", value.min(), value.max());
	}
}
