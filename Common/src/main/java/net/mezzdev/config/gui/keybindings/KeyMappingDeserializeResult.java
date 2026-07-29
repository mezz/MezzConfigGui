package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.api.value.IConfigValueSerializer;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;

final class KeyMappingDeserializeResult<T> implements IConfigValueSerializer.IDeserializeResult<T> {
	private final @Nullable T result;
	private final List<String> errors;

	public KeyMappingDeserializeResult(T result) {
		this(result, List.of());
	}

	public KeyMappingDeserializeResult(@Nullable T result, String error) {
		this(result, List.of(error));
	}

	public KeyMappingDeserializeResult(@Nullable T result, List<String> errors) {
		this.result = result;
		this.errors = List.copyOf(errors);
	}

	@Override
	public Optional<T> getResult() {
		return Optional.ofNullable(result);
	}

	@Override
	@Unmodifiable
	public List<String> getErrors() {
		return errors;
	}
}
