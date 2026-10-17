package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

final class NeoForgeEnumSerializer<T extends Enum<T>> implements IConfigValueSerializer<T> {
	private final Class<T> enumClass;
	private final List<T> validValues;

	public NeoForgeEnumSerializer(Class<T> enumClass, Collection<T> validValues) {
		this.enumClass = enumClass;
		this.validValues = List.copyOf(validValues);
	}

	@Override
	public String serialize(T value) {
		return value.name();
	}

	@Override
	public IDeserializeResult<T> deserialize(String string) {
		string = string.trim();
		if (string.startsWith("\"") && string.endsWith("\"")) {
			string = string.substring(1, string.length() - 1);
		}
		try {
			T value = Enum.valueOf(enumClass, string);
			if (!isValid(value)) {
				return IDeserializeResult.failure("Invalid enum name: %s".formatted(string));
			}
			return IDeserializeResult.success(value);
		} catch (IllegalArgumentException e) {
			return IDeserializeResult.failure("Invalid enum name: %s".formatted(e.getMessage()));
		}
	}

	@Override
	public boolean isValid(@Nullable T value) {
		return validValues.contains(value);
	}

	@Override
	public Optional<List<T>> getAllValidValues() {
		return Optional.of(validValues);
	}

	@Override
	public String getValidValuesDescription() {
		String names = validValues.stream()
			.map(Enum::name)
			.collect(Collectors.joining(", "));
		return "[%s]".formatted(names);
	}

}
