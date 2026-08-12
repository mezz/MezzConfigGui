package net.mezzdev.config.gui;

import net.mezzdev.config.api.schema.ConfigSchemaType;
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
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.model.AppliedConfigValueChange;
import net.mezzdev.config.gui.model.ConfigValueChange;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigChangesHandlerTest {
	@Test
	void stopsAtFailureAndReportsOnlySuccessfulChanges() {
		TestConfigValue first = new TestConfigValue("first", ConfigValueRestartRequirement.WORLD_RESTART, false);
		TestConfigValue failing = new TestConfigValue("failing", ConfigValueRestartRequirement.GAME_RESTART, true);
		TestConfigValue unattempted = new TestConfigValue("unattempted", ConfigValueRestartRequirement.GAME_RESTART, false);

		ConfigChangesResult result = ConfigChangesHandler.applySequentially(List.of(
			new ConfigValueChange<>(first, "first changed"),
			new ConfigValueChange<>(failing, "failing changed"),
			new ConfigValueChange<>(unattempted, "unattempted changed")
		));

		assertFalse(result.succeeded());
		assertEquals(ConfigValueRestartRequirement.WORLD_RESTART, result.restartRequirement());
		assertEquals("first changed", first.getValue());
		assertEquals("failing", failing.getValue());
		assertEquals("unattempted", unattempted.getValue());
		AppliedConfigValueChange<?> appliedChange = result.appliedChanges().getFirst();
		assertEquals("first", appliedChange.oldValue());
		assertEquals("first changed", appliedChange.newValue());
		assertSame(failing, result.failure().orElseThrow().change().configValue());
	}

	@Test
	void batchesSchemaChangesAndWaitsForAuthoritativeCompletion() {
		TestMezzConfigValue first = new TestMezzConfigValue("first");
		TestMezzConfigValue second = new TestMezzConfigValue("second");
		IConfigScreenValue<String> firstScreenValue = IConfigScreenValue.configValue(first);
		IConfigScreenValue<String> secondScreenValue = IConfigScreenValue.configValue(second);
		TestConfigSchema schema = new TestConfigSchema();

		CompletableFuture<ConfigChangesResult> resultFuture = ConfigChangesHandler.applyBySchema(
			List.of(
				new ConfigValueChange<>(firstScreenValue, "first changed"),
				new ConfigValueChange<>(secondScreenValue, "second changed")
			),
			ignored -> Optional.of(schema)
		);

		assertFalse(resultFuture.isDone());
		assertEquals(1, schema.getRequestCount());
		assertEquals("first", first.getValue());
		assertEquals("second", second.getValue());

		schema.completeRequest();
		ConfigChangesResult result = resultFuture.join();

		assertTrue(result.succeeded());
		assertEquals("first changed", first.getValue());
		assertEquals("second changed", second.getValue());
		assertEquals(2, result.appliedChanges().size());
	}

	private static final class TestConfigValue implements IConfigScreenValue<String> {
		private final String name;
		private final ConfigValueRestartRequirement restartRequirement;
		private final boolean fails;
		private String value;

		private TestConfigValue(String value, ConfigValueRestartRequirement restartRequirement, boolean fails) {
			this.name = value;
			this.value = value;
			this.restartRequirement = restartRequirement;
			this.fails = fails;
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
		public String getValue() {
			return value;
		}

		@Override
		public String getDefaultValue() {
			return name;
		}

		@Override
		public boolean set(String value) {
			if (fails) {
				throw new IllegalStateException("schema is no longer active");
			}
			if (Objects.equals(this.value, value)) {
				return false;
			}
			this.value = value;
			return true;
		}

		@Override
		public Runnable addListener(Consumer<String> listener) {
			return () -> {};
		}

		@Override
		public ConfigValueRestartRequirement getRestartRequirement() {
			return restartRequirement;
		}

		@Override
		public IConfigValueSerializer<String> getSerializer() {
			return TestSerializer.INSTANCE;
		}
	}

	private static final class TestConfigSchema implements IConfigSchema {
		private final List<Runnable> pendingUpdates = new ArrayList<>();
		private CompletableFuture<Void> request = new CompletableFuture<>();
		private int requestCount;

		@Override
		public String getModId() {
			return "test";
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
		public boolean canEdit() {
			return true;
		}

		@Override
		public Optional<java.nio.file.Path> getPath() {
			return Optional.empty();
		}

		@Override
		public List<? extends IConfigCategory> getCategories() {
			return List.of();
		}

		@Override
		public List<? extends IConfigEditorCategory> getEditorCategories() {
			return List.of();
		}

		@Override
		public List<? extends IAppliedConfigValueChange<?>> batchUpdate(Consumer<IConfigBatchUpdater> updateBatch) {
			throw new UnsupportedOperationException();
		}

		@Override
		public CompletableFuture<Void> requestBatchUpdate(Consumer<IConfigBatchUpdater> updateBatch) {
			requestCount++;
			updateBatch.accept(new IConfigBatchUpdater() {
				@Override
				public <T> IConfigBatchUpdater set(IConfigValue<T> configValue, T value) {
					pendingUpdates.add(() -> configValue.set(value));
					return this;
				}
			});
			return request;
		}

		@Override
		public Runnable addListener(IConfigValueBatchChangeListener listener) {
			return () -> {};
		}

		private int getRequestCount() {
			return requestCount;
		}

		private void completeRequest() {
			pendingUpdates.forEach(Runnable::run);
			request.complete(null);
		}
	}

	private static final class TestMezzConfigValue implements IConfigValue<String> {
		private final String name;
		private String value;

		private TestMezzConfigValue(String value) {
			this.name = value;
			this.value = value;
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
		public String getValue() {
			return value;
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
			if (Objects.equals(this.value, value)) {
				return false;
			}
			this.value = value;
			return true;
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
			return value != null;
		}

		@Override
		public Optional<List<String>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public String getValidValuesDescription() {
			return "any non-null string";
		}
	}
}
