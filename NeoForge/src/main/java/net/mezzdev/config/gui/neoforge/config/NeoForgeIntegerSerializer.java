package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.ConfigValueRange;
import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigValueEditorSerializer;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

final class NeoForgeIntegerSerializer implements IConfigValueEditorSerializer<Integer> {
	private final ConfigValueRange<Integer> range;

	public NeoForgeIntegerSerializer(int min, int max) {
		this.range = new ConfigValueRange<>(min, max);
	}

	@Override
	public Optional<ConfigValueRange<Integer>> getRange() {
		return Optional.of(range);
	}

	@Override
	public String serialize(Integer value) {
		return value.toString();
	}

	@Override
	public IDeserializeResult<Integer> deserialize(String string) {
		string = string.trim();
		try {
			int value = Integer.parseInt(string);
			if (!isValid(value)) {
				return IDeserializeResult.failure("Invalid integer. Must be: " + getValidValuesDescription());
			}
			return IDeserializeResult.success(value);
		} catch (NumberFormatException e) {
			return IDeserializeResult.failure("Unable to parse int: '%s' with error:\n%s".formatted(string, e.getMessage()));
		}
	}

	@Override
	public boolean isValid(Integer value) {
		return value >= range.min() && value <= range.max();
	}

	@Override
	public Optional<Collection<Integer>> getAllValidValues() {
		int min = range.min();
		int max = range.max();
		int count = max - min + 1;
		if (count > 0 && count < 20) {
			List<Integer> values = IntStream.rangeClosed(min, max)
				.boxed()
				.toList();
			return Optional.of(values);
		}
		return Optional.empty();
	}

	@Override
	public Component getLocalizedValueName(String configValueLocalizationKey, Integer value) {
		return Component.literal(value.toString());
	}

	@Override
	public String getValidValuesDescription() {
		int min = range.min();
		int max = range.max();
		if (min == Integer.MIN_VALUE && max == Integer.MAX_VALUE) {
			return "Any integer";
		}
		if (max == Integer.MAX_VALUE) {
			return "Any integer greater than or equal to %s".formatted(min);
		}
		return "An integer in the range [%s, %s] (inclusive)".formatted(min, max);
	}

	@Override
	public ConfigValueEditorType<Integer> getEditorType() {
		return ConfigValueEditorTypes.INTEGER;
	}

}
