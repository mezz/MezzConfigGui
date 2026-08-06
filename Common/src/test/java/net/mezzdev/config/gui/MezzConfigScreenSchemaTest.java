package net.mezzdev.config.gui;

import net.mezzdev.config.api.schema.IConfigBatchUpdater;
import net.mezzdev.config.api.schema.IConfigCategory;
import net.mezzdev.config.api.schema.IConfigEditorCategory;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.value.ConfigValueEditMode;
import net.mezzdev.config.api.value.IAppliedConfigValueChange;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.IConfigValueBatchChangeListener;
import net.mezzdev.config.api.value.IConfigValueChangeListener;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
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
		IConfigScreenValue<String> restartValue = IConfigScreenValue.configValue(new TestConfigValue("restart", ConfigValueEditMode.RESTART));

		assertEquals(ConfigValueApplyMode.IMMEDIATE, immediateValue.getApplyMode());
		assertFalse(immediateValue.requiresRestart());
		assertEquals(ConfigValueApplyMode.ON_APPLY, batchValue.getApplyMode());
		assertFalse(batchValue.requiresRestart());
		assertEquals(ConfigValueApplyMode.ON_APPLY, restartValue.getApplyMode());
		assertTrue(restartValue.requiresRestart());
	}

	@Test
	void explicitApplyModeKeepsMezzConfigRestartRequirement() {
		IConfigScreenValue<String> restartValue = IConfigScreenValue.configValue(
			new TestConfigValue("restart", ConfigValueEditMode.RESTART),
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
		List<TestConfigCategory> categories,
		List<IConfigEditorCategory> editorCategories
	) implements IConfigSchema {
		private TestConfigSchema {
			categories = List.copyOf(categories);
			editorCategories = List.copyOf(editorCategories);
		}

		@Override
		public Path getPath() {
			return Path.of("test.ini");
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
		List<IConfigEditorCategory> editorCategories
	) implements IConfigValue<String> {
		private TestConfigValue(String name, ConfigValueEditMode editMode) {
			this(name, editMode, List.of());
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
