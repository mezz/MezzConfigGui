package net.mezzdev.config.gui;

import net.mezzdev.config.api.sorting.ISortingConfig;
import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigListValueEditorOptions;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueIcon;
import net.mezzdev.config.gui.api.IConfigValueIconProvider;
import net.mezzdev.config.gui.api.IConfigValueLocalizationProvider;
import net.mezzdev.config.gui.api.ISortableConfigValueFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

final class SortableConfigValueFactory implements ISortableConfigValueFactory {
	static final SortableConfigValueFactory INSTANCE = new SortableConfigValueFactory();
	private static final ConfigValueApplyMode DEFAULT_APPLY_MODE = ConfigValueApplyMode.ON_APPLY;

	private SortableConfigValueFactory() {

	}

	@Override
	public <T> IConfigScreenValue<List<T>> create(
		String name,
		String localizationKey,
		ISortingConfig<T> sortingConfig,
		Collection<T> values,
		IConfigValueSerializer<T> valueSerializer
	) {
		return create(name, localizationKey, sortingConfig, values, valueSerializer, DEFAULT_APPLY_MODE);
	}

	@Override
	public <T> IConfigScreenValue<List<T>> create(
		String name,
		String localizationKey,
		ISortingConfig<T> sortingConfig,
		Collection<T> values,
		IConfigValueSerializer<T> valueSerializer,
		ConfigValueApplyMode applyMode
	) {
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(localizationKey, "localizationKey");
		Objects.requireNonNull(sortingConfig, "sortingConfig");
		Objects.requireNonNull(values, "values");
		Objects.requireNonNull(valueSerializer, "valueSerializer");
		Objects.requireNonNull(applyMode, "applyMode");

		List<T> valuesCopy = List.copyOf(values);
		List<T> defaultValues = List.copyOf(sortingConfig.getDefaultSortedValues(valuesCopy));
		IConfigListValueEditorSerializer<T> listSerializer = new SortableListSerializer<>(
			defaultValues,
			sortingConfig.allowsRemovingValues(),
			valueSerializer
		);
		return new SortableConfigValue<>(
			name,
			localizationKey,
			sortingConfig,
			defaultValues,
			listSerializer,
			applyMode
		);
	}

	@Override
	public IConfigScreenValue<List<String>> createStringList(
		String name,
		String localizationKey,
		ISortingConfig<String> sortingConfig,
		Collection<String> values,
		Map<String, Component> valueNames,
		Map<String, Component> valueDescriptions,
		Map<String, IConfigValueIcon> valueIcons
	) {
		return createStringList(
			name,
			localizationKey,
			sortingConfig,
			values,
			valueNames,
			valueDescriptions,
			valueIcons,
			DEFAULT_APPLY_MODE
		);
	}

	@Override
	public IConfigScreenValue<List<String>> createStringList(
		String name,
		String localizationKey,
		ISortingConfig<String> sortingConfig,
		Collection<String> values,
		Map<String, Component> valueNames,
		Map<String, Component> valueDescriptions,
		Map<String, IConfigValueIcon> valueIcons,
		ConfigValueApplyMode applyMode
	) {
		Objects.requireNonNull(values, "values");
		IConfigValueSerializer<String> valueSerializer = new StringRuntimeValueSerializer(
			values,
			valueNames,
			valueDescriptions,
			valueIcons
		);
		return create(name, localizationKey, sortingConfig, values, valueSerializer, applyMode);
	}

	private static final class SortableConfigValue<T> implements IConfigScreenValue<List<T>>, IConfigLocalizedValue {
		private final String name;
		private final String localizationKey;
		private final ISortingConfig<T> sortingConfig;
		private final List<T> values;
		private final IConfigListValueEditorSerializer<T> serializer;
		private final ConfigValueApplyMode applyMode;
		private final List<Consumer<List<T>>> listeners = new ArrayList<>();

		private SortableConfigValue(
			String name,
			String localizationKey,
			ISortingConfig<T> sortingConfig,
			List<T> values,
			IConfigListValueEditorSerializer<T> serializer,
			ConfigValueApplyMode applyMode
		) {
			this.name = name;
			this.localizationKey = localizationKey;
			this.sortingConfig = sortingConfig;
			this.values = List.copyOf(values);
			this.serializer = serializer;
			this.applyMode = applyMode;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getLocalizationKey() {
			return localizationKey;
		}

		@Override
		public Component getLocalizedName() {
			return Component.translatable(localizationKey);
		}

		@Override
		public Component getLocalizedDescription() {
			return Component.translatable(localizationKey + ".description");
		}

		@Override
		public List<T> getValue() {
			return sortingConfig.getSortedValues(values);
		}

		@Override
		public List<T> getDefaultValue() {
			return values;
		}

		@Override
		public boolean set(List<T> value) {
			List<T> valueCopy = List.copyOf(value);
			if (!serializer.isValid(valueCopy)) {
				return false;
			}
			if (sortingConfig.setSortedValues(valueCopy)) {
				for (Consumer<List<T>> listener : listeners) {
					listener.accept(valueCopy);
				}
				return true;
			}
			return false;
		}

		@Override
		public void addListener(Consumer<List<T>> listener) {
			listeners.add(Objects.requireNonNull(listener, "listener"));
		}

		@Override
		public ConfigValueApplyMode getApplyMode() {
			return applyMode;
		}

		@Override
		public IConfigListValueEditorSerializer<T> getSerializer() {
			return serializer;
		}
	}

	private static final class SortableListSerializer<T> implements IConfigListValueEditorSerializer<T>, IConfigListValueEditorOptions {
		private final RuntimeValueSerializer<T> valueSerializer;
		private final boolean allowsRemovingValues;

		private SortableListSerializer(
			List<T> validValues,
			boolean allowsRemovingValues,
			IConfigValueSerializer<T> valueSerializer
		) {
			this.valueSerializer = new RuntimeValueSerializer<>(validValues, valueSerializer);
			this.allowsRemovingValues = allowsRemovingValues;
		}

		@Override
		public String serialize(List<T> values) {
			return values.stream()
				.map(valueSerializer::serialize)
				.collect(Collectors.joining(", "));
		}

		@Override
		public IDeserializeResult<List<T>> deserialize(String string) {
			String checkedString = string.trim();
			if (checkedString.startsWith("[")) {
				if (!checkedString.endsWith("]")) {
					String errorMessage = """
						No closing brace found.
						List must have no braces, or be wrapped in [ and ].""";
					return IDeserializeResult.failure(errorMessage);
				}
				checkedString = checkedString.substring(1, checkedString.length() - 1);
			}
			String[] split = checkedString.split(",");

			List<String> errors = new ArrayList<>();
			List<T> results = Arrays.stream(split)
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.map(valueSerializer::deserialize)
				.<T>mapMulti((result, consumer) -> {
					result.getResult().ifPresent(consumer);
					errors.addAll(result.getErrors());
				})
				.toList();

			if (results.stream().distinct().count() != results.size()) {
				errors.add("List values must not contain duplicates.");
			}

			return IDeserializeResult.of(results, errors);
		}

		@Override
		public boolean isValid(List<T> value) {
			return value.stream().allMatch(valueSerializer::isValid) &&
				value.stream().distinct().count() == value.size();
		}

		@Override
		public Optional<Collection<List<T>>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public Component getLocalizedValueName(String configValueLocalizationKey, List<T> values) {
			if (values.isEmpty()) {
				return Component.translatable("mezz_config.config.value.list.empty");
			}
			MutableComponent result = Component.empty();
			for (int i = 0; i < values.size(); i++) {
				if (i > 0) {
					result.append(Component.literal(", "));
				}
				result.append(ConfigValueLocalization.getValueName(valueSerializer, configValueLocalizationKey, values.get(i)));
			}
			return result;
		}

		@Override
		public String getValidValuesDescription() {
			return "A comma-separated list containing values of:\n%s".formatted(valueSerializer.getValidValuesDescription());
		}

		@Override
		public IConfigValueSerializer<T> getElementSerializer() {
			return valueSerializer;
		}

		@Override
		public ConfigValueEditorType<List<T>> getEditorType() {
			return ConfigValueEditorTypes.getList();
		}

		@Override
		public boolean allowsRemovingValues() {
			return allowsRemovingValues;
		}
	}

	private static final class RuntimeValueSerializer<T> implements IConfigValueSerializer<T>, IConfigValueLocalizationProvider<T>, IConfigValueIconProvider<T> {
		private final List<T> validValues;
		private final IConfigValueSerializer<T> valueSerializer;

		private RuntimeValueSerializer(List<T> validValues, IConfigValueSerializer<T> valueSerializer) {
			this.validValues = List.copyOf(validValues);
			this.valueSerializer = valueSerializer;
		}

		@Override
		public String serialize(T value) {
			return valueSerializer.serialize(value);
		}

		@Override
		public IDeserializeResult<T> deserialize(String string) {
			return valueSerializer.deserialize(string);
		}

		@Override
		public boolean isValid(T value) {
			return valueSerializer.isValid(value);
		}

		@Override
		public Optional<Collection<T>> getAllValidValues() {
			return Optional.of(validValues);
		}

		@Override
		public Component getLocalizedValueName(String configValueLocalizationKey, T value) {
			return ConfigValueLocalization.getValueName(valueSerializer, configValueLocalizationKey, value);
		}

		@Override
		public Optional<Component> getLocalizedValueDescription(String configValueLocalizationKey, T value) {
			return ConfigValueLocalization.getValueDescription(valueSerializer, configValueLocalizationKey, value);
		}

		@Override
		public Optional<IConfigValueIcon> getIcon(T value) {
			if (valueSerializer instanceof IConfigValueIconProvider<?> iconProvider) {
				@SuppressWarnings("unchecked")
				IConfigValueIconProvider<T> typedIconProvider = (IConfigValueIconProvider<T>) iconProvider;
				return typedIconProvider.getIcon(value);
			}
			return Optional.empty();
		}

		@Override
		public String getValidValuesDescription() {
			return valueSerializer.getValidValuesDescription();
		}
	}

	private static final class StringRuntimeValueSerializer implements IConfigValueSerializer<String>, IConfigValueLocalizationProvider<String>, IConfigValueIconProvider<String> {
		private final List<String> validValues;
		private final Map<String, Component> valueNames;
		private final Map<String, Component> valueDescriptions;
		private final Map<String, IConfigValueIcon> valueIcons;

		private StringRuntimeValueSerializer(
			Collection<String> validValues,
			Map<String, Component> valueNames,
			Map<String, Component> valueDescriptions,
			Map<String, IConfigValueIcon> valueIcons
		) {
			this.validValues = List.copyOf(validValues);
			this.valueNames = Map.copyOf(valueNames);
			this.valueDescriptions = Map.copyOf(valueDescriptions);
			this.valueIcons = Map.copyOf(valueIcons);
		}

		@Override
		public String serialize(String value) {
			return value;
		}

		@Override
		public IDeserializeResult<String> deserialize(String string) {
			if (string.isBlank()) {
				return IDeserializeResult.failure("Value must not be blank.");
			}
			return IDeserializeResult.success(string);
		}

		@Override
		public boolean isValid(String value) {
			return !value.isBlank();
		}

		@Override
		public Optional<Collection<String>> getAllValidValues() {
			return Optional.of(validValues);
		}

		@Override
		public Component getLocalizedValueName(String configValueLocalizationKey, String value) {
			return valueNames.getOrDefault(value, Component.literal(value));
		}

		@Override
		public Optional<Component> getLocalizedValueDescription(String configValueLocalizationKey, String value) {
			return Optional.ofNullable(valueDescriptions.get(value));
		}

		@Override
		public Optional<IConfigValueIcon> getIcon(String value) {
			return Optional.ofNullable(valueIcons.get(value));
		}

		@Override
		public String getValidValuesDescription() {
			return "a non-blank value";
		}
	}

}
