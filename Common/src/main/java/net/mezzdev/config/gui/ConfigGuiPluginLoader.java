package net.mezzdev.config.gui;

import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.sorting.ISortingConfig;
import net.mezzdev.config.api.value.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import net.mezzdev.config.gui.api.IConfigScreenBuilder;
import net.mezzdev.config.gui.api.IConfigScreenCategoryBuilder;
import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.mezzdev.config.gui.api.IConfigScreenConfig;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigScreenValueBuilder;
import net.mezzdev.config.gui.api.IConfigValueEditorFactory;
import net.mezzdev.config.gui.api.ISortingConfigGuiBuilder;
import net.mezzdev.config.gui.api.ISortableConfigValueFactory;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.model.ConfigValueChange;
import net.mezzdev.config.gui.keybindings.KeyMappingConfigValues;
import net.mezzdev.config.gui.screenlist.ConfigScreenFactoryEntry;
import net.mezzdev.config.gui.screenlist.ConfigScreenFactoryRegistry;
import net.mezzdev.config.gui.util.ConfigNameUtil;
import net.mezzdev.config.gui.util.ErrorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class ConfigGuiPluginLoader {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final String KEY_MAPPINGS_CATEGORY_NAME = "keyMappings";
	private static final ConfigScreenValueProvider DEFAULT_KEY_MAPPINGS_PROVIDER = lookup -> {
		if (!ConfigGuiOptions.showKeyMappings()) {
			return List.of();
		}
		return KeyMappingConfigValues.createForModId(lookup.modId());
	};

	private ConfigGuiPluginLoader() {

	}

	public static Map<String, IConfigScreenFactory> createScreenFactories(
		Collection<? extends IConfigScreenConfig> configScreens,
		List<? extends IConfigGuiPlugin> plugins
	) {
		return createScreenFactoryRegistry(configScreens, plugins).getFactories();
	}

	public static ConfigScreenFactoryRegistry createScreenFactoryRegistry(
		Collection<? extends IConfigScreenConfig> configScreens,
		List<? extends IConfigGuiPlugin> plugins
	) {
		return createScreenFactoryRegistryFromInternalConfigs(
			configScreens.stream()
				.map(PublicConfigScreenConfig::new)
				.toList(),
			plugins
		);
	}

	static Map<String, IConfigScreenFactory> createScreenFactoriesFromInternalConfigs(
		Collection<? extends ConfigScreenConfig> configScreens,
		List<? extends IConfigGuiPlugin> plugins
	) {
		return createScreenFactoryRegistryFromInternalConfigs(configScreens, plugins).getFactories();
	}

	static ConfigScreenFactoryRegistry createScreenFactoryRegistryFromInternalConfigs(
		Collection<? extends ConfigScreenConfig> configScreens,
		List<? extends IConfigGuiPlugin> plugins
	) {
		return createScreenFactoryRegistryFromInternalConfigs(configScreens, plugins, true);
	}

	static ConfigScreenFactoryRegistry createScreenFactoryRegistryFromInternalConfigs(
		Collection<? extends ConfigScreenConfig> configScreens,
		List<? extends IConfigGuiPlugin> plugins,
		boolean includeAutomaticMezzConfigScreens
	) {
		List<ConfigScreenConfig> allConfigScreens = List.copyOf(configScreens);
		if (includeAutomaticMezzConfigScreens) {
			allConfigScreens = addAutomaticMezzConfigScreens(configScreens);
		}
		Map<String, ConfigGuiRegistration> registrations = createConfigGuiRegistrations(allConfigScreens);
		for (IConfigGuiPlugin plugin : plugins) {
			addPlugin(registrations, plugin);
		}
		ConfigScreenNavigation navigation = new ConfigScreenNavigation();
		Map<String, IConfigScreenFactory> factories = new LinkedHashMap<>();
		List<ConfigScreenFactoryEntry> entries = new ArrayList<>();
		registrations.forEach((modId, registration) -> addFactory(factories, entries, modId, registration, navigation));
		logDiscoverySummary(allConfigScreens, plugins, factories);
		return new ConfigScreenFactoryRegistry(factories, entries, navigation);
	}

	private static List<ConfigScreenConfig> addAutomaticMezzConfigScreens(Collection<? extends ConfigScreenConfig> configScreens) {
		return combineConfigScreens(configScreens, MezzConfigScreenConfigs.getActiveConfigScreens());
	}

	static List<ConfigScreenConfig> combineConfigScreens(
		Collection<? extends ConfigScreenConfig> configScreens,
		Collection<? extends ConfigScreenConfig> additionalConfigScreens
	) {
		List<ConfigScreenConfig> allConfigScreens = new ArrayList<>(configScreens);
		allConfigScreens.addAll(additionalConfigScreens);
		return List.copyOf(allConfigScreens);
	}

	private static void logDiscoverySummary(
		Collection<? extends ConfigScreenConfig> configScreens,
		List<? extends IConfigGuiPlugin> plugins,
		Map<String, IConfigScreenFactory> factories
	) {
		if (ConfigGuiOptions.getDiscoveryLogging() != ConfigGuiOptions.DiscoveryLogging.VERBOSE) {
			return;
		}
		LOGGER.info(
			"Created {} config screen factories from {} internal config screens and {} config GUI plugins.",
			factories.size(),
			configScreens.size(),
			plugins.size()
		);
	}

	private static Map<String, ConfigGuiRegistration> createConfigGuiRegistrations(
		Collection<? extends ConfigScreenConfig> configScreens
	) {
		Map<String, List<ConfigScreenConfig>> configScreensByModId = new LinkedHashMap<>();
		for (ConfigScreenConfig configScreen : configScreens) {
			String modId = configScreen.getModId();
			configScreensByModId.computeIfAbsent(modId, ignored -> new ArrayList<>())
				.add(configScreen);
		}
		Map<String, ConfigGuiRegistration> registrations = new LinkedHashMap<>();
		configScreensByModId.forEach((modId, sources) -> registrations.put(
			modId,
			new ConfigGuiRegistration(modId, new MergedConfigScreenConfig(modId, sources))
		));
		return registrations;
	}

	private static void addPlugin(Map<String, ConfigGuiRegistration> registrations, IConfigGuiPlugin plugin) {
		try {
			String modId = validateModId(plugin.getModId());
			ConfigGuiRegistration registration = registrations.computeIfAbsent(modId, ConfigGuiRegistration::new);
			plugin.register(registration);
		} catch (RuntimeException | LinkageError e) {
			LOGGER.error("Failed to load config GUI plugin: {}", plugin.getClass(), e);
		}
	}

	private static boolean addFactory(Map<String, IConfigScreenFactory> factories, String modId, IConfigScreenFactory factory) {
		@Nullable
		IConfigScreenFactory previous = factories.putIfAbsent(modId, factory);
		if (previous != null) {
			LOGGER.error("Duplicate config GUI plugin for mod id: {}", modId);
			return false;
		}
		return true;
	}

	private static void addFactory(
		Map<String, IConfigScreenFactory> factories,
		List<ConfigScreenFactoryEntry> entries,
		String modId,
		ConfigGuiRegistration registration,
		ConfigScreenNavigation navigation
	) {
		try {
			Optional<ConfigScreenFactoryEntry> entry = registration.createFactoryEntry(navigation);
			entry.ifPresent(configScreenFactoryEntry -> {
				if (addFactory(factories, modId, configScreenFactoryEntry.factory())) {
					entries.add(configScreenFactoryEntry);
				}
			});
		} catch (RuntimeException | LinkageError e) {
			LOGGER.error("Failed to create config screen factory for mod id: {}", modId, e);
		}
	}

	private static String validateModId(@Nullable String modId) {
		String checkedModId = ErrorUtil.checkNotNull(modId, "modId");
		if (checkedModId.isBlank()) {
			throw new IllegalArgumentException("modId must not be blank.");
		}
		return checkedModId;
	}

	private static final class ConfigGuiRegistration implements IConfigGuiRegistration {
		private final String modId;
		private final Map<ConfigValueEditorType<?>, IConfigValueEditorFactory<?>> valueEditorFactories = new LinkedHashMap<>();
		private final List<Consumer<IConfigScreenBuilder>> screenCustomizers = new ArrayList<>();
		@Nullable
		private final ConfigScreenConfig configScreen;

		@Nullable
		private ConfigScreenFactoryConfig config;

		private ConfigGuiRegistration(String modId) {
			this(modId, null);
		}

		private ConfigGuiRegistration(String modId, @Nullable ConfigScreenConfig configScreen) {
			this.modId = modId;
			this.configScreen = configScreen;
		}

		@Override
		public <T> void registerValueEditor(ConfigValueEditorType<T> editorType, IConfigValueEditorFactory<T> editorFactory) {
			ConfigValueEditorType<T> checkedEditorType = ErrorUtil.checkNotNull(editorType, "editorType");
			IConfigValueEditorFactory<T> checkedEditorFactory = ErrorUtil.checkNotNull(editorFactory, "editorFactory");
			IConfigValueEditorFactory<?> previous = valueEditorFactories.putIfAbsent(checkedEditorType, checkedEditorFactory);
			if (previous != null) {
				LOGGER.error("Duplicate config value editor for mod id: {}, editor type: {}", modId, checkedEditorType);
			}
		}

		@Override
		public ISortableConfigValueFactory getSortableConfigValueFactory() {
			return SortableConfigValueFactory.INSTANCE;
		}

		@Override
		public void configureScreen(Consumer<IConfigScreenBuilder> screenBuilderConsumer) {
			Consumer<IConfigScreenBuilder> checkedScreenBuilderConsumer = ErrorUtil.checkNotNull(screenBuilderConsumer, "screenBuilderConsumer");
			screenCustomizers.add(checkedScreenBuilderConsumer);
		}

		@Override
		public void registerScreen(
			Component title,
			Supplier<? extends IConfigSchema> schemaSupplier
		) {
			if (config != null) {
				throw new IllegalStateException("A config screen has already been registered for mod id: " + modId);
			}
			config = new ConfigScreenFactoryConfig(title, createScreenSchemaSupplier(schemaSupplier));
		}

		public Optional<IConfigScreenFactory> createFactory() {
			return createFactoryEntry(new ConfigScreenNavigation())
				.map(ConfigScreenFactoryEntry::factory);
		}

		public Optional<ConfigScreenFactoryEntry> createFactoryEntry(ConfigScreenNavigation navigation) {
			ConfigScreenFactoryConfig config = getConfigScreenFactoryConfig();
			if (config == null) {
				return Optional.empty();
			}
			validateConfigScreenFactoryInputs(config.title(), config.schemaSupplier());
			return Optional.of(new ConfigScreenFactoryEntry(
				modId,
				config.title(),
				createScreenFactory(
					modId,
					config,
					screenCustomizers,
					valueEditorFactories,
					navigation
				)
			));
		}

		@Nullable
		private ConfigScreenFactoryConfig getConfigScreenFactoryConfig() {
			@Nullable
			ConfigScreenFactoryConfig config = this.config;
			if (config != null) {
				return config;
			}
			@Nullable
			ConfigScreenConfig configScreen = this.configScreen;
			if (configScreen == null) {
				return null;
			}
			return new ConfigScreenFactoryConfig(
				configScreen.getTitle(),
				configScreen::getSchema
			);
		}
	}

	private static final class ConfigScreenBuilder implements IConfigScreenBuilder {
		private final String modId;
		private final Map<String, ConfigScreenCategoryBuilder> categories = new LinkedHashMap<>();
		private Component title;
		private boolean clearDefaultCategories;

		public ConfigScreenBuilder(String modId, Component title) {
			this.modId = modId;
			this.title = title;
		}

		@Override
		public void setTitle(Component title) {
			this.title = ErrorUtil.checkNotNull(title, "title");
		}

		@Override
		public IConfigScreenBuilder clearDefaultCategories() {
			clearDefaultCategories = true;
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder addCategory(String name) {
			String categoryName = validateName(name, "category name");
			ConfigScreenCategoryBuilder categoryBuilder = categories.computeIfAbsent(categoryName, ConfigScreenCategoryBuilder::new);
			if (categoryBuilder.isOrdered()) {
				throw new IllegalArgumentException("Duplicate config screen category for mod id: " + modId + ", category: " + categoryName);
			}
			categoryBuilder.setOrdered();
			return categoryBuilder;
		}

		@Override
		public IConfigScreenCategoryBuilder configureCategory(String name) {
			String categoryName = validateName(name, "category name");
			return categories.computeIfAbsent(categoryName, ConfigScreenCategoryBuilder::new);
		}

		public Component getTitle() {
			return title;
		}

		public boolean isClearDefaultCategories() {
			return clearDefaultCategories;
		}

		public List<ConfiguredScreenCategory> getCategories() {
			return categories.values()
				.stream()
				.map(ConfigScreenCategoryBuilder::build)
				.toList();
		}
	}

	private static final class ConfigScreenCategoryBuilder implements IConfigScreenCategoryBuilder {
		private final String name;
		private final List<ConfigScreenValueProvider> valueProviders = new ArrayList<>();
		private final List<ConfigScreenValueProvider> hiddenValueProviders = new ArrayList<>();
		private final List<ConfigScreenValueInsertion> valueInsertions = new ArrayList<>();
		private final List<ConfigScreenValueApplyModeOverride> applyModeOverrides = new ArrayList<>();
		private final List<ConfigScreenValueRestartRequirementOverride> restartRequirementOverrides = new ArrayList<>();
		@Nullable
		private Component title;
		@Nullable
		private Component description;
		@Nullable
		private ConfigValueApplyMode defaultApplyMode;
		private boolean ordered;
		private boolean clearDefaultValues;
		private boolean containsKeyMappings;

		public ConfigScreenCategoryBuilder(String name) {
			this.name = name;
		}

		public void setOrdered() {
			ordered = true;
		}

		public boolean isOrdered() {
			return ordered;
		}

		@Override
		public IConfigScreenCategoryBuilder setTitle(Component title) {
			this.title = ErrorUtil.checkNotNull(title, "title");
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder setDescription(Component description) {
			this.description = ErrorUtil.checkNotNull(description, "description");
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder clearDefaultValues() {
			clearDefaultValues = true;
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder setDefaultApplyMode(ConfigValueApplyMode applyMode) {
			this.defaultApplyMode = ErrorUtil.checkNotNull(applyMode, "applyMode");
			return this;
		}

		@Override
		public IConfigScreenValueBuilder getValueBuilder(IConfigValue<?> value) {
			return new ConfigScreenValueBuilder(ConfigScreenValueMatcher.configValue(value));
		}

		@Override
		public IConfigScreenValueBuilder getScreenValueBuilder(IConfigScreenValue<?> value) {
			return new ConfigScreenValueBuilder(ConfigScreenValueMatcher.screenValue(value));
		}

		@Override
		public IConfigScreenValueBuilder getValueBuilderByName(String valueName) {
			return new ConfigScreenValueBuilder(ConfigScreenValueMatcher.named(valueName));
		}

		@Override
		public IConfigScreenCategoryBuilder setValueApplyMode(IConfigValue<?> value, ConfigValueApplyMode applyMode) {
			ConfigScreenValueMatcher valueMatcher = ConfigScreenValueMatcher.configValue(value);
			ConfigValueApplyMode checkedApplyMode = ErrorUtil.checkNotNull(applyMode, "applyMode");
			applyModeOverrides.add(new ConfigScreenValueApplyModeOverride(valueMatcher, checkedApplyMode));
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder setScreenValueApplyMode(IConfigScreenValue<?> value, ConfigValueApplyMode applyMode) {
			ConfigScreenValueMatcher valueMatcher = ConfigScreenValueMatcher.screenValue(value);
			ConfigValueApplyMode checkedApplyMode = ErrorUtil.checkNotNull(applyMode, "applyMode");
			applyModeOverrides.add(new ConfigScreenValueApplyModeOverride(valueMatcher, checkedApplyMode));
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder setValueApplyModeByName(String valueName, ConfigValueApplyMode applyMode) {
			ConfigScreenValueMatcher valueMatcher = ConfigScreenValueMatcher.named(valueName);
			ConfigValueApplyMode checkedApplyMode = ErrorUtil.checkNotNull(applyMode, "applyMode");
			applyModeOverrides.add(new ConfigScreenValueApplyModeOverride(valueMatcher, checkedApplyMode));
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder setValueRestartRequirement(
			IConfigValue<?> value,
			ConfigValueRestartRequirement restartRequirement
		) {
			ConfigScreenValueMatcher valueMatcher = ConfigScreenValueMatcher.configValue(value);
			ConfigValueRestartRequirement checkedRestartRequirement = ErrorUtil.checkNotNull(restartRequirement, "restartRequirement");
			restartRequirementOverrides.add(new ConfigScreenValueRestartRequirementOverride(valueMatcher, checkedRestartRequirement));
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder setScreenValueRestartRequirement(
			IConfigScreenValue<?> value,
			ConfigValueRestartRequirement restartRequirement
		) {
			ConfigScreenValueMatcher valueMatcher = ConfigScreenValueMatcher.screenValue(value);
			ConfigValueRestartRequirement checkedRestartRequirement = ErrorUtil.checkNotNull(restartRequirement, "restartRequirement");
			restartRequirementOverrides.add(new ConfigScreenValueRestartRequirementOverride(valueMatcher, checkedRestartRequirement));
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder setValueRestartRequirementByName(
			String valueName,
			ConfigValueRestartRequirement restartRequirement
		) {
			ConfigScreenValueMatcher valueMatcher = ConfigScreenValueMatcher.named(valueName);
			ConfigValueRestartRequirement checkedRestartRequirement = ErrorUtil.checkNotNull(restartRequirement, "restartRequirement");
			restartRequirementOverrides.add(new ConfigScreenValueRestartRequirementOverride(valueMatcher, checkedRestartRequirement));
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder addValue(IConfigValue<?> value) {
			IConfigValue<?> checkedValue = ErrorUtil.checkNotNull(value, "value");
			return addScreenValue(IConfigScreenValue.configValue(checkedValue));
		}

		@Override
		public IConfigScreenCategoryBuilder addScreenValue(IConfigScreenValue<?> value) {
			IConfigScreenValue<?> checkedValue = ErrorUtil.checkNotNull(value, "value");
			return addScreenValues(List.of(checkedValue));
		}

		@Override
		public IConfigScreenCategoryBuilder addValues(Collection<? extends IConfigValue<?>> values) {
			Collection<? extends IConfigValue<?>> checkedValues = ErrorUtil.checkNotNull(values, "values");
			List<IConfigScreenValue<?>> valuesCopy = checkedValues.stream()
				.map(ConfigGuiPluginLoader::createConfigScreenValue)
				.toList();
			return addScreenValues(valuesCopy);
		}

		@Override
		public IConfigScreenCategoryBuilder addScreenValues(Collection<? extends IConfigScreenValue<?>> values) {
			Collection<? extends IConfigScreenValue<?>> checkedValues = ErrorUtil.checkNotNull(values, "values");
			List<IConfigScreenValue<?>> valuesCopy = List.copyOf(checkedValues);
			return addValueProvider(lookup -> valuesCopy);
		}

		@Override
		public <T> ISortingConfigGuiBuilder<T> addSortingConfig(
			String name,
			String localizationKey,
			ISortingConfig<T> sortingConfig,
			Collection<T> values,
			IConfigValueSerializer<T> valueSerializer
		) {
			SortableConfigValueFactory.SortingConfigGuiBuilder<T> builder = SortableConfigValueFactory.INSTANCE.builder(
				name,
				localizationKey,
				sortingConfig,
				values,
				valueSerializer
			);
			addValueProvider(lookup -> List.of(builder.build()));
			return builder;
		}

		@Override
		public ISortingConfigGuiBuilder<String> addStringSortingConfig(
			String name,
			String localizationKey,
			ISortingConfig<String> sortingConfig,
			Collection<String> values
		) {
			SortableConfigValueFactory.SortingConfigGuiBuilder<String> builder = SortableConfigValueFactory.INSTANCE.stringBuilder(
				name,
				localizationKey,
				sortingConfig,
				values
			);
			addValueProvider(lookup -> List.of(builder.build()));
			return builder;
		}

		@Override
		public IConfigScreenCategoryBuilder hideValue(IConfigValue<?> value) {
			IConfigValue<?> checkedValue = ErrorUtil.checkNotNull(value, "value");
			return hideScreenValue(IConfigScreenValue.configValue(checkedValue));
		}

		@Override
		public IConfigScreenCategoryBuilder hideScreenValue(IConfigScreenValue<?> value) {
			IConfigScreenValue<?> checkedValue = ErrorUtil.checkNotNull(value, "value");
			return hideScreenValues(List.of(checkedValue));
		}

		@Override
		public IConfigScreenCategoryBuilder hideValues(Collection<? extends IConfigValue<?>> values) {
			Collection<? extends IConfigValue<?>> checkedValues = ErrorUtil.checkNotNull(values, "values");
			List<IConfigScreenValue<?>> valuesCopy = checkedValues.stream()
				.map(ConfigGuiPluginLoader::createConfigScreenValue)
				.toList();
			return hideScreenValues(valuesCopy);
		}

		@Override
		public IConfigScreenCategoryBuilder hideScreenValues(Collection<? extends IConfigScreenValue<?>> values) {
			Collection<? extends IConfigScreenValue<?>> checkedValues = ErrorUtil.checkNotNull(values, "values");
			List<IConfigScreenValue<?>> valuesCopy = List.copyOf(checkedValues);
			return addHiddenValueProvider(lookup -> valuesCopy);
		}

		@Override
		public IConfigScreenCategoryBuilder addValueByName(String valueName) {
			ConfigScreenValueMatcher valueMatcher = ConfigScreenValueMatcher.named(valueName);
			return addValueProvider(lookup -> findValue(lookup, valueMatcher)
				.map(List::of)
				.orElseGet(List::of)
			);
		}

		@Override
		public IConfigScreenCategoryBuilder hideValueByName(String valueName) {
			ConfigScreenValueMatcher valueMatcher = ConfigScreenValueMatcher.named(valueName);
			return addHiddenValueProvider(lookup -> findValue(lookup, valueMatcher)
				.map(List::of)
				.orElseGet(List::of)
			);
		}

		@Override
		public IConfigScreenCategoryBuilder addValuesByName(Collection<String> valueNames) {
			Collection<String> checkedValueNames = ErrorUtil.checkNotNull(valueNames, "valueNames");
			for (String valueName : checkedValueNames) {
				addValueByName(valueName);
			}
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder hideValuesByName(Collection<String> valueNames) {
			Collection<String> checkedValueNames = ErrorUtil.checkNotNull(valueNames, "valueNames");
			for (String valueName : checkedValueNames) {
				hideValueByName(valueName);
			}
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder addKeyMapping(KeyMapping keyMapping) {
			KeyMapping checkedKeyMapping = ErrorUtil.checkNotNull(keyMapping, "keyMapping");
			return addKeyMappings(List.of(checkedKeyMapping));
		}

		@Override
		public IConfigScreenCategoryBuilder addKeyMappings(Collection<? extends KeyMapping> keyMappings) {
			Collection<? extends KeyMapping> checkedKeyMappings = ErrorUtil.checkNotNull(keyMappings, "keyMappings");
			List<KeyMapping> keyMappingsCopy = List.copyOf(checkedKeyMappings);
			containsKeyMappings = true;
			return addValueProvider(lookup -> {
				if (!ConfigGuiOptions.showKeyMappings()) {
					return List.of();
				}
				return KeyMappingConfigValues.create(keyMappingsCopy);
			});
		}

		private IConfigScreenCategoryBuilder addValueProvider(ConfigScreenValueProvider valueProvider) {
			ConfigScreenValueProvider checkedValueProvider = ErrorUtil.checkNotNull(valueProvider, "valueProvider");
			valueProviders.add(checkedValueProvider);
			return this;
		}

		private IConfigScreenCategoryBuilder addHiddenValueProvider(ConfigScreenValueProvider valueProvider) {
			ConfigScreenValueProvider checkedValueProvider = ErrorUtil.checkNotNull(valueProvider, "valueProvider");
			hiddenValueProviders.add(checkedValueProvider);
			return this;
		}

		private IConfigScreenCategoryBuilder insertValueProvider(
			ConfigScreenValueMatcher valueMatcher,
			ConfigScreenValueProvider valueProvider,
			ConfigScreenValueInsertionPosition position
		) {
			ConfigScreenValueMatcher checkedValueMatcher = ErrorUtil.checkNotNull(valueMatcher, "valueMatcher");
			ConfigScreenValueProvider checkedValueProvider = ErrorUtil.checkNotNull(valueProvider, "valueProvider");
			ConfigScreenValueInsertionPosition checkedPosition = ErrorUtil.checkNotNull(position, "position");
			valueInsertions.add(new ConfigScreenValueInsertion(checkedValueMatcher, checkedValueProvider, checkedPosition));
			return this;
		}

		public ConfiguredScreenCategory build() {
			return new ConfiguredScreenCategory(
				name,
				title,
				description,
				defaultApplyMode,
				ordered,
				clearDefaultValues,
				containsKeyMappings,
				valueProviders,
				hiddenValueProviders,
				valueInsertions,
				applyModeOverrides,
				restartRequirementOverrides
			);
		}

		private final class ConfigScreenValueBuilder implements IConfigScreenValueBuilder {
			private final ConfigScreenValueMatcher valueMatcher;

			private ConfigScreenValueBuilder(ConfigScreenValueMatcher valueMatcher) {
				this.valueMatcher = valueMatcher;
			}

			@Override
			public IConfigScreenValueBuilder setApplyMode(ConfigValueApplyMode applyMode) {
				ConfigValueApplyMode checkedApplyMode = ErrorUtil.checkNotNull(applyMode, "applyMode");
				applyModeOverrides.add(new ConfigScreenValueApplyModeOverride(valueMatcher, checkedApplyMode));
				return this;
			}

			@Override
			public IConfigScreenValueBuilder setRestartRequirement(ConfigValueRestartRequirement restartRequirement) {
				ConfigValueRestartRequirement checkedRestartRequirement = ErrorUtil.checkNotNull(restartRequirement, "restartRequirement");
				restartRequirementOverrides.add(new ConfigScreenValueRestartRequirementOverride(valueMatcher, checkedRestartRequirement));
				return this;
			}

			@Override
			public IConfigScreenValueBuilder hide() {
				addHiddenValueProvider(lookup -> findValue(lookup, valueMatcher)
					.map(List::of)
					.orElseGet(List::of)
				);
				return this;
			}

			@Override
			public IConfigScreenValueBuilder insertBefore(IConfigValue<?> value) {
				IConfigValue<?> checkedValue = ErrorUtil.checkNotNull(value, "value");
				IConfigScreenValue<?> screenValue = IConfigScreenValue.configValue(checkedValue);
				return insertBefore(screenValue);
			}

			@Override
			public IConfigScreenValueBuilder insertBefore(IConfigScreenValue<?> value) {
				IConfigScreenValue<?> checkedValue = ErrorUtil.checkNotNull(value, "value");
				insertValueProvider(
					valueMatcher,
					lookup -> List.of(checkedValue),
					ConfigScreenValueInsertionPosition.BEFORE
				);
				return this;
			}

			@Override
			public IConfigScreenValueBuilder insertAfter(IConfigValue<?> value) {
				IConfigValue<?> checkedValue = ErrorUtil.checkNotNull(value, "value");
				IConfigScreenValue<?> screenValue = IConfigScreenValue.configValue(checkedValue);
				return insertAfter(screenValue);
			}

			@Override
			public IConfigScreenValueBuilder insertAfter(IConfigScreenValue<?> value) {
				IConfigScreenValue<?> checkedValue = ErrorUtil.checkNotNull(value, "value");
				insertValueProvider(
					valueMatcher,
					lookup -> List.of(checkedValue),
					ConfigScreenValueInsertionPosition.AFTER
				);
				return this;
			}
		}
	}

	private record ConfiguredScreenCategory(
		String name,
		@Nullable Component title,
		@Nullable Component description,
		@Nullable ConfigValueApplyMode defaultApplyMode,
		boolean ordered,
		boolean clearDefaultValues,
		boolean containsKeyMappings,
		List<ConfigScreenValueProvider> valueProviders,
		List<ConfigScreenValueProvider> hiddenValueProviders,
		List<ConfigScreenValueInsertion> valueInsertions,
		List<ConfigScreenValueApplyModeOverride> applyModeOverrides,
		List<ConfigScreenValueRestartRequirementOverride> restartRequirementOverrides
	) {
		private ConfiguredScreenCategory {
			valueProviders = List.copyOf(valueProviders);
			hiddenValueProviders = List.copyOf(hiddenValueProviders);
			valueInsertions = List.copyOf(valueInsertions);
			applyModeOverrides = List.copyOf(applyModeOverrides);
			restartRequirementOverrides = List.copyOf(restartRequirementOverrides);
		}

		private static ConfiguredScreenCategory createDefaultKeyMappingsCategory(ConfigScreenValueProvider defaultKeyMappingsProvider) {
			return new ConfiguredScreenCategory(
				KEY_MAPPINGS_CATEGORY_NAME,
				null,
				null,
				null,
				false,
				false,
				true,
				List.of(defaultKeyMappingsProvider),
				List.of(),
				List.of(),
				List.of(),
				List.of()
			);
		}

		private ConfiguredScreenCategory withDefaultKeyMappings(ConfigScreenValueProvider defaultKeyMappingsProvider) {
			List<ConfigScreenValueProvider> valueProviders = new ArrayList<>(this.valueProviders);
			valueProviders.add(defaultKeyMappingsProvider);
			return new ConfiguredScreenCategory(
				name,
				title,
				description,
				defaultApplyMode,
				ordered,
				clearDefaultValues,
				containsKeyMappings,
				valueProviders,
				hiddenValueProviders,
				valueInsertions,
				applyModeOverrides,
				restartRequirementOverrides
			);
		}
	}

	private enum ConfigScreenValueInsertionPosition {
		BEFORE,
		AFTER
	}

	private record ConfigScreenValueInsertion(
		ConfigScreenValueMatcher valueMatcher,
		ConfigScreenValueProvider valueProvider,
		ConfigScreenValueInsertionPosition position
	) {

	}

	private record ConfigScreenValueApplyModeOverride(
		ConfigScreenValueMatcher valueMatcher,
		ConfigValueApplyMode applyMode
	) {

	}

	private record ConfigScreenValueRestartRequirementOverride(
		ConfigScreenValueMatcher valueMatcher,
		ConfigValueRestartRequirement restartRequirement
	) {

	}

	@FunctionalInterface
	private interface ConfigScreenValueMatcher {
		static ConfigScreenValueMatcher configValue(IConfigValue<?> configValue) {
			IConfigValue<?> checkedConfigValue = ErrorUtil.checkNotNull(configValue, "configValue");
			return new ConfigScreenValueMatcher() {
				@Override
				public boolean matches(String categoryName, IConfigScreenValue<?> value) {
					return value.getIdentityKey() == checkedConfigValue;
				}

				@Override
				public String toString() {
					return checkedConfigValue.getName();
				}
			};
		}

		static ConfigScreenValueMatcher screenValue(IConfigScreenValue<?> configValue) {
			IConfigScreenValue<?> checkedConfigValue = ErrorUtil.checkNotNull(configValue, "configValue");
			Object identityKey = checkedConfigValue.getIdentityKey();
			return new ConfigScreenValueMatcher() {
				@Override
				public boolean matches(String categoryName, IConfigScreenValue<?> value) {
					return value.getIdentityKey() == identityKey;
				}

				@Override
				public String toString() {
					return checkedConfigValue.getName();
				}
			};
		}

		static ConfigScreenValueMatcher named(String name) {
			String checkedName = validateName(name, "value name");
			return new ConfigScreenValueMatcher() {
				@Override
				public boolean matches(String categoryName, IConfigScreenValue<?> value) {
					if (value.getName().equals(checkedName)) {
						return true;
					}
					String qualifiedName = categoryName + "." + value.getName();
					return qualifiedName.equals(checkedName);
				}

				@Override
				public String toString() {
					return checkedName;
				}
			};
		}

		boolean matches(String categoryName, IConfigScreenValue<?> value);
	}

	private static final class CustomizedConfigScreenSchema implements ConfigScreenSchema {
		private final String modId;
		private final ConfigScreenSchema schema;
		private final List<ConfiguredScreenCategory> configuredCategories;
		private final boolean clearDefaultCategories;

		public CustomizedConfigScreenSchema(
			String modId,
			ConfigScreenSchema schema,
			List<ConfiguredScreenCategory> configuredCategories,
			boolean clearDefaultCategories
		) {
			this.modId = modId;
			this.schema = schema;
			this.configuredCategories = List.copyOf(configuredCategories);
			this.clearDefaultCategories = clearDefaultCategories;
		}

		@Override
		public List<? extends ConfigScreenCategory> getCategories() {
			return createScreenCategories(modId, schema.getCategories(), configuredCategories, clearDefaultCategories);
		}
	}

	private static List<ConfigScreenCategory> createScreenCategories(
		String modId,
		List<? extends ConfigScreenCategory> originalCategories,
		List<ConfiguredScreenCategory> configuredCategories,
		boolean clearDefaultCategories
	) {
		return createScreenCategories(
			modId,
			originalCategories,
			configuredCategories,
			clearDefaultCategories,
			DEFAULT_KEY_MAPPINGS_PROVIDER
		);
	}

	static List<ConfigScreenCategory> createScreenCategoriesForTests(
		String modId,
		List<? extends ConfigScreenCategory> originalCategories,
		Consumer<IConfigScreenBuilder> screenCustomizer,
		ConfigScreenValueProvider defaultKeyMappingsProvider
	) {
		ConfigScreenBuilder screenBuilder = new ConfigScreenBuilder(modId, Component.empty());
		screenCustomizer.accept(screenBuilder);
		return createScreenCategories(
			modId,
			originalCategories,
			screenBuilder.getCategories(),
			screenBuilder.isClearDefaultCategories(),
			defaultKeyMappingsProvider
		);
	}

	static ConfigScreenSchema createCustomizedScreenSchemaForTests(
		String modId,
		Supplier<? extends ConfigScreenSchema> schemaSupplier,
		List<Consumer<IConfigScreenBuilder>> screenCustomizers
	) {
		return createCustomizedScreenSchemaForTests(
			modId,
			schemaSupplier,
			screenCustomizers,
			lookup -> List.of()
		);
	}

	static ConfigScreenSchema createCustomizedScreenSchemaForTests(
		String modId,
		Supplier<? extends ConfigScreenSchema> schemaSupplier,
		List<Consumer<IConfigScreenBuilder>> screenCustomizers,
		ConfigScreenValueProvider defaultKeyMappingsProvider
	) {
		ConfigScreenFactoryConfig config = new ConfigScreenFactoryConfig(
			Component.empty(),
			schemaSupplier
		);
		ConfigScreenBuilder screenBuilder = createScreenBuilder(modId, config, screenCustomizers);
		ConfigScreenSchema schema = schemaSupplier.get();
		if (schema == null) {
			throw new NullPointerException("schemaSupplier must not return null.");
		}
		List<ConfiguredScreenCategory> configuredCategories = screenBuilder.getCategories();
		boolean clearDefaultCategories = screenBuilder.isClearDefaultCategories();
		return () -> createScreenCategories(
			modId,
			schema.getCategories(),
			configuredCategories,
			clearDefaultCategories,
			defaultKeyMappingsProvider
		);
	}

	private static List<ConfigScreenCategory> createScreenCategories(
		String modId,
		List<? extends ConfigScreenCategory> originalCategories,
		List<ConfiguredScreenCategory> configuredCategories,
		boolean clearDefaultCategories,
		ConfigScreenValueProvider defaultKeyMappingsProvider
	) {
		configuredCategories = addDefaultKeyMappingsCategory(configuredCategories, defaultKeyMappingsProvider, clearDefaultCategories);
		List<ResolvedScreenCategory> resolvedCategories = originalCategories.stream()
			.map(ConfigGuiPluginLoader::resolve)
			.toList();
		Optional<String> categoryLocalizationPrefix = getCategoryLocalizationPrefix(resolvedCategories);
		Set<String> emittedCategoryNames = new HashSet<>();
		List<ConfigScreenCategory> screenCategories = new ArrayList<>();
		for (ConfiguredScreenCategory configuredCategory : configuredCategories) {
			if (configuredCategory.ordered()) {
				addConfiguredScreenCategory(
					modId,
					categoryLocalizationPrefix,
					resolvedCategories,
					screenCategories,
					configuredCategory
				);
				emittedCategoryNames.add(configuredCategory.name());
			}
		}

		for (ResolvedScreenCategory originalCategory : resolvedCategories) {
			if (emittedCategoryNames.contains(originalCategory.name())) {
				continue;
			}
			Optional<ConfiguredScreenCategory> configuredCategory = findConfiguredCategory(configuredCategories, originalCategory.name());
			if (configuredCategory.isPresent()) {
				addConfiguredScreenCategory(
					modId,
					categoryLocalizationPrefix,
					resolvedCategories,
					screenCategories,
					configuredCategory.get()
				);
				emittedCategoryNames.add(originalCategory.name());
				continue;
			}
			if (clearDefaultCategories) {
				continue;
			}
			if (!originalCategory.values().isEmpty()) {
				screenCategories.add(new ConfiguredScreenConfigCategory(
					originalCategory.name(),
					originalCategory.title(),
					originalCategory.description(),
					originalCategory.group(),
					originalCategory.values()
				));
			}
		}
		for (ConfiguredScreenCategory configuredCategory : configuredCategories) {
			if (emittedCategoryNames.add(configuredCategory.name())) {
				addConfiguredScreenCategory(
					modId,
					categoryLocalizationPrefix,
					resolvedCategories,
					screenCategories,
					configuredCategory
				);
			}
		}
		return screenCategories.stream()
			.sorted(Comparator.comparing(ConfigScreenCategory::getGroup))
			.toList();
	}

	private static List<ConfiguredScreenCategory> addDefaultKeyMappingsCategory(
		List<ConfiguredScreenCategory> configuredCategories,
		ConfigScreenValueProvider defaultKeyMappingsProvider,
		boolean clearDefaultCategories
	) {
		if (hasConfiguredKeyMappings(configuredCategories)) {
			return configuredCategories;
		}
		List<ConfiguredScreenCategory> categories = new ArrayList<>(configuredCategories);
		for (int i = 0; i < categories.size(); i++) {
			ConfiguredScreenCategory category = categories.get(i);
			if (category.name().equals(KEY_MAPPINGS_CATEGORY_NAME)) {
				if (category.clearDefaultValues()) {
					return categories;
				}
				categories.set(i, category.withDefaultKeyMappings(defaultKeyMappingsProvider));
				return categories;
			}
		}
		if (clearDefaultCategories) {
			return configuredCategories;
		}
		categories.add(ConfiguredScreenCategory.createDefaultKeyMappingsCategory(defaultKeyMappingsProvider));
		return categories;
	}

	private static boolean hasConfiguredKeyMappings(List<ConfiguredScreenCategory> configuredCategories) {
		return configuredCategories.stream()
			.anyMatch(ConfiguredScreenCategory::containsKeyMappings);
	}

	private static Optional<ConfiguredScreenCategory> findConfiguredCategory(
		List<ConfiguredScreenCategory> configuredCategories,
		String name
	) {
		return configuredCategories.stream()
			.filter(configuredCategory -> configuredCategory.name().equals(name))
			.findFirst();
	}

	private static void addConfiguredScreenCategory(
		String modId,
		Optional<String> categoryLocalizationPrefix,
		List<ResolvedScreenCategory> resolvedCategories,
		List<ConfigScreenCategory> screenCategories,
		ConfiguredScreenCategory configuredCategory
	) {
		List<IConfigScreenValue<?>> values = new ArrayList<>();
		Set<Object> usedValueKeys = createIdentitySet();
		Set<Object> hiddenValueKeys = getHiddenConfiguredValueKeys(modId, resolvedCategories, configuredCategory);
		ConfigScreenValueLookup lookup = new ConfigScreenValueLookup(modId, configuredCategory.name(), resolvedCategories);
		if (!configuredCategory.clearDefaultValues()) {
			addOriginalValues(configuredCategory, lookup, resolvedCategories, hiddenValueKeys, usedValueKeys, values);
		}
		for (ConfigScreenValueProvider valueProvider : configuredCategory.valueProviders()) {
			for (IConfigScreenValue<?> configValue : valueProvider.getValues(lookup)) {
				addConfiguredValue(usedValueKeys, values, configValue);
			}
		}
		values = applyConfiguredValueSettings(modId, configuredCategory, values);
		if (!values.isEmpty()) {
			screenCategories.add(new ConfiguredScreenConfigCategory(
				configuredCategory.name(),
				getCategoryTitle(modId, categoryLocalizationPrefix, resolvedCategories, configuredCategory),
				getCategoryDescription(modId, categoryLocalizationPrefix, resolvedCategories, configuredCategory),
				getCategoryGroup(resolvedCategories, configuredCategory),
				values
			));
		}
	}

	private static ConfigScreenCategoryGroup getCategoryGroup(
		List<ResolvedScreenCategory> resolvedCategories,
		ConfiguredScreenCategory configuredCategory
	) {
		if (configuredCategory.containsKeyMappings() || configuredCategory.name().equals(KEY_MAPPINGS_CATEGORY_NAME)) {
			return ConfigScreenCategoryGroup.KEY_MAPPINGS;
		}
		return findResolvedCategory(resolvedCategories, configuredCategory.name())
			.map(ResolvedScreenCategory::group)
			.orElse(ConfigScreenCategoryGroup.MOD_OWNED);
	}

	private static List<IConfigScreenValue<?>> applyConfiguredValueSettings(
		String modId,
		ConfiguredScreenCategory configuredCategory,
		List<IConfigScreenValue<?>> values
	) {
		if (values.isEmpty() ||
			(configuredCategory.defaultApplyMode() == null &&
			configuredCategory.applyModeOverrides().isEmpty() &&
			configuredCategory.restartRequirementOverrides().isEmpty())
		) {
			return values;
		}
		List<IConfigScreenValue<?>> configuredValues = new ArrayList<>(values.size());
		for (IConfigScreenValue<?> value : values) {
			ConfigValueApplyMode applyMode = getConfiguredApplyMode(modId, configuredCategory, value, values);
			ConfigValueRestartRequirement restartRequirement = getConfiguredRestartRequirement(modId, configuredCategory, value, values);
			configuredValues.add(withValueSettings(value, applyMode, restartRequirement));
		}
		return List.copyOf(configuredValues);
	}

	private static ConfigValueApplyMode getConfiguredApplyMode(
		String modId,
		ConfiguredScreenCategory configuredCategory,
		IConfigScreenValue<?> value,
		List<IConfigScreenValue<?>> values
	) {
		@Nullable
		ConfigValueApplyMode applyMode = configuredCategory.defaultApplyMode();
		for (ConfigScreenValueApplyModeOverride applyModeOverride : configuredCategory.applyModeOverrides()) {
			ConfigScreenValueMatcher valueMatcher = applyModeOverride.valueMatcher();
			if (valueMatcher.matches(configuredCategory.name(), value)) {
				validateApplyModeMatcher(modId, configuredCategory, valueMatcher, values);
				applyMode = applyModeOverride.applyMode();
			}
		}
		if (applyMode == null) {
			return value.getApplyMode();
		}
		return applyMode;
	}

	private static ConfigValueRestartRequirement getConfiguredRestartRequirement(
		String modId,
		ConfiguredScreenCategory configuredCategory,
		IConfigScreenValue<?> value,
		List<IConfigScreenValue<?>> values
	) {
		ConfigValueRestartRequirement restartRequirement = value.getRestartRequirement();
		for (ConfigScreenValueRestartRequirementOverride restartRequirementOverride : configuredCategory.restartRequirementOverrides()) {
			ConfigScreenValueMatcher valueMatcher = restartRequirementOverride.valueMatcher();
			if (valueMatcher.matches(configuredCategory.name(), value)) {
				validateRestartRequirementMatcher(modId, configuredCategory, valueMatcher, values);
				restartRequirement = restartRequirementOverride.restartRequirement();
			}
		}
		return restartRequirement;
	}

	private static void validateApplyModeMatcher(
		String modId,
		ConfiguredScreenCategory configuredCategory,
		ConfigScreenValueMatcher valueMatcher,
		List<IConfigScreenValue<?>> values
	) {
		long matchCount = values.stream()
			.filter(value -> valueMatcher.matches(configuredCategory.name(), value))
			.limit(2)
			.count();
		if (matchCount > 1) {
			LOGGER.error(
				"Config value apply mode matcher matched multiple values for mod id: {}, category: {}, value matcher: {}",
				modId,
				configuredCategory.name(),
				valueMatcher
			);
		}
	}

	private static void validateRestartRequirementMatcher(
		String modId,
		ConfiguredScreenCategory configuredCategory,
		ConfigScreenValueMatcher valueMatcher,
		List<IConfigScreenValue<?>> values
	) {
		long matchCount = values.stream()
			.filter(value -> valueMatcher.matches(configuredCategory.name(), value))
			.limit(2)
			.count();
		if (matchCount > 1) {
			LOGGER.error(
				"Config value restart requirement matcher matched multiple values for mod id: {}, category: {}, value matcher: {}",
				modId,
				configuredCategory.name(),
				valueMatcher
			);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> IConfigScreenValue<T> withValueSettings(
		IConfigScreenValue<T> value,
		ConfigValueApplyMode applyMode,
		ConfigValueRestartRequirement restartRequirement
	) {
		IConfigScreenValue<T> configuredValue = value;
		if (applyMode != value.getApplyMode()) {
			configuredValue = IConfigScreenValue.withApplyMode(configuredValue, applyMode);
		}
		if (restartRequirement != value.getRestartRequirement()) {
			configuredValue = IConfigScreenValue.withRestartRequirement(configuredValue, restartRequirement);
		}
		return configuredValue;
	}

	private static Set<Object> getHiddenConfiguredValueKeys(
		String modId,
		List<ResolvedScreenCategory> resolvedCategories,
		ConfiguredScreenCategory configuredCategory
	) {
		Set<Object> hiddenValueKeys = createIdentitySet();
		ConfigScreenValueLookup lookup = new ConfigScreenValueLookup(modId, configuredCategory.name(), resolvedCategories);
		for (ConfigScreenValueProvider valueProvider : configuredCategory.hiddenValueProviders()) {
			for (IConfigScreenValue<?> configValue : valueProvider.getValues(lookup)) {
				hiddenValueKeys.add(configValue.getIdentityKey());
			}
		}
		return hiddenValueKeys;
	}

	private static Component getCategoryTitle(
		String modId,
		Optional<String> categoryLocalizationPrefix,
		List<ResolvedScreenCategory> resolvedCategories,
		ConfiguredScreenCategory configuredCategory
	) {
		Component title = configuredCategory.title();
		if (title != null) {
			return title;
		}
		return findResolvedCategory(resolvedCategories, configuredCategory.name())
			.map(ResolvedScreenCategory::title)
			.orElseGet(() -> {
				String localizationKey = getDefaultCategoryLocalizationKey(modId, categoryLocalizationPrefix, configuredCategory.name());
				return Component.translatableWithFallback(localizationKey, ConfigNameUtil.getDisplayNameFallback(configuredCategory.name()));
			});
	}

	private static Component getCategoryDescription(
		String modId,
		Optional<String> categoryLocalizationPrefix,
		List<ResolvedScreenCategory> resolvedCategories,
		ConfiguredScreenCategory configuredCategory
	) {
		Component description = configuredCategory.description();
		if (description != null) {
			return description;
		}
		return findResolvedCategory(resolvedCategories, configuredCategory.name())
			.map(ResolvedScreenCategory::description)
			.orElseGet(() -> {
				String localizationKey = getDefaultCategoryLocalizationKey(modId, categoryLocalizationPrefix, configuredCategory.name());
				return Component.translatableWithFallback(localizationKey + ".description", "");
			});
	}

	private static Optional<ResolvedScreenCategory> findResolvedCategory(
		List<ResolvedScreenCategory> resolvedCategories,
		String name
	) {
		return resolvedCategories.stream()
			.filter(category -> category.name().equals(name))
			.findFirst();
	}

	private static String getDefaultCategoryLocalizationKey(
		String modId,
		Optional<String> categoryLocalizationPrefix,
		String name
	) {
		return categoryLocalizationPrefix
			.map(prefix -> prefix + "." + name)
			.orElseGet(() -> modId + ".config." + name);
	}

	private static Optional<String> getCategoryLocalizationPrefix(List<ResolvedScreenCategory> resolvedCategories) {
		for (ResolvedScreenCategory category : resolvedCategories) {
			Optional<String> translationKey = getTranslationKey(category.title());
			if (translationKey.isPresent()) {
				String suffix = "." + category.name();
				String key = translationKey.get();
				if (key.endsWith(suffix) && key.length() > suffix.length()) {
					return Optional.of(key.substring(0, key.length() - suffix.length()));
				}
			}
		}
		return Optional.empty();
	}

	private static Optional<String> getTranslationKey(Component component) {
		if (component.getContents() instanceof TranslatableContents translatableContents) {
			return Optional.of(translatableContents.getKey());
		}
		return Optional.empty();
	}

	private static void addConfiguredValue(
		Set<Object> usedValueKeys,
		List<IConfigScreenValue<?>> values,
		IConfigScreenValue<?> configValue
	) {
		if (!usedValueKeys.add(configValue.getIdentityKey())) {
			return;
		}
		values.add(configValue);
	}

	private static void addOriginalValues(
		ConfiguredScreenCategory configuredCategory,
		ConfigScreenValueLookup lookup,
		List<ResolvedScreenCategory> originalCategories,
		Set<Object> hiddenValueKeys,
		Set<Object> usedValueKeys,
		List<IConfigScreenValue<?>> values
	) {
		for (ResolvedScreenCategory originalCategory : originalCategories) {
			if (!originalCategory.name().equals(configuredCategory.name())) {
				continue;
			}
			for (IConfigScreenValue<?> value : originalCategory.values()) {
				addInsertedValues(configuredCategory, lookup, value, ConfigScreenValueInsertionPosition.BEFORE, usedValueKeys, values);
				if (!hiddenValueKeys.contains(value.getIdentityKey())) {
					addConfiguredValue(usedValueKeys, values, value);
				}
				addInsertedValues(configuredCategory, lookup, value, ConfigScreenValueInsertionPosition.AFTER, usedValueKeys, values);
			}
		}
	}

	private static void addInsertedValues(
		ConfiguredScreenCategory configuredCategory,
		ConfigScreenValueLookup lookup,
		IConfigScreenValue<?> anchorValue,
		ConfigScreenValueInsertionPosition position,
		Set<Object> usedValueKeys,
		List<IConfigScreenValue<?>> values
	) {
		for (ConfigScreenValueInsertion insertion : configuredCategory.valueInsertions()) {
			if (insertion.position() != position) {
				continue;
			}
			ConfigScreenValueMatcher valueMatcher = insertion.valueMatcher();
			if (!valueMatcher.matches(configuredCategory.name(), anchorValue)) {
				continue;
			}
			for (IConfigScreenValue<?> insertedValue : insertion.valueProvider().getValues(lookup)) {
				addConfiguredValue(usedValueKeys, values, insertedValue);
			}
		}
	}

	private static Set<Object> createIdentitySet() {
		return Collections.newSetFromMap(new IdentityHashMap<>());
	}

	private static List<IConfigScreenValue<?>> getAllValues(List<ResolvedScreenCategory> screenCategories) {
		List<IConfigScreenValue<?>> values = new ArrayList<>();
		for (ResolvedScreenCategory screenCategory : screenCategories) {
			values.addAll(screenCategory.values());
		}
		return List.copyOf(values);
	}

	private static Optional<IConfigScreenValue<?>> findValue(ConfigScreenValueLookup lookup, ConfigScreenValueMatcher valueMatcher) {
		Optional<IConfigScreenValue<?>> categoryValue = findValueInCategory(lookup, valueMatcher);
		if (categoryValue.isPresent()) {
			return categoryValue;
		}
		return findValueInAllCategories(lookup, valueMatcher);
	}

	private static Optional<IConfigScreenValue<?>> findValueInCategory(ConfigScreenValueLookup lookup, ConfigScreenValueMatcher valueMatcher) {
		@Nullable
		IConfigScreenValue<?> result = null;
		for (ResolvedScreenCategory category : lookup.resolvedCategories()) {
			if (!category.name().equals(lookup.categoryName())) {
				continue;
			}
			for (IConfigScreenValue<?> value : category.values()) {
				if (valueMatcher.matches(category.name(), value)) {
					if (result != null) {
						LOGGER.error(
							"Config value matcher matched multiple values for mod id: {}, category: {}, value matcher: {}",
							lookup.modId(),
							lookup.categoryName(),
							valueMatcher
						);
						return Optional.of(result);
					}
					result = value;
				}
			}
		}
		return Optional.ofNullable(result);
	}

	private static Optional<IConfigScreenValue<?>> findValueInAllCategories(ConfigScreenValueLookup lookup, ConfigScreenValueMatcher valueMatcher) {
		@Nullable
		IConfigScreenValue<?> result = null;
		for (ResolvedScreenCategory category : lookup.resolvedCategories()) {
			for (IConfigScreenValue<?> value : category.values()) {
				if (valueMatcher.matches(category.name(), value)) {
					if (result != null) {
						LOGGER.error(
							"Config value matcher matched multiple values for mod id: {}, category: {}, value matcher: {}",
							lookup.modId(),
							lookup.categoryName(),
							valueMatcher
						);
						return Optional.of(result);
					}
					result = value;
				}
			}
		}
		return Optional.ofNullable(result);
	}

	private static ResolvedScreenCategory resolve(ConfigScreenCategory category) {
		return new ResolvedScreenCategory(
			category.getName(),
			category.getLocalizedName(),
			category.getLocalizedDescription(),
			category.getGroup(),
			List.copyOf(category.getConfigValues())
		);
	}

	record ResolvedScreenCategory(
		String name,
		Component title,
		Component description,
		ConfigScreenCategoryGroup group,
		List<IConfigScreenValue<?>> values
	) {
		ResolvedScreenCategory {
			values = List.copyOf(values);
		}
	}

	record ConfigScreenValueLookup(
		String modId,
		String categoryName,
		List<ResolvedScreenCategory> resolvedCategories
	) {
		ConfigScreenValueLookup {
			resolvedCategories = List.copyOf(resolvedCategories);
		}

		public List<IConfigScreenValue<?>> getAllValues() {
			return ConfigGuiPluginLoader.getAllValues(resolvedCategories);
		}
	}

	private record ConfiguredScreenConfigCategory(
		String name,
		Component title,
		Component description,
		ConfigScreenCategoryGroup group,
		List<IConfigScreenValue<?>> values
	) implements ConfigScreenCategory {
		private ConfiguredScreenConfigCategory {
			values = List.copyOf(values);
		}

		@Override
		public ConfigScreenCategoryGroup getGroup() {
			return group;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getLocalizationKey() {
			return name;
		}

		@Override
		public Component getLocalizedName() {
			return title;
		}

		@Override
		public Component getLocalizedDescription() {
			return description;
		}

		@Override
		public Collection<? extends IConfigScreenValue<?>> getConfigValues() {
			return values;
		}
	}

	private static IConfigScreenValue<?> createConfigScreenValue(IConfigValue<?> value) {
		return IConfigScreenValue.configValue(value);
	}

	@FunctionalInterface
	interface ConfigScreenValueProvider {
		List<? extends IConfigScreenValue<?>> getValues(ConfigScreenValueLookup lookup);
	}

	private static IConfigScreenFactory createScreenFactory(
		String modId,
		ConfigScreenFactoryConfig config,
		List<Consumer<IConfigScreenBuilder>> screenCustomizers,
		Map<ConfigValueEditorType<?>, IConfigValueEditorFactory<?>> valueEditorFactories,
		ConfigScreenNavigation navigation
	) {
		validateConfigScreenFactoryInputs(config.title(), config.schemaSupplier());
		List<Consumer<IConfigScreenBuilder>> screenCustomizersCopy = List.copyOf(screenCustomizers);
		Map<ConfigValueEditorType<?>, IConfigValueEditorFactory<?>> valueEditorFactoriesCopy = Map.copyOf(valueEditorFactories);
		return parent -> {
			ConfigScreenBuilder screenBuilder = createScreenBuilder(modId, config, screenCustomizersCopy);
			ConfigScreenSchema schema = createCustomizedScreenSchema(modId, config.schemaSupplier(), screenBuilder);
			Component title = screenBuilder.getTitle();
			ConfigChangesHandler changesHandler = createChangesHandler(modId, title);
			return ConfigScreen.create(parent, modId, title, schema, changesHandler, valueEditorFactoriesCopy, navigation);
		};
	}

	private static ConfigScreenBuilder createScreenBuilder(
		String modId,
		ConfigScreenFactoryConfig config,
		List<Consumer<IConfigScreenBuilder>> screenCustomizers
	) {
		ConfigScreenBuilder screenBuilder = new ConfigScreenBuilder(modId, config.title());
		for (Consumer<IConfigScreenBuilder> screenCustomizer : screenCustomizers) {
			screenCustomizer.accept(screenBuilder);
		}
		return screenBuilder;
	}

	private static ConfigScreenSchema createCustomizedScreenSchema(
		String modId,
		Supplier<? extends ConfigScreenSchema> schemaSupplier,
		ConfigScreenBuilder screenBuilder
	) {
		ConfigScreenSchema schema = schemaSupplier.get();
		if (schema == null) {
			throw new NullPointerException("schemaSupplier must not return null.");
		}
		return new CustomizedConfigScreenSchema(
			modId,
			schema,
			screenBuilder.getCategories(),
			screenBuilder.isClearDefaultCategories()
		);
	}

	private static ConfigChangesHandler createChangesHandler(
		String modId,
		Component title
	) {
		return changes -> {
			ConfigValueRestartRequirement restartRequirement = applyChanges(changes);
			if (restartRequirement != ConfigValueRestartRequirement.NONE) {
				notifyRestartDeferred(modId, title, restartRequirement);
			}
			return restartRequirement != ConfigValueRestartRequirement.NONE;
		};
	}

	private static ConfigValueRestartRequirement applyChanges(List<ConfigValueChange<?>> changes) {
		ConfigValueRestartRequirement restartRequirement = ConfigValueRestartRequirement.NONE;
		for (ConfigValueChange<?> change : changes) {
			if (!change.apply()) {
				continue;
			}
			ConfigValueRestartRequirement changeRequirement = change.configValue().getRestartRequirement();
			if (changeRequirement == ConfigValueRestartRequirement.GAME_RESTART) {
				restartRequirement = changeRequirement;
			} else if (changeRequirement == ConfigValueRestartRequirement.WORLD_RESTART &&
				restartRequirement == ConfigValueRestartRequirement.NONE) {
				restartRequirement = changeRequirement;
			}
		}
		return restartRequirement;
	}

	private static void notifyRestartDeferred(
		String modId,
		Component title,
		ConfigValueRestartRequirement restartRequirement
	) {
		String translationKey = switch (restartRequirement) {
			case NONE -> throw new IllegalArgumentException("restartRequirement must require a restart.");
			case WORLD_RESTART -> "mezz_config.config.screen.restart.nextWorldLoad";
			case GAME_RESTART -> "mezz_config.config.screen.restart.nextGameStart";
		};
		Component message = Component.translatable(translationKey, title);
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(message, false);
		}
		if (restartRequirement == ConfigValueRestartRequirement.WORLD_RESTART) {
			LOGGER.info("Config changes for {} were saved and will be applied the next time a world is opened.", modId);
		} else {
			LOGGER.info("Config changes for {} were saved and will be applied the next time the game is started.", modId);
		}
	}

	private static void validateConfigScreenFactoryInputs(
		Component title,
		Supplier<? extends ConfigScreenSchema> schemaSupplier
	) {
		ErrorUtil.checkNotNull(title, "title");
		ErrorUtil.checkNotNull(schemaSupplier, "schemaSupplier");
	}

	private static String validateName(@Nullable String name, String parameterName) {
		String checkedName = ErrorUtil.checkNotNull(name, parameterName);
		if (checkedName.isBlank()) {
			throw new IllegalArgumentException(parameterName + " must not be blank.");
		}
		return checkedName;
	}

	private static Supplier<ConfigScreenSchema> createScreenSchemaSupplier(Supplier<? extends IConfigSchema> schemaSupplier) {
		ErrorUtil.checkNotNull(schemaSupplier, "schemaSupplier");
		return () -> {
			IConfigSchema schema = schemaSupplier.get();
			if (schema == null) {
				throw new NullPointerException("schemaSupplier must not return null.");
			}
			return ConfigScreenSchema.from(schema);
		};
	}

	private record PublicConfigScreenConfig(IConfigScreenConfig configScreen) implements ConfigScreenConfig {
		@Override
		public String getModId() {
			return configScreen.getModId();
		}

		@Override
		public Component getTitle() {
			return configScreen.getTitle();
		}

		@Override
		public ConfigScreenSchema getSchema() {
			return ConfigScreenSchema.from(configScreen.getSchema());
		}
	}

	private record ConfigScreenFactoryConfig(
		Component title,
		Supplier<? extends ConfigScreenSchema> schemaSupplier
	) {

	}
}
