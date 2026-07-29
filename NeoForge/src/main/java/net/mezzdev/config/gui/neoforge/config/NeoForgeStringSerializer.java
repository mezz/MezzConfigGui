package net.mezzdev.config.gui.neoforge.config;

import net.neoforged.neoforge.common.ModConfigSpec;

final class NeoForgeStringSerializer extends NeoForgeTextSerializer<String> {
	public NeoForgeStringSerializer(ModConfigSpec.ValueSpec valueSpec) {
		super(valueSpec, "Text accepted by the NeoForge config spec");
	}

	@Override
	public String serialize(String value) {
		return value;
	}

	@Override
	public NeoForgeDeserializeResult<String> deserialize(String string) {
		if (!isValid(string)) {
			return new NeoForgeDeserializeResult<>(null, "Invalid text. Must be: " + getValidValuesDescription());
		}
		return new NeoForgeDeserializeResult<>(string);
	}
}
