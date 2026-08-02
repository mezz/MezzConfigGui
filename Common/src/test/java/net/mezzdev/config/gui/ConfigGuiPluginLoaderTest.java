package net.mezzdev.config.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigScreenBuilder;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigScreenValueReference;
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
			(modId, allValues) -> {
				assertEquals(MOD_ID, modId);
				assertEquals(List.of(originalValue), allValues);
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
			(modId, allValues) -> List.of()
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
			(modId, allValues) -> List.of(keyMappingValue)
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
			(modId, allValues) -> {
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
	void manualCategoryValuesReplaceDefaultValuesForThatCategory() {
		TestConfigValue primaryValue = new TestConfigValue("primary");
		TestConfigValue secondaryValue = new TestConfigValue("secondary");
		TestConfigValue modeValue = new TestConfigValue("mode");
		TestCategory originalCategory = new TestCategory("controls", List.of(primaryValue, secondaryValue, modeValue));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(originalCategory),
			screenBuilder -> screenBuilder.addCategory("controls")
				.addScreenValue(modeValue),
			(modId, allValues) -> List.of()
		);

		assertEquals(List.of("controls"), categoryNames(categories));
		assertEquals(List.of("mode"), valueNames(categories.getFirst()));
	}

	@Test
	void manualScreenLayoutOmitsUnconfiguredDefaultCategories() {
		TestConfigValue generalValue = new TestConfigValue("enabled");
		TestConfigValue advancedValue = new TestConfigValue("refreshTicks");
		TestConfigValue overviewValue = new TestConfigValue("combined");
		TestCategory generalCategory = new TestCategory("general", List.of(generalValue));
		TestCategory advancedCategory = new TestCategory("advanced", List.of(advancedValue));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(generalCategory, advancedCategory),
			screenBuilder -> screenBuilder.addCategory("overview")
				.addScreenValue(overviewValue),
			(modId, allValues) -> List.of()
		);

		assertEquals(List.of("overview"), categoryNames(categories));
		assertEquals(List.of("combined"), valueNames(categories.getFirst()));
	}

	@Test
	void manualConfiguredCategoryValuesOmitUnconfiguredDefaultCategories() {
		TestConfigValue generalValue = new TestConfigValue("enabled");
		TestConfigValue advancedValue = new TestConfigValue("refreshTicks");
		TestConfigValue replacementValue = new TestConfigValue("replacement");
		TestCategory generalCategory = new TestCategory("general", List.of(generalValue));
		TestCategory advancedCategory = new TestCategory("advanced", List.of(advancedValue));

		List<ConfigScreenCategory> categories = createCategories(
			List.of(generalCategory, advancedCategory),
			screenBuilder -> screenBuilder.configureCategory("advanced")
				.addScreenValue(replacementValue),
			(modId, allValues) -> List.of()
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
			(modId, allValues) -> List.of()
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
			(modId, allValues) -> List.of()
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
				.setValueRequiresRestart(IConfigScreenValueReference.screenValue(restartValue)),
			(modId, allValues) -> List.of()
		);

		List<? extends IConfigScreenValue<?>> values = List.copyOf(categories.getFirst().getConfigValues());
		assertFalse(values.get(0).requiresRestart());
		assertTrue(values.get(1).requiresRestart());
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
		String name
	) implements IConfigScreenValue<String>, IConfigLocalizedValue {
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
