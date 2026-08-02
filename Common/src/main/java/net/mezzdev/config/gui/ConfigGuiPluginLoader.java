package net.mezzdev.config.gui;

import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.gui.api.ConfigRestartResult;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import net.mezzdev.config.gui.api.IConfigRestartHandler;
import net.mezzdev.config.gui.api.IConfigScreenBuilder;
import net.mezzdev.config.gui.api.IConfigScreenCategoryBuilder;
import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.mezzdev.config.gui.api.IConfigScreenConfig;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditorFactory;
import net.mezzdev.config.gui.api.ISortableConfigValueFactory;
import net.mezzdev.config.gui.model.ConfigValueChange;
import net.mezzdev.config.gui.keybindings.KeyMappingConfigValues;
import net.mezzdev.config.gui.util.ErrorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class ConfigGuiPluginLoader {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final String KEY_MAPPINGS_CATEGORY_NAME = "keyMappings";
	private static final ConfigScreenValueProvider DEFAULT_KEY_MAPPINGS_PROVIDER = (modId, allValues) -> KeyMappingConfigValues.createForModId(modId);

	private ConfigGuiPluginLoader() {

	}

	public static Map<String, IConfigScreenFactory> createScreenFactories(
		Collection<? extends IConfigScreenConfig> configScreens,
		List<? extends IConfigGuiPlugin> plugins
	) {
		return createScreenFactoriesFromInternalConfigs(
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
		Map<String, ConfigGuiRegistration> registrations = createConfigGuiRegistrations(configScreens);
		for (IConfigGuiPlugin plugin : plugins) {
			addPlugin(registrations, plugin);
		}
		Map<String, IConfigScreenFactory> factories = new LinkedHashMap<>();
		registrations.forEach((modId, registration) -> addFactory(factories, modId, registration));
		return Collections.unmodifiableMap(factories);
	}

	private static Map<String, ConfigGuiRegistration> createConfigGuiRegistrations(
		Collection<? extends ConfigScreenConfig> configScreens
	) {
		Map<String, ConfigGuiRegistration> registrations = new LinkedHashMap<>();
		for (ConfigScreenConfig configScreen : configScreens) {
			String modId = configScreen.getModId();
			ConfigGuiRegistration registration = new ConfigGuiRegistration(modId, configScreen);
			@Nullable
			ConfigGuiRegistration previous = registrations.putIfAbsent(modId, registration);
			if (previous != null) {
				LOGGER.error("Duplicate config screen for mod id: {}", modId);
			}
		}
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

	private static void addFactory(Map<String, IConfigScreenFactory> factories, String modId, IConfigScreenFactory factory) {
		@Nullable
		IConfigScreenFactory previous = factories.putIfAbsent(modId, factory);
		if (previous != null) {
			LOGGER.error("Duplicate config GUI plugin for mod id: {}", modId);
		}
	}

	private static void addFactory(Map<String, IConfigScreenFactory> factories, String modId, ConfigGuiRegistration registration) {
		try {
			Optional<IConfigScreenFactory> factory = registration.createFactory();
			factory.ifPresent(configScreenFactory -> addFactory(factories, modId, configScreenFactory));
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
			Supplier<? extends IConfigSchema> schemaSupplier,
			IConfigRestartHandler restartHandler
		) {
			if (config != null) {
				throw new IllegalStateException("A config screen has already been registered for mod id: " + modId);
			}
			config = new ConfigScreenFactoryConfig(title, createScreenSchemaSupplier(schemaSupplier), restartHandler);
		}

		public Optional<IConfigScreenFactory> createFactory() {
			ConfigScreenFactoryConfig config = getConfigScreenFactoryConfig();
			if (config == null) {
				return Optional.empty();
			}
			ConfigScreenBuilder screenBuilder = new ConfigScreenBuilder(modId, config.title(), config.restartHandler());
			for (Consumer<IConfigScreenBuilder> screenCustomizer : screenCustomizers) {
				screenCustomizer.accept(screenBuilder);
			}

			Supplier<? extends ConfigScreenSchema> schemaSupplier = config.schemaSupplier();
			List<ConfiguredScreenCategory> configuredCategories = screenBuilder.getCategories();
			Supplier<? extends ConfigScreenSchema> originalSchemaSupplier = schemaSupplier;
			schemaSupplier = () -> {
				ConfigScreenSchema schema = originalSchemaSupplier.get();
				if (schema == null) {
					throw new NullPointerException("schemaSupplier must not return null.");
				}
				return new CustomizedConfigScreenSchema(modId, schema, configuredCategories);
			};
			return Optional.of(createScreenFactory(
				modId,
				screenBuilder.getTitle(),
				schemaSupplier,
				screenBuilder.getRestartHandler(),
				valueEditorFactories
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
				configScreen::getSchema,
				configScreen::onRestartRequired
			);
		}
	}

	private static final class ConfigScreenBuilder implements IConfigScreenBuilder {
		private final String modId;
		private final Map<String, ConfigScreenCategoryBuilder> categories = new LinkedHashMap<>();
		private Component title;
		private IConfigRestartHandler restartHandler;

		public ConfigScreenBuilder(String modId, Component title, IConfigRestartHandler restartHandler) {
			this.modId = modId;
			this.title = title;
			this.restartHandler = restartHandler;
		}

		@Override
		public void setTitle(Component title) {
			this.title = ErrorUtil.checkNotNull(title, "title");
		}

		@Override
		public void setRestartHandler(IConfigRestartHandler restartHandler) {
			this.restartHandler = ErrorUtil.checkNotNull(restartHandler, "restartHandler");
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

		public IConfigRestartHandler getRestartHandler() {
			return restartHandler;
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
		private final List<ConfigScreenValueApplyModeOverride> applyModeOverrides = new ArrayList<>();
		private final List<ConfigScreenValueRestartRequirementOverride> restartRequirementOverrides = new ArrayList<>();
		@Nullable
		private Component title;
		@Nullable
		private Component description;
		@Nullable
		private ConfigValueApplyMode defaultApplyMode;
		private boolean ordered;
		private boolean containsKeyMappings;
		private boolean hasManualValues;

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
		public IConfigScreenCategoryBuilder setDefaultApplyMode(ConfigValueApplyMode applyMode) {
			this.defaultApplyMode = ErrorUtil.checkNotNull(applyMode, "applyMode");
			return this;
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
		public IConfigScreenCategoryBuilder setValueRequiresRestart(IConfigValue<?> value, boolean requiresRestart) {
			ConfigScreenValueMatcher valueMatcher = ConfigScreenValueMatcher.configValue(value);
			restartRequirementOverrides.add(new ConfigScreenValueRestartRequirementOverride(valueMatcher, requiresRestart));
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder setScreenValueRequiresRestart(IConfigScreenValue<?> value, boolean requiresRestart) {
			ConfigScreenValueMatcher valueMatcher = ConfigScreenValueMatcher.screenValue(value);
			restartRequirementOverrides.add(new ConfigScreenValueRestartRequirementOverride(valueMatcher, requiresRestart));
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder setValueRequiresRestartByName(String valueName, boolean requiresRestart) {
			ConfigScreenValueMatcher valueMatcher = ConfigScreenValueMatcher.named(valueName);
			restartRequirementOverrides.add(new ConfigScreenValueRestartRequirementOverride(valueMatcher, requiresRestart));
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder addScreenValue(IConfigScreenValue<?> value) {
			IConfigScreenValue<?> checkedValue = ErrorUtil.checkNotNull(value, "value");
			return addScreenValues(List.of(checkedValue));
		}

		@Override
		public IConfigScreenCategoryBuilder addScreenValues(Collection<? extends IConfigScreenValue<?>> values) {
			Collection<? extends IConfigScreenValue<?>> checkedValues = ErrorUtil.checkNotNull(values, "values");
			List<IConfigScreenValue<?>> valuesCopy = List.copyOf(checkedValues);
			return addValueProvider((modId, allValues) -> valuesCopy);
		}

		@Override
		public IConfigScreenCategoryBuilder addScreenValues(Supplier<? extends Collection<? extends IConfigScreenValue<?>>> valuesSupplier) {
			Supplier<? extends Collection<? extends IConfigScreenValue<?>>> checkedSupplier = ErrorUtil.checkNotNull(valuesSupplier, "valuesSupplier");
			return addValueProvider((modId, allValues) -> getConfigValues(checkedSupplier));
		}

		@Override
		public IConfigScreenCategoryBuilder hideScreenValue(IConfigScreenValue<?> value) {
			IConfigScreenValue<?> checkedValue = ErrorUtil.checkNotNull(value, "value");
			return hideScreenValues(List.of(checkedValue));
		}

		@Override
		public IConfigScreenCategoryBuilder hideScreenValues(Collection<? extends IConfigScreenValue<?>> values) {
			Collection<? extends IConfigScreenValue<?>> checkedValues = ErrorUtil.checkNotNull(values, "values");
			List<IConfigScreenValue<?>> valuesCopy = List.copyOf(checkedValues);
			return addHiddenValueProvider((modId, allValues) -> valuesCopy);
		}

		@Override
		public IConfigScreenCategoryBuilder hideScreenValues(Supplier<? extends Collection<? extends IConfigScreenValue<?>>> valuesSupplier) {
			Supplier<? extends Collection<? extends IConfigScreenValue<?>>> checkedSupplier = ErrorUtil.checkNotNull(valuesSupplier, "valuesSupplier");
			return addHiddenValueProvider((modId, allValues) -> getConfigValues(checkedSupplier));
		}

		@Override
		public IConfigScreenCategoryBuilder addValueByName(String valueName) {
			ConfigScreenValueMatcher valueMatcher = ConfigScreenValueMatcher.named(valueName);
			return addValueProvider((modId, allValues) -> findValue(modId, valueMatcher, allValues)
				.map(List::of)
				.orElseGet(List::of)
			);
		}

		@Override
		public IConfigScreenCategoryBuilder hideValueByName(String valueName) {
			ConfigScreenValueMatcher valueMatcher = ConfigScreenValueMatcher.named(valueName);
			return addHiddenValueProvider((modId, allValues) -> findValue(modId, valueMatcher, allValues)
				.map(List::of)
				.orElseGet(List::of)
			);
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
			return addValueProvider((modId, allValues) -> KeyMappingConfigValues.create(keyMappingsCopy));
		}

		@Override
		public IConfigScreenCategoryBuilder addKeyMappings(Supplier<? extends Collection<? extends KeyMapping>> keyMappingsSupplier) {
			Supplier<? extends Collection<? extends KeyMapping>> checkedSupplier = ErrorUtil.checkNotNull(keyMappingsSupplier, "keyMappingsSupplier");
			containsKeyMappings = true;
			return addValueProvider((modId, allValues) -> KeyMappingConfigValues.create(getKeyMappings(checkedSupplier)));
		}

		private IConfigScreenCategoryBuilder addValueProvider(ConfigScreenValueProvider valueProvider) {
			ConfigScreenValueProvider checkedValueProvider = ErrorUtil.checkNotNull(valueProvider, "valueProvider");
			hasManualValues = true;
			valueProviders.add(checkedValueProvider);
			return this;
		}

		private IConfigScreenCategoryBuilder addHiddenValueProvider(ConfigScreenValueProvider valueProvider) {
			ConfigScreenValueProvider checkedValueProvider = ErrorUtil.checkNotNull(valueProvider, "valueProvider");
			hiddenValueProviders.add(checkedValueProvider);
			return this;
		}

		public ConfiguredScreenCategory build() {
			return new ConfiguredScreenCategory(
				name,
				title,
				description,
				defaultApplyMode,
				ordered,
				hasManualValues,
				containsKeyMappings,
				valueProviders,
				hiddenValueProviders,
				applyModeOverrides,
				restartRequirementOverrides
			);
		}
	}

	private record ConfiguredScreenCategory(
		String name,
		@Nullable Component title,
		@Nullable Component description,
		@Nullable ConfigValueApplyMode defaultApplyMode,
		boolean ordered,
		boolean hasManualValues,
		boolean containsKeyMappings,
		List<ConfigScreenValueProvider> valueProviders,
		List<ConfigScreenValueProvider> hiddenValueProviders,
		List<ConfigScreenValueApplyModeOverride> applyModeOverrides,
		List<ConfigScreenValueRestartRequirementOverride> restartRequirementOverrides
	) {
		private ConfiguredScreenCategory {
			valueProviders = List.copyOf(valueProviders);
			hiddenValueProviders = List.copyOf(hiddenValueProviders);
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
				hasManualValues,
				containsKeyMappings,
				valueProviders,
				hiddenValueProviders,
				applyModeOverrides,
				restartRequirementOverrides
			);
		}
	}

	private record ConfigScreenValueApplyModeOverride(
		ConfigScreenValueMatcher valueMatcher,
		ConfigValueApplyMode applyMode
	) {

	}

	private record ConfigScreenValueRestartRequirementOverride(
		ConfigScreenValueMatcher valueMatcher,
		boolean requiresRestart
	) {

	}

	@FunctionalInterface
	private interface ConfigScreenValueMatcher {
		static ConfigScreenValueMatcher configValue(IConfigValue<?> configValue) {
			IConfigValue<?> checkedConfigValue = ErrorUtil.checkNotNull(configValue, "configValue");
			return new ConfigScreenValueMatcher() {
				@Override
				public boolean matches(IConfigScreenValue<?> value) {
					return value.getConfigValue()
						.filter(configValue -> configValue == checkedConfigValue)
						.isPresent();
				}

				@Override
				public String toString() {
					return checkedConfigValue.getName();
				}
			};
		}

		static ConfigScreenValueMatcher screenValue(IConfigScreenValue<?> configValue) {
			IConfigScreenValue<?> checkedConfigValue = ErrorUtil.checkNotNull(configValue, "configValue");
			return new ConfigScreenValueMatcher() {
				@Override
				public boolean matches(IConfigScreenValue<?> value) {
					return checkedConfigValue == value;
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
				public boolean matches(IConfigScreenValue<?> value) {
					return value.getName().equals(checkedName);
				}

				@Override
				public String toString() {
					return checkedName;
				}
			};
		}

		boolean matches(IConfigScreenValue<?> value);
	}

	private static final class CustomizedConfigScreenSchema implements ConfigScreenSchema {
		private final String modId;
		private final ConfigScreenSchema schema;
		private final List<ConfiguredScreenCategory> configuredCategories;

		public CustomizedConfigScreenSchema(
			String modId,
			ConfigScreenSchema schema,
			List<ConfiguredScreenCategory> configuredCategories
		) {
			this.modId = modId;
			this.schema = schema;
			this.configuredCategories = List.copyOf(configuredCategories);
		}

		@Override
		public List<? extends ConfigScreenCategory> getCategories() {
			return createScreenCategories(modId, schema.getCategories(), configuredCategories);
		}
	}

	private static List<ConfigScreenCategory> createScreenCategories(
		String modId,
		List<? extends ConfigScreenCategory> originalCategories,
		List<ConfiguredScreenCategory> configuredCategories
	) {
		return createScreenCategories(
			modId,
			originalCategories,
			configuredCategories,
			DEFAULT_KEY_MAPPINGS_PROVIDER
		);
	}

	static List<ConfigScreenCategory> createScreenCategoriesForTests(
		String modId,
		List<? extends ConfigScreenCategory> originalCategories,
		Consumer<IConfigScreenBuilder> screenCustomizer,
		ConfigScreenValueProvider defaultKeyMappingsProvider
	) {
		ConfigScreenBuilder screenBuilder = new ConfigScreenBuilder(modId, Component.empty(), () -> ConfigRestartResult.HANDLED);
		screenCustomizer.accept(screenBuilder);
		return createScreenCategories(
			modId,
			originalCategories,
			screenBuilder.getCategories(),
			defaultKeyMappingsProvider
		);
	}

	private static List<ConfigScreenCategory> createScreenCategories(
		String modId,
		List<? extends ConfigScreenCategory> originalCategories,
		List<ConfiguredScreenCategory> configuredCategories,
		ConfigScreenValueProvider defaultKeyMappingsProvider
	) {
		configuredCategories = addDefaultKeyMappingsCategory(configuredCategories, defaultKeyMappingsProvider);
		boolean hasManualScreenLayout = hasManualScreenLayout(configuredCategories);
		List<ResolvedScreenCategory> resolvedCategories = originalCategories.stream()
			.map(ConfigGuiPluginLoader::resolve)
			.toList();
		Optional<String> categoryLocalizationPrefix = getCategoryLocalizationPrefix(resolvedCategories);
		List<IConfigScreenValue<?>> allValues = getAllValues(resolvedCategories);
		Set<IConfigScreenValue<?>> usedValues = new HashSet<>();
		Set<String> emittedCategoryNames = new HashSet<>();
		List<ConfigScreenCategory> screenCategories = new ArrayList<>();
		for (ConfiguredScreenCategory configuredCategory : configuredCategories) {
			if (configuredCategory.ordered()) {
				addConfiguredScreenCategory(
					modId,
					categoryLocalizationPrefix,
					resolvedCategories,
					allValues,
					usedValues,
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
					allValues,
					usedValues,
					screenCategories,
					configuredCategory.get()
				);
				emittedCategoryNames.add(originalCategory.name());
				continue;
			}
			if (hasManualScreenLayout) {
				continue;
			}
			List<IConfigScreenValue<?>> remainingValues = getRemainingValues(originalCategory, usedValues);
			if (!remainingValues.isEmpty()) {
				screenCategories.add(new ConfiguredScreenConfigCategory(
					originalCategory.name(),
					originalCategory.title(),
					originalCategory.description(),
					remainingValues
				));
			}
		}
		for (ConfiguredScreenCategory configuredCategory : configuredCategories) {
			if (emittedCategoryNames.add(configuredCategory.name())) {
				addConfiguredScreenCategory(
					modId,
					categoryLocalizationPrefix,
					resolvedCategories,
					allValues,
					usedValues,
					screenCategories,
					configuredCategory
				);
			}
		}
		return List.copyOf(screenCategories);
	}

	private static List<ConfiguredScreenCategory> addDefaultKeyMappingsCategory(
		List<ConfiguredScreenCategory> configuredCategories,
		ConfigScreenValueProvider defaultKeyMappingsProvider
	) {
		if (hasConfiguredKeyMappings(configuredCategories)) {
			return configuredCategories;
		}
		List<ConfiguredScreenCategory> categories = new ArrayList<>(configuredCategories);
		for (int i = 0; i < categories.size(); i++) {
			ConfiguredScreenCategory category = categories.get(i);
			if (category.name().equals(KEY_MAPPINGS_CATEGORY_NAME)) {
				categories.set(i, category.withDefaultKeyMappings(defaultKeyMappingsProvider));
				return categories;
			}
		}
		categories.add(ConfiguredScreenCategory.createDefaultKeyMappingsCategory(defaultKeyMappingsProvider));
		return categories;
	}

	private static boolean hasConfiguredKeyMappings(List<ConfiguredScreenCategory> configuredCategories) {
		return configuredCategories.stream()
			.anyMatch(ConfiguredScreenCategory::containsKeyMappings);
	}

	private static boolean hasManualScreenLayout(List<ConfiguredScreenCategory> configuredCategories) {
		return configuredCategories.stream()
			.anyMatch(configuredCategory -> configuredCategory.ordered() || configuredCategory.hasManualValues());
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
		List<IConfigScreenValue<?>> allValues,
		Set<IConfigScreenValue<?>> usedValues,
		List<ConfigScreenCategory> screenCategories,
		ConfiguredScreenCategory configuredCategory
	) {
		List<IConfigScreenValue<?>> values = new ArrayList<>();
		addHiddenConfiguredValues(modId, allValues, usedValues, configuredCategory);
		boolean includeOriginalValues = !configuredCategory.hasManualValues();
		if (includeOriginalValues && !configuredCategory.ordered()) {
			addRemainingOriginalValues(configuredCategory.name(), resolvedCategories, usedValues, values);
		}
		for (ConfigScreenValueProvider valueProvider : configuredCategory.valueProviders()) {
			for (IConfigScreenValue<?> configValue : valueProvider.getValues(modId, allValues)) {
				addConfiguredValue(modId, usedValues, values, configValue);
			}
		}
		if (includeOriginalValues && configuredCategory.ordered()) {
			addRemainingOriginalValues(configuredCategory.name(), resolvedCategories, usedValues, values);
		}
		values = applyConfiguredValueSettings(modId, configuredCategory, values);
		if (!values.isEmpty()) {
			screenCategories.add(new ConfiguredScreenConfigCategory(
				configuredCategory.name(),
				getCategoryTitle(modId, categoryLocalizationPrefix, resolvedCategories, configuredCategory),
				getCategoryDescription(modId, categoryLocalizationPrefix, resolvedCategories, configuredCategory),
				values
			));
		}
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
			boolean requiresRestart = getConfiguredRestartRequirement(modId, configuredCategory, value, values);
			configuredValues.add(withValueSettings(value, applyMode, requiresRestart));
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
			if (valueMatcher.matches(value)) {
				validateApplyModeMatcher(modId, configuredCategory, valueMatcher, values);
				applyMode = applyModeOverride.applyMode();
			}
		}
		if (applyMode == null) {
			return value.getApplyMode();
		}
		return applyMode;
	}

	private static boolean getConfiguredRestartRequirement(
		String modId,
		ConfiguredScreenCategory configuredCategory,
		IConfigScreenValue<?> value,
		List<IConfigScreenValue<?>> values
	) {
		boolean requiresRestart = value.requiresRestart();
		for (ConfigScreenValueRestartRequirementOverride restartRequirementOverride : configuredCategory.restartRequirementOverrides()) {
			ConfigScreenValueMatcher valueMatcher = restartRequirementOverride.valueMatcher();
			if (valueMatcher.matches(value)) {
				validateRestartRequirementMatcher(modId, configuredCategory, valueMatcher, values);
				requiresRestart = restartRequirementOverride.requiresRestart();
			}
		}
		return requiresRestart;
	}

	private static void validateApplyModeMatcher(
		String modId,
		ConfiguredScreenCategory configuredCategory,
		ConfigScreenValueMatcher valueMatcher,
		List<IConfigScreenValue<?>> values
	) {
		long matchCount = values.stream()
			.filter(valueMatcher::matches)
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
			.filter(valueMatcher::matches)
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
		boolean requiresRestart
	) {
		IConfigScreenValue<T> configuredValue = value;
		if (applyMode != value.getApplyMode()) {
			configuredValue = IConfigScreenValue.withApplyMode(configuredValue, applyMode);
		}
		if (requiresRestart != value.requiresRestart()) {
			configuredValue = IConfigScreenValue.withRestartRequirement(configuredValue, requiresRestart);
		}
		return configuredValue;
	}

	private static void addHiddenConfiguredValues(
		String modId,
		List<IConfigScreenValue<?>> allValues,
		Set<IConfigScreenValue<?>> usedValues,
		ConfiguredScreenCategory configuredCategory
	) {
		for (ConfigScreenValueProvider valueProvider : configuredCategory.hiddenValueProviders()) {
			for (IConfigScreenValue<?> configValue : valueProvider.getValues(modId, allValues)) {
				usedValues.add(configValue);
			}
		}
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
				return Component.translatableWithFallback(localizationKey, getDisplayNameFallback(configuredCategory.name()));
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

	private static String getDisplayNameFallback(String name) {
		String[] words = name
			.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
			.replace('-', '_')
			.replace('.', '_')
			.split("_+");
		StringBuilder result = new StringBuilder();
		for (String word : words) {
			if (word.isBlank()) {
				continue;
			}
			if (!result.isEmpty()) {
				result.append(' ');
			}
			String lowercaseWord = word.toLowerCase(Locale.ROOT);
			result.append(Character.toUpperCase(lowercaseWord.charAt(0)));
			if (lowercaseWord.length() > 1) {
				result.append(lowercaseWord.substring(1));
			}
		}
		if (result.isEmpty()) {
			return name;
		}
		return result.toString();
	}

	private static void addConfiguredValue(
		String modId,
		Set<IConfigScreenValue<?>> usedValues,
		List<IConfigScreenValue<?>> values,
		IConfigScreenValue<?> configValue
	) {
		if (!usedValues.add(configValue)) {
			LOGGER.error("Duplicate config value in configured screen categories for mod id: {}, value: {}", modId, configValue.getName());
			return;
		}
		values.add(configValue);
	}

	private static void addRemainingOriginalValues(
		String categoryName,
		List<ResolvedScreenCategory> originalCategories,
		Set<IConfigScreenValue<?>> usedValues,
		List<IConfigScreenValue<?>> values
	) {
		for (ResolvedScreenCategory originalCategory : originalCategories) {
			if (!originalCategory.name().equals(categoryName)) {
				continue;
			}
			for (IConfigScreenValue<?> value : originalCategory.values()) {
				if (usedValues.add(value)) {
					values.add(value);
				}
			}
		}
	}

	private static List<IConfigScreenValue<?>> getRemainingValues(ResolvedScreenCategory originalCategory, Set<IConfigScreenValue<?>> usedValues) {
		List<IConfigScreenValue<?>> remainingValues = new ArrayList<>();
		for (IConfigScreenValue<?> value : originalCategory.values()) {
			if (usedValues.add(value)) {
				remainingValues.add(value);
			}
		}
		return remainingValues;
	}

	private static List<IConfigScreenValue<?>> getAllValues(List<ResolvedScreenCategory> screenCategories) {
		List<IConfigScreenValue<?>> values = new ArrayList<>();
		for (ResolvedScreenCategory screenCategory : screenCategories) {
			values.addAll(screenCategory.values());
		}
		return List.copyOf(values);
	}

	private static Optional<IConfigScreenValue<?>> findValue(
		String modId,
		ConfigScreenValueMatcher valueMatcher,
		List<IConfigScreenValue<?>> values
	) {
		@Nullable
		IConfigScreenValue<?> result = null;
		for (IConfigScreenValue<?> value : values) {
			if (valueMatcher.matches(value)) {
				if (result != null) {
					LOGGER.error("Config value matcher matched multiple values for mod id: {}, value matcher: {}", modId, valueMatcher);
					return Optional.of(result);
				}
				result = value;
			}
		}
		return Optional.ofNullable(result);
	}

	private static ResolvedScreenCategory resolve(ConfigScreenCategory category) {
		return new ResolvedScreenCategory(
			category.getName(),
			category.getLocalizedName(),
			category.getLocalizedDescription(),
			List.copyOf(category.getConfigValues())
		);
	}

	private record ResolvedScreenCategory(
		String name,
		Component title,
		Component description,
		List<IConfigScreenValue<?>> values
	) {
		private ResolvedScreenCategory {
			values = List.copyOf(values);
		}
	}

	private record ConfiguredScreenConfigCategory(
		String name,
		Component title,
		Component description,
		List<IConfigScreenValue<?>> values
	) implements ConfigScreenCategory {
		private ConfiguredScreenConfigCategory {
			values = List.copyOf(values);
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

	private static List<IConfigScreenValue<?>> getConfigValues(Supplier<? extends Collection<? extends IConfigScreenValue<?>>> valuesSupplier) {
		Collection<? extends IConfigScreenValue<?>> values = valuesSupplier.get();
		if (values == null) {
			throw new NullPointerException("valuesSupplier must not return null.");
		}
		return List.copyOf(values);
	}

	private static List<KeyMapping> getKeyMappings(Supplier<? extends Collection<? extends KeyMapping>> keyMappingsSupplier) {
		Collection<? extends KeyMapping> keyMappings = keyMappingsSupplier.get();
		if (keyMappings == null) {
			throw new NullPointerException("keyMappingsSupplier must not return null.");
		}
		return List.copyOf(keyMappings);
	}

	@FunctionalInterface
	interface ConfigScreenValueProvider {
		List<? extends IConfigScreenValue<?>> getValues(String modId, List<IConfigScreenValue<?>> allValues);
	}

	private static IConfigScreenFactory createScreenFactory(
		String modId,
		Component title,
		Supplier<? extends ConfigScreenSchema> schemaSupplier,
		IConfigRestartHandler restartHandler,
		Map<ConfigValueEditorType<?>, IConfigValueEditorFactory<?>> valueEditorFactories
	) {
		validateConfigScreenFactoryInputs(title, schemaSupplier, restartHandler);
		Map<ConfigValueEditorType<?>, IConfigValueEditorFactory<?>> valueEditorFactoriesCopy = Map.copyOf(valueEditorFactories);
		return parent -> {
			ConfigScreenSchema schema = schemaSupplier.get();
			if (schema == null) {
				throw new NullPointerException("schemaSupplier must not return null.");
			}
			ConfigChangesHandler changesHandler = createChangesHandler(modId, title, restartHandler);
			return ConfigScreen.create(parent, title, schema, changesHandler, valueEditorFactoriesCopy);
		};
	}

	private static ConfigChangesHandler createChangesHandler(
		String modId,
		Component title,
		IConfigRestartHandler restartHandler
	) {
		return changes -> {
			boolean requiresRestart = applyChanges(changes);
			if (requiresRestart) {
				ConfigRestartResult restartResult = ErrorUtil.checkNotNull(restartHandler.onRestartRequired(), "restartHandler result");
				if (restartResult == ConfigRestartResult.NEXT_GAME_START) {
					notifyRestartDeferred(modId, title);
				}
			}
			return requiresRestart;
		};
	}

	private static boolean applyChanges(List<ConfigValueChange<?>> changes) {
		boolean requiresRestart = false;
		for (ConfigValueChange<?> change : changes) {
			if (change.apply()) {
				requiresRestart |= change.configValue().requiresRestart();
			}
		}
		return requiresRestart;
	}

	private static void notifyRestartDeferred(String modId, Component title) {
		Component message = Component.translatable("mezz_config.config.screen.restart.nextGameStart", title);
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(message, false);
		}
		LOGGER.info("Config changes for {} were saved and will be applied the next time the game is started.", modId);
	}

	private static void validateConfigScreenFactoryInputs(
		Component title,
		Supplier<? extends ConfigScreenSchema> schemaSupplier,
		IConfigRestartHandler restartHandler
	) {
		ErrorUtil.checkNotNull(title, "title");
		ErrorUtil.checkNotNull(schemaSupplier, "schemaSupplier");
		ErrorUtil.checkNotNull(restartHandler, "restartHandler");
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

		@Override
		public ConfigRestartResult onRestartRequired() {
			return configScreen.onRestartRequired();
		}
	}

	private record ConfigScreenFactoryConfig(
		Component title,
		Supplier<? extends ConfigScreenSchema> schemaSupplier,
		IConfigRestartHandler restartHandler
	) {

	}
}
