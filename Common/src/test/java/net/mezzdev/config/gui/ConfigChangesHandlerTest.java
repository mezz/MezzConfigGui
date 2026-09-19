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
import net.mezzdev.config.gui.model.AppliedConfigValueChange;
import net.mezzdev.config.gui.model.ConfigValueChange;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
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
		AppliedConfigValueChange<?> appliedChange = result.appliedChanges().get(0);
		assertEquals("first", appliedChange.oldValue());
		assertEquals("first changed", appliedChange.newValue());
		assertSame(failing, result.failure().orElseThrow().change().configValue());
	}

	@Test
	void batchesLocalClientSchemaChangesSynchronously() {
		TestMezzConfigValue first = new TestMezzConfigValue("first");
		TestMezzConfigValue second = new TestMezzConfigValue("second");
		IConfigScreenValue<String> firstScreenValue = new IdentityOnlyScreenValue(IConfigScreenValue.configValue(first));
		IConfigScreenValue<String> secondScreenValue = IConfigScreenValue.configValue(second);
		TestConfigSchema schema = new TestConfigSchema(ConfigSchemaType.CLIENT, Optional.of(Path.of("client.ini")), first, second);

		CompletableFuture<ConfigChangesResult> resultFuture = ConfigChangesHandler.applyBySchema(
			List.of(
				new ConfigValueChange<>(firstScreenValue, "first changed"),
				new ConfigValueChange<>(secondScreenValue, "second changed")
			),
			ignored -> Optional.of(schema)
		);

		assertTrue(resultFuture.isDone());
		assertEquals(1, schema.getBatchCount());
		ConfigChangesResult result = resultFuture.join();

		assertTrue(result.succeeded());
		assertEquals("first changed", first.get());
		assertEquals("second changed", second.get());
		assertEquals(2, result.appliedChanges().size());
	}

	@Test
	void batchesLocallyAuthoritativeServerSchemaChangesSynchronously() {
		TestMezzConfigValue value = new TestMezzConfigValue("server");
		IConfigScreenValue<String> screenValue = IConfigScreenValue.configValue(value);
		TestConfigSchema schema = new TestConfigSchema(
			ConfigSchemaType.SERVER,
			Optional.of(Path.of("serverconfig/server.ini")),
			value
		);

		ConfigChangesResult result = ConfigChangesHandler.applyBySchema(
				List.of(new ConfigValueChange<>(screenValue, "changed")),
				ignored -> Optional.of(schema)
			)
			.join();

		assertTrue(result.succeeded());
		assertEquals(1, schema.getBatchCount());
		assertEquals("changed", value.get());
	}

	@Test
	void recordsPendingRestartRequiredChangesWithoutChangingEffectiveValue() {
		TestMezzConfigValue restartRequiredValue = new TestMezzConfigValue("restartRequired", true);
		IConfigScreenValue<String> screenValue = IConfigScreenValue.configValue(restartRequiredValue);
		TestConfigSchema schema = new TestConfigSchema(
			ConfigSchemaType.CLIENT,
			Optional.of(Path.of("client.ini")),
			restartRequiredValue
		);

		CompletableFuture<ConfigChangesResult> resultFuture = ConfigChangesHandler.applyBySchema(
			List.of(new ConfigValueChange<>(screenValue, "restartRequired changed")),
			ignored -> Optional.of(schema)
		);

		ConfigChangesResult result = resultFuture.join();

		assertTrue(result.succeeded());
		assertEquals("restartRequired", restartRequiredValue.get());
		assertEquals("restartRequired changed", restartRequiredValue.getPendingValue());
		assertEquals("restartRequired changed", screenValue.getValue());
		assertEquals(ConfigValueRestartRequirement.GAME_RESTART, result.restartRequirement());
		AppliedConfigValueChange<?> appliedChange = result.appliedChanges().get(0);
		assertEquals("restartRequired", appliedChange.oldValue());
		assertEquals("restartRequired changed", appliedChange.newValue());
	}

	@Test
	void continuesFollowingBatchesOnTheConfiguredExecutor() {
		TestMezzConfigValue remote = new TestMezzConfigValue("remote");
		IConfigScreenValue<String> remoteScreenValue = IConfigScreenValue.configValue(remote);
		TestConfigValue localScreenValue = new TestConfigValue(
			"local",
			ConfigValueRestartRequirement.NONE,
			false
		);
		TestConfigSchema schema = new TestConfigSchema(ConfigSchemaType.SERVER, Optional.empty(), remote);
		List<Runnable> continuationTasks = new ArrayList<>();
		CompletableFuture<Void> remoteRequest = new CompletableFuture<>();

		CompletableFuture<ConfigChangesResult> resultFuture = ConfigChangesHandler.applyBySchema(
			List.of(
				new ConfigValueChange<>(remoteScreenValue, "remote changed"),
				new ConfigValueChange<>(localScreenValue, "local changed")
			),
			value -> {
				if (value.getIdentityKey() == remote) {
					return Optional.of(schema);
				}
				return Optional.empty();
			},
			continuationTasks::add,
			(ignoredSchema, ignoredChanges) -> remoteRequest
		);

		assertEquals(0, schema.getBatchCount());
		assertEquals(0, remote.getSetCount());
		remoteRequest.complete(null);

		assertFalse(resultFuture.isDone());
		assertEquals("local", localScreenValue.getValue());
		assertEquals(1, continuationTasks.size());

		continuationTasks.get(0).run();

		assertTrue(resultFuture.join().succeeded());
		assertEquals("local changed", localScreenValue.getValue());
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
		private final ConfigSchemaType type;
		private final Optional<Path> path;
		private final List<IConfigValue<?>> configValues;
		private int batchCount;

		private TestConfigSchema(
			ConfigSchemaType type,
			Optional<Path> path,
			IConfigValue<?>... configValues
		) {
			this.type = type;
			this.path = path;
			this.configValues = List.of(configValues);
		}

		@Override
		public String getId() {
			return "test.ini";
		}

		@Override
		public String getModId() {
			return "test";
		}

		@Override
		public ConfigSchemaType getType() {
			return type;
		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public Optional<Path> getPath() {
			return path;
		}

		@Override
		public List<? extends IConfigCategory> getCategories() {
			return List.of(new TestConfigCategory(configValues));
		}

		@Override
		public List<? extends IConfigEditorCategory> getEditorCategories() {
			return List.of();
		}

		@Override
		public List<? extends IAppliedConfigValueChange<?>> batchUpdate(Consumer<IConfigBatchUpdater> updateBatch) {
			batchCount++;
			updateBatch.accept(new IConfigBatchUpdater() {
				@Override
				public <T> IConfigBatchUpdater set(IConfigValue<T> configValue, T value) {
					configValue.set(value);
					return this;
				}
			});
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

		private int getBatchCount() {
			return batchCount;
		}
	}

	private record TestConfigCategory(
		List<IConfigValue<?>> configValues
	) implements IConfigCategory {
		@Override
		public String getName() {
			return "test";
		}

		@Override
		public String getLocalizationKey() {
			return "test";
		}

		@Override
		public List<? extends IConfigValue<?>> getConfigValues() {
			return configValues;
		}
	}

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

	private static final class TestMezzConfigValue implements IConfigValue<String>, IConfigValueEditorInfo<String> {
		private final String name;
		private final boolean restartRequired;
		private String value;
		private String pendingValue;
		private int setCount;

		private TestMezzConfigValue(String value) {
			this(value, false);
		}

		private TestMezzConfigValue(String value, boolean restartRequired) {
			this.name = value;
			this.restartRequired = restartRequired;
			this.value = value;
			this.pendingValue = value;
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
		public String get() {
			return value;
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
			return name;
		}

		@Override
		public ConfigValueEditMode getEditMode() {
			return ConfigValueEditMode.BATCH;
		}

		@Override
		public ConfigValueRestartRequirement getRestartRequirement() {
			if (restartRequired) {
				return ConfigValueRestartRequirement.GAME_RESTART;
			}
			return ConfigValueRestartRequirement.NONE;
		}

		@Override
		public List<? extends IConfigEditorCategory> getEditorCategories() {
			return List.of();
		}

		@Override
		public boolean set(String value) {
			setCount++;
			String storedValue = this.value;
			if (restartRequired) {
				storedValue = pendingValue;
			}
			if (Objects.equals(storedValue, value)) {
				return false;
			}
			if (restartRequired) {
				pendingValue = value;
			} else {
				this.value = value;
				pendingValue = value;
			}
			return true;
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

		private int getSetCount() {
			return setCount;
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
