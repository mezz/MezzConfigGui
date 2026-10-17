package net.mezzdev.config.gui.forge.config;

import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

abstract class ForgeTextSerializer<T> implements IConfigValueSerializer<T> {
	private final ForgeConfigSpec.ValueSpec valueSpec;
	private final String validValuesDescription;

	protected ForgeTextSerializer(ForgeConfigSpec.ValueSpec valueSpec, String validValuesDescription) {
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
