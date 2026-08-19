package net.mezzdev.config.gui.remote;

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
import net.mezzdev.config.gui.model.ConfigValueChange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
	void acceptedUpdateUsesServerPendingOverlayWithoutMutatingRemoteMirror() {
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
			4,
			List.of(valueData("saved-before"))
		));

		assertTrue(editor.isEditable(schema));
		assertEquals("saved-before", screenValue.getValue());
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
		assertEquals("effective", value.getValue());
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
		private RemoteConfigMessage message;

		private boolean accept(RemoteConfigRequestChunkPayload payload) {
			reassembler.accept(payload.payloadInternal())
				.map(RemoteConfigPayloadCodec::decode)
				.ifPresent(decoded -> message = decoded);
			return true;
		}

		private RemoteConfigMessage take() {
			RemoteConfigMessage result = message;
			message = null;
			return result;
		}
	}

	private static final class TestConfigSchema implements IConfigSchema {
		private final TestConfigCategory category;
		private int batchCount;

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
			return ConfigSchemaType.SERVER;
		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public Optional<Path> getPath() {
			return Optional.empty();
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

	private static final class TestConfigValue implements IConfigValue<String> {
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
		public String getValue() {
			return effectiveValue;
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
