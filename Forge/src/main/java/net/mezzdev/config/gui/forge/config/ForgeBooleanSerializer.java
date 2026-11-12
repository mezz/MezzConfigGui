package net.mezzdev.config.gui.forge.config;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

final class ForgeBooleanSerializer implements IConfigValueSerializer<Boolean> {
	public static final ForgeBooleanSerializer INSTANCE = new ForgeBooleanSerializer();

	private ForgeBooleanSerializer() {

	}

	@Override
	public Class<Boolean> getValueClass() {
		return Boolean.class;
	}

	@Override
	public String serialize(Boolean value) {
		return value.toString();
	}

	@Override
	public IDeserializeResult<Boolean> deserialize(String string) {
		string = string.trim();
		if ("true".equalsIgnoreCase(string)) {
			return IDeserializeResult.success(true);
		}
		if ("false".equalsIgnoreCase(string)) {
			return IDeserializeResult.success(false);
		}
		return IDeserializeResult.failure("string must be 'true' or 'false'");
	}

	@Override
	public boolean isValid(@Nullable Boolean value) {
		return value != null;
	}

	@Override
	public Optional<List<Boolean>> getAllValidValues() {
		return Optional.of(List.of(true, false));
	}

	@Override
	public String getValidValuesDescription() {
		return "[true, false]";
	}

}
