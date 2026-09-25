package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.IConfigListValueSerializer;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

final class NeoForgeListSerializer<T> implements IConfigListValueSerializer<T> {
	private final ModConfigSpec.ValueSpec valueSpec;
	private final IConfigValueSerializer<T> elementSerializer;

	public NeoForgeListSerializer(
		ModConfigSpec.ValueSpec valueSpec,
		IConfigValueSerializer<T> elementSerializer
	) {
		this.valueSpec = valueSpec;
		this.elementSerializer = elementSerializer;
	}

	@Override
	public IConfigValueSerializer<T> getElementSerializer() {
		return elementSerializer;
	}

	public List<T> normalize(List<?> values) {
		// TOML loads enum names as strings and small long values as integers.
		return values.stream()
			.map(value -> {
				if (value instanceof Enum<?> enumValue) {
					return enumValue.name();
				}
				return String.valueOf(value);
			})
			.map(value -> elementSerializer.deserialize(value).getResult()
				.orElseThrow(() -> new IllegalArgumentException("Invalid native list element: " + value)))
			.toList();
	}

	@Override
	public String serialize(List<T> value) {
		return value.stream()
			.map(elementSerializer::serialize)
			.collect(Collectors.joining(", "));
	}

	@Override
	public IDeserializeResult<List<T>> deserialize(String string) {
		string = string.trim();
		if (string.startsWith("[")) {
			if (!string.endsWith("]")) {
				String errorMessage = """
					No closing brace found.
					List must have no braces, or be wrapped in [ and ].""";
				return IDeserializeResult.failure(errorMessage);
			}
			string = string.substring(1, string.length() - 1);
		}
		String[] split = string.split(",");

		List<String> errors = new ArrayList<>();
		List<T> results = Arrays.stream(split)
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.map(elementSerializer::deserialize)
			.<T>mapMulti((result, consumer) -> {
				result.getResult().ifPresent(consumer);
				errors.addAll(result.getDiagnostics());
			})
			.toList();

		if (errors.isEmpty()) {
			return IDeserializeResult.success(results);
		}
		if (results.isEmpty()) {
			return IDeserializeResult.failure(errors);
		}
		return IDeserializeResult.partialSuccess(results, errors);
	}

	@Override
	public boolean isValid(@Nullable List<@Nullable T> value) {
		if (value == null) {
			return false;
		}
		if (valueSpec instanceof ModConfigSpec.ListValueSpec listValueSpec && !listValueSpec.getSizeRange().test(value.size())) {
			return false;
		}
		if (!value.stream().allMatch(this::isValidElement)) {
			return false;
		}
		return isValidList(value);
	}

	private boolean isValidElement(@Nullable T value) {
		if (value == null || !elementSerializer.isValid(value)) {
			return false;
		}
		if (valueSpec instanceof ModConfigSpec.ListValueSpec listValueSpec) {
			try {
				return listValueSpec.testElement(value);
			} catch (RuntimeException ignored) {
				return false;
			}
		}
		return true;
	}

	private boolean isValidList(List<T> value) {
		try {
			return valueSpec.test(value);
		} catch (RuntimeException ignored) {
			return false;
		}
	}

	@Override
	public Optional<List<List<T>>> getAllValidValues() {
		return Optional.empty();
	}

	@Override
	public String getValidValuesDescription() {
		return "A comma-separated list containing values of:\n%s".formatted(elementSerializer.getValidValuesDescription());
	}
}
