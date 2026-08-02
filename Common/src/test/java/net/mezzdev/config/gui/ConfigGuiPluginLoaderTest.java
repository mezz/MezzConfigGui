package net.mezzdev.config.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigScreenBuilder;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditorSerializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigGuiPluginLoaderTest {
	private static final String MOD_ID = "test_mod";

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
		assertSame(keyMappingValue, List.copyOf(categories.get(1).getConfigValues()).getFirst());
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
	void usesConfiguredKeyMappingsCategoryForDefaultMappingsWhenNoMappingsWereAddedExplicitly() {
		TestConfigValue keyMappingValue = new TestConfigValue("key.test_mod.open");

		List<ConfigScreenCategory> categories = createCategories(
			List.of(),
			screenBuilder -> screenBuilder.addCategory("keyMappings")
				.setTitle(Component.literal("Controls")),
			lookup -> List.of(keyMappingValue)
		);

		assertEquals(List.of("keyMappings"), categoryNames(categories));
		assertEquals("Controls", categories.getFirst().getLocalizedName().getString());
		assertSame(keyMappingValue, List.copyOf(categories.getFirst().getConfigValues()).getFirst());
	}

	@Test
	void supportsMultipleCustomKeyMappingCategoriesAndSuppressesDefaultCategory() {
		AtomicBoolean defaultProviderCalled = new AtomicBoolean(false);
		KeyMapping primaryKeyMapping = keyMapping("key.test_mod.primary", GLFW.GLFW_KEY_K);
		KeyMapping secondaryKeyMapping = keyMapping("key.test_mod.secondary", GLFW.GLFW_KEY_L);

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
		KeyMapping keyMapping = keyMapping("key.test_mod.openScreen", GLFW.GLFW_KEY_G);

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
		assertEquals(List.of("dragDelayInMilliseconds", "key.test_mod.openScreen"), valueNames(categories.getFirst()));
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
		assertSame(bookmarkRows, List.copyOf(categories.get(1).getConfigValues()).getFirst());
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
		assertSame(bookmarkRows, List.copyOf(categories.getFirst().getConfigValues()).getFirst());
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
		assertEquals(List.of("primary", "secondary", "mode", "custom"), valueNames(categories.getFirst()));
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
		assertEquals(List.of("mode"), valueNames(categories.getFirst()));
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
		assertEquals(List.of("combined"), valueNames(categories.getFirst()));
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
		assertEquals(List.of("combined"), valueNames(categories.getFirst()));
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
		assertEquals(List.of("enabled"), valueNames(categories.getFirst()));
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
		assertEquals(List.of("replacement"), valueNames(categories.getFirst()));
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
		assertEquals("Advanced Settings", categories.getFirst().getLocalizedName().getString());
		assertEquals(List.of("refreshTicks"), valueNames(categories.getFirst()));
	}

	@Test
	void appliesConfiguredApplyModesToCategoryValues() {
		TestConfigValue immediateValue = new TestConfigValue("enabled");
		TestConfigValue onApplyValue = new TestConfigValue("mode");
		TestCategory originalCategory = new TestCategory("general", List.of(immediateValue, onApplyValue));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(originalCategory),
			screenBuilder -> screenBuilder.configureCategory("general")
				.setDefaultApplyMode(ConfigValueApplyMode.IMMEDIATE)
				.setScreenValueApplyMode(onApplyValue, ConfigValueApplyMode.ON_APPLY),
			lookup -> List.of()
		);

		List<? extends IConfigScreenValue<?>> values = List.copyOf(categories.getFirst().getConfigValues());
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
				.setScreenValueRequiresRestart(restartValue),
			lookup -> List.of()
		);

		List<? extends IConfigScreenValue<?>> values = List.copyOf(categories.getFirst().getConfigValues());
		assertFalse(values.get(0).requiresRestart());
		assertTrue(values.get(1).requiresRestart());
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
		TestConfigValue opacity = new TestConfigValue("client.opacity", true);
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
		));
		TestCategory commonCategory = new TestCategory(commonCategoryName, List.of(
			commonEnabled,
			commonAliases,
			commonCacheBudget
		));
		KeyMapping openKey = keyMapping("key.test_mod.openNativeScreen", GLFW.GLFW_KEY_J);
		KeyMapping toggleKey = keyMapping("key.test_mod.toggleNativeOverlay", GLFW.GLFW_KEY_O);
		AtomicBoolean defaultProviderCalled = new AtomicBoolean(false);

		List<ConfigScreenCategory> categories = createCategories(
			List.of(clientCategory, commonCategory),
			screenBuilder -> {
				screenBuilder.addCategory("quick")
					.setTitle(Component.literal("Quick"))
					.setDescription(Component.literal("Frequently changed native values"))
					.setDefaultApplyMode(ConfigValueApplyMode.IMMEDIATE)
					.setValueApplyModeByName("client.mode", ConfigValueApplyMode.ON_APPLY)
					.setValueRequiresRestartByName("client.mode")
					.addValuesByName(List.of(
						"client.enabled",
						"client.mode",
						"client.rowCount"
					));
				screenBuilder.addCategory("lists")
					.setTitle(Component.literal("Native Lists"))
					.setDescription(Component.literal("Native list values"))
					.setDefaultApplyMode(ConfigValueApplyMode.ON_APPLY)
					.setValueApplyModeByName("client.aliases", ConfigValueApplyMode.IMMEDIATE)
					.setValueRequiresRestartByName("client.opacitySteps")
					.addValuesByName(List.of(
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
					.addKeyMappings(() -> List.of(toggleKey));
				screenBuilder.configureCategory(clientCategoryName)
					.setTitle(Component.literal("Remaining Native Values"))
					.setDescription(Component.literal("Native values kept in their original category"))
					.setDefaultApplyMode(ConfigValueApplyMode.ON_APPLY)
					.setValueApplyModeByName("client.extraEffects", ConfigValueApplyMode.IMMEDIATE)
					.setValueRequiresRestartByName("client.label")
					.hideValuesByName(List.of(
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
				screenBuilder.configureCategory(commonCategoryName)
					.setTitle(Component.literal("Common Native Values"))
					.setDescription(Component.literal("Common native values kept in their original category"))
					.setDefaultApplyMode(ConfigValueApplyMode.ON_APPLY)
					.setValueApplyModeByName("common.enabled", ConfigValueApplyMode.IMMEDIATE)
					.setValueRequiresRestartByName("common.cacheBudget");
			},
			lookup -> {
				defaultProviderCalled.set(true);
				return List.of(new TestConfigValue("key.test_mod.default"));
			}
		);

		assertFalse(defaultProviderCalled.get());
		assertEquals(List.of("quick", "lists", "keyMappings", clientCategoryName, commonCategoryName), categoryNames(categories));
		assertEquals("Quick", categories.get(0).getLocalizedName().getString());
		assertEquals("Frequently changed native values", categories.get(0).getLocalizedDescription().getString());
		assertEquals("Native Lists", categories.get(1).getLocalizedName().getString());
		assertEquals("Native list values", categories.get(1).getLocalizedDescription().getString());
		assertEquals("Key Mappings", categories.get(2).getLocalizedName().getString());
		assertEquals("Native screen key mappings", categories.get(2).getLocalizedDescription().getString());
		assertEquals("Remaining Native Values", categories.get(3).getLocalizedName().getString());
		assertEquals("Native values kept in their original category", categories.get(3).getLocalizedDescription().getString());
		assertEquals("Common Native Values", categories.get(4).getLocalizedName().getString());
		assertEquals("Common native values kept in their original category", categories.get(4).getLocalizedDescription().getString());
		assertEquals(List.of("client.enabled", "client.mode", "client.rowCount"), valueNames(categories.get(0)));
		assertEquals(List.of(
			"client.enabledHistory",
			"client.favoriteRows",
			"client.favoriteModes",
			"client.aliases",
			"client.cacheBreakpoints",
			"client.opacitySteps"
		), valueNames(categories.get(1)));
		assertEquals(List.of("key.test_mod.openNativeScreen", "key.test_mod.toggleNativeOverlay"), valueNames(categories.get(2)));
		assertEquals(List.of("client.extraEffects", "client.label", "client.opacity", "client.cacheBudget"), valueNames(categories.get(3)));
		assertEquals(List.of("common.enabled", "common.aliases", "common.cacheBudget"), valueNames(categories.get(4)));

		assertEquals(ConfigValueApplyMode.IMMEDIATE, valueByName(categories.get(0), "client.enabled").getApplyMode());
		assertEquals(ConfigValueApplyMode.ON_APPLY, valueByName(categories.get(0), "client.mode").getApplyMode());
		assertEquals(ConfigValueApplyMode.IMMEDIATE, valueByName(categories.get(0), "client.rowCount").getApplyMode());
		assertTrue(valueByName(categories.get(0), "client.mode").requiresRestart());
		assertEquals(ConfigValueApplyMode.IMMEDIATE, valueByName(categories.get(1), "client.aliases").getApplyMode());
		assertTrue(valueByName(categories.get(1), "client.opacitySteps").requiresRestart());
		assertEquals(ConfigValueApplyMode.IMMEDIATE, valueByName(categories.get(3), "client.extraEffects").getApplyMode());
		assertTrue(valueByName(categories.get(3), "client.label").requiresRestart());
		assertTrue(valueByName(categories.get(3), "client.opacity").requiresRestart());
		assertEquals(ConfigValueApplyMode.IMMEDIATE, valueByName(categories.get(4), "common.enabled").getApplyMode());
		assertTrue(valueByName(categories.get(4), "common.cacheBudget").requiresRestart());
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

	private static List<String> categoryNames(List<ConfigScreenCategory> categories) {
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

	private static KeyMapping keyMapping(String name, int keyCode) {
		return new KeyMapping(
			name,
			InputConstants.Type.KEYSYM,
			keyCode,
			"key.categories.%s".formatted(MOD_ID)
		);
	}

	private record TestCategory(
		String name,
		List<IConfigScreenValue<?>> values
	) implements ConfigScreenCategory {
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

	private record TestConfigValue(
		String name,
		boolean requiresRestart
	) implements IConfigScreenValue<String>, IConfigLocalizedValue {
		private TestConfigValue(String name) {
			this(name, false);
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
		public void addListener(Consumer<String> listener) {

		}

		@Override
		public IConfigValueEditorSerializer<String> getSerializer() {
			return TestSerializer.INSTANCE;
		}

		@Override
		public boolean requiresRestart() {
			return requiresRestart;
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
		public Optional<Collection<String>> getAllValidValues() {
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
