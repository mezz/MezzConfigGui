package net.mezzdev.config.gui;

import net.mezzdev.config.api.sorting.ISortingConfig;
import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
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
import net.mezzdev.config.gui.api.ISortingConfigGuiBuilder;
import net.mezzdev.config.gui.api.ISortableConfigValueFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

final class SortableConfigValueFactory implements ISortableConfigValueFactory {
	static final SortableConfigValueFactory INSTANCE = new SortableConfigValueFactory();
	private static final Logger LOGGER = LogManager.getLogger();
	private static final ConfigValueApplyMode DEFAULT_APPLY_MODE = ConfigValueApplyMode.ON_APPLY;

	private SortableConfigValueFactory() {

	}

	<T> SortingConfigGuiBuilder<T> builder(
		String name,
		String localizationKey,
		ISortingConfig<T> sortingConfig,
		Collection<T> values,
		IConfigValueSerializer<T> valueSerializer
	) {
		Objects.requireNonNull(values, "values");
		List<T> valuesCopy = List.copyOf(values);
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(localizationKey, "localizationKey");
		Objects.requireNonNull(sortingConfig, "sortingConfig");
		Objects.requireNonNull(valueSerializer, "valueSerializer");
		return new SortingConfigGuiBuilder<>(name, localizationKey, sortingConfig, valuesCopy, valueSerializer);
	}

	SortingConfigGuiBuilder<String> stringBuilder(
		String name,
		String localizationKey,
		ISortingConfig<String> sortingConfig,
		Collection<String> values
	) {
		Objects.requireNonNull(values, "values");
		List<String> valuesCopy = List.copyOf(values);
		return builder(name, localizationKey, sortingConfig, valuesCopy, StringValueSerializer.INSTANCE);
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
		SortingConfigGuiBuilder<T> builder = builder(name, localizationKey, sortingConfig, values, valueSerializer);
		builder.setApplyMode(applyMode);
		return builder.build();
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
		SortingConfigGuiBuilder<String> builder = stringBuilder(name, localizationKey, sortingConfig, values);
		builder.setValueNames(valueNames);
		builder.setValueDescriptions(valueDescriptions);
		builder.setValueIcons(valueIcons);
		builder.setApplyMode(applyMode);
		return builder.build();
	}

	private static <T> IConfigScreenValue<List<T>> create(
		String name,
		String localizationKey,
		ISortingConfig<T> sortingConfig,
		Collection<T> values,
		IConfigValueSerializer<T> valueSerializer,
		RuntimeValueDisplay<T> display,
		ConfigValueApplyMode applyMode,
		ConfigValueRestartRequirement restartRequirement
	) {
		List<T> valuesCopy = List.copyOf(values);
		List<T> defaultValues = List.copyOf(sortingConfig.getDefaultSortedValues(valuesCopy));
		IConfigListValueEditorSerializer<T> listSerializer = new SortableListSerializer<>(
			defaultValues,
			sortingConfig.allowsRemovingValues(),
			valueSerializer,
			display
		);
		return new SortableConfigValue<>(
			name,
			localizationKey,
			sortingConfig,
			defaultValues,
			listSerializer,
			applyMode,
			restartRequirement
		);
	}

	static final class SortingConfigGuiBuilder<T> implements ISortingConfigGuiBuilder<T> {
		private final String name;
		private final String localizationKey;
		private final ISortingConfig<T> sortingConfig;
		private final List<T> values;
		private final IConfigValueSerializer<T> valueSerializer;
		private Function<T, Optional<Component>> valueNameFactory = value -> Optional.empty();
		private Function<T, Optional<Component>> valueDescriptionFactory = value -> Optional.empty();
		private Function<T, Optional<IConfigValueIcon>> valueIconFactory = value -> Optional.empty();
		private ConfigValueApplyMode applyMode = DEFAULT_APPLY_MODE;
		private ConfigValueRestartRequirement restartRequirement = ConfigValueRestartRequirement.NONE;

		private SortingConfigGuiBuilder(
			String name,
			String localizationKey,
			ISortingConfig<T> sortingConfig,
			List<T> values,
			IConfigValueSerializer<T> valueSerializer
		) {
			this.name = name;
			this.localizationKey = localizationKey;
			this.sortingConfig = sortingConfig;
			this.values = List.copyOf(values);
			this.valueSerializer = valueSerializer;
		}

		@Override
		public ISortingConfigGuiBuilder<T> setApplyMode(ConfigValueApplyMode applyMode) {
			this.applyMode = Objects.requireNonNull(applyMode, "applyMode");
			return this;
		}

		@Override
		public ISortingConfigGuiBuilder<T> setRestartRequirement(ConfigValueRestartRequirement restartRequirement) {
			this.restartRequirement = Objects.requireNonNull(restartRequirement, "restartRequirement");
			return this;
		}

		@Override
		public ISortingConfigGuiBuilder<T> setValueNames(Map<T, Component> valueNames) {
			Map<T, Component> valueNamesCopy = Map.copyOf(Objects.requireNonNull(valueNames, "valueNames"));
			this.valueNameFactory = value -> Optional.ofNullable(valueNamesCopy.get(value));
			return this;
		}

		@Override
		public ISortingConfigGuiBuilder<T> setValueName(Function<T, Component> valueNameFactory) {
			Function<T, Component> checkedFactory = Objects.requireNonNull(valueNameFactory, "valueNameFactory");
			this.valueNameFactory = value -> Optional.of(Objects.requireNonNull(checkedFactory.apply(value), "valueName"));
			return this;
		}

		@Override
		public ISortingConfigGuiBuilder<T> setValueDescriptions(Map<T, Component> valueDescriptions) {
			Map<T, Component> valueDescriptionsCopy = Map.copyOf(Objects.requireNonNull(valueDescriptions, "valueDescriptions"));
			this.valueDescriptionFactory = value -> Optional.ofNullable(valueDescriptionsCopy.get(value));
			return this;
		}

		@Override
		public ISortingConfigGuiBuilder<T> setValueDescription(Function<T, Optional<Component>> valueDescriptionFactory) {
			Function<T, Optional<Component>> checkedFactory = Objects.requireNonNull(valueDescriptionFactory, "valueDescriptionFactory");
			this.valueDescriptionFactory = value -> Objects.requireNonNull(checkedFactory.apply(value), "valueDescription");
			return this;
		}

		@Override
		public ISortingConfigGuiBuilder<T> setValueIcons(Map<T, IConfigValueIcon> valueIcons) {
			Map<T, IConfigValueIcon> valueIconsCopy = Map.copyOf(Objects.requireNonNull(valueIcons, "valueIcons"));
			this.valueIconFactory = value -> Optional.ofNullable(valueIconsCopy.get(value));
			return this;
		}

		@Override
		public ISortingConfigGuiBuilder<T> setValueIcon(Function<T, Optional<IConfigValueIcon>> valueIconFactory) {
			Function<T, Optional<IConfigValueIcon>> checkedFactory = Objects.requireNonNull(valueIconFactory, "valueIconFactory");
			this.valueIconFactory = value -> Objects.requireNonNull(checkedFactory.apply(value), "valueIcon");
			return this;
		}

		IConfigScreenValue<List<T>> build() {
			RuntimeValueDisplay<T> display = new RuntimeValueDisplay<>(
				valueNameFactory,
				valueDescriptionFactory,
				valueIconFactory
			);
			return create(
				name,
				localizationKey,
				sortingConfig,
				values,
				valueSerializer,
				display,
				applyMode,
				restartRequirement
			);
		}
	}

	private static final class SortableConfigValue<T> implements IConfigScreenValue<List<T>>, IConfigLocalizedValue {
		private final String name;
		private final String localizationKey;
		private final ISortingConfig<T> sortingConfig;
		private final List<T> values;
		private final IConfigListValueEditorSerializer<T> serializer;
		private final ConfigValueApplyMode applyMode;
		private final ConfigValueRestartRequirement restartRequirement;
		private final List<Consumer<List<T>>> listeners = new ArrayList<>();
		@Nullable
		private List<T> lastNotifiedValue;
		@Nullable
		private Runnable removeSortingConfigListener;

		private SortableConfigValue(
			String name,
			String localizationKey,
			ISortingConfig<T> sortingConfig,
			List<T> values,
			IConfigListValueEditorSerializer<T> serializer,
			ConfigValueApplyMode applyMode,
			ConfigValueRestartRequirement restartRequirement
		) {
			this.name = name;
			this.localizationKey = localizationKey;
			this.sortingConfig = sortingConfig;
			this.values = List.copyOf(values);
			this.serializer = serializer;
			this.applyMode = applyMode;
			this.restartRequirement = restartRequirement;
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
			return List.copyOf(sortingConfig.getSortedValues(values));
		}

		@Override
		public List<T> getDefaultValue() {
			return values;
		}

		@Override
		public boolean set(List<T> value) {
			if (value == null || !serializer.isValid(value)) {
				throw new IllegalArgumentException(
					"Invalid sortable config value '%s'. %s".formatted(value, serializer.getValidValuesDescription())
				);
			}
			List<T> valueCopy = List.copyOf(value);
			if (getValue().equals(valueCopy)) {
				return false;
			}
			return sortingConfig.setSortedValues(values, valueCopy);
		}

		@Override
		public Runnable addListener(Consumer<List<T>> listener) {
			Consumer<List<T>> checkedListener = Objects.requireNonNull(listener, "listener");
			synchronized (this) {
				if (listeners.isEmpty()) {
					lastNotifiedValue = getValue();
					removeSortingConfigListener = Objects.requireNonNull(
						sortingConfig.addChangeListener(this::onSortingConfigChanged),
						"sorting config listener removal callback"
					);
				}
				listeners.add(checkedListener);
			}
			return () -> removeListener(checkedListener);
		}

		private void removeListener(Consumer<List<T>> listener) {
			@Nullable
			Runnable removeSortingConfigListener = null;
			synchronized (this) {
				listeners.remove(listener);
				if (listeners.isEmpty()) {
					lastNotifiedValue = null;
					removeSortingConfigListener = this.removeSortingConfigListener;
					this.removeSortingConfigListener = null;
				}
			}
			if (removeSortingConfigListener != null) {
				removeSortingConfigListener.run();
			}
		}

		private void onSortingConfigChanged() {
			List<T> newValue = getValue();
			List<Consumer<List<T>>> listeners;
			synchronized (this) {
				if (this.listeners.isEmpty() || Objects.equals(lastNotifiedValue, newValue)) {
					return;
				}
				lastNotifiedValue = newValue;
				listeners = List.copyOf(this.listeners);
			}
			for (Consumer<List<T>> listener : listeners) {
				try {
					listener.accept(newValue);
				} catch (RuntimeException exception) {
					LOGGER.error("Sortable config value listener failed for {}.", name, exception);
				}
			}
		}

		@Override
		public ConfigValueApplyMode getApplyMode() {
			return applyMode;
		}

		@Override
		public ConfigValueRestartRequirement getRestartRequirement() {
			return restartRequirement;
		}

		@Override
		public IConfigListValueEditorSerializer<T> getSerializer() {
			return serializer;
		}
	}

	private record RuntimeValueDisplay<T>(
		Function<T, Optional<Component>> valueNameFactory,
		Function<T, Optional<Component>> valueDescriptionFactory,
		Function<T, Optional<IConfigValueIcon>> valueIconFactory
	) {
		private Optional<Component> getValueName(T value) {
			return Objects.requireNonNull(valueNameFactory.apply(value), "valueName");
		}

		private Optional<Component> getValueDescription(T value) {
			return Objects.requireNonNull(valueDescriptionFactory.apply(value), "valueDescription");
		}

		private Optional<IConfigValueIcon> getValueIcon(T value) {
			return Objects.requireNonNull(valueIconFactory.apply(value), "valueIcon");
		}
	}

	private static final class SortableListSerializer<T> implements IConfigListValueEditorSerializer<T>, IConfigListValueEditorOptions {
		private final RuntimeValueSerializer<T> valueSerializer;
		private final boolean allowsRemovingValues;

		private SortableListSerializer(
			List<T> validValues,
			boolean allowsRemovingValues,
			IConfigValueSerializer<T> valueSerializer,
			RuntimeValueDisplay<T> display
		) {
			this.valueSerializer = new RuntimeValueSerializer<>(validValues, valueSerializer, display);
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
					errors.addAll(result.getDiagnostics());
				})
				.toList();

			if (results.stream().distinct().count() != results.size()) {
				errors.add("List values must not contain duplicates.");
			}

			if (errors.isEmpty()) {
				return IDeserializeResult.success(results);
			}
			if (results.isEmpty()) {
				return IDeserializeResult.failure(errors);
			}
			return IDeserializeResult.partialSuccess(results, errors);
		}

		@Override
		public boolean isValid(List<T> value) {
			return value.stream().allMatch(valueSerializer::isValid) &&
				value.stream().distinct().count() == value.size();
		}

		@Override
		public Optional<List<List<T>>> getAllValidValues() {
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
		private final RuntimeValueDisplay<T> display;

		private RuntimeValueSerializer(
			List<T> validValues,
			IConfigValueSerializer<T> valueSerializer,
			RuntimeValueDisplay<T> display
		) {
			this.validValues = List.copyOf(validValues);
			this.valueSerializer = valueSerializer;
			this.display = display;
		}

		@Override
		public String serialize(T value) {
			return valueSerializer.serialize(value);
		}

		@Override
		public IDeserializeResult<T> deserialize(String string) {
			IDeserializeResult<T> result = valueSerializer.deserialize(string);
			Optional<T> value = result.getResult();
			if (value.isPresent() && !isRuntimeValue(value.get())) {
				List<String> errors = new ArrayList<>(result.getDiagnostics());
				errors.add("Unknown value '%s'. Must be %s.".formatted(valueSerializer.serialize(value.get()), getValidValuesDescription()));
				return IDeserializeResult.failure(errors);
			}
			return result;
		}

		@Override
		public boolean isValid(T value) {
			return valueSerializer.isValid(value) && isRuntimeValue(value);
		}

		@Override
		public Optional<List<T>> getAllValidValues() {
			return Optional.of(validValues);
		}

		@Override
		public Component getLocalizedValueName(String configValueLocalizationKey, T value) {
			Optional<Component> providedName = display.getValueName(value);
			if (providedName.isPresent()) {
				return providedName.get();
			}
			return ConfigValueLocalization.getValueName(valueSerializer, configValueLocalizationKey, value);
		}

		@Override
		public Optional<Component> getLocalizedValueDescription(String configValueLocalizationKey, T value) {
			Optional<Component> providedDescription = display.getValueDescription(value);
			if (providedDescription.isPresent()) {
				return providedDescription;
			}
			return ConfigValueLocalization.getValueDescription(valueSerializer, configValueLocalizationKey, value);
		}

		@Override
		public Optional<IConfigValueIcon> getIcon(T value) {
			Optional<IConfigValueIcon> providedIcon = display.getValueIcon(value);
			if (providedIcon.isPresent()) {
				return providedIcon;
			}
			if (valueSerializer instanceof IConfigValueIconProvider<?> iconProvider) {
				@SuppressWarnings("unchecked")
				IConfigValueIconProvider<T> typedIconProvider = (IConfigValueIconProvider<T>) iconProvider;
				return typedIconProvider.getIcon(value);
			}
			return Optional.empty();
		}

		@Override
		public String getValidValuesDescription() {
			if (validValues.isEmpty()) {
				return "no values are currently available";
			}
			return "one of: " + validValues.stream()
				.map(valueSerializer::serialize)
				.collect(Collectors.joining(", "));
		}

		private boolean isRuntimeValue(T value) {
			return validValues.contains(value);
		}
	}

	private enum StringValueSerializer implements IConfigValueSerializer<String> {
		INSTANCE;

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
		public Optional<List<String>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public String getValidValuesDescription() {
			return "a non-blank value";
		}
	}

}
