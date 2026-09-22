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
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.model.ConfigValueChange;
import net.mezzdev.config.gui.ConfigScreenSchema;
import net.mezzdev.config.gui.info.ConfigServerInfo;
import net.mezzdev.config.gui.info.ServerConfigAccess;
import net.mezzdev.config.gui.model.ConfigCategoryWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteConfigEditorTest {
	private static final RemoteSchemaKey SCHEMA_KEY = new RemoteSchemaKey("test", "server.ini");
	private final RemoteConfigEditor editor = RemoteConfigEditor.getInstance();

	@AfterEach
	void disconnect() {
		RemoteConfigEditor.onClientDisconnect();
	}

	@Test
	void serverInfoFollowsConfirmedAccessWithoutRebuildingTheCategory() {
		RequestCapture capture = connectAndCapture();
		TestConfigValue value = new TestConfigValue("effective");
		TestConfigSchema schema = new TestConfigSchema(value);
		IConfigScreenValue<String> screenValue = editor.createScreenValue(schema, value);
		ConfigScreenSchema screenSchema = ConfigScreenSchema.from(schema);
		ConfigServerInfo info = new ConfigServerInfo(screenSchema);
		var descriptions = info.forValues(Stream.of(screenValue, screenValue));
		ConfigCategoryWidget category = new ConfigCategoryWidget(screenSchema.getCategories().getFirst(), List.of(),
			List.of(new ConfigCategoryWidget.Section(0, Component.literal("Nested"), Component.empty())), () -> {}, descriptions);

		assertEquals(ServerConfigAccess.CHECKING.getDescription(), category.getInfo().lines().getFirst());
		assertFalse(editor.isEditable(schema));
		RemoteConfigMessage.SnapshotRequest request = (RemoteConfigMessage.SnapshotRequest) capture.take();
		sendResponse(new RemoteConfigMessage.SnapshotResponse(request.requestId(), SCHEMA_KEY, true, false, "", 0, List.of()));
		assertEquals(ServerConfigAccess.OP_REQUIRED.getDescription(), category.getInfo().lines().getFirst());
		assertFalse(editor.isEditable(schema));

		editor.ensureSnapshot(schema);
		request = (RemoteConfigMessage.SnapshotRequest) capture.take();
		sendResponse(new RemoteConfigMessage.SnapshotResponse(request.requestId(), SCHEMA_KEY, true, true, "", 1, List.of(valueData("effective"))));
		assertEquals(List.of(ServerConfigAccess.EDITABLE.getDescription()), descriptions.get());
		assertEquals(ServerConfigAccess.EDITABLE.getDescription(), category.getCategoryHeader().getInfo().lines().getFirst());
		assertEquals(ServerConfigAccess.EDITABLE.getDescription(), category.getSectionHeader(0).getInfo().lines().getFirst());
		assertTrue(editor.isEditable(schema));

		CompletableFuture<Void> update = editor.requestUpdate(schema, List.of(new ConfigValueChange<>(screenValue, "changed")));
		RemoteConfigMessage.UpdateRequest updateRequest = (RemoteConfigMessage.UpdateRequest) capture.take();
		sendResponse(new RemoteConfigMessage.UpdateResponse(updateRequest.requestId(), SCHEMA_KEY, false, false, "Permission revoked", 1, List.of()));
		assertTrue(update.isCompletedExceptionally());
		assertEquals(ServerConfigAccess.OP_REQUIRED.getDescription(), category.getInfo().lines().getFirst());
		assertFalse(editor.isEditable(schema));

		RemoteConfigEditor.onClientTick(false);
		assertEquals(ServerConfigAccess.READ_ONLY.getDescription(), category.getInfo().lines().getFirst());
		RemoteConfigEditor.onClientDisconnect();
		assertEquals(ServerConfigAccess.UNAVAILABLE.getDescription(), category.getInfo().lines().getFirst());
	}

	@Test
	void failedOrTimedOutChecksDoNotClaimPermissionIsStillBeingChecked() {
		RequestCapture capture = connectAndCapture();
		TestConfigSchema schema = new TestConfigSchema(new TestConfigValue("effective"));
		editor.ensureSnapshot(schema);
		assertEquals(ServerConfigAccess.CHECKING, editor.getServerAccess(schema));
		editor.tick(Long.MAX_VALUE);
		assertEquals(ServerConfigAccess.UNAVAILABLE, editor.getServerAccess(schema));
		editor.ensureSnapshot(schema);
		RemoteConfigMessage.SnapshotRequest request = (RemoteConfigMessage.SnapshotRequest) capture.take();
		sendResponse(new RemoteConfigMessage.SnapshotResponse(request.requestId(), SCHEMA_KEY, false, false, "Unavailable", 0, List.of()));
		assertEquals(ServerConfigAccess.UNAVAILABLE, editor.getServerAccess(schema));
	}

	@Test
	void localServerInfoTracksWorldAvailabilityAndClientConfigsHaveNoServerInfo() {
		TestConfigValue value = new TestConfigValue("effective");
		TestConfigSchema schema = new TestConfigSchema(value);
		schema.path = Path.of("serverconfig", "test.ini");
		ConfigScreenSchema screenSchema = ConfigScreenSchema.from(schema);
		var descriptions = new ConfigServerInfo(screenSchema).forValues(Stream.of(IConfigScreenValue.configValue(value)));
		assertEquals(List.of(ServerConfigAccess.LOCAL.getDescription()), descriptions.get());
		assertTrue(editor.isEditable(schema));
		schema.active = false;
		assertEquals(List.of(ServerConfigAccess.UNAVAILABLE.getDescription()), descriptions.get());
		assertFalse(editor.isEditable(schema));
		schema.type = ConfigSchemaType.CLIENT;
		assertTrue(new ConfigServerInfo(screenSchema).forValues(Stream.of(IConfigScreenValue.configValue(value))).get().isEmpty());
	}

	@Test
	void missingChannelKeepsRemoteControlsReadOnlyWithoutLocalMutation() {
		AtomicInteger sendCount = new AtomicInteger();
		RemoteConfigNetworking.setClientSender(payload -> {
			sendCount.incrementAndGet();
			return false;
		});
		RemoteConfigEditor.onClientConnected(false);
		TestConfigValue value = new TestConfigValue("effective");
		TestConfigSchema schema = new TestConfigSchema(value);
		IConfigScreenValue<String> screenValue = editor.createScreenValue(schema, value);

		assertFalse(editor.isEditable(schema));
		assertEquals("effective", screenValue.getValue());
		assertEquals(0, sendCount.get());
		assertEquals(0, schema.batchCount());
		assertEquals(0, value.setCount());
	}

	@Test
	void readOnlySnapshotUsesTheEffectiveMirrorWithoutAnOverlay() {
		RequestCapture capture = connectAndCapture();
		TestConfigValue value = new TestConfigValue("effective");
		TestConfigSchema schema = new TestConfigSchema(value);
		IConfigScreenValue<String> screenValue = editor.createScreenValue(schema, value);
		RemoteConfigMessage.SnapshotRequest snapshotRequest = (RemoteConfigMessage.SnapshotRequest) capture.take();

		sendResponse(new RemoteConfigMessage.SnapshotResponse(
			snapshotRequest.requestId(),
			SCHEMA_KEY,
			true,
			false,
			"Server operator permission is required.",
			0,
			List.of()
		));

		assertFalse(editor.isEditable(schema));
		assertEquals("effective", screenValue.getValue());
		assertEquals(0, schema.batchCount());
		assertEquals(0, value.setCount());
	}

	@Test
	void acceptedUpdateUsesServerPendingOverlayWithoutMutatingRemoteMirror() {
		RequestCapture capture = connectAndCapture();
		TestConfigValue value = new TestConfigValue("effective");
		TestConfigSchema schema = new TestConfigSchema(value);
		Component customName = Component.literal("View Distance");
		Component customDescription = Component.literal("Server rendering radius");
		IConfigScreenValue<String> customizedValue = new LocalizedScreenValue<>(
			IConfigScreenValue.withApplyMode(
				IConfigScreenValue.configValue(value),
				ConfigValueApplyMode.IMMEDIATE
			),
			customName,
			customDescription
		);
		IConfigScreenValue<String> screenValue = editor.createScreenValue(schema, customizedValue);
		RemoteConfigMessage.SnapshotRequest snapshotRequest = (RemoteConfigMessage.SnapshotRequest) capture.take();
		sendResponse(new RemoteConfigMessage.SnapshotResponse(
			snapshotRequest.requestId(),
			SCHEMA_KEY,
			true,
			true,
			"",
			4,
			List.of(valueData("saved-before"))
		));

		assertTrue(editor.isEditable(schema));
		assertEquals("saved-before", screenValue.getValue());
		assertEquals(ConfigValueApplyMode.IMMEDIATE, screenValue.getApplyMode());
		assertEquals(ConfigValueRestartRequirement.GAME_RESTART, screenValue.getRestartRequirement());
		assertEquals(customName, ConfigValueLocalization.getName(screenValue));
		assertEquals(customDescription, ConfigValueLocalization.getDescription(screenValue));
		assertTrue(screenValue.getConfigValue().filter(configValue -> configValue == value).isPresent());
		CompletableFuture<Void> updateFuture = editor.requestUpdate(
			schema,
			List.of(new ConfigValueChange<>(screenValue, "saved-after"))
		);
		RemoteConfigMessage.UpdateRequest updateRequest = (RemoteConfigMessage.UpdateRequest) capture.take();

		assertEquals(4, updateRequest.expectedRevision());
		assertEquals(List.of(valueData("saved-after")), updateRequest.proposedValues());
		assertEquals(0, schema.batchCount());
		assertEquals(0, value.setCount());
		sendResponse(new RemoteConfigMessage.UpdateResponse(
			updateRequest.requestId(),
			SCHEMA_KEY,
			true,
			true,
			"",
			5,
			List.of(valueData("saved-after"))
		));

		assertTrue(updateFuture.isDone());
		assertFalse(updateFuture.isCompletedExceptionally());
		assertEquals("saved-after", screenValue.getValue());
		assertEquals("effective", value.get());
		assertEquals("effective", value.getPendingValue());
		assertEquals(0, schema.batchCount());
		assertEquals(0, value.setCount());
	}

	@Test
	void independentEffectiveSyncClearsTheOverlayAndRefreshesTheEditorSnapshot() {
		RequestCapture capture = connectAndCapture();
		TestConfigValue value = new TestConfigValue("effective");
		TestConfigSchema schema = new TestConfigSchema(value);
		IConfigScreenValue<String> screenValue = editor.createScreenValue(schema, value);
		RemoteConfigMessage.SnapshotRequest snapshotRequest = (RemoteConfigMessage.SnapshotRequest) capture.take();
		sendResponse(new RemoteConfigMessage.SnapshotResponse(
			snapshotRequest.requestId(),
			SCHEMA_KEY,
			true,
			true,
			"",
			1,
			List.of(valueData("pending"))
		));
		List<String> observedValues = new ArrayList<>();
		screenValue.addListener(observedValues::add);

		value.applyRemoteSnapshot("external");

		assertEquals("external", screenValue.getValue());
		assertEquals(List.of("external"), observedValues);
		assertTrue(capture.take() instanceof RemoteConfigMessage.SnapshotRequest);
	}

	@Test
	void disconnectAndTimeoutCompleteRequestsOnceAndClearRetainedState() {
		RequestCapture capture = connectAndCapture();
		TestConfigValue value = new TestConfigValue("effective");
		TestConfigSchema schema = new TestConfigSchema(value);
		IConfigScreenValue<String> screenValue = editor.createScreenValue(schema, value);
		RemoteConfigMessage.SnapshotRequest snapshotRequest = (RemoteConfigMessage.SnapshotRequest) capture.take();
		sendResponse(new RemoteConfigMessage.SnapshotResponse(
			snapshotRequest.requestId(),
			SCHEMA_KEY,
			true,
			true,
			"",
			0,
			List.of(valueData("overlay"))
		));

		CompletableFuture<Void> timedOut = editor.requestUpdate(
			schema,
			List.of(new ConfigValueChange<>(screenValue, "first"))
		);
		capture.take();
		AtomicInteger timeoutCompletions = new AtomicInteger();
		timedOut.whenComplete((ignored, throwable) -> timeoutCompletions.incrementAndGet());
		editor.tick(Long.MAX_VALUE);
		editor.tick(Long.MAX_VALUE);

		assertTrue(timedOut.isCompletedExceptionally());
		assertEquals(1, timeoutCompletions.get());

		CompletableFuture<Void> disconnected = editor.requestUpdate(
			schema,
			List.of(new ConfigValueChange<>(screenValue, "second"))
		);
		capture.take();
		AtomicInteger disconnectCompletions = new AtomicInteger();
		disconnected.whenComplete((ignored, throwable) -> disconnectCompletions.incrementAndGet());
		RemoteConfigEditor.onClientDisconnect();
		RemoteConfigEditor.onClientDisconnect();

		assertTrue(disconnected.isCompletedExceptionally());
		assertEquals(1, disconnectCompletions.get());
		assertFalse(editor.isEditable(schema));
		assertEquals("effective", screenValue.getValue());
	}

	private static RequestCapture connectAndCapture() {
		RequestCapture capture = new RequestCapture();
		RemoteConfigNetworking.setClientSender(capture::accept);
		RemoteConfigEditor.onClientConnected(true);
		return capture;
	}

	private static void sendResponse(RemoteConfigMessage response) {
		for (byte[] chunk : RemoteConfigPayloadChunker.split(RemoteConfigPayloadCodec.encode(response))) {
			RemoteConfigEditor.handleResponseChunk(new RemoteConfigResponseChunkPayload(chunk));
		}
	}

	private static RemoteValueData valueData(String value) {
		return new RemoteValueData(new RemoteValueKey("general", "value"), value);
	}

	private static final class RequestCapture {
		private final RemoteConfigPayloadReassembler reassembler = new RemoteConfigPayloadReassembler();
		@Nullable
		private RemoteConfigMessage message;

		private boolean accept(RemoteConfigRequestChunkPayload payload) {
			reassembler.accept(payload.payloadInternal())
				.map(RemoteConfigPayloadCodec::decode)
				.ifPresent(decoded -> message = decoded);
			return true;
		}

		private RemoteConfigMessage take() {
			RemoteConfigMessage result = Objects.requireNonNull(message, "No request was captured");
			message = null;
			return result;
		}
	}

	private static final class TestConfigSchema implements IConfigSchema {
		private final TestConfigCategory category;
		private int batchCount;
		private boolean active = true;
		@Nullable
		private Path path;
		private ConfigSchemaType type = ConfigSchemaType.SERVER;

		private TestConfigSchema(TestConfigValue value) {
			this.category = new TestConfigCategory(value);
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
			return type;
		}

		@Override
		public boolean isActive() {
			return active;
		}

		@Override
		public Optional<Path> getPath() {
			return Optional.ofNullable(path);
		}

		@Override
		public List<? extends IConfigCategory> getCategories() {
			return List.of(category);
		}

		@Override
		public List<? extends IConfigEditorCategory> getEditorCategories() {
			return List.of(category);
		}

		@Override
		public List<? extends IAppliedConfigValueChange<?>> batchUpdate(Consumer<IConfigBatchUpdater> updateBatch) {
			batchCount++;
			throw new AssertionError("A remote schema must never be mutated locally.");
		}

		@Override
		public Runnable addBatchListener(IConfigValueBatchChangeListener listener) {
			return () -> {};
		}

		@Override
		public Runnable addPendingBatchListener(IConfigValueBatchChangeListener listener) {
			return () -> {};
		}

		private int batchCount() {
			return batchCount;
		}
	}

	private record TestConfigCategory(TestConfigValue value) implements IConfigCategory {
		@Override
		public String getName() {
			return "general";
		}

		@Override
		public String getLocalizationKey() {
			return "test.general";
		}

		@Override
		public List<? extends IConfigValue<?>> getConfigValues() {
			return List.of(value);
		}
	}

	private static final class TestConfigValue implements IConfigValue<String>, IConfigValueEditorInfo<String> {
		private String effectiveValue;
		private IConfigValueChangeListener<String> pendingListener = ignored -> {};
		private int setCount;

		private TestConfigValue(String effectiveValue) {
			this.effectiveValue = effectiveValue;
		}

		@Override
		public String getName() {
			return "value";
		}

		@Override
		public String getLocalizationKey() {
			return "test.value";
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
			return effectiveValue;
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
			setCount++;
			throw new AssertionError("A remote value must never be mutated locally.");
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

		private int setCount() {
			return setCount;
		}

		private void applyRemoteSnapshot(String value) {
			String oldValue = effectiveValue;
			effectiveValue = value;
			pendingListener.onConfigValueChanged(new TestAppliedChange(this, oldValue, value));
		}
	}

	private record TestAppliedChange(
		IConfigValue<String> configValue,
		String oldValue,
		String newValue
	) implements IAppliedConfigValueChange<String> {}

	private record LocalizedScreenValue<T>(
		IConfigScreenValue<T> delegate,
		Component localizedName,
		Component localizedDescription
	) implements IConfigScreenValue<T>, IConfigLocalizedValue {
		@Override
		public String getName() {
			return delegate.getName();
		}

		@Override
		public String getLocalizationKey() {
			return delegate.getLocalizationKey();
		}

		@Override
		public T getValue() {
			return delegate.getValue();
		}

		@Override
		public T getDefaultValue() {
			return delegate.getDefaultValue();
		}

		@Override
		public boolean set(T value) {
			return delegate.set(value);
		}

		@Override
		public Runnable addListener(Consumer<T> listener) {
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
		public IConfigValueSerializer<T> getSerializer() {
			return delegate.getSerializer();
		}

		@Override
		public Optional<IConfigValue<T>> getConfigValue() {
			return delegate.getConfigValue();
		}

		@Override
		public Component getLocalizedName() {
			return localizedName;
		}

		@Override
		public Component getLocalizedDescription() {
			return localizedDescription;
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
		public String getValidValuesDescription() {
			return "any string";
		}
	}
}
