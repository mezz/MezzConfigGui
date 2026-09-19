package net.mezzdev.config.gui;

import net.mezzdev.config.api.schema.ConfigSchemaType;
import net.mezzdev.config.api.schema.update.IConfigBatchUpdater;
import net.mezzdev.config.api.schema.category.IConfigCategory;
import net.mezzdev.config.api.schema.category.IConfigEditorCategory;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.value.editor.ConfigValueEditMode;
import net.mezzdev.config.api.value.editor.IConfigValueEditorInfo;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.change.IAppliedConfigValueChange;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.change.IConfigValueBatchChangeListener;
import net.mezzdev.config.api.value.change.IConfigValueChangeListener;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class MezzConfigScreenSchemaTest {
	@Test
	void automaticScreensReuseValueIndexesAndKeepThemScopedToEachScreen() {
		List<TestConfigValue> values = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			values.add(new TestConfigValue("value" + i, ConfigValueEditMode.BATCH));
		}
		TestConfigCategory category = new TestConfigCategory("general", "test.config.general", values);
		TestConfigSchema schema = new TestConfigSchema(List.of(category), List.of(category));
		TestConfigSchema unopened = new TestConfigSchema("other", Path.of("other.ini"), true, List.of(category), List.of(category));
		List<ConfigScreenConfig> configs = MezzConfigScreenConfigs.getConfigScreens(List.of(schema, unopened));
		assertEquals(0, schema.categoryReads().get());
		assertEquals(0, unopened.categoryReads().get());
		ConfigScreenConfig config = configs.stream().filter(c -> c.getModId().equals("test")).findFirst().orElseThrow();
		ConfigScreenSchema screen = config.getSchema();
		List<? extends IConfigScreenValue<?>> screenValues = List.copyOf(screen.getCategories().get(0).getConfigValues());
		int initialReads = schema.categoryReads().get();
		for (IConfigScreenValue<?> value : screenValues) {
			assertSame(schema, screen.findBackingSchema(value).orElseThrow());
			assertSame(schema, screen.findBackingSchema(IConfigScreenValue.withApplyMode(value, ConfigValueApplyMode.IMMEDIATE)).orElseThrow());
		}
		assertEquals(initialReads, schema.categoryReads().get(), "Lookups must reuse the value index");
		assertEquals(0, unopened.categoryReads().get());
		config.getSchema().getCategories();
		assertEquals(initialReads * 2, schema.categoryReads().get(), "Reopening builds fresh screen adapters");
	}

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

		assertEquals(ConfigValueRestartRequirement.NONE, noRestartValue.getRestartRequirement());
		assertEquals(ConfigValueRestartRequirement.WORLD_RESTART, worldRestartValue.getRestartRequirement());
		assertEquals(ConfigValueRestartRequirement.GAME_RESTART, gameRestartValue.getRestartRequirement());
	}

	@Test
	void explicitApplyModeKeepsMezzConfigRestartRequirement() {
		IConfigScreenValue<String> restartValue = IConfigScreenValue.configValue(
			new TestConfigValue("restart", ConfigValueEditMode.BATCH, ConfigValueRestartRequirement.GAME_RESTART),
			ConfigValueApplyMode.IMMEDIATE
		);

		assertEquals(ConfigValueApplyMode.IMMEDIATE, restartValue.getApplyMode());
		assertEquals(ConfigValueRestartRequirement.GAME_RESTART, restartValue.getRestartRequirement());
	}

	@Test
	void configValueUsesMezzConfigPendingValueAndListener() {
		TestPendingConfigValue backingValue = new TestPendingConfigValue("effective");
		IConfigScreenValue<String> screenValue = IConfigScreenValue.configValue(backingValue);
		List<String> listenerValues = new ArrayList<>();
		screenValue.addListener(listenerValues::add);

		backingValue.set("pending");

		assertEquals("effective", backingValue.get());
		assertEquals("pending", screenValue.getValue());
		assertEquals(List.of("pending"), listenerValues);
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
		assertEquals("label", List.copyOf(categories.get(0).getConfigValues()).get(0).getName());
		assertEquals("test.config.quick", categories.get(1).getLocalizationKey());
		assertEquals(Component.translatable("test.config.quick"), categories.get(1).getLocalizedName());
		assertEquals(Component.translatable("test.config.quick.description"), categories.get(1).getLocalizedDescription());
		assertEquals("test.config.advanced", categories.get(2).getLocalizationKey());
		IConfigScreenValue<?> quickValue = List.copyOf(categories.get(1).getConfigValues()).get(0);
		IConfigScreenValue<?> advancedValue = List.copyOf(categories.get(2).getConfigValues()).get(0);
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
		assertEquals(List.of("enabled"), valueNames(categories.get(0)));
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
		List<IConfigSchema> schemas = List.of(
			new TestConfigSchema("second_mod", Path.of("second.ini"), true, List.of(otherCategory), List.of(otherCategory)),
			new TestConfigSchema("first_mod", Path.of("b.ini"), true, List.of(secondCategory), List.of(secondCategory)),
			new TestConfigSchema("first_mod", Path.of("a.ini"), true, List.of(firstCategory), List.of(firstCategory))
		);

		List<ConfigScreenConfig> screens = MezzConfigScreenConfigs.getConfigScreens(schemas);

		assertEquals(List.of("first_mod", "second_mod"), screenModIds(screens));
		assertEquals(
			net.mezzdev.config.gui.internal.LegacyTranslations.translatableWithFallback("first_mod.config.screen.title", "First Mod Configuration"),
			screens.get(0).getTitle()
		);
		assertEquals(
			net.mezzdev.config.gui.internal.LegacyTranslations.translatableWithFallback("second_mod.config.screen.title", "Second Mod Configuration"),
			screens.get(1).getTitle()
		);
		List<? extends ConfigScreenCategory> categories = screens.get(0).getSchema().getCategories();
		assertEquals(List.of("general"), categoryNames(categories));
		assertEquals(List.of("first", "second"), valueNames(categories.get(0)));
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
		List<IConfigSchema> schemas = List.of(
			new TestConfigSchema("first_mod", Path.of("active.ini"), true, List.of(activeCategory), List.of(activeCategory)),
			new TestConfigSchema("first_mod", Path.of("inactive.ini"), false, List.of(inactiveCategory), List.of(inactiveCategory))
		);

		List<ConfigScreenConfig> screens = MezzConfigScreenConfigs.getConfigScreens(schemas);
		List<? extends ConfigScreenCategory> categories = screens.get(0).getSchema().getCategories();

		assertEquals(List.of("general"), categoryNames(categories));
		assertEquals(List.of("active"), valueNames(categories.get(0)));
	}

	@Test
	void automaticConfigScreensIncludeActiveRemoteServerSchemasWithoutLocalPaths() {
		TestConfigCategory serverCategory = new TestConfigCategory(
			"server",
			"test.config.server",
			List.of(new TestConfigValue("serverValue", ConfigValueEditMode.BATCH))
		);
		List<IConfigSchema> schemas = List.of(
			new TestConfigSchema(
				"test",
				Path.of("server.ini"),
				true,
				List.of(serverCategory),
				List.of(serverCategory),
				ConfigSchemaType.SERVER
			)
		);

		List<? extends ConfigScreenCategory> categories = MezzConfigScreenConfigs.getConfigScreens(schemas)
			.get(0)
			.getSchema()
			.getCategories();

		assertEquals(List.of("server"), categoryNames(categories));
		assertEquals(List.of("serverValue"), valueNames(categories.get(0)));
	}

	@Test
	void screenValuesResolveTheirBackingSchema() {
		TestConfigValue value = new TestConfigValue("value", ConfigValueEditMode.BATCH);
		TestConfigCategory category = new TestConfigCategory("general", "test.config.general", List.of(value));
		TestConfigSchema schema = new TestConfigSchema(List.of(category), List.of(category));
		ConfigScreenSchema screenSchema = ConfigScreenSchema.from(schema);
		IConfigScreenValue<?> screenValue = screenSchema.getCategories()
			.get(0)
			.getConfigValues()
			.iterator()
			.next();

		assertSame(schema, screenSchema.findBackingSchema(screenValue).orElseThrow());
	}

	@Test
	void customizedScreenValuesResolveTheirBackingSchemaByIdentity() {
		TestConfigValue value = new TestConfigValue("value", ConfigValueEditMode.BATCH);
		TestConfigCategory category = new TestConfigCategory("general", "test.config.general", List.of(value));
		TestConfigSchema schema = new TestConfigSchema(List.of(category), List.of(category));
		ConfigScreenSchema screenSchema = ConfigScreenSchema.from(schema);
		IConfigScreenValue<String> customizedValue = new IdentityOnlyScreenValue(
			IConfigScreenValue.configValue(value)
		);

		assertSame(schema, screenSchema.findBackingSchema(customizedValue).orElseThrow());
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
		List<IConfigEditorCategory> editorCategories,
		ConfigSchemaType type,
		AtomicInteger categoryReads
	) implements IConfigSchema {
		private TestConfigSchema(String modId, Path path, boolean active, List<TestConfigCategory> categories,
			List<IConfigEditorCategory> editorCategories, ConfigSchemaType type) {
			this(modId, path, active, categories, editorCategories, type, new AtomicInteger());
		}

		private TestConfigSchema(
			String modId,
			Path path,
			boolean active,
			List<TestConfigCategory> categories,
			List<IConfigEditorCategory> editorCategories
		) {
			this(modId, path, active, categories, editorCategories, ConfigSchemaType.CLIENT_PER_WORLD);
		}

		private TestConfigSchema(
			List<TestConfigCategory> categories,
			List<IConfigEditorCategory> editorCategories
		) {
			this(
				"test",
				Path.of("test.ini"),
				true,
				categories,
				editorCategories,
				ConfigSchemaType.CLIENT
			);
		}

		private TestConfigSchema {
			modId = Objects.requireNonNull(modId, "modId");
			path = Objects.requireNonNull(path, "path");
			categories = List.copyOf(categories);
			editorCategories = List.copyOf(editorCategories);
		}

		@Override
		public String getId() {
			return path.toString();
		}

		@Override
		public String getModId() {
			return modId;
		}

		@Override
		public ConfigSchemaType getType() {
			return type;
		}

		@Override
		public boolean isActive() {
			return active;
		}

		@Override
		public Optional<Path> getPath() {
			if (active && type != ConfigSchemaType.SERVER) {
				return Optional.of(path);
			}
			return Optional.empty();
		}

		@Override
		public List<? extends IConfigCategory> getCategories() {
			categoryReads.incrementAndGet();
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
		public Runnable addBatchListener(IConfigValueBatchChangeListener listener) {
			return () -> {};
		}

		@Override
		public Runnable addPendingBatchListener(IConfigValueBatchChangeListener listener) {
			return () -> {};
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
		public List<? extends IConfigValue<?>> getConfigValues() {
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
	) implements IConfigValue<String>, IConfigValueEditorInfo<String> {
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
		public IConfigValueSerializer<String> getSerializer() {
			return TestSerializer.INSTANCE;
		}
	}

	private static final class TestPendingConfigValue implements IConfigValue<String>, IConfigValueEditorInfo<String> {
		private final String effectiveValue;
		private String pendingValue;
		private IConfigValueChangeListener<String> pendingListener = ignored -> {};

		private TestPendingConfigValue(String value) {
			this.effectiveValue = value;
			this.pendingValue = value;
		}

		@Override
		public String getName() {
			return "pending";
		}

		@Override
		public String getLocalizationKey() {
			return "test.config.value.pending";
		}

		@Override
		public String get() {
			return effectiveValue;
		}

		@Override
		public IConfigValueEditorInfo<String> getEditorInfo() {
			return this;
		}

		@Override
		public String getPendingValue() {
			return pendingValue;
		}

		@Override
		public String getDefaultValue() {
			return effectiveValue;
		}

		@Override
		public ConfigValueEditMode getEditMode() {
			return ConfigValueEditMode.BATCH;
		}

		@Override
		public ConfigValueRestartRequirement getRestartRequirement() {
			return ConfigValueRestartRequirement.GAME_RESTART;
		}

		@Override
		public List<? extends IConfigEditorCategory> getEditorCategories() {
			return List.of();
		}

		@Override
		public boolean set(String value) {
			String oldValue = pendingValue;
			if (Objects.equals(oldValue, value)) {
				return false;
			}
			pendingValue = value;
			pendingListener.onConfigValueChanged(new TestAppliedConfigValueChange(this, oldValue, value));
			return true;
		}

		@Override
		public Runnable addListener(IConfigValueChangeListener<String> listener) {
			return () -> {};
		}

		@Override
		public Runnable addPendingListener(IConfigValueChangeListener<String> listener) {
			pendingListener = listener;
			return () -> pendingListener = ignored -> {};
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
		public IConfigValueSerializer<String> getSerializer() {
			return TestSerializer.INSTANCE;
		}
	}

	private record TestAppliedConfigValueChange(
		IConfigValue<String> configValue,
		String oldValue,
		String newValue
	) implements IAppliedConfigValueChange<String> {}

	private record IdentityOnlyScreenValue(
		IConfigScreenValue<String> delegate
	) implements IConfigScreenValue<String> {
		@Override
		public String getName() {
			return delegate.getName();
		}

		@Override
		public String getLocalizationKey() {
			return delegate.getLocalizationKey();
		}

		@Override
		public String getValue() {
			return delegate.getValue();
		}

		@Override
		public String getDefaultValue() {
			return delegate.getDefaultValue();
		}

		@Override
		public boolean set(String value) {
			return delegate.set(value);
		}

		@Override
		public Runnable addListener(Consumer<String> listener) {
			return delegate.addListener(listener);
		}

		@Override
		public ConfigValueApplyMode getApplyMode() {
			return delegate.getApplyMode();
		}

		@Override
		public ConfigValueRestartRequirement getRestartRequirement() {
			return delegate.getRestartRequirement();
		}

		@Override
		public Object getIdentityKey() {
			return delegate.getIdentityKey();
		}

		@Override
		public IConfigValueSerializer<String> getSerializer() {
			return delegate.getSerializer();
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
		public Optional<List<String>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public String getValidValuesDescription() {
			return "";
		}
	}
}
