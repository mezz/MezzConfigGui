package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

final class NeoForgeListSerializer<T> implements IConfigListValueEditorSerializer<T> {
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
				errors.addAll(result.getErrors());
			})
			.toList();

		return IDeserializeResult.of(results, errors);
	}

	@Override
	public boolean isValid(List<T> value) {
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

	private boolean isValidElement(T value) {
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
	public Optional<Collection<List<T>>> getAllValidValues() {
		return Optional.empty();
	}

	@Override
	public Component getLocalizedValueName(String configValueLocalizationKey, List<T> value) {
		if (value.isEmpty()) {
			return Component.translatable("mezz_config.config.value.list.empty");
		}
		return Component.literal(serialize(value));
	}

	@Override
	public String getValidValuesDescription() {
		return "A comma-separated list containing values of:\n%s".formatted(elementSerializer.getValidValuesDescription());
	}

	@Override
	public ConfigValueEditorType<List<T>> getEditorType() {
		return ConfigValueEditorTypes.getList();
	}
}
