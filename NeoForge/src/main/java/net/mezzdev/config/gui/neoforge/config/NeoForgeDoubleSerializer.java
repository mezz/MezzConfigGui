package net.mezzdev.config.gui.neoforge.config;

import net.neoforged.neoforge.common.ModConfigSpec;

final class NeoForgeDoubleSerializer extends NeoForgeTextSerializer<Double> {
	public NeoForgeDoubleSerializer(ModConfigSpec.ValueSpec valueSpec, double min, double max) {
		super(valueSpec, getValidValuesDescription(min, max));
	}

	private static String getValidValuesDescription(double min, double max) {
		if (min == -Double.MAX_VALUE && max == Double.MAX_VALUE) {
			return "Any finite decimal number";
		}
		if (max == Double.MAX_VALUE) {
			return "Any finite decimal number greater than or equal to %s".formatted(min);
		}
		return "A finite decimal number in the range [%s, %s] (inclusive)".formatted(min, max);
	}

	@Override
	public String serialize(Double value) {
		return value.toString();
	}

	@Override
	public NeoForgeDeserializeResult<Double> deserialize(String string) {
		string = string.trim();
		try {
			double value = Double.parseDouble(string);
			if (!isValid(value)) {
				return new NeoForgeDeserializeResult<>(null, "Invalid decimal number. Must be: " + getValidValuesDescription());
			}
			return new NeoForgeDeserializeResult<>(value);
		} catch (NumberFormatException e) {
			return new NeoForgeDeserializeResult<>(null, "Unable to parse decimal number: '%s' with error:\n%s".formatted(string, e.getMessage()));
		}
	}

	@Override
	public boolean isValid(Double value) {
		return value != null && Double.isFinite(value) && super.isValid(value);
	}
}
