package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.IDeserializeResult;
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
	public IDeserializeResult<String> deserialize(String string) {
		if (!isValid(string)) {
			return IDeserializeResult.failure("Invalid text. Must be: " + getValidValuesDescription());
		}
		return IDeserializeResult.success(string);
	}
}
