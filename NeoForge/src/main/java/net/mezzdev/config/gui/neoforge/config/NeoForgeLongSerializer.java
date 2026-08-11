package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.ConfigValueRange;
import net.mezzdev.config.api.value.IDeserializeResult;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Optional;

final class NeoForgeLongSerializer extends NeoForgeTextSerializer<Long> {
	private final ConfigValueRange<Long> range;

	public NeoForgeLongSerializer(ModConfigSpec.ValueSpec valueSpec, long min, long max) {
		super(valueSpec, getValidValuesDescription(min, max));
		this.range = new ConfigValueRange<>(min, max);
	}

	@Override
	public Optional<ConfigValueRange<Long>> getRange() {
		return Optional.of(range);
	}

	private static String getValidValuesDescription(long min, long max) {
		if (min == Long.MIN_VALUE && max == Long.MAX_VALUE) {
			return "Any long integer";
		}
		if (max == Long.MAX_VALUE) {
			return "Any long integer greater than or equal to %s".formatted(min);
		}
		return "A long integer in the range [%s, %s] (inclusive)".formatted(min, max);
	}

	@Override
	public String serialize(Long value) {
		return value.toString();
	}

	@Override
	public IDeserializeResult<Long> deserialize(String string) {
		string = string.trim();
		try {
			long value = Long.parseLong(string);
			if (!isValid(value)) {
				return IDeserializeResult.failure("Invalid long integer. Must be: " + getValidValuesDescription());
			}
			return IDeserializeResult.success(value);
		} catch (NumberFormatException e) {
			return IDeserializeResult.failure("Unable to parse long integer: '%s' with error:\n%s".formatted(string, e.getMessage()));
		}
	}
}
