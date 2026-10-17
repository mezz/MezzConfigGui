package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

abstract class NeoForgeTextSerializer<T> implements IConfigValueSerializer<T> {
	private final ModConfigSpec.ValueSpec valueSpec;
	private final String validValuesDescription;

	protected NeoForgeTextSerializer(ModConfigSpec.ValueSpec valueSpec, String validValuesDescription) {
		this.valueSpec = valueSpec;
		this.validValuesDescription = validValuesDescription;
	}

	@Override
	public boolean isValid(@Nullable T value) {
		if (value == null) {
			return false;
		}
		try {
			return valueSpec.test(value);
		} catch (RuntimeException ignored) {
			return false;
		}
	}

	@Override
	public Optional<List<T>> getAllValidValues() {
		return Optional.empty();
	}

	@Override
	public String getValidValuesDescription() {
		return validValuesDescription;
	}

}
