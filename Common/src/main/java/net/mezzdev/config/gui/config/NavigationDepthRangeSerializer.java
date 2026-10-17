package net.mezzdev.config.gui.config;

import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import org.jspecify.annotations.Nullable;

/** Serializes the finite minimum and maximum sidebar depths. */
final class NavigationDepthRangeSerializer implements IConfigValueSerializer<ConfigValueRange<Integer>> {
	static final NavigationDepthRangeSerializer INSTANCE = new NavigationDepthRangeSerializer();
	static final int MAX_DEPTH = 10;

	private NavigationDepthRangeSerializer() {
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

}
