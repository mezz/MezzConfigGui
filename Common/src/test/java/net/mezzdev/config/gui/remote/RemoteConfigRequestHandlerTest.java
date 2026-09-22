package net.mezzdev.config.gui.remote;

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
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteConfigRequestHandlerTest {
	private static final RemoteSchemaKey SCHEMA_KEY = new RemoteSchemaKey("test", "server.ini");

	@Test
	void appliesOneValidatedBatchAndReturnsEffectiveAndRestartPendingValues() {
		TestConfigValue immediate = new TestConfigValue("immediate", "old", false);
		TestConfigValue restart = new TestConfigValue("restart", "effective", true);
		TestConfigSchema schema = TestConfigSchema.create(immediate, restart);
		try (RemoteConfigRequestHandler handler = new RemoteConfigRequestHandler(() -> List.of(schema))) {
			RemoteConfigMessage.SnapshotResponse snapshot = handler.handleSnapshot(
				new RemoteConfigMessage.SnapshotRequest(1, SCHEMA_KEY),
				true
			);
			RemoteConfigMessage.UpdateResponse response = handler.handleUpdate(
				new RemoteConfigMessage.UpdateRequest(
					2,
					SCHEMA_KEY,
					snapshot.revision(),
					List.of(value("immediate", "new"), value("restart", "pending"))
				),
				true
			);

			assertTrue(response.accepted());
			assertEquals(1, response.revision());
			assertEquals(1, schema.batchCount());
			assertEquals("new", immediate.get());
			assertEquals("new", immediate.getPendingValue());
			assertEquals("effective", restart.get());
			assertEquals("pending", restart.getPendingValue());
			assertEquals(List.of(value("immediate", "new"), value("restart", "pending")), response.pendingValues());
		}
	}

	@Test
	void checksPermissionAgainForEveryRequest() {
		TestConfigValue value = new TestConfigValue("value", "old", false);
		TestConfigSchema schema = TestConfigSchema.create(value);
		try (RemoteConfigRequestHandler handler = new RemoteConfigRequestHandler(() -> List.of(schema))) {
			RemoteConfigMessage.SnapshotResponse positiveHint = handler.handleSnapshot(
				new RemoteConfigMessage.SnapshotRequest(1, SCHEMA_KEY),
				true
			);
			RemoteConfigMessage.UpdateResponse response = handler.handleUpdate(
				new RemoteConfigMessage.UpdateRequest(2, SCHEMA_KEY, positiveHint.revision(), List.of(value("value", "new"))),
				false
			);

			assertTrue(positiveHint.canEdit());
			assertFalse(response.accepted());
			assertFalse(response.canEdit());
			assertEquals(0, schema.batchCount());
			assertEquals("old", value.get());
			assertEquals(List.of(), response.pendingValues());
		}
	}

	@Test
	void permissionDenialDoesNotSerializePendingValues() {
		TestConfigValue value = new TestConfigValue("value", "secret", true, RejectingSerializeSerializer.INSTANCE);
		TestConfigSchema schema = TestConfigSchema.create(value);
		try (RemoteConfigRequestHandler handler = new RemoteConfigRequestHandler(() -> List.of(schema))) {
			RemoteConfigMessage.SnapshotResponse snapshot = handler.handleSnapshot(
				new RemoteConfigMessage.SnapshotRequest(1, SCHEMA_KEY),
				false
			);
			RemoteConfigMessage.UpdateResponse update = handler.handleUpdate(
				new RemoteConfigMessage.UpdateRequest(2, SCHEMA_KEY, 0, List.of(value("value", "new"))),
				false
			);

			assertTrue(snapshot.available());
			assertFalse(snapshot.canEdit());
			assertEquals(List.of(), snapshot.pendingValues());
			assertFalse(update.accepted());
			assertFalse(update.canEdit());
			assertEquals(List.of(), update.pendingValues());
			assertEquals(0, schema.batchCount());
		}
	}

	@Test
	void staleOperatorsRefreshInsteadOfOverwriting() {
		TestConfigValue value = new TestConfigValue("value", "old", false);
		TestConfigSchema schema = TestConfigSchema.create(value);
		try (RemoteConfigRequestHandler handler = new RemoteConfigRequestHandler(() -> List.of(schema))) {
			long sharedRevision = handler.handleSnapshot(
					new RemoteConfigMessage.SnapshotRequest(1, SCHEMA_KEY),
					true
				)
				.revision();
			RemoteConfigMessage.UpdateResponse first = handler.handleUpdate(
				new RemoteConfigMessage.UpdateRequest(2, SCHEMA_KEY, sharedRevision, List.of(value("value", "first"))),
				true
			);
			RemoteConfigMessage.UpdateResponse second = handler.handleUpdate(
				new RemoteConfigMessage.UpdateRequest(3, SCHEMA_KEY, sharedRevision, List.of(value("value", "second"))),
				true
			);

			assertTrue(first.accepted());
			assertFalse(second.accepted());
			assertEquals(first.revision(), second.revision());
			assertEquals(List.of(value("value", "first")), second.pendingValues());
			assertEquals("first", value.get());
			assertEquals(1, schema.batchCount());
		}
	}

	@Test
	void rejectsMissingAndDuplicateSchemas() {
		TestConfigSchema schema = TestConfigSchema.create(new TestConfigValue("value", "old", false));
		try (RemoteConfigRequestHandler missing = new RemoteConfigRequestHandler(List::of);
			RemoteConfigRequestHandler duplicate = new RemoteConfigRequestHandler(() -> List.of(schema, schema))
		) {
			RemoteConfigMessage.SnapshotRequest request = new RemoteConfigMessage.SnapshotRequest(1, SCHEMA_KEY);

			assertFalse(missing.handleSnapshot(request, true).available());
			assertFalse(duplicate.handleSnapshot(request, true).available());
		}
	}

	@Test
	void rejectsUnknownDuplicateAndInvalidValuesWithoutMutation() {
		TestConfigValue valid = new TestConfigValue("valid", "old", false);
		TestConfigValue invalid = new TestConfigValue("invalid", "old", false);
		TestConfigSchema schema = TestConfigSchema.create(valid, invalid);
		try (RemoteConfigRequestHandler handler = new RemoteConfigRequestHandler(() -> List.of(schema))) {
			List<List<RemoteValueData>> invalidRequests = List.of(
				List.of(value("valid", "one"), value("valid", "two")),
				List.of(new RemoteValueData(new RemoteValueKey("unknown", "valid"), "new")),
				List.of(value("unknown", "new")),
				List.of(value("valid", "new"), value("invalid", "invalid"))
			);

			long requestId = 1;
			for (List<RemoteValueData> proposedValues : invalidRequests) {
				RemoteConfigMessage.UpdateResponse response = handler.handleUpdate(
					new RemoteConfigMessage.UpdateRequest(requestId++, SCHEMA_KEY, 0, proposedValues),
					true
				);
				assertFalse(response.accepted());
			}

			assertEquals(0, schema.batchCount());
			assertEquals("old", valid.get());
			assertEquals("old", invalid.get());
		}
	}

	@Test
	void rejectsDuplicateAuthoritativeCategoriesAndValues() {
		TestConfigValue first = new TestConfigValue("same", "old", false);
		TestConfigValue second = new TestConfigValue("same", "old", false);
		TestConfigSchema duplicateCategories = new TestConfigSchema(List.of(
			new TestConfigCategory("general", List.of(first)),
			new TestConfigCategory("general", List.of(second))
		));
		TestConfigSchema duplicateValues = new TestConfigSchema(List.of(
			new TestConfigCategory("general", List.of(first, second))
		));

		for (TestConfigSchema schema : List.of(duplicateCategories, duplicateValues)) {
			try (RemoteConfigRequestHandler handler = new RemoteConfigRequestHandler(() -> List.of(schema))) {
				RemoteConfigMessage.UpdateResponse response = handler.handleUpdate(
					new RemoteConfigMessage.UpdateRequest(1, SCHEMA_KEY, 0, List.of(value("same", "new"))),
					true
				);
				assertFalse(response.accepted());
				assertEquals(0, schema.batchCount());
			}
		}
	}

	private static RemoteValueData value(String valueName, String serializedValue) {
		return new RemoteValueData(new RemoteValueKey("general", valueName), serializedValue);
	}

	private static final class TestConfigSchema implements IConfigSchema {
		private final List<TestConfigCategory> categories;
		private final List<IConfigValueBatchChangeListener> pendingListeners = new ArrayList<>();
		private int batchCount;

		private TestConfigSchema(List<TestConfigCategory> categories) {
			this.categories = List.copyOf(categories);
		}

		private static TestConfigSchema create(TestConfigValue... values) {
			return new TestConfigSchema(List.of(new TestConfigCategory("general", List.of(values))));
		}

		@Override
		public String getId() {
			return SCHEMA_KEY.schemaId();
		}

		@Override
		public String getModId() {
			return SCHEMA_KEY.modId();
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
			return Optional.of(Path.of("serverconfig/server.ini"));
		}

		@Override
		public List<? extends IConfigCategory> getCategories() {
			return categories;
		}

		@Override
		public List<? extends IConfigEditorCategory> getEditorCategories() {
			return categories;
		}

		@Override
		public List<? extends IAppliedConfigValueChange<?>> batchUpdate(Consumer<IConfigBatchUpdater> updateBatch) {
			List<PendingSet<?>> pendingSets = new ArrayList<>();
			updateBatch.accept(new IConfigBatchUpdater() {
				@Override
				public <T> IConfigBatchUpdater set(IConfigValue<T> configValue, T value) {
					pendingSets.add(new PendingSet<>(configValue, value));
					return this;
				}
			});
			List<IAppliedConfigValueChange<?>> changes = new ArrayList<>();
			for (PendingSet<?> pendingSet : pendingSets) {
				@Nullable
				IAppliedConfigValueChange<?> change = pendingSet.apply();
				if (change != null) {
					changes.add(change);
				}
			}
			batchCount++;
			if (!changes.isEmpty()) {
				List<IAppliedConfigValueChange<?>> snapshot = List.copyOf(changes);
				pendingListeners.forEach(listener -> listener.onConfigValuesChanged(snapshot));
			}
			return changes;
		}

		@Override
		public Runnable addBatchListener(IConfigValueBatchChangeListener listener) {
			return () -> {};
		}

		@Override
		public Runnable addPendingBatchListener(IConfigValueBatchChangeListener listener) {
			pendingListeners.add(listener);
			return () -> pendingListeners.remove(listener);
		}

		private int batchCount() {
			return batchCount;
		}
	}

	private record TestConfigCategory(
		String name,
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
			return "test." + name;
		}

		@Override
		public List<? extends IConfigValue<?>> getConfigValues() {
			return values;
		}
	}

	private static final class TestConfigValue implements IConfigValue<String>, IConfigValueEditorInfo<String> {
		private final String name;
		private final String defaultValue;
		private final boolean restartRequired;
		private final IConfigValueSerializer<String> serializer;
		private String effectiveValue;
		private String pendingValue;

		private TestConfigValue(String name, String value, boolean restartRequired) {
			this(name, value, restartRequired, TestSerializer.INSTANCE);
		}

		private TestConfigValue(
			String name,
			String value,
			boolean restartRequired,
			IConfigValueSerializer<String> serializer
		) {
			this.name = name;
			this.defaultValue = value;
			this.restartRequired = restartRequired;
			this.serializer = serializer;
			this.effectiveValue = value;
			this.pendingValue = value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getLocalizationKey() {
			return "test." + name;
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
			return defaultValue;
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
			return apply(value) != null;
		}

		@Nullable
		private IAppliedConfigValueChange<String> apply(String value) {
			String oldValue = pendingValue;
			if (Objects.equals(oldValue, value)) {
				return null;
			}
			pendingValue = value;
			if (!restartRequired) {
				effectiveValue = value;
			}
			return new TestAppliedChange(this, oldValue, value);
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
			return serializer;
		}
	}

	private record PendingSet<T>(IConfigValue<T> value, T proposedValue) {
		@Nullable
		private IAppliedConfigValueChange<T> apply() {
			@SuppressWarnings("unchecked")
			TestConfigValue testValue = (TestConfigValue) value;
			@SuppressWarnings("unchecked") @Nullable
			IAppliedConfigValueChange<T> change = (IAppliedConfigValueChange<T>) testValue.apply((String) proposedValue);
			return change;
		}
	}

	private record TestAppliedChange(
		IConfigValue<String> configValue,
		String oldValue,
		String newValue
	) implements IAppliedConfigValueChange<String> {}

	private enum TestSerializer implements IConfigValueSerializer<String> {
		INSTANCE;

		@Override
		public String serialize(String value) {
			return value;
		}

		@Override
		public IDeserializeResult<String> deserialize(String string) {
			if (string.equals("invalid")) {
				return IDeserializeResult.failure("invalid");
			}
			return IDeserializeResult.success(string);
		}

		@Override
		public boolean isValid(String value) {
			return true;
		}

		@Override
		public String getValidValuesDescription() {
			return "any string except invalid";
		}
	}

	private enum RejectingSerializeSerializer implements IConfigValueSerializer<String> {
		INSTANCE;

		@Override
		public String serialize(String value) {
			throw new AssertionError("Unauthorized requests must not serialize pending server values.");
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
		public String getValidValuesDescription() {
			return "any string";
		}
	}
}
