package net.mezzdev.config.gui;

import net.mezzdev.config.gui.api.ConfigRestartResult;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import net.mezzdev.config.gui.api.IConfigRestartHandler;
import net.mezzdev.config.gui.api.IConfigScreenBuilder;
import net.mezzdev.config.gui.api.IConfigScreenCategoryBuilder;
import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.mezzdev.config.gui.api.IConfigScreenConfig;
import net.mezzdev.config.gui.api.IConfigScreenValueReference;
import net.mezzdev.config.gui.api.IConfigValueEditorFactory;
import net.mezzdev.config.gui.api.ISortableConfigValueFactory;
import net.mezzdev.config.api.schema.IConfigCategory;
import net.mezzdev.config.api.schema.IConfigEditableSchema;
import net.mezzdev.config.api.value.ConfigValueChange;
import net.mezzdev.config.api.value.ConfigValueEditorType;
import net.mezzdev.config.api.value.ConfigValueUpdateType;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.gui.keybindings.KeyMappingConfigValues;
import net.mezzdev.config.gui.util.ErrorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
		Map<String, ConfigGuiRegistration> registrations = createConfigGuiRegistrations(configScreens);
		for (IConfigGuiPlugin plugin : plugins) {
			addPlugin(registrations, plugin);
		}
		Map<String, IConfigScreenFactory> factories = new LinkedHashMap<>();
		registrations.forEach((modId, registration) -> addFactory(factories, modId, registration));
		return Collections.unmodifiableMap(factories);
	}

	private static Map<String, ConfigGuiRegistration> createConfigGuiRegistrations(
		Collection<? extends IConfigScreenConfig> configScreens
	) {
		Map<String, ConfigGuiRegistration> registrations = new LinkedHashMap<>();
		for (IConfigScreenConfig configScreen : configScreens) {
			String modId = configScreen.getModId();
			ConfigGuiRegistration registration = new ConfigGuiRegistration(modId, configScreen);
			@Nullable ConfigGuiRegistration previous = registrations.putIfAbsent(modId, registration);
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
		@Nullable IConfigScreenFactory previous = factories.putIfAbsent(modId, factory);
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
		private final IConfigScreenConfig configScreen;

		@Nullable
		private ConfigScreenFactoryConfig config;

		private ConfigGuiRegistration(String modId) {
			this(modId, null);
		}

		private ConfigGuiRegistration(String modId, @Nullable IConfigScreenConfig configScreen) {
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
			Supplier<? extends IConfigEditableSchema> schemaSupplier,
			IConfigRestartHandler restartHandler
		) {
			if (config != null) {
				throw new IllegalStateException("A config screen has already been registered for mod id: " + modId);
			}
			config = new ConfigScreenFactoryConfig(title, schemaSupplier, restartHandler);
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

			Supplier<? extends IConfigEditableSchema> schemaSupplier = config.schemaSupplier();
			List<ConfiguredScreenCategory> configuredCategories = screenBuilder.getCategories();
			Supplier<? extends IConfigEditableSchema> originalSchemaSupplier = schemaSupplier;
			schemaSupplier = () -> {
				IConfigEditableSchema schema = originalSchemaSupplier.get();
				if (schema == null) {
					throw new NullPointerException("schemaSupplier must not return null.");
				}
				return new CustomizedConfigEditableSchema(modId, schema, configuredCategories);
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
			@Nullable ConfigScreenFactoryConfig config = this.config;
			if (config != null) {
				return config;
			}
			@Nullable IConfigScreenConfig configScreen = this.configScreen;
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
		@Nullable
		private Component title;
		@Nullable
		private Component description;
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
		public IConfigScreenCategoryBuilder addValue(IConfigValue<?> value) {
			IConfigValue<?> checkedValue = ErrorUtil.checkNotNull(value, "value");
			return addValues(List.of(checkedValue));
		}

		@Override
		public IConfigScreenCategoryBuilder addValues(Collection<? extends IConfigValue<?>> values) {
			Collection<? extends IConfigValue<?>> checkedValues = ErrorUtil.checkNotNull(values, "values");
			List<IConfigValue<?>> valuesCopy = List.copyOf(checkedValues);
			return addValueProvider((modId, allValues) -> valuesCopy);
		}

		@Override
		public IConfigScreenCategoryBuilder addValues(Supplier<? extends Collection<? extends IConfigValue<?>>> valuesSupplier) {
			Supplier<? extends Collection<? extends IConfigValue<?>>> checkedSupplier = ErrorUtil.checkNotNull(valuesSupplier, "valuesSupplier");
			return addValueProvider((modId, allValues) -> getConfigValues(checkedSupplier));
		}

		@Override
		public IConfigScreenCategoryBuilder hideValue(IConfigValue<?> value) {
			IConfigValue<?> checkedValue = ErrorUtil.checkNotNull(value, "value");
			return hideValues(List.of(checkedValue));
		}

		@Override
		public IConfigScreenCategoryBuilder hideValues(Collection<? extends IConfigValue<?>> values) {
			Collection<? extends IConfigValue<?>> checkedValues = ErrorUtil.checkNotNull(values, "values");
			List<IConfigValue<?>> valuesCopy = List.copyOf(checkedValues);
			return addHiddenValueProvider((modId, allValues) -> valuesCopy);
		}

		@Override
		public IConfigScreenCategoryBuilder hideValues(Supplier<? extends Collection<? extends IConfigValue<?>>> valuesSupplier) {
			Supplier<? extends Collection<? extends IConfigValue<?>>> checkedSupplier = ErrorUtil.checkNotNull(valuesSupplier, "valuesSupplier");
			return addHiddenValueProvider((modId, allValues) -> getConfigValues(checkedSupplier));
		}

		@Override
		public IConfigScreenCategoryBuilder addValueReference(IConfigScreenValueReference valueReference) {
			IConfigScreenValueReference checkedValueReference = ErrorUtil.checkNotNull(valueReference, "valueReference");
			return addValueProvider((modId, allValues) ->
				findValue(modId, checkedValueReference, allValues)
					.map(List::of)
					.orElseGet(List::of)
			);
		}

		@Override
		public IConfigScreenCategoryBuilder hideValueReference(IConfigScreenValueReference valueReference) {
			IConfigScreenValueReference checkedValueReference = ErrorUtil.checkNotNull(valueReference, "valueReference");
			return addHiddenValueProvider((modId, allValues) ->
				findValue(modId, checkedValueReference, allValues)
					.map(List::of)
					.orElseGet(List::of)
			);
		}

		@Override
		public IConfigScreenCategoryBuilder addValueReferences(Collection<? extends IConfigScreenValueReference> valueReferences) {
			Collection<? extends IConfigScreenValueReference> checkedValueReferences = ErrorUtil.checkNotNull(valueReferences, "valueReferences");
			for (IConfigScreenValueReference valueReference : checkedValueReferences) {
				addValueReference(valueReference);
			}
			return this;
		}

		@Override
		public IConfigScreenCategoryBuilder hideValueReferences(Collection<? extends IConfigScreenValueReference> valueReferences) {
			Collection<? extends IConfigScreenValueReference> checkedValueReferences = ErrorUtil.checkNotNull(valueReferences, "valueReferences");
			for (IConfigScreenValueReference valueReference : checkedValueReferences) {
				hideValueReference(valueReference);
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
			return new ConfiguredScreenCategory(name, title, description, ordered, hasManualValues, containsKeyMappings, valueProviders, hiddenValueProviders);
		}
	}

	private record ConfiguredScreenCategory(
		String name,
		@Nullable
		Component title,
		@Nullable
		Component description,
		boolean ordered,
		boolean hasManualValues,
		boolean containsKeyMappings,
		List<ConfigScreenValueProvider> valueProviders,
		List<ConfigScreenValueProvider> hiddenValueProviders
	) {
		private ConfiguredScreenCategory {
			valueProviders = List.copyOf(valueProviders);
			hiddenValueProviders = List.copyOf(hiddenValueProviders);
		}

		private static ConfiguredScreenCategory createDefaultKeyMappingsCategory(ConfigScreenValueProvider defaultKeyMappingsProvider) {
			return new ConfiguredScreenCategory(
				KEY_MAPPINGS_CATEGORY_NAME,
				null,
				null,
				false,
				false,
				true,
				List.of(defaultKeyMappingsProvider),
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
				ordered,
				hasManualValues,
				containsKeyMappings,
				valueProviders,
				hiddenValueProviders
			);
		}
	}

	private static final class CustomizedConfigEditableSchema implements IConfigEditableSchema {
		private final String modId;
		private final IConfigEditableSchema schema;
		private final Set<IConfigValue<?>> schemaValues;
		private final List<ConfiguredScreenCategory> configuredCategories;

		public CustomizedConfigEditableSchema(
			String modId,
			IConfigEditableSchema schema,
			List<ConfiguredScreenCategory> configuredCategories
		) {
			this.modId = modId;
			this.schema = schema;
			this.schemaValues = getSchemaValues(schema);
			this.configuredCategories = List.copyOf(configuredCategories);
		}

		@Override
		public Path getPath() {
			return schema.getPath();
		}

		@Override
		public List<? extends IConfigCategory> getCategories() {
			return createScreenCategories(modId, schema.getCategories(), configuredCategories);
		}

		@Override
		public void clearListeners() {
			schema.clearListeners();
		}

		@Override
		public ConfigValueUpdateType getUpdateType(List<ConfigValueChange<?>> changes) {
			ConfigValueUpdateType updateType = schema.getUpdateType(getSchemaChanges(changes));
			for (ConfigValueChange<?> change : getScreenChanges(changes)) {
				updateType = max(updateType, change.configValue().getUpdateType());
			}
			return updateType;
		}

		@Override
		public ConfigValueUpdateType applyChanges(List<ConfigValueChange<?>> changes) {
			ConfigValueUpdateType updateType = schema.applyChanges(getSchemaChanges(changes));
			for (ConfigValueChange<?> change : getScreenChanges(changes)) {
				if (applyChange(change)) {
					updateType = max(updateType, change.configValue().getUpdateType());
				}
			}
			return updateType;
		}

		private List<ConfigValueChange<?>> getSchemaChanges(List<ConfigValueChange<?>> changes) {
			List<ConfigValueChange<?>> schemaChanges = new ArrayList<>();
			for (ConfigValueChange<?> change : changes) {
				if (schemaValues.contains(change.configValue())) {
					schemaChanges.add(change);
				}
			}
			return schemaChanges;
		}

		private List<ConfigValueChange<?>> getScreenChanges(List<ConfigValueChange<?>> changes) {
			List<ConfigValueChange<?>> screenChanges = new ArrayList<>();
			for (ConfigValueChange<?> change : changes) {
				if (!schemaValues.contains(change.configValue())) {
					screenChanges.add(change);
				}
			}
			return screenChanges;
		}
	}

	private static List<IConfigCategory> createScreenCategories(
		String modId,
		List<? extends IConfigCategory> originalCategories,
		List<ConfiguredScreenCategory> configuredCategories
	) {
		return createScreenCategories(
			modId,
			originalCategories,
			configuredCategories,
			DEFAULT_KEY_MAPPINGS_PROVIDER
		);
	}

	static List<IConfigCategory> createScreenCategoriesForTests(
		String modId,
		List<? extends IConfigCategory> originalCategories,
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

	private static List<IConfigCategory> createScreenCategories(
		String modId,
		List<? extends IConfigCategory> originalCategories,
		List<ConfiguredScreenCategory> configuredCategories,
		ConfigScreenValueProvider defaultKeyMappingsProvider
	) {
		configuredCategories = addDefaultKeyMappingsCategory(configuredCategories, defaultKeyMappingsProvider);
		boolean hasManualScreenLayout = hasManualScreenLayout(configuredCategories);
		List<ResolvedScreenCategory> resolvedCategories = originalCategories.stream()
			.map(ConfigGuiPluginLoader::resolve)
			.toList();
		Optional<String> categoryLocalizationPrefix = getCategoryLocalizationPrefix(resolvedCategories);
		List<IConfigValue<?>> allValues = getAllValues(resolvedCategories);
		Set<IConfigValue<?>> usedValues = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<String> emittedCategoryNames = new HashSet<>();
		List<IConfigCategory> screenCategories = new ArrayList<>();
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
			List<IConfigValue<?>> remainingValues = getRemainingValues(originalCategory, usedValues);
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
		List<IConfigValue<?>> allValues,
		Set<IConfigValue<?>> usedValues,
		List<IConfigCategory> screenCategories,
		ConfiguredScreenCategory configuredCategory
	) {
		List<IConfigValue<?>> values = new ArrayList<>();
		addHiddenConfiguredValues(modId, allValues, usedValues, configuredCategory);
		boolean includeOriginalValues = !configuredCategory.hasManualValues();
		if (includeOriginalValues && !configuredCategory.ordered()) {
			addRemainingOriginalValues(configuredCategory.name(), resolvedCategories, usedValues, values);
		}
		for (ConfigScreenValueProvider valueProvider : configuredCategory.valueProviders()) {
			for (IConfigValue<?> configValue : valueProvider.getValues(modId, allValues)) {
				addConfiguredValue(modId, usedValues, values, configValue);
			}
		}
		if (includeOriginalValues && configuredCategory.ordered()) {
			addRemainingOriginalValues(configuredCategory.name(), resolvedCategories, usedValues, values);
		}
		if (!values.isEmpty()) {
			screenCategories.add(new ConfiguredScreenConfigCategory(
				configuredCategory.name(),
				getCategoryTitle(modId, categoryLocalizationPrefix, resolvedCategories, configuredCategory),
				getCategoryDescription(modId, categoryLocalizationPrefix, resolvedCategories, configuredCategory),
				values
			));
		}
	}

	private static void addHiddenConfiguredValues(
		String modId,
		List<IConfigValue<?>> allValues,
		Set<IConfigValue<?>> usedValues,
		ConfiguredScreenCategory configuredCategory
	) {
		for (ConfigScreenValueProvider valueProvider : configuredCategory.hiddenValueProviders()) {
			for (IConfigValue<?> configValue : valueProvider.getValues(modId, allValues)) {
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
		Set<IConfigValue<?>> usedValues,
		List<IConfigValue<?>> values,
		IConfigValue<?> configValue
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
		Set<IConfigValue<?>> usedValues,
		List<IConfigValue<?>> values
	) {
		for (ResolvedScreenCategory originalCategory : originalCategories) {
			if (!originalCategory.name().equals(categoryName)) {
				continue;
			}
			for (IConfigValue<?> value : originalCategory.values()) {
				if (usedValues.add(value)) {
					values.add(value);
				}
			}
		}
	}

	private static List<IConfigValue<?>> getRemainingValues(ResolvedScreenCategory originalCategory, Set<IConfigValue<?>> usedValues) {
		List<IConfigValue<?>> remainingValues = new ArrayList<>();
		for (IConfigValue<?> value : originalCategory.values()) {
			if (usedValues.add(value)) {
				remainingValues.add(value);
			}
		}
		return remainingValues;
	}

	private static List<IConfigValue<?>> getAllValues(List<ResolvedScreenCategory> screenCategories) {
		List<IConfigValue<?>> values = new ArrayList<>();
		for (ResolvedScreenCategory screenCategory : screenCategories) {
			values.addAll(screenCategory.values());
		}
		return List.copyOf(values);
	}

	private static Optional<IConfigValue<?>> findValue(
		String modId,
		IConfigScreenValueReference valueReference,
		List<IConfigValue<?>> values
	) {
		@Nullable IConfigValue<?> result = null;
		for (IConfigValue<?> value : values) {
			if (valueReference.matches(value)) {
				if (result != null) {
					LOGGER.error("Config value reference matched multiple values for mod id: {}, value reference: {}", modId, valueReference);
					return Optional.of(result);
				}
				result = value;
			}
		}
		return Optional.ofNullable(result);
	}

	private static ResolvedScreenCategory resolve(IConfigCategory category) {
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
		List<IConfigValue<?>> values
	) {
		private ResolvedScreenCategory {
			values = List.copyOf(values);
		}
	}

	private record ConfiguredScreenConfigCategory(
		String name,
		Component title,
		Component description,
		List<IConfigValue<?>> values
	) implements IConfigCategory {
		private ConfiguredScreenConfigCategory {
			values = List.copyOf(values);
		}

		@Override
		public String getName() {
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
		public Collection<? extends IConfigValue<?>> getConfigValues() {
			return values;
		}
	}

	private static List<IConfigValue<?>> getConfigValues(Supplier<? extends Collection<? extends IConfigValue<?>>> valuesSupplier) {
		Collection<? extends IConfigValue<?>> values = valuesSupplier.get();
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
		List<? extends IConfigValue<?>> getValues(String modId, List<IConfigValue<?>> allValues);
	}

	private static IConfigScreenFactory createScreenFactory(
		String modId,
		Component title,
		Supplier<? extends IConfigEditableSchema> schemaSupplier,
		IConfigRestartHandler restartHandler,
		Map<ConfigValueEditorType<?>, IConfigValueEditorFactory<?>> valueEditorFactories
	) {
		validateConfigScreenFactoryInputs(title, schemaSupplier, restartHandler);
		Map<ConfigValueEditorType<?>, IConfigValueEditorFactory<?>> valueEditorFactoriesCopy = Map.copyOf(valueEditorFactories);
		return parent -> {
			IConfigEditableSchema schema = schemaSupplier.get();
			if (schema == null) {
				throw new NullPointerException("schemaSupplier must not return null.");
			}
			ConfigChangesHandler changesHandler = createChangesHandler(modId, title, schema, restartHandler);
			return ConfigScreen.create(parent, title, schema, changesHandler, valueEditorFactoriesCopy);
		};
	}

	private static ConfigChangesHandler createChangesHandler(
		String modId,
		Component title,
		IConfigEditableSchema schema,
		IConfigRestartHandler restartHandler
	) {
		return changes -> {
			ConfigValueUpdateType updateType = schema.applyChanges(changes);
			if (updateType == ConfigValueUpdateType.RESTART) {
				ConfigRestartResult restartResult = ErrorUtil.checkNotNull(restartHandler.onRestartRequired(), "restartHandler result");
				if (restartResult == ConfigRestartResult.NEXT_GAME_START) {
					notifyRestartDeferred(modId, title);
				}
			}
			return updateType;
		};
	}

	private static Set<IConfigValue<?>> getSchemaValues(IConfigEditableSchema schema) {
		Set<IConfigValue<?>> result = Collections.newSetFromMap(new IdentityHashMap<>());
		for (IConfigCategory category : schema.getCategories()) {
			result.addAll(category.getConfigValues());
		}
		return result;
	}

	private static <T> boolean applyChange(ConfigValueChange<T> change) {
		return change.configValue().set(change.value());
	}

	private static ConfigValueUpdateType max(ConfigValueUpdateType first, ConfigValueUpdateType second) {
		if (first == ConfigValueUpdateType.RESTART || second == ConfigValueUpdateType.RESTART) {
			return ConfigValueUpdateType.RESTART;
		}
		if (first == ConfigValueUpdateType.ON_APPLY || second == ConfigValueUpdateType.ON_APPLY) {
			return ConfigValueUpdateType.ON_APPLY;
		}
		return ConfigValueUpdateType.IMMEDIATE;
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
		Supplier<? extends IConfigEditableSchema> schemaSupplier,
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

	private record ConfigScreenFactoryConfig(
		Component title,
		Supplier<? extends IConfigEditableSchema> schemaSupplier,
		IConfigRestartHandler restartHandler
	) {

	}
}
