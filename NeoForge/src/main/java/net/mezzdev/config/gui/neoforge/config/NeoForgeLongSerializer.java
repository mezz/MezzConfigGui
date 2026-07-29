package net.mezzdev.config.gui.neoforge.config;

import net.neoforged.neoforge.common.ModConfigSpec;

final class NeoForgeLongSerializer extends NeoForgeTextSerializer<Long> {
	public NeoForgeLongSerializer(ModConfigSpec.ValueSpec valueSpec, long min, long max) {
		super(valueSpec, getValidValuesDescription(min, max));
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
	public NeoForgeDeserializeResult<Long> deserialize(String string) {
		string = string.trim();
		try {
			long value = Long.parseLong(string);
			if (!isValid(value)) {
				return new NeoForgeDeserializeResult<>(null, "Invalid long integer. Must be: " + getValidValuesDescription());
			}
			return new NeoForgeDeserializeResult<>(value);
		} catch (NumberFormatException e) {
			return new NeoForgeDeserializeResult<>(null, "Unable to parse long integer: '%s' with error:\n%s".formatted(string, e.getMessage()));
		}
	}
}
