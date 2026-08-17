package net.mezzdev.config.gui;

import net.mezzdev.config.api.schema.ConfigOwnership;
import net.mezzdev.config.api.schema.ConfigScope;
import net.mezzdev.config.api.schema.IConfigBatchUpdater;
import net.mezzdev.config.api.schema.IConfigCategory;
import net.mezzdev.config.api.schema.IConfigEditorCategory;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.value.ConfigValueEditMode;
import net.mezzdev.config.api.value.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.IAppliedConfigValueChange;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
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
		IConfigScreenValue<String> firstScreenValue = new IdentityOnlyScreenValue(IConfigScreenValue.configValue(first));
		IConfigScreenValue<String> secondScreenValue = IConfigScreenValue.configValue(second);
		TestConfigSchema schema = new TestConfigSchema(first, second);

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

	@Test
	void recordsPendingRestartRequiredChangesWithoutChangingEffectiveValue() {
		TestMezzConfigValue restartRequiredValue = new TestMezzConfigValue("restartRequired", true);
		IConfigScreenValue<String> screenValue = IConfigScreenValue.configValue(restartRequiredValue);
		TestConfigSchema schema = new TestConfigSchema(
			ConfigOwnership.CLIENT,
			ConfigScope.INSTALLATION,
			restartRequiredValue
		);

		CompletableFuture<ConfigChangesResult> resultFuture = ConfigChangesHandler.applyBySchema(
			List.of(new ConfigValueChange<>(screenValue, "restartRequired changed")),
			ignored -> Optional.of(schema)
		);

		schema.completeRequest();
		ConfigChangesResult result = resultFuture.join();

		assertTrue(result.succeeded());
		assertEquals("restartRequired", restartRequiredValue.getValue());
		assertEquals("restartRequired changed", restartRequiredValue.getPendingValue());
		assertEquals("restartRequired changed", screenValue.getValue());
		assertEquals(ConfigValueRestartRequirement.GAME_RESTART, result.restartRequirement());
		AppliedConfigValueChange<?> appliedChange = result.appliedChanges().getFirst();
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
		TestConfigSchema schema = new TestConfigSchema(remote);
		List<Runnable> continuationTasks = new ArrayList<>();

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
			continuationTasks::add
		);

		schema.completeRequest();

		assertFalse(resultFuture.isDone());
		assertEquals("local", localScreenValue.getValue());
		assertEquals(1, continuationTasks.size());

		continuationTasks.getFirst().run();

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
		private final ConfigOwnership ownership;
		private final ConfigScope scope;
		private final List<IConfigValue<?>> configValues;
		private final List<Runnable> pendingUpdates = new ArrayList<>();
		private CompletableFuture<Void> request = new CompletableFuture<>();
		private int requestCount;

		private TestConfigSchema(IConfigValue<?>... configValues) {
			this(ConfigOwnership.SERVER, ConfigScope.WORLD, configValues);
		}

		private TestConfigSchema(
			ConfigOwnership ownership,
			ConfigScope scope,
			IConfigValue<?>... configValues
		) {
			this.ownership = ownership;
			this.scope = scope;
			this.configValues = List.of(configValues);
		}

		@Override
		public String getModId() {
			return "test";
		}

		@Override
		public ConfigOwnership getOwnership() {
			return ownership;
		}

		@Override
		public ConfigScope getScope() {
			return scope;
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
			return List.of(new TestConfigCategory(configValues));
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
		public Runnable addListener(Consumer<? super List<? extends IAppliedConfigValueChange<?>>> listener) {
			return () -> {};
		}

		@Override
		public Runnable addPendingListener(Consumer<? super List<? extends IAppliedConfigValueChange<?>>> listener) {
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

	private static final class TestMezzConfigValue implements IConfigValue<String> {
		private final String name;
		private final boolean restartRequired;
		private String value;
		private String pendingValue;

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
		public String getValue() {
			return value;
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
		public Runnable addListener(Consumer<? super IAppliedConfigValueChange<String>> listener) {
			return () -> {};
		}

		@Override
		public Runnable addPendingListener(Consumer<? super IAppliedConfigValueChange<String>> listener) {
			return () -> {};
		}

		@Override
		public Runnable addBatchListener(Consumer<? super List<? extends IAppliedConfigValueChange<?>>> listener) {
			return () -> {};
		}

		@Override
		public Runnable addPendingBatchListener(Consumer<? super List<? extends IAppliedConfigValueChange<?>>> listener) {
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
