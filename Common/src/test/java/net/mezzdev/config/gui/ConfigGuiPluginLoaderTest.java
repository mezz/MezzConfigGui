package net.mezzdev.config.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.api.schema.ConfigSchemaType;
import net.mezzdev.config.api.schema.update.IConfigBatchUpdater;
import net.mezzdev.config.api.schema.category.IConfigCategory;
import net.mezzdev.config.api.schema.category.IConfigEditorCategory;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.sorting.ISortingConfig;
import net.mezzdev.config.api.migration.ISortingConfigMigrator;
import net.mezzdev.config.api.value.editor.ConfigValueEditMode;
import net.mezzdev.config.api.value.editor.IConfigValueEditorInfo;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.change.IAppliedConfigValueChange;
import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.change.IConfigValueBatchChangeListener;
import net.mezzdev.config.api.value.change.IConfigValueChangeListener;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import net.mezzdev.config.gui.api.IConfigScreenBuilder;
import net.mezzdev.config.gui.api.IConfigScreenCategoryBuilder;
import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigValueIcon;
import net.mezzdev.config.gui.api.IConfigValueIconProvider;
import net.mezzdev.config.gui.config.ConfigGuiOptionsTestUtil;
import net.mezzdev.config.gui.model.ConfigValueChange;
import net.mezzdev.config.gui.remote.RemoteConfigEditor;
import net.mezzdev.config.gui.screenlist.ConfigScreenFactoryRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigGuiPluginLoaderTest {
	private static final String MOD_ID = "test_mod";

	@Test
	void editorTypesUseReferenceIdentityAndRejectDuplicateUids() throws ReflectiveOperationException {
		ConfigValueEditorType<String> stringType = ConfigValueEditorType.create(MOD_ID, "shared");
		ConfigValueEditorType<Integer> integerType = ConfigValueEditorType.create(MOD_ID, "shared");
		assertNotEquals(stringType, integerType);

		Class<?> registrationClass = Class.forName("net.mezzdev.config.gui.ConfigGuiPluginLoader$ConfigGuiRegistration");
		var constructor = registrationClass.getDeclaredConstructor(String.class);
		constructor.setAccessible(true);
		IConfigGuiRegistration registration = (IConfigGuiRegistration) constructor.newInstance(MOD_ID);
		registration.registerValueEditor(stringType, ignored -> {
			throw new AssertionError("Editor factory should not be called");
		});
		registration.registerValueEditor(integerType, ignored -> {
			throw new AssertionError("Editor factory should not be called");
		});

		Field factoriesField = registrationClass.getDeclaredField("valueEditorFactories");
		factoriesField.setAccessible(true);
		Map<?, ?> factories = (Map<?, ?>) factoriesField.get(registration);
		assertEquals(1, factories.size());
	}

	@Test
	void addsDefaultKeyMappingsCategoryAfterOriginalCategories() {
		TestConfigValue originalValue = new TestConfigValue("enabled");
		TestConfigValue keyMappingValue = new TestConfigValue("key.test_mod.open");
		TestCategory originalCategory = new TestCategory("general", List.of(originalValue));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(originalCategory),
			screenBuilder -> {},
			lookup -> {
				assertEquals(MOD_ID, lookup.modId());
				assertEquals(List.of(originalValue), lookup.getAllValues());
				return List.of(keyMappingValue);
			}
		);

		assertEquals(List.of("general", "keyMappings"), categoryNames(categories));
		assertSame(keyMappingValue, List.copyOf(categories.get(1).getConfigValues()).get(0));
	}

	@Test
	void skipsDefaultKeyMappingsCategoryWhenNoMappingsAreDetected() {
		TestCategory originalCategory = new TestCategory("general", List.of(new TestConfigValue("enabled")));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(originalCategory),
			screenBuilder -> {},
			lookup -> List.of()
		);

		assertEquals(List.of("general"), categoryNames(categories));
	}

	@Test
	void defaultKeyMappingsProviderReturnsNoValuesWhenKeyMappingsAreHidden() {
		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("showKeyMappings", false)) {
			ConfigGuiPluginLoader.ConfigScreenValueProvider defaultProvider = getDefaultKeyMappingsProvider();
			ConfigGuiPluginLoader.ConfigScreenValueLookup lookup = new ConfigGuiPluginLoader.ConfigScreenValueLookup(
				MOD_ID,
				"keyMappings",
				List.of()
			);

			assertEquals(List.of(), defaultProvider.getValues(lookup));
		}
	}

	@Test
	void explicitKeyMappingsAreHiddenWhenKeyMappingsAreHidden() {
		KeyMapping keyMapping = keyMapping("key.test_mod.open", InputConstants.KEY_K);

		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("showKeyMappings", false)) {
			List<ConfigScreenCategory> categories = createCategories(
				List.of(),
				screenBuilder -> screenBuilder.addCategory("keys")
					.addKeyMapping(keyMapping),
				lookup -> List.of(new TestConfigValue("key.test_mod.default"))
			);

			assertEquals(List.of(), categories);
		}
	}

	@Test
	void usesConfiguredKeyMappingsCategoryForDefaultMappingsWhenNoMappingsWereAddedExplicitly() {
		TestConfigValue keyMappingValue = new TestConfigValue("key.test_mod.open");

		List<ConfigScreenCategory> categories = createCategories(
			List.of(),
			screenBuilder -> screenBuilder.addCategory("keyMappings")
				.setTitle(Component.literal("Controls")),
			lookup -> List.of(keyMappingValue)
		);

		assertEquals(List.of("keyMappings"), categoryNames(categories));
		assertEquals("Controls", categories.get(0).getLocalizedName().getString());
		assertSame(keyMappingValue, List.copyOf(categories.get(0).getConfigValues()).get(0));
	}

	@Test
	void supportsMultipleCustomKeyMappingCategoriesAndSuppressesDefaultCategory() {
		AtomicBoolean defaultProviderCalled = new AtomicBoolean(false);
		KeyMapping primaryKeyMapping = keyMapping("key.test_mod.primary", InputConstants.KEY_K);
		KeyMapping secondaryKeyMapping = keyMapping("key.test_mod.secondary", InputConstants.KEY_L);

		List<ConfigScreenCategory> categories = createCategories(
			List.of(),
			screenBuilder -> {
				screenBuilder.addCategory("primaryKeys")
					.addKeyMapping(primaryKeyMapping);
				screenBuilder.addCategory("secondaryKeys")
					.addKeyMappings(List.of(secondaryKeyMapping));
			},
			lookup -> {
				defaultProviderCalled.set(true);
				return List.of(new TestConfigValue("key.test_mod.default"));
			}
		);

		assertEquals(List.of("primaryKeys", "secondaryKeys"), categoryNames(categories));
		assertEquals(List.of("key.test_mod.primary"), valueNames(categories.get(0)));
		assertEquals(List.of("key.test_mod.secondary"), valueNames(categories.get(1)));
		assertFalse(defaultProviderCalled.get());
	}

	@Test
	void addedKeyMappingsAppendToDefaultValuesForThatCategory() {
		AtomicBoolean defaultProviderCalled = new AtomicBoolean(false);
		TestConfigValue originalValue = new TestConfigValue("dragDelayInMilliseconds");
		TestCategory originalCategory = new TestCategory("input", List.of(originalValue));
		KeyMapping keyMapping = keyMapping("key.test_mod.openScreen", InputConstants.KEY_G);

		List<ConfigScreenCategory> categories = createCategories(
			List.of(originalCategory),
			screenBuilder -> screenBuilder.addCategory("input")
				.addKeyMapping(keyMapping),
			lookup -> {
				defaultProviderCalled.set(true);
				return List.of(new TestConfigValue("key.test_mod.default"));
			}
		);

		assertEquals(List.of("input"), categoryNames(categories));
		assertEquals(List.of("dragDelayInMilliseconds", "key.test_mod.openScreen"), valueNames(categories.get(0)));
		assertFalse(defaultProviderCalled.get());
	}

	@Test
	void categoryScopedValueNamesResolveBeforeGlobalValueNames() {
		TestConfigValue ingredientRows = new TestConfigValue("maxRows");
		TestConfigValue bookmarkRows = new TestConfigValue("maxRows");
		TestCategory ingredientCategory = new TestCategory("ingredientList", List.of(ingredientRows));
		TestCategory bookmarkCategory = new TestCategory("bookmarkList", List.of(bookmarkRows));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(ingredientCategory, bookmarkCategory),
			screenBuilder -> screenBuilder.configureCategory("bookmarkList")
				.clearDefaultValues()
				.addValueByName("maxRows"),
			lookup -> List.of()
		);

		assertEquals(List.of("ingredientList", "bookmarkList"), categoryNames(categories));
		assertSame(bookmarkRows, List.copyOf(categories.get(1).getConfigValues()).get(0));
	}

	@Test
	void qualifiedValueNamesCanResolveAcrossCategories() {
		TestConfigValue ingredientRows = new TestConfigValue("maxRows");
		TestConfigValue bookmarkRows = new TestConfigValue("maxRows");
		TestCategory ingredientCategory = new TestCategory("ingredientList", List.of(ingredientRows));
		TestCategory bookmarkCategory = new TestCategory("bookmarkList", List.of(bookmarkRows));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(ingredientCategory, bookmarkCategory),
			screenBuilder -> screenBuilder.addCategory("quick")
				.addValueByName("bookmarkList.maxRows"),
			lookup -> List.of()
		);

		assertEquals(List.of("quick", "ingredientList", "bookmarkList"), categoryNames(categories));
		assertSame(bookmarkRows, List.copyOf(categories.get(0).getConfigValues()).get(0));
	}

	@Test
	void addedCategoryValuesAppendToDefaultValuesForThatCategory() {
		TestConfigValue primaryValue = new TestConfigValue("primary");
		TestConfigValue secondaryValue = new TestConfigValue("secondary");
		TestConfigValue modeValue = new TestConfigValue("mode");
		TestConfigValue customValue = new TestConfigValue("custom");
		TestCategory originalCategory = new TestCategory("controls", List.of(primaryValue, secondaryValue, modeValue));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(originalCategory),
			screenBuilder -> screenBuilder.addCategory("controls")
				.addScreenValue(modeValue)
				.addScreenValue(customValue),
			lookup -> List.of()
		);

		assertEquals(List.of("controls"), categoryNames(categories));
		assertEquals(List.of("primary", "secondary", "mode", "custom"), valueNames(categories.get(0)));
	}

	@Test
	void clearDefaultValuesReplacesDefaultValuesForThatCategory() {
		TestConfigValue primaryValue = new TestConfigValue("primary");
		TestConfigValue secondaryValue = new TestConfigValue("secondary");
		TestConfigValue modeValue = new TestConfigValue("mode");
		TestCategory originalCategory = new TestCategory("controls", List.of(primaryValue, secondaryValue, modeValue));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(originalCategory),
			screenBuilder -> screenBuilder.addCategory("controls")
				.clearDefaultValues()
				.addScreenValue(modeValue),
			lookup -> List.of()
		);

		assertEquals(List.of("controls"), categoryNames(categories));
		assertEquals(List.of("mode"), valueNames(categories.get(0)));
	}

	@Test
	void customizedMezzConfigValuesReceiveRemoteWrappingAfterComposition() {
		TestBackingConfigValue backingValue = new TestBackingConfigValue("restartRequiredValue");
		TestRemoteConfigSchema backingSchema = new TestRemoteConfigSchema(backingValue);
		IConfigScreenValue<String> customizedValue = IConfigScreenValue.withApplyMode(
			IConfigScreenValue.configValue(backingValue),
			ConfigValueApplyMode.IMMEDIATE
		);
		RemoteConfigEditor.onClientConnected(false);
		try {
			ConfigScreenSchema customizedSchema = ConfigGuiPluginLoader.createCustomizedScreenSchemaForTests(
				MOD_ID,
				() -> ConfigScreenSchema.from(backingSchema),
				List.of(screenBuilder -> screenBuilder.configureCategory("general")
					.clearDefaultValues()
					.addScreenValue(customizedValue)),
				lookup -> List.of()
			);

			IConfigScreenValue<?> result = customizedSchema.getCategories()
				.get(0)
				.getConfigValues()
				.iterator()
				.next();

			assertNotSame(customizedValue, result);
			assertSame(backingValue, result.getIdentityKey());
			assertEquals(ConfigValueApplyMode.IMMEDIATE, result.getApplyMode());
			assertSame(backingSchema, customizedSchema.findBackingSchema(result).orElseThrow());
		} finally {
			RemoteConfigEditor.onClientDisconnect();
		}
	}

	@Test
	void valueBuilderInsertsValuesRelativeToDefaultValues() {
		TestConfigValue maxRows = new TestConfigValue("maxRows");
		TestConfigValue maxColumns = new TestConfigValue("maxColumns");
		TestConfigValue horizontalAlignment = new TestConfigValue("horizontalAlignment");
		TestConfigValue verticalAlignment = new TestConfigValue("verticalAlignment");
		TestConfigValue buttonNavigationVisibility = new TestConfigValue("buttonNavigationVisibility");
		TestConfigValue drawBackground = new TestConfigValue("drawBackground");
		TestConfigValue toastReflowEnabled = new TestConfigValue("toastReflowEnabled");
		TestConfigValue alignment = new TestConfigValue("alignment");
		TestCategory originalCategory = new TestCategory("ingredientList", List.of(
			maxRows,
			maxColumns,
			horizontalAlignment,
			verticalAlignment,
			buttonNavigationVisibility,
			drawBackground,
			toastReflowEnabled
		));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(originalCategory),
			screenBuilder -> {
				IConfigScreenCategoryBuilder categoryBuilder = screenBuilder.configureCategory("ingredientList");
				categoryBuilder.getValueBuilderByName("maxColumns")
					.insertAfter(alignment);
				categoryBuilder.getValueBuilderByName("horizontalAlignment")
					.hide();
				categoryBuilder.getValueBuilderByName("verticalAlignment")
					.hide();
			},
			lookup -> List.of()
		);

		ConfigScreenCategory category = categories.get(0);
		assertEquals(List.of("ingredientList"), categoryNames(categories));
		assertEquals(
			List.of("maxRows", "maxColumns", "alignment", "buttonNavigationVisibility", "drawBackground", "toastReflowEnabled"),
			valueNames(category)
		);
		IConfigScreenValue<?> insertedValue = valueByName(category, "alignment");
		assertEquals(ConfigValueApplyMode.ON_APPLY, insertedValue.getApplyMode());
		assertEquals(ConfigValueRestartRequirement.NONE, insertedValue.getRestartRequirement());
	}

	@Test
	void categoryAddsSortingConfigBackedByRuntimeValues() {
		TestSortingConfig sortingConfig = new TestSortingConfig(true);
		IConfigValueIcon firstIcon = (guiGraphics, area) -> {};

		List<ConfigScreenCategory> categories = createCategories(
			List.of(),
			screenBuilder -> screenBuilder.addCategory("sorting")
				.addStringSortingConfig(
					"sortOrder",
					"test.sortOrder",
					sortingConfig,
					List.of("second", "first")
				)
				.setValueNames(Map.of("first", Component.literal("First")))
				.setValueDescriptions(Map.of("first", Component.literal("First description")))
				.setValueIcons(Map.of("first", firstIcon))
				.setApplyMode(ConfigValueApplyMode.IMMEDIATE)
				.setRestartRequirement(ConfigValueRestartRequirement.WORLD_RESTART),
			lookup -> List.of()
		);

		assertEquals(List.of("sorting"), categoryNames(categories));
		IConfigScreenValue<?> value = valueByName(categories.get(0), "sortOrder");
		assertEquals("test.sortOrder", value.getLocalizationKey());
		assertEquals(List.of("first", "second"), value.getDefaultValue());
		assertEquals(List.of("first", "second"), value.getValue());
		assertEquals(ConfigValueApplyMode.IMMEDIATE, value.getApplyMode());
		assertEquals(ConfigValueRestartRequirement.WORLD_RESTART, value.getRestartRequirement());

		IConfigListValueEditorSerializer<String> listSerializer = getStringListSerializer(value);
		assertEquals(List.of("first", "second"), List.copyOf(listSerializer.getElementSerializer().getAllValidValues().orElseThrow()));
		assertEquals("First", ConfigValueLocalization
			.getValueName(listSerializer.getElementSerializer(), "test.sortOrder", "first")
			.getString());
		assertEquals("second", ConfigValueLocalization
			.getValueName(listSerializer.getElementSerializer(), "test.sortOrder", "second")
			.getString());
		assertEquals("First description", ConfigValueLocalization
			.getValueDescription(listSerializer.getElementSerializer(), "test.sortOrder", "first")
			.orElseThrow()
			.getString());
		assertTrue(ConfigValueLocalization
			.getValueDescription(listSerializer.getElementSerializer(), "test.sortOrder", "second")
			.isEmpty());

		IConfigValueIconProvider<String> iconProvider = getStringIconProvider(listSerializer);
		assertSame(firstIcon, iconProvider.getIcon("first").orElseThrow());
		assertTrue(iconProvider.getIcon("second").isEmpty());

		assertTrue(getStringListValue(value).set(List.of("second", "first")));
		assertEquals(List.of(List.of("second", "first")), sortingConfig.savedValues);
	}

	@Test
	void screenCustomizersAreDeferredUntilScreenCreation() {
		AtomicInteger factoryCreationCustomizerCalls = new AtomicInteger();
		IConfigGuiPlugin plugin = new IConfigGuiPlugin() {
			@Override
			public String getModId() {
				return MOD_ID;
			}

			@Override
			public void register(IConfigGuiRegistration registration) {
				registration.configureScreen(screenBuilder -> {
					factoryCreationCustomizerCalls.incrementAndGet();
					screenBuilder.addCategory("runtime")
						.addScreenValue(new TestConfigValue("runtime"));
				});
			}
		};

		Map<String, IConfigScreenFactory> factories = ConfigGuiPluginLoader.createScreenFactoryRegistryFromInternalConfigs(
				List.of(new TestScreenConfig(MOD_ID, Component.literal("Test"), () -> List.of())),
				List.of(plugin),
				false
			)
			.getFactories();

		assertTrue(factories.containsKey(MOD_ID));
		assertEquals(0, factoryCreationCustomizerCalls.get());
	}

	@Test
	void screenFactoryRegistryKeepsScreenListEntries() {
		ConfigScreenFactoryRegistry registry = ConfigGuiPluginLoader.createScreenFactoryRegistryFromInternalConfigs(
			List.of(new TestScreenConfig(MOD_ID, Component.literal("Test Title"), () -> List.of())),
			List.of(),
			false
		);

		assertEquals(List.of(MOD_ID), List.copyOf(registry.getFactories().keySet()));
		assertEquals(1, registry.getEntries().size());
		assertEquals(MOD_ID, registry.getEntries().get(0).modId());
		assertEquals("Test Title", registry.getEntries().get(0).title().getString());
		assertSame(registry.getFactories().get(MOD_ID), registry.getEntries().get(0).factory());
	}

	@Test
	void configSourcesForTheSameModAreMergedWithoutInactiveSourcesMaskingActiveSources() {
		AtomicBoolean nativeConfigActive = new AtomicBoolean(false);
		TestConfigValue nativeValue = new TestConfigValue("native");
		TestConfigValue mezzConfigValue = new TestConfigValue("mezzConfig");
		ConfigScreenConfig nativeConfig = new TestScreenConfig(
			MOD_ID,
			Component.literal("Native Title"),
			() -> {
				if (nativeConfigActive.get()) {
					return List.of(new TestCategory(
						"general",
						List.of(nativeValue),
						ConfigScreenCategoryGroup.LOADER_NATIVE
					));
				}
				return List.of();
			}
		);
		ConfigScreenConfig mezzConfig = new TestScreenConfig(
			MOD_ID,
			Component.literal("MezzConfig Title"),
			() -> List.of(new TestCategory("general", List.of(mezzConfigValue)))
		);

		List<ConfigScreenConfig> combined = ConfigGuiPluginLoader.combineConfigScreens(
			List.of(nativeConfig),
			List.of(mezzConfig)
		);
		ConfigScreenFactoryRegistry registry = ConfigGuiPluginLoader.createScreenFactoryRegistryFromInternalConfigs(
			combined,
			List.of(),
			false
		);
		MergedConfigScreenConfig merged = new MergedConfigScreenConfig(MOD_ID, combined);

		assertEquals(2, combined.size());
		assertEquals(1, registry.getEntries().size());
		assertEquals("Native Title", merged.getTitle().getString());
		List<? extends ConfigScreenCategory> categoriesBeforeNativeLoad = merged.getSchema().getCategories();
		assertEquals(List.of("general"), categoryNames(categoriesBeforeNativeLoad));
		assertEquals(List.of("mezzConfig"), valueNames(categoriesBeforeNativeLoad.get(0)));

		nativeConfigActive.set(true);
		List<? extends ConfigScreenCategory> categoriesAfterNativeLoad = merged.getSchema().getCategories();
		assertEquals(List.of("general"), categoryNames(categoriesAfterNativeLoad));
		assertEquals(List.of("mezzConfig", "native"), valueNames(categoriesAfterNativeLoad.get(0)));
	}

	@Test
	void mergedCategoriesUseIntentionalSourceOrder() {
		TestCategory nativeClient = new TestCategory(
			"nativeClient",
			List.of(new TestConfigValue("nativeClientValue")),
			ConfigScreenCategoryGroup.LOADER_NATIVE
		);
		TestCategory nativeServer = new TestCategory(
			"nativeServer",
			List.of(new TestConfigValue("nativeServerValue")),
			ConfigScreenCategoryGroup.LOADER_NATIVE_SERVER
		);
		TestCategory mezzConfig = new TestCategory("mezzConfig", List.of(new TestConfigValue("mezzConfigValue")));
		MergedConfigScreenConfig merged = new MergedConfigScreenConfig(MOD_ID, List.of(
			new TestScreenConfig(MOD_ID, Component.literal("Native"), () -> List.of(nativeClient, nativeServer)),
			new TestScreenConfig(MOD_ID, Component.literal("MezzConfig"), () -> List.of(mezzConfig))
		));

		List<ConfigScreenCategory> categories = createCategories(
			merged.getSchema().getCategories(),
			screenBuilder -> {},
			lookup -> List.of(new TestConfigValue("key.test_mod.open"))
		);

		assertEquals(List.of("mezzConfig", "nativeClient", "keyMappings", "nativeServer"), categoryNames(categories));
	}

	@Test
	void deferredScreenCustomizersUseCurrentStateEachTime() {
		AtomicInteger customizerCalls = new AtomicInteger();
		AtomicInteger schemaVersion = new AtomicInteger(1);
		Consumer<IConfigScreenBuilder> screenCustomizer = screenBuilder -> {
			int customizerCall = customizerCalls.incrementAndGet();
			screenBuilder.addCategory("runtime")
				.addScreenValue(new TestConfigValue("runtime" + customizerCall));
		};

		ConfigScreenSchema firstSchema = ConfigGuiPluginLoader.createCustomizedScreenSchemaForTests(
			MOD_ID,
			() -> {
				int version = schemaVersion.get();
				return () -> List.of(new TestCategory("base", List.of(new TestConfigValue("base" + version))));
			},
			List.of(screenCustomizer)
		);
		schemaVersion.incrementAndGet();
		ConfigScreenSchema secondSchema = ConfigGuiPluginLoader.createCustomizedScreenSchemaForTests(
			MOD_ID,
			() -> {
				int version = schemaVersion.get();
				return () -> List.of(new TestCategory("base", List.of(new TestConfigValue("base" + version))));
			},
			List.of(screenCustomizer)
		);

		assertEquals(2, customizerCalls.get());
		List<? extends ConfigScreenCategory> firstCategories = firstSchema.getCategories();
		List<? extends ConfigScreenCategory> secondCategories = secondSchema.getCategories();
		assertEquals(List.of("runtime", "base"), categoryNames(firstCategories));
		assertEquals(List.of("runtime", "base"), categoryNames(secondCategories));
		assertEquals(List.of("runtime1"), valueNames(firstCategories.get(0)));
		assertEquals(List.of("base1"), valueNames(firstCategories.get(1)));
		assertEquals(List.of("runtime2"), valueNames(secondCategories.get(0)));
		assertEquals(List.of("base2"), valueNames(secondCategories.get(1)));
	}

	@Test
	void configuredCategoriesDoNotOmitUnconfiguredDefaultCategories() {
		TestConfigValue generalValue = new TestConfigValue("enabled");
		TestConfigValue advancedValue = new TestConfigValue("refreshTicks");
		TestConfigValue overviewValue = new TestConfigValue("combined");
		TestCategory generalCategory = new TestCategory("general", List.of(generalValue));
		TestCategory advancedCategory = new TestCategory("advanced", List.of(advancedValue));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(generalCategory, advancedCategory),
			screenBuilder -> screenBuilder.addCategory("overview")
				.addScreenValue(overviewValue),
			lookup -> List.of()
		);

		assertEquals(List.of("overview", "general", "advanced"), categoryNames(categories));
		assertEquals(List.of("combined"), valueNames(categories.get(0)));
	}

	@Test
	void clearDefaultCategoriesOmitsUnconfiguredDefaultCategories() {
		TestConfigValue generalValue = new TestConfigValue("enabled");
		TestConfigValue advancedValue = new TestConfigValue("refreshTicks");
		TestConfigValue overviewValue = new TestConfigValue("combined");
		TestCategory generalCategory = new TestCategory("general", List.of(generalValue));
		TestCategory advancedCategory = new TestCategory("advanced", List.of(advancedValue));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(generalCategory, advancedCategory),
			screenBuilder -> {
				screenBuilder.clearDefaultCategories();
				screenBuilder.addCategory("overview")
					.addScreenValue(overviewValue);
			},
			lookup -> List.of()
		);

		assertEquals(List.of("overview"), categoryNames(categories));
		assertEquals(List.of("combined"), valueNames(categories.get(0)));
	}

	@Test
	void configuredCategoryValuesAppendWithoutOmittingUnconfiguredDefaultCategories() {
		TestConfigValue generalValue = new TestConfigValue("enabled");
		TestConfigValue advancedValue = new TestConfigValue("refreshTicks");
		TestConfigValue replacementValue = new TestConfigValue("replacement");
		TestCategory generalCategory = new TestCategory("general", List.of(generalValue));
		TestCategory advancedCategory = new TestCategory("advanced", List.of(advancedValue));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(generalCategory, advancedCategory),
			screenBuilder -> screenBuilder.configureCategory("advanced")
				.addScreenValue(replacementValue),
			lookup -> List.of()
		);

		assertEquals(List.of("general", "advanced"), categoryNames(categories));
		assertEquals(List.of("enabled"), valueNames(categories.get(0)));
		assertEquals(List.of("refreshTicks", "replacement"), valueNames(categories.get(1)));
	}

	@Test
	void clearDefaultCategoriesAndValuesPreserveManualReplacementWorkflow() {
		TestConfigValue generalValue = new TestConfigValue("enabled");
		TestConfigValue advancedValue = new TestConfigValue("refreshTicks");
		TestConfigValue replacementValue = new TestConfigValue("replacement");
		TestCategory generalCategory = new TestCategory("general", List.of(generalValue));
		TestCategory advancedCategory = new TestCategory("advanced", List.of(advancedValue));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(generalCategory, advancedCategory),
			screenBuilder -> {
				screenBuilder.clearDefaultCategories();
				screenBuilder.configureCategory("advanced")
					.clearDefaultValues()
					.addScreenValue(replacementValue);
			},
			lookup -> List.of()
		);

		assertEquals(List.of("advanced"), categoryNames(categories));
		assertEquals(List.of("replacement"), valueNames(categories.get(0)));
	}

	@Test
	void titleOnlyConfiguredCategoryKeepsDefaultValues() {
		TestConfigValue advancedValue = new TestConfigValue("refreshTicks");
		TestCategory advancedCategory = new TestCategory("advanced", List.of(advancedValue));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(advancedCategory),
			screenBuilder -> screenBuilder.configureCategory("advanced")
				.setTitle(Component.literal("Advanced Settings")),
			lookup -> List.of()
		);

		assertEquals(List.of("advanced"), categoryNames(categories));
		assertEquals("Advanced Settings", categories.get(0).getLocalizedName().getString());
		assertEquals(List.of("refreshTicks"), valueNames(categories.get(0)));
	}

	@Test
	void appliesConfiguredApplyModesToCategoryValues() {
		TestConfigValue immediateValue = new TestConfigValue("enabled");
		TestConfigValue onApplyValue = new TestConfigValue("mode");
		TestCategory originalCategory = new TestCategory("general", List.of(immediateValue, onApplyValue));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(originalCategory),
			screenBuilder -> {
				IConfigScreenCategoryBuilder categoryBuilder = screenBuilder.configureCategory("general")
					.setDefaultApplyMode(ConfigValueApplyMode.IMMEDIATE);
				categoryBuilder.getScreenValueBuilder(onApplyValue)
					.setApplyMode(ConfigValueApplyMode.ON_APPLY);
			},
			lookup -> List.of()
		);

		List<? extends IConfigScreenValue<?>> values = List.copyOf(categories.get(0).getConfigValues());
		assertEquals(ConfigValueApplyMode.IMMEDIATE, values.get(0).getApplyMode());
		assertEquals(ConfigValueApplyMode.ON_APPLY, values.get(1).getApplyMode());
	}

	@Test
	void appliesConfiguredRestartRequirementsToCategoryValues() {
		TestConfigValue regularValue = new TestConfigValue("enabled");
		TestConfigValue restartValue = new TestConfigValue("requiresRestart");
		TestCategory originalCategory = new TestCategory("general", List.of(regularValue, restartValue));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(originalCategory),
			screenBuilder -> screenBuilder.configureCategory("general")
				.getScreenValueBuilder(restartValue)
				.setRestartRequirement(ConfigValueRestartRequirement.WORLD_RESTART),
			lookup -> List.of()
		);

		List<? extends IConfigScreenValue<?>> values = List.copyOf(categories.get(0).getConfigValues());
		assertEquals(ConfigValueRestartRequirement.NONE, values.get(0).getRestartRequirement());
		assertEquals(ConfigValueRestartRequirement.WORLD_RESTART, values.get(1).getRestartRequirement());
	}

	@Test
	void decoratorsUseStableIdentityWithoutChangingEquals() {
		TestConfigValue value = new TestConfigValue("enabled");
		IConfigScreenValue<String> decoratedValue = IConfigScreenValue.withApplyMode(value, ConfigValueApplyMode.IMMEDIATE);
		TestCategory originalCategory = new TestCategory("general", List.of(value));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(originalCategory),
			screenBuilder -> {
				IConfigScreenCategoryBuilder categoryBuilder = screenBuilder.configureCategory("general");
				categoryBuilder.addScreenValue(decoratedValue);
				categoryBuilder.getScreenValueBuilder(decoratedValue)
					.setRestartRequirement(ConfigValueRestartRequirement.WORLD_RESTART);
			},
			lookup -> List.of()
		);

		assertFalse(value.equals(decoratedValue));
		assertFalse(decoratedValue.equals(value));
		assertSame(value.getIdentityKey(), decoratedValue.getIdentityKey());
		List<? extends IConfigScreenValue<?>> values = List.copyOf(categories.get(0).getConfigValues());
		assertEquals(1, values.size());
		assertEquals(ConfigValueRestartRequirement.WORLD_RESTART, values.get(0).getRestartRequirement());
	}

	@Test
	void configValueChangesKeepTheStrongestRestartRequirement() {
		IConfigScreenValue<String> worldRestartValue = IConfigScreenValue.withRestartRequirement(
			new TestConfigValue("worldRestart"),
			ConfigValueRestartRequirement.WORLD_RESTART
		);
		IConfigScreenValue<String> gameRestartValue = IConfigScreenValue.withRestartRequirement(
			new TestConfigValue("gameRestart"),
			ConfigValueRestartRequirement.GAME_RESTART
		);

		assertEquals(
			ConfigValueRestartRequirement.WORLD_RESTART,
			ConfigValueChange.getRestartRequirement(List.of(new ConfigValueChange<>(worldRestartValue, "changed")))
		);
		assertEquals(
			ConfigValueRestartRequirement.GAME_RESTART,
			ConfigValueChange.getRestartRequirement(List.of(
				new ConfigValueChange<>(worldRestartValue, "changed"),
				new ConfigValueChange<>(gameRestartValue, "changed")
			))
		);
	}

	@Test
	void valueBuilderConfiguresMezzConfigValue() {
		TestBackingConfigValue backingValue = new TestBackingConfigValue("mode");
		IConfigScreenValue<String> screenValue = IConfigScreenValue.configValue(backingValue);
		TestCategory originalCategory = new TestCategory("general", List.of(screenValue));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(originalCategory),
			screenBuilder -> screenBuilder.configureCategory("general")
				.getValueBuilder(backingValue)
				.setApplyMode(ConfigValueApplyMode.IMMEDIATE)
				.setRestartRequirement(ConfigValueRestartRequirement.GAME_RESTART),
			lookup -> List.of()
		);

		IConfigScreenValue<?> configuredValue = List.copyOf(categories.get(0).getConfigValues()).get(0);
		assertEquals(ConfigValueApplyMode.IMMEDIATE, configuredValue.getApplyMode());
		assertEquals(ConfigValueRestartRequirement.GAME_RESTART, configuredValue.getRestartRequirement());
	}

	@Test
	void configuresNativeScreenValuesByNameAcrossCustomCategories() {
		String clientCategoryName = "%s-client.toml".formatted(MOD_ID);
		String commonCategoryName = "%s-common.toml".formatted(MOD_ID);
		TestConfigValue enabled = new TestConfigValue("client.enabled");
		TestConfigValue extraEffects = new TestConfigValue("client.extraEffects");
		TestConfigValue secretDiagnostics = new TestConfigValue("client.secretDiagnostics");
		TestConfigValue mode = new TestConfigValue("client.mode");
		TestConfigValue label = new TestConfigValue("client.label");
		TestConfigValue rowCount = new TestConfigValue("client.rowCount");
		TestConfigValue opacity = new TestConfigValue("client.opacity", ConfigValueRestartRequirement.GAME_RESTART);
		TestConfigValue enabledHistory = new TestConfigValue("client.enabledHistory");
		TestConfigValue favoriteRows = new TestConfigValue("client.favoriteRows");
		TestConfigValue favoriteModes = new TestConfigValue("client.favoriteModes");
		TestConfigValue aliases = new TestConfigValue("client.aliases");
		TestConfigValue cacheBudget = new TestConfigValue("client.cacheBudget");
		TestConfigValue cacheBreakpoints = new TestConfigValue("client.cacheBreakpoints");
		TestConfigValue opacitySteps = new TestConfigValue("client.opacitySteps");
		TestConfigValue commonEnabled = new TestConfigValue("common.enabled");
		TestConfigValue commonAliases = new TestConfigValue("common.aliases");
		TestConfigValue commonCacheBudget = new TestConfigValue("common.cacheBudget");
		TestCategory clientCategory = new TestCategory(clientCategoryName, List.of(
			enabled,
			extraEffects,
			secretDiagnostics,
			mode,
			label,
			rowCount,
			opacity,
			enabledHistory,
			favoriteRows,
			favoriteModes,
			aliases,
			cacheBudget,
			cacheBreakpoints,
			opacitySteps
		), ConfigScreenCategoryGroup.LOADER_NATIVE);
		TestCategory commonCategory = new TestCategory(commonCategoryName, List.of(
			commonEnabled,
			commonAliases,
			commonCacheBudget
		), ConfigScreenCategoryGroup.LOADER_NATIVE);
		KeyMapping openKey = keyMapping("key.test_mod.openNativeScreen", InputConstants.KEY_J);
		KeyMapping toggleKey = keyMapping("key.test_mod.toggleNativeOverlay", InputConstants.KEY_O);
		AtomicBoolean defaultProviderCalled = new AtomicBoolean(false);

		List<ConfigScreenCategory> categories = createCategories(
			List.of(clientCategory, commonCategory),
			screenBuilder -> {
				IConfigScreenCategoryBuilder quickCategory = screenBuilder.addCategory("quick")
					.setTitle(Component.literal("Quick"))
					.setDescription(Component.literal("Frequently changed native values"))
					.setDefaultApplyMode(ConfigValueApplyMode.IMMEDIATE);
				quickCategory.getValueBuilderByName("client.mode")
					.setApplyMode(ConfigValueApplyMode.ON_APPLY)
					.setRestartRequirement(ConfigValueRestartRequirement.GAME_RESTART);
				quickCategory.addValuesByName(List.of(
					"client.enabled",
					"client.mode",
					"client.rowCount"
				));

				IConfigScreenCategoryBuilder listsCategory = screenBuilder.addCategory("lists")
					.setTitle(Component.literal("Native Lists"))
					.setDescription(Component.literal("Native list values"))
					.setDefaultApplyMode(ConfigValueApplyMode.ON_APPLY);
				listsCategory.getValueBuilderByName("client.aliases")
					.setApplyMode(ConfigValueApplyMode.IMMEDIATE);
				listsCategory.getValueBuilderByName("client.opacitySteps")
					.setRestartRequirement(ConfigValueRestartRequirement.GAME_RESTART);
				listsCategory.addValuesByName(List.of(
					"client.enabledHistory",
					"client.favoriteRows",
					"client.favoriteModes",
					"client.aliases",
					"client.cacheBreakpoints",
					"client.opacitySteps"
				));

				screenBuilder.addCategory("keyMappings")
					.setTitle(Component.literal("Key Mappings"))
					.setDescription(Component.literal("Native screen key mappings"))
					.addKeyMapping(openKey)
					.addKeyMapping(toggleKey);

				IConfigScreenCategoryBuilder clientCategoryBuilder = screenBuilder.configureCategory(clientCategoryName)
					.setTitle(Component.literal("Remaining Native Values"))
					.setDescription(Component.literal("Native values kept in their original category"))
					.setDefaultApplyMode(ConfigValueApplyMode.ON_APPLY);
				clientCategoryBuilder.getValueBuilderByName("client.extraEffects")
					.setApplyMode(ConfigValueApplyMode.IMMEDIATE);
				clientCategoryBuilder.getValueBuilderByName("client.label")
					.setRestartRequirement(ConfigValueRestartRequirement.GAME_RESTART);
				clientCategoryBuilder.hideValuesByName(List.of(
					"client.enabled",
					"client.secretDiagnostics",
					"client.mode",
					"client.rowCount",
					"client.enabledHistory",
					"client.favoriteRows",
					"client.favoriteModes",
					"client.aliases",
					"client.cacheBreakpoints",
					"client.opacitySteps"
				));

				IConfigScreenCategoryBuilder commonCategoryBuilder = screenBuilder.configureCategory(commonCategoryName)
					.setTitle(Component.literal("Common Native Values"))
					.setDescription(Component.literal("Common native values kept in their original category"))
					.setDefaultApplyMode(ConfigValueApplyMode.ON_APPLY);
				commonCategoryBuilder.getValueBuilderByName("common.enabled")
					.setApplyMode(ConfigValueApplyMode.IMMEDIATE);
				commonCategoryBuilder.getValueBuilderByName("common.cacheBudget")
					.setRestartRequirement(ConfigValueRestartRequirement.GAME_RESTART);
			},
			lookup -> {
				defaultProviderCalled.set(true);
				return List.of(new TestConfigValue("key.test_mod.default"));
			}
		);

		assertFalse(defaultProviderCalled.get());
		assertEquals(List.of("quick", "lists", clientCategoryName, commonCategoryName, "keyMappings"), categoryNames(categories));
		assertEquals("Quick", categories.get(0).getLocalizedName().getString());
		assertEquals("Frequently changed native values", categories.get(0).getLocalizedDescription().getString());
		assertEquals("Native Lists", categories.get(1).getLocalizedName().getString());
		assertEquals("Native list values", categories.get(1).getLocalizedDescription().getString());
		assertEquals("Remaining Native Values", categories.get(2).getLocalizedName().getString());
		assertEquals("Native values kept in their original category", categories.get(2).getLocalizedDescription().getString());
		assertEquals("Common Native Values", categories.get(3).getLocalizedName().getString());
		assertEquals("Common native values kept in their original category", categories.get(3).getLocalizedDescription().getString());
		assertEquals("Key Mappings", categories.get(4).getLocalizedName().getString());
		assertEquals("Native screen key mappings", categories.get(4).getLocalizedDescription().getString());
		assertEquals(List.of("client.enabled", "client.mode", "client.rowCount"), valueNames(categories.get(0)));
		assertEquals(List.of(
			"client.enabledHistory",
			"client.favoriteRows",
			"client.favoriteModes",
			"client.aliases",
			"client.cacheBreakpoints",
			"client.opacitySteps"
		), valueNames(categories.get(1)));
		assertEquals(List.of("client.extraEffects", "client.label", "client.opacity", "client.cacheBudget"), valueNames(categories.get(2)));
		assertEquals(List.of("common.enabled", "common.aliases", "common.cacheBudget"), valueNames(categories.get(3)));
		assertEquals(List.of("key.test_mod.openNativeScreen", "key.test_mod.toggleNativeOverlay"), valueNames(categories.get(4)));

		assertEquals(ConfigValueApplyMode.IMMEDIATE, valueByName(categories.get(0), "client.enabled").getApplyMode());
		assertEquals(ConfigValueApplyMode.ON_APPLY, valueByName(categories.get(0), "client.mode").getApplyMode());
		assertEquals(ConfigValueApplyMode.IMMEDIATE, valueByName(categories.get(0), "client.rowCount").getApplyMode());
		assertEquals(ConfigValueRestartRequirement.GAME_RESTART, valueByName(categories.get(0), "client.mode").getRestartRequirement());
		assertEquals(ConfigValueApplyMode.IMMEDIATE, valueByName(categories.get(1), "client.aliases").getApplyMode());
		assertEquals(ConfigValueRestartRequirement.GAME_RESTART, valueByName(categories.get(1), "client.opacitySteps").getRestartRequirement());
		assertEquals(ConfigValueApplyMode.IMMEDIATE, valueByName(categories.get(2), "client.extraEffects").getApplyMode());
		assertEquals(ConfigValueRestartRequirement.GAME_RESTART, valueByName(categories.get(2), "client.label").getRestartRequirement());
		assertEquals(ConfigValueRestartRequirement.GAME_RESTART, valueByName(categories.get(2), "client.opacity").getRestartRequirement());
		assertEquals(ConfigValueApplyMode.IMMEDIATE, valueByName(categories.get(3), "common.enabled").getApplyMode());
		assertEquals(ConfigValueRestartRequirement.GAME_RESTART, valueByName(categories.get(3), "common.cacheBudget").getRestartRequirement());
	}

	private static List<ConfigScreenCategory> createCategories(
		List<? extends ConfigScreenCategory> originalCategories,
		Consumer<IConfigScreenBuilder> screenCustomizer,
		ConfigGuiPluginLoader.ConfigScreenValueProvider defaultKeyMappingsProvider
	) {
		return ConfigGuiPluginLoader.createScreenCategoriesForTests(
			MOD_ID,
			originalCategories,
			screenCustomizer,
			defaultKeyMappingsProvider
		);
	}

	private static ConfigGuiPluginLoader.ConfigScreenValueProvider getDefaultKeyMappingsProvider() {
		try {
			Field field = ConfigGuiPluginLoader.class.getDeclaredField("DEFAULT_KEY_MAPPINGS_PROVIDER");
			field.setAccessible(true);
			return (ConfigGuiPluginLoader.ConfigScreenValueProvider) field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Failed to get default key mappings provider.", e);
		}
	}

	private static List<String> categoryNames(List<? extends ConfigScreenCategory> categories) {
		return categories.stream()
			.map(ConfigScreenCategory::getName)
			.toList();
	}

	private static List<String> valueNames(ConfigScreenCategory category) {
		return category.getConfigValues()
			.stream()
			.map(IConfigScreenValue::getName)
			.toList();
	}

	private static IConfigScreenValue<?> valueByName(ConfigScreenCategory category, String name) {
		return category.getConfigValues()
			.stream()
			.filter(value -> value.getName().equals(name))
			.findFirst()
			.orElseThrow();
	}

	@SuppressWarnings("unchecked")
	private static IConfigScreenValue<List<String>> getStringListValue(IConfigScreenValue<?> value) {
		return (IConfigScreenValue<List<String>>) value;
	}

	@SuppressWarnings("unchecked")
	private static IConfigListValueEditorSerializer<String> getStringListSerializer(IConfigScreenValue<?> value) {
		return (IConfigListValueEditorSerializer<String>) value.getSerializer();
	}

	@SuppressWarnings("unchecked")
	private static IConfigValueIconProvider<String> getStringIconProvider(IConfigListValueEditorSerializer<String> listSerializer) {
		return (IConfigValueIconProvider<String>) listSerializer.getElementSerializer();
	}

	private static KeyMapping keyMapping(String name, int keyCode) {
		return TestMinecraft.keyMapping(
			name,
			InputConstants.Type.KEYSYM,
			keyCode,
			"key.categories.%s".formatted(MOD_ID)
		);
	}

	private record TestCategory(
		String name,
		List<IConfigScreenValue<?>> values,
		ConfigScreenCategoryGroup group
	) implements ConfigScreenCategory {
		private TestCategory(String name, List<IConfigScreenValue<?>> values) {
			this(name, values, ConfigScreenCategoryGroup.MOD_OWNED);
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
			return Component.literal(name);
		}

		@Override
		public Component getLocalizedDescription() {
			return Component.empty();
		}

		@Override
		public Collection<? extends IConfigScreenValue<?>> getConfigValues() {
			return values;
		}
	}

	private record TestScreenConfig(
		String modId,
		Component title,
		ConfigScreenSchema schema
	) implements ConfigScreenConfig {
		@Override
		public String getModId() {
			return modId;
		}

		@Override
		public Component getTitle() {
			return title;
		}

		@Override
		public ConfigScreenSchema getSchema() {
			return schema;
		}

	}

	private record TestConfigValue(
		String name,
		ConfigValueRestartRequirement restartRequirement
	) implements IConfigScreenValue<String>, IConfigLocalizedValue {
		private TestConfigValue(String name) {
			this(name, ConfigValueRestartRequirement.NONE);
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
			return Component.literal(name);
		}

		@Override
		public Component getLocalizedDescription() {
			return Component.empty();
		}

		@Override
		public String getValue() {
			return name;
		}

		@Override
		public String getDefaultValue() {
			return name;
		}

		@Override
		public boolean set(String value) {
			return false;
		}

		@Override
		public Runnable addListener(Consumer<String> listener) {
			return () -> {};
		}

		@Override
		public IConfigValueEditorSerializer<String> getSerializer() {
			return TestSerializer.INSTANCE;
		}

		@Override
		public ConfigValueRestartRequirement getRestartRequirement() {
			return restartRequirement;
		}
	}

	private static final class TestSortingConfig implements ISortingConfig<String> {
		private final boolean allowsRemovingValues;
		private final List<List<String>> savedValues = new ArrayList<>();
		private List<String> sortedValues = List.of();
		private boolean hasSortedValues;

		private TestSortingConfig(boolean allowsRemovingValues) {
			this.allowsRemovingValues = allowsRemovingValues;
		}

		@Override
		public List<String> getSortedValues(Collection<String> allValues) {
			if (hasSortedValues) {
				return sortedValues;
			}
			return getDefaultSortedValues(allValues);
		}

		@Override
		public List<String> getDefaultSortedValues(Collection<String> allValues) {
			return allValues.stream()
				.distinct()
				.sorted()
				.toList();
		}

		@Override
		public boolean setSortedValues(Collection<String> allValues, List<String> sortedValues) {
			this.sortedValues = List.copyOf(sortedValues);
			this.savedValues.add(this.sortedValues);
			this.hasSortedValues = true;
			return true;
		}

		@Override
		public Comparator<String> getComparator(Collection<String> allValues) {
			return Comparator.comparingInt(value -> getSortedValues(allValues).indexOf(value));
		}

		@Override
		public boolean isVisible(Collection<String> allValues, String value) {
			return getSortedValues(allValues).contains(value);
		}

		@Override
		public boolean allowsRemovingValues() {
			return allowsRemovingValues;
		}

		@Override
		public Runnable addChangeListener(Runnable listener) {
			return () -> {};
		}

		@Override
		public ISortingConfig<String> setLegacyMigration(List<Path> legacyPaths, ISortingConfigMigrator<String> migrator) {
			return this;
		}
	}

	private record TestBackingConfigValue(
		String name
	) implements IConfigValue<String>, IConfigValueEditorInfo<String> {
		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getLocalizationKey() {
			return name;
		}

		@Override
		public String get() {
			return name;
		}

		@Override
		public IConfigValueEditorInfo<String> getEditorInfo() {
			return this;
		}

		@Override
		public String getPendingValue() {
			return name;
		}

		@Override
		public String getDefaultValue() {
			return name;
		}

		@Override
		public ConfigValueEditMode getEditMode() {
			return ConfigValueEditMode.BATCH;
		}

		@Override
		public ConfigValueRestartRequirement getRestartRequirement() {
			return ConfigValueRestartRequirement.NONE;
		}

		@Override
		public List<? extends IConfigEditorCategory> getEditorCategories() {
			return List.of();
		}

		@Override
		public boolean set(String value) {
			return false;
		}

		@Override
		public Runnable addListener(IConfigValueChangeListener<String> listener) {
			return () -> {};
		}

		@Override
		public Runnable addPendingListener(IConfigValueChangeListener<String> listener) {
			return () -> {};
		}

		@Override
		public Runnable addBatchListener(IConfigValueBatchChangeListener listener) {
			return () -> {};
		}

		@Override
		public Runnable addPendingBatchListener(IConfigValueBatchChangeListener listener) {
			return () -> {};
		}

		@Override
		public IConfigValueEditorSerializer<String> getSerializer() {
			return TestSerializer.INSTANCE;
		}
	}

	private static final class TestRemoteConfigSchema implements IConfigSchema {
		private final TestRemoteConfigCategory category;

		private TestRemoteConfigSchema(IConfigValue<String> value) {
			this.category = new TestRemoteConfigCategory(value);
		}

		@Override
		public String getId() {
			return "server.ini";
		}

		@Override
		public String getModId() {
			return MOD_ID;
		}

		@Override
		public ConfigSchemaType getType() {
			return ConfigSchemaType.SERVER;
		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public Optional<Path> getPath() {
			return Optional.empty();
		}

		@Override
		public List<? extends IConfigCategory> getCategories() {
			return List.of(category);
		}

		@Override
		public List<? extends IConfigEditorCategory> getEditorCategories() {
			return List.of(category);
		}

		@Override
		public List<? extends IAppliedConfigValueChange<?>> batchUpdate(Consumer<IConfigBatchUpdater> updateBatch) {
			throw new AssertionError("A remote schema must not be updated locally.");
		}

		@Override
		public Runnable addBatchListener(IConfigValueBatchChangeListener listener) {
			return () -> {};
		}

		@Override
		public Runnable addPendingBatchListener(IConfigValueBatchChangeListener listener) {
			return () -> {};
		}
	}

	private record TestRemoteConfigCategory(
		IConfigValue<String> value
	) implements IConfigCategory, IConfigEditorCategory {
		@Override
		public String getName() {
			return "general";
		}

		@Override
		public String getLocalizationKey() {
			return "test.general";
		}

		@Override
		public List<? extends IConfigValue<?>> getConfigValues() {
			return List.of(value);
		}
	}

	private enum TestSerializer implements IConfigValueEditorSerializer<String> {
		INSTANCE;

		@Override
		public String serialize(String value) {
			return value;
		}

		@Override
		public IDeserializeResult<String> deserialize(String string) {
			return IDeserializeResult.success(string);
		}

		@Override
		public boolean isValid(String value) {
			return true;
		}

		@Override
		public Optional<List<String>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public Component getLocalizedValueName(String configValueLocalizationKey, String value) {
			return Component.literal(value);
		}

		@Override
		public String getValidValuesDescription() {
			return "";
		}

		@Override
		public ConfigValueEditorType<String> getEditorType() {
			return ConfigValueEditorTypes.getText();
		}
	}

}
