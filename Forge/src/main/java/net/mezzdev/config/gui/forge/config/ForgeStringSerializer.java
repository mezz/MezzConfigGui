package net.mezzdev.config.gui.forge.config;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.minecraftforge.common.ForgeConfigSpec;

final class ForgeStringSerializer extends ForgeTextSerializer<String> {
	public ForgeStringSerializer(ForgeConfigSpec.ValueSpec valueSpec) {
		super(valueSpec, "Text accepted by the Forge config spec");
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
