package net.mezzdev.config.gui;

import net.mezzdev.config.api.files.IConfigManager;
import net.mezzdev.config.api.schema.IConfigBatchUpdater;
import net.mezzdev.config.api.schema.IConfigCategory;
import net.mezzdev.config.api.schema.IConfigEditorCategory;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.value.ConfigValueEditMode;
import net.mezzdev.config.api.value.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.IAppliedConfigValueChange;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.IConfigValueBatchChangeListener;
import net.mezzdev.config.api.value.IConfigValueChangeListener;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MezzConfigScreenSchemaTest {
	@Test
	void configValueUsesMezzConfigEditMode() {
		IConfigScreenValue<String> immediateValue = IConfigScreenValue.configValue(new TestConfigValue("immediate", ConfigValueEditMode.IMMEDIATE));
		IConfigScreenValue<String> batchValue = IConfigScreenValue.configValue(new TestConfigValue("batch", ConfigValueEditMode.BATCH));

		assertEquals(ConfigValueApplyMode.IMMEDIATE, immediateValue.getApplyMode());
		assertEquals(ConfigValueApplyMode.ON_APPLY, batchValue.getApplyMode());
	}

	@Test
	void configValueUsesMezzConfigRestartRequirement() {
		IConfigScreenValue<String> noRestartValue = IConfigScreenValue.configValue(new TestConfigValue("noRestart", ConfigValueEditMode.BATCH));
		IConfigScreenValue<String> worldRestartValue = IConfigScreenValue.configValue(new TestConfigValue(
			"worldRestart",
			ConfigValueEditMode.BATCH,
			ConfigValueRestartRequirement.WORLD_RESTART
		));
		IConfigScreenValue<String> gameRestartValue = IConfigScreenValue.configValue(new TestConfigValue(
			"restart",
			ConfigValueEditMode.BATCH,
			ConfigValueRestartRequirement.GAME_RESTART
		));

		assertFalse(noRestartValue.requiresRestart());
		assertTrue(worldRestartValue.requiresRestart());
		assertTrue(gameRestartValue.requiresRestart());
	}

	@Test
	void explicitApplyModeKeepsMezzConfigRestartRequirement() {
		IConfigScreenValue<String> restartValue = IConfigScreenValue.configValue(
			new TestConfigValue("restart", ConfigValueEditMode.BATCH, ConfigValueRestartRequirement.GAME_RESTART),
			ConfigValueApplyMode.IMMEDIATE
		);

		assertEquals(ConfigValueApplyMode.IMMEDIATE, restartValue.getApplyMode());
		assertTrue(restartValue.requiresRestart());
	}

	@Test
	void schemaUsesMezzConfigEditorCategories() {
		TestEditorCategory quick = new TestEditorCategory("quick", "test.config.quick");
		TestEditorCategory advanced = new TestEditorCategory("advanced", "test.config.advanced");
		TestConfigCategory general = new TestConfigCategory("general", "test.config.general", List.of());
		TestConfigValue enabled = new TestConfigValue("enabled", ConfigValueEditMode.IMMEDIATE, List.of(quick, advanced));
		TestConfigValue label = new TestConfigValue("label", ConfigValueEditMode.BATCH);
		TestConfigSchema schema = new TestConfigSchema(List.of(
			new TestConfigCategory(general.name(), general.localizationKey(), List.of(enabled, label))
		), List.of(general, quick, advanced));

		List<? extends ConfigScreenCategory> categories = ConfigScreenSchema.from(schema).getCategories();

		assertEquals(List.of("general", "quick", "advanced"), categoryNames(categories));
		assertEquals("label", List.copyOf(categories.get(0).getConfigValues()).getFirst().getName());
		assertEquals("test.config.quick", categories.get(1).getLocalizationKey());
		assertEquals(Component.translatable("test.config.quick"), categories.get(1).getLocalizedName());
		assertEquals(Component.translatable("test.config.quick.description"), categories.get(1).getLocalizedDescription());
		assertEquals("test.config.advanced", categories.get(2).getLocalizationKey());
		IConfigScreenValue<?> quickValue = List.copyOf(categories.get(1).getConfigValues()).getFirst();
		IConfigScreenValue<?> advancedValue = List.copyOf(categories.get(2).getConfigValues()).getFirst();
		assertSame(quickValue, advancedValue);
		assertEquals("enabled", quickValue.getName());
		assertEquals(ConfigValueApplyMode.IMMEDIATE, quickValue.getApplyMode());
	}

	@Test
	void schemaOmitsStorageCategoryWhenAllValuesUseEditorCategories() {
		TestEditorCategory quick = new TestEditorCategory("quick", "test.config.quick");
		TestConfigCategory general = new TestConfigCategory("general", "test.config.general", List.of());
		TestConfigValue enabled = new TestConfigValue("enabled", ConfigValueEditMode.BATCH, List.of(quick));
		TestConfigSchema schema = new TestConfigSchema(List.of(
			new TestConfigCategory(general.name(), general.localizationKey(), List.of(enabled))
		), List.of(general, quick));

		List<? extends ConfigScreenCategory> categories = ConfigScreenSchema.from(schema).getCategories();

		assertEquals(List.of("quick"), categoryNames(categories));
		assertEquals(List.of("enabled"), valueNames(categories.getFirst()));
	}

	@Test
	void automaticConfigScreensGroupSchemasByModIdAndMergeCategories() {
		TestConfigCategory firstCategory = new TestConfigCategory(
			"general",
			"test.config.general",
			List.of(new TestConfigValue("first", ConfigValueEditMode.BATCH))
		);
		TestConfigCategory secondCategory = new TestConfigCategory(
			"general",
			"test.config.general",
			List.of(new TestConfigValue("second", ConfigValueEditMode.IMMEDIATE))
		);
		TestConfigCategory otherCategory = new TestConfigCategory(
			"client",
			"test.config.client",
			List.of(new TestConfigValue("other", ConfigValueEditMode.BATCH))
		);
		TestConfigManager manager = new TestConfigManager(List.of(
			new TestConfigSchema("second_mod", Path.of("second.ini"), true, List.of(otherCategory), List.of(otherCategory)),
			new TestConfigSchema("first_mod", Path.of("b.ini"), true, List.of(secondCategory), List.of(secondCategory)),
			new TestConfigSchema("first_mod", Path.of("a.ini"), true, List.of(firstCategory), List.of(firstCategory))
		));

		List<ConfigScreenConfig> screens = MezzConfigScreenConfigs.getConfigScreens(manager);

		assertEquals(List.of("first_mod", "second_mod"), screenModIds(screens));
		assertEquals(
			Component.translatableWithFallback("first_mod.config.screen.title", "First Mod Configuration"),
			screens.getFirst().getTitle()
		);
		assertEquals(
			Component.translatableWithFallback("second_mod.config.screen.title", "Second Mod Configuration"),
			screens.get(1).getTitle()
		);
		List<? extends ConfigScreenCategory> categories = screens.getFirst().getSchema().getCategories();
		assertEquals(List.of("general"), categoryNames(categories));
		assertEquals(List.of("first", "second"), valueNames(categories.getFirst()));
	}

	@Test
	void automaticConfigScreensSkipInactiveSchemasWhenBuildingCategories() {
		TestConfigCategory activeCategory = new TestConfigCategory(
			"general",
			"test.config.general",
			List.of(new TestConfigValue("active", ConfigValueEditMode.BATCH))
		);
		TestConfigCategory inactiveCategory = new TestConfigCategory(
			"inactive",
			"test.config.inactive",
			List.of(new TestConfigValue("inactive", ConfigValueEditMode.BATCH))
		);
		TestConfigManager manager = new TestConfigManager(List.of(
			new TestConfigSchema("first_mod", Path.of("active.ini"), true, List.of(activeCategory), List.of(activeCategory)),
			new TestConfigSchema("first_mod", Path.of("inactive.ini"), false, List.of(inactiveCategory), List.of(inactiveCategory))
		));

		List<ConfigScreenConfig> screens = MezzConfigScreenConfigs.getConfigScreens(manager);
		List<? extends ConfigScreenCategory> categories = screens.getFirst().getSchema().getCategories();

		assertEquals(List.of("general"), categoryNames(categories));
		assertEquals(List.of("active"), valueNames(categories.getFirst()));
	}

	private static List<String> screenModIds(List<? extends ConfigScreenConfig> screens) {
		return screens.stream()
			.map(ConfigScreenConfig::getModId)
			.toList();
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

	private record TestConfigSchema(
		String modId,
		Path path,
		boolean active,
		List<TestConfigCategory> categories,
		List<IConfigEditorCategory> editorCategories
	) implements IConfigSchema {
		private TestConfigSchema(
			List<TestConfigCategory> categories,
			List<IConfigEditorCategory> editorCategories
		) {
			this("test", Path.of("test.ini"), true, categories, editorCategories);
		}

		private TestConfigSchema {
			modId = Objects.requireNonNull(modId, "modId");
			path = Objects.requireNonNull(path, "path");
			categories = List.copyOf(categories);
			editorCategories = List.copyOf(editorCategories);
		}

		@Override
		public String getModId() {
			return modId;
		}

		@Override
		public Optional<Path> getPath() {
			if (active) {
				return Optional.of(path);
			}
			return Optional.empty();
		}

		@Override
		public List<? extends IConfigCategory> getCategories() {
			return categories;
		}

		@Override
		public List<? extends IConfigEditorCategory> getEditorCategories() {
			return editorCategories;
		}

		@Override
		public List<? extends IAppliedConfigValueChange<?>> batchUpdate(Consumer<IConfigBatchUpdater> updateBatch) {
			return List.of();
		}

		@Override
		public Runnable addListener(IConfigValueBatchChangeListener listener) {
			return () -> {};
		}

		@Override
		public void clearListeners() {

		}
	}

	private record TestConfigManager(
		Collection<? extends IConfigSchema> schemas
	) implements IConfigManager {
		private TestConfigManager {
			schemas = List.copyOf(schemas);
		}

		@Override
		public Collection<? extends IConfigSchema> getSchemas() {
			return schemas;
		}
	}

	private record TestConfigCategory(
		String name,
		String localizationKey,
		List<TestConfigValue> values
	) implements IConfigCategory {
		private TestConfigCategory {
			values = List.copyOf(values);
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
		public Collection<? extends IConfigValue<?>> getConfigValues() {
			return values;
		}
	}

	private record TestEditorCategory(
		String name,
		String localizationKey
	) implements IConfigEditorCategory {
		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getLocalizationKey() {
			return localizationKey;
		}
	}

	private record TestConfigValue(
		String name,
		ConfigValueEditMode editMode,
		ConfigValueRestartRequirement restartRequirement,
		List<IConfigEditorCategory> editorCategories
	) implements IConfigValue<String> {
		private TestConfigValue(String name, ConfigValueEditMode editMode) {
			this(name, editMode, ConfigValueRestartRequirement.NONE, List.of());
		}

		private TestConfigValue(String name, ConfigValueEditMode editMode, ConfigValueRestartRequirement restartRequirement) {
			this(name, editMode, restartRequirement, List.of());
		}

		private TestConfigValue(String name, ConfigValueEditMode editMode, List<IConfigEditorCategory> editorCategories) {
			this(name, editMode, ConfigValueRestartRequirement.NONE, editorCategories);
		}

		private TestConfigValue {
			editorCategories = List.copyOf(editorCategories);
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getLocalizationKey() {
			return "test.config.value." + name;
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
		public ConfigValueEditMode getEditMode() {
			return editMode;
		}

		@Override
		public ConfigValueRestartRequirement getRestartRequirement() {
			return restartRequirement;
		}

		@Override
		public List<? extends IConfigEditorCategory> getEditorCategories() {
			return editorCategories;
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
		public Runnable addBatchListener(IConfigValueBatchChangeListener listener) {
			return () -> {};
		}

		@Override
		public IConfigValueSerializer<String> getSerializer() {
			return TestSerializer.INSTANCE;
		}
	}

	private enum TestSerializer implements IConfigValueSerializer<String> {
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
		public String getValidValuesDescription() {
			return "";
		}
	}
}
