package net.mezzdev.config.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.api.schema.IConfigCategory;
import net.mezzdev.config.api.value.ConfigValueEditorType;
import net.mezzdev.config.api.value.ConfigValueEditorTypes;
import net.mezzdev.config.api.value.ConfigValueUpdateType;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.IConfigValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigScreenBuilder;
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

class ConfigGuiPluginLoaderTest {
	private static final String MOD_ID = "test_mod";

	@Test
	void addsDefaultKeyMappingsCategoryAfterOriginalCategories() {
		TestConfigValue originalValue = new TestConfigValue("enabled");
		TestConfigValue keyMappingValue = new TestConfigValue("key.test_mod.open");
		TestCategory originalCategory = new TestCategory("general", List.of(originalValue));

		List<IConfigCategory> categories = createCategories(
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

		List<IConfigCategory> categories = createCategories(
			List.of(originalCategory),
			screenBuilder -> {},
			(modId, allValues) -> List.of()
		);

		assertEquals(List.of("general"), categoryNames(categories));
	}

	@Test
	void usesConfiguredKeyMappingsCategoryForDefaultMappingsWhenNoMappingsWereAddedExplicitly() {
		TestConfigValue keyMappingValue = new TestConfigValue("key.test_mod.open");

		List<IConfigCategory> categories = createCategories(
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

		List<IConfigCategory> categories = createCategories(
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

		List<IConfigCategory> categories = createCategories(
			List.of(originalCategory),
			screenBuilder -> screenBuilder.addCategory("controls")
				.addValue(modeValue),
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

		List<IConfigCategory> categories = createCategories(
			List.of(generalCategory, advancedCategory),
			screenBuilder -> screenBuilder.addCategory("overview")
				.addValue(overviewValue),
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

		List<IConfigCategory> categories = createCategories(
			List.of(generalCategory, advancedCategory),
			screenBuilder -> screenBuilder.configureCategory("advanced")
				.addValue(replacementValue),
			(modId, allValues) -> List.of()
		);

		assertEquals(List.of("advanced"), categoryNames(categories));
		assertEquals(List.of("replacement"), valueNames(categories.getFirst()));
	}

	@Test
	void titleOnlyConfiguredCategoryKeepsDefaultValues() {
		TestConfigValue advancedValue = new TestConfigValue("refreshTicks");
		TestCategory advancedCategory = new TestCategory("advanced", List.of(advancedValue));

		List<IConfigCategory> categories = createCategories(
			List.of(advancedCategory),
			screenBuilder -> screenBuilder.configureCategory("advanced")
				.setTitle(Component.literal("Advanced Settings")),
			(modId, allValues) -> List.of()
		);

		assertEquals(List.of("advanced"), categoryNames(categories));
		assertEquals("Advanced Settings", categories.getFirst().getLocalizedName().getString());
		assertEquals(List.of("refreshTicks"), valueNames(categories.getFirst()));
	}

	private static List<IConfigCategory> createCategories(
		List<? extends IConfigCategory> originalCategories,
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

	private static List<String> categoryNames(List<IConfigCategory> categories) {
		return categories.stream()
			.map(IConfigCategory::getName)
			.toList();
	}

	private static List<String> valueNames(IConfigCategory category) {
		return category.getConfigValues()
			.stream()
			.map(IConfigValue::getName)
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
		List<IConfigValue<?>> values
	) implements IConfigCategory {
		@Override
		public String getName() {
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
		public Collection<? extends IConfigValue<?>> getConfigValues() {
			return values;
		}
	}

	private record TestConfigValue(
		String name
	) implements IConfigValue<String> {
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
		public ConfigValueUpdateType getUpdateType() {
			return ConfigValueUpdateType.IMMEDIATE;
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
			return new TestDeserializeResult(string);
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

	private record TestDeserializeResult(
		String value
	) implements IConfigValueEditorSerializer.IDeserializeResult<String> {
		@Override
		public Optional<String> getResult() {
			return Optional.of(value);
		}

		@Override
		public List<String> getErrors() {
			return List.of();
		}
	}
}
