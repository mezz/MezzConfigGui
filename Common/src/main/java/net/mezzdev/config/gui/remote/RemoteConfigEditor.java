package net.mezzdev.config.gui.remote;

import net.mezzdev.config.api.schema.ConfigSchemaType;
import net.mezzdev.config.api.schema.category.IConfigCategory;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.model.ConfigValueChange;
import net.mezzdev.config.gui.info.ServerConfigAccess;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class RemoteConfigEditor {
	static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
	static final int MAX_PENDING_REQUESTS = 128;
	private static final long REQUEST_TIMEOUT_NANOS = REQUEST_TIMEOUT.toNanos();
	private static final Logger LOGGER = LogManager.getLogger();
	private static final RemoteConfigEditor INSTANCE = new RemoteConfigEditor();

	private final Object lock = new Object();
	private final RemoteConfigPayloadReassembler responseReassembler = new RemoteConfigPayloadReassembler();
	private final Map<Long, PendingRequest> pendingRequests = new LinkedHashMap<>();
	private final Map<IConfigSchema, SchemaState> states = new IdentityHashMap<>();
	private final Set<IConfigSchema> wantedSchemas = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Map<IConfigSchema, Map<IConfigValue<?>, List<Consumer<Object>>>> valueListeners = new IdentityHashMap<>();
	private boolean channelAvailable;
	private boolean connected;
	private long nextRequestId;

	private RemoteConfigEditor() {

	}

	public static RemoteConfigEditor getInstance() {
		return INSTANCE;
	}

	public static boolean isRemoteSchema(IConfigSchema schema) {
		Objects.requireNonNull(schema, "schema");
		return schema.isActive() &&
			schema.getType() == ConfigSchemaType.SERVER &&
			schema.getPath().isEmpty();
	}

	public boolean isEditable(IConfigSchema schema) {
		Objects.requireNonNull(schema, "schema");
		if (!schema.isActive()) {
			return false;
		}
		if (!isRemoteSchema(schema)) {
			return schema.getPath().isPresent();
		}
		synchronized (lock) {
			SchemaState state = states.get(schema);
			return channelAvailable && state != null && state.available() && state.canEdit();
		}
	}

	public ServerConfigAccess getServerAccess(IConfigSchema schema) {
		if (!schema.isActive()) {
			return ServerConfigAccess.UNAVAILABLE;
		}
		if (schema.getPath().isPresent()) {
			return ServerConfigAccess.LOCAL;
		}
		synchronized (lock) {
			if (!connected) {
				return ServerConfigAccess.UNAVAILABLE;
			}
			if (!channelAvailable) {
				return ServerConfigAccess.READ_ONLY;
			}
			SchemaState state = states.get(schema);
			if (state == null) {
				if (hasPendingSnapshot(schema)) {
					return ServerConfigAccess.CHECKING;
				}
				return ServerConfigAccess.UNAVAILABLE;
			}
			if (!state.available()) {
				return ServerConfigAccess.UNAVAILABLE;
			}
			if (!state.canEdit()) {
				return ServerConfigAccess.OP_REQUIRED;
			}
			return ServerConfigAccess.EDITABLE;
		}
	}

	public <T> IConfigScreenValue<T> createScreenValue(IConfigSchema schema, IConfigValue<T> value) {
		Objects.requireNonNull(schema, "schema");
		Objects.requireNonNull(value, "value");
		return createScreenValue(schema, IConfigScreenValue.configValue(value));
	}

	public <T> IConfigScreenValue<T> createScreenValue(IConfigSchema schema, IConfigScreenValue<T> value) {
		Objects.requireNonNull(schema, "schema");
		IConfigScreenValue<T> delegate = Objects.requireNonNull(value, "value");
		if (!isRemoteSchema(schema)) {
			return delegate;
		}
		IConfigValue<T> backingValue = findBackingValue(schema, delegate);
		ensureSnapshot(schema);
		if (delegate instanceof IConfigLocalizedValue localizedValue) {
			return new LocalizedRemoteValue<>(this, schema, backingValue, delegate, localizedValue);
		}
		return new RemoteConfigScreenValue<>(this, schema, backingValue, delegate);
	}

	public void ensureSnapshot(IConfigSchema schema) {
		Objects.requireNonNull(schema, "schema");
		boolean shouldRequest;
		synchronized (lock) {
			if (!isRemoteSchema(schema)) {
				return;
			}
			wantedSchemas.add(schema);
			shouldRequest = channelAvailable && !hasPendingSnapshot(schema);
		}
		if (shouldRequest) {
			try {
				requestSnapshot(schema);
			} catch (RuntimeException exception) {
				LOGGER.debug("Unable to request a remote config snapshot for {}.", RemoteSchemaKey.from(schema), exception);
			}
		}
	}

	public CompletableFuture<@Nullable Void> requestUpdate(
		IConfigSchema schema,
		List<ConfigValueChange<?>> changes
	) {
		Objects.requireNonNull(schema, "schema");
		Objects.requireNonNull(changes, "changes");
		RemoteConfigMessage.UpdateRequest request;
		PendingRequest pending;
		synchronized (lock) {
			if (!isRemoteSchema(schema)) {
				return CompletableFuture.failedFuture(new IllegalStateException("This is not an active remote server config."));
			}
			SchemaState state = states.get(schema);
			if (!channelAvailable || state == null || !state.available()) {
				return CompletableFuture.failedFuture(new IllegalStateException("The server does not support remote config editing."));
			}
			if (!state.canEdit()) {
				return CompletableFuture.failedFuture(new IllegalStateException("Server operator permission is required."));
			}
			List<RemoteValueData> proposedValues = serializeChanges(schema, changes);
			long requestId = allocateRequestId();
			request = new RemoteConfigMessage.UpdateRequest(
				requestId,
				RemoteSchemaKey.from(schema),
				state.revision(),
				proposedValues
			);
			pending = registerPending(requestId, schema, RequestKind.UPDATE);
		}
		if (!RemoteConfigNetworking.sendToServer(request)) {
			handleChannelLoss("The remote config editor channel is unavailable.");
		}
		return pending.future();
	}

	public static void onClientConnected(boolean channelAvailable) {
		INSTANCE.resetConnection(true, "The previous remote config editor session ended.");
		synchronized (INSTANCE.lock) {
			INSTANCE.connected = true;
		}
		INSTANCE.updateChannelAvailability(channelAvailable);
	}

	public static void onClientTick(boolean channelAvailable) {
		INSTANCE.updateChannelAvailability(channelAvailable);
		INSTANCE.tick(System.nanoTime());
	}

	public static void onClientDisconnect() {
		INSTANCE.resetConnection(true, "Disconnected before the remote config request completed.");
	}

	public static void handleResponseChunk(RemoteConfigResponseChunkPayload chunk) {
		INSTANCE.acceptResponseChunk(chunk);
	}

	private CompletableFuture<@Nullable Void> requestSnapshot(IConfigSchema schema) {
		RemoteConfigMessage.SnapshotRequest request;
		PendingRequest pending;
		synchronized (lock) {
			if (!channelAvailable || !isRemoteSchema(schema)) {
				return CompletableFuture.failedFuture(new IllegalStateException("The remote config editor channel is unavailable."));
			}
			PendingRequest existing = findPendingSnapshot(schema);
			if (existing != null) {
				return existing.future();
			}
			long requestId = allocateRequestId();
			request = new RemoteConfigMessage.SnapshotRequest(requestId, RemoteSchemaKey.from(schema));
			pending = registerPending(requestId, schema, RequestKind.SNAPSHOT);
		}
		if (!RemoteConfigNetworking.sendToServer(request)) {
			handleChannelLoss("The remote config editor channel is unavailable.");
		}
		return pending.future();
	}

	private PendingRequest registerPending(long requestId, IConfigSchema schema, RequestKind kind) {
		if (pendingRequests.size() >= MAX_PENDING_REQUESTS) {
			throw new IllegalStateException("Too many remote config requests are already pending.");
		}
		PendingRequest pending = new PendingRequest(
			RemoteSchemaKey.from(schema),
			schema,
			kind,
			new CompletableFuture<>(),
			System.nanoTime()
		);
		pendingRequests.put(requestId, pending);
		return pending;
	}

	private long allocateRequestId() {
		for (int i = 0; i <= MAX_PENDING_REQUESTS; i++) {
			if (nextRequestId == Long.MAX_VALUE) {
				nextRequestId = 1;
			} else {
				nextRequestId++;
			}
			if (!pendingRequests.containsKey(nextRequestId)) {
				return nextRequestId;
			}
		}
		throw new IllegalStateException("Unable to allocate a remote config request id.");
	}

	private boolean hasPendingSnapshot(IConfigSchema schema) {
		return findPendingSnapshot(schema) != null;
	}

	@Nullable
	private PendingRequest findPendingSnapshot(IConfigSchema schema) {
		return pendingRequests.values()
			.stream()
			.filter(pending -> pending.kind() == RequestKind.SNAPSHOT)
			.filter(pending -> pending.schema() == schema)
			.findFirst()
			.orElse(null);
	}

	private void acceptResponseChunk(RemoteConfigResponseChunkPayload chunk) {
		try {
			responseReassembler.accept(chunk.payloadInternal())
				.map(RemoteConfigPayloadCodec::decode)
				.ifPresent(this::handleResponse);
		} catch (RuntimeException exception) {
			LOGGER.warn("Rejected malformed remote config response fragment: {}", getExceptionMessage(exception));
			LOGGER.debug("Malformed remote config response fragment details.", exception);
		}
	}

	private void handleResponse(RemoteConfigMessage response) {
		if (!(response instanceof RemoteConfigMessage.SnapshotResponse) &&
			!(response instanceof RemoteConfigMessage.UpdateResponse)
		) {
			LOGGER.warn("Rejected a remote config request sent in the clientbound channel.");
			return;
		}

		PendingRequest pending;
		List<ValueNotification> notifications = List.of();
		RuntimeException failure = null;
		boolean succeeded = false;
		synchronized (lock) {
			pending = pendingRequests.remove(response.requestId());
			if (pending == null) {
				return;
			}
			try {
				validateResponse(response, pending);
				if (response instanceof RemoteConfigMessage.SnapshotResponse snapshot) {
					notifications = installSnapshot(
						pending.schema(),
						snapshot.available(),
						snapshot.canEdit(),
						snapshot.revision(),
						snapshot.pendingValues()
					);
					if (snapshot.available()) {
						succeeded = true;
					} else {
						failure = new IllegalStateException(getResponseError(snapshot.error()));
					}
				} else if (response instanceof RemoteConfigMessage.UpdateResponse update) {
					notifications = installSnapshot(
						pending.schema(),
						true,
						update.canEdit(),
						update.revision(),
						update.pendingValues()
					);
					if (update.accepted()) {
						succeeded = true;
					} else {
						failure = new IllegalStateException(getResponseError(update.error()));
					}
				}
			} catch (RuntimeException exception) {
				SchemaState oldState = states.remove(pending.schema());
				notifications = createNotifications(pending.schema(), oldState, null);
				failure = exception;
			}
		}

		notifyValues(notifications);
		if (succeeded) {
			pending.future().complete(null);
		} else {
			RuntimeException exception = failure;
			if (exception == null) {
				exception = new IllegalStateException("The remote config response was rejected.");
			}
			LOGGER.debug("Remote config request failed for {}.", pending.key(), exception);
			pending.future().completeExceptionally(exception);
		}
	}

	private static void validateResponse(RemoteConfigMessage response, PendingRequest pending) {
		if (!pending.key().equals(response.schemaKey())) {
			throw new IllegalArgumentException("Remote config response schema does not match the pending request.");
		}
		if (pending.kind() == RequestKind.SNAPSHOT && !(response instanceof RemoteConfigMessage.SnapshotResponse)) {
			throw new IllegalArgumentException("Remote config response type does not match the snapshot request.");
		}
		if (pending.kind() == RequestKind.UPDATE && !(response instanceof RemoteConfigMessage.UpdateResponse)) {
			throw new IllegalArgumentException("Remote config response type does not match the update request.");
		}
		if (!isRemoteSchema(pending.schema()) || !RemoteSchemaKey.from(pending.schema()).equals(pending.key())) {
			throw new IllegalStateException("The requested remote config schema is no longer active.");
		}
	}

	private List<ValueNotification> installSnapshot(
		IConfigSchema schema,
		boolean available,
		boolean canEdit,
		long revision,
		List<RemoteValueData> pendingValues
	) {
		SchemaState oldState = states.get(schema);
		if (!available || !canEdit) {
			if (!pendingValues.isEmpty()) {
				throw new IllegalArgumentException("A read-only remote config response must not contain pending values.");
			}
			states.put(schema, new SchemaState(available, false, revision, new IdentityHashMap<>()));
			return createNotifications(schema, oldState, states.get(schema));
		}
		Map<IConfigValue<?>, Object> values = deserializeSnapshot(schema, pendingValues);
		SchemaState newState = new SchemaState(true, canEdit, revision, values);
		states.put(schema, newState);
		return createNotifications(schema, oldState, newState);
	}

	private static Map<IConfigValue<?>, Object> deserializeSnapshot(
		IConfigSchema schema,
		List<RemoteValueData> pendingValues
	) {
		Map<RemoteValueKey, IConfigValue<?>> knownValues = getKnownValues(schema);
		if (pendingValues.size() != knownValues.size()) {
			throw new IllegalArgumentException("Remote config snapshot is incomplete.");
		}
		Set<RemoteValueKey> receivedKeys = new HashSet<>();
		Map<IConfigValue<?>, Object> values = new IdentityHashMap<>();
		for (RemoteValueData valueData : pendingValues) {
			if (!receivedKeys.add(valueData.key())) {
				throw new IllegalArgumentException("Remote config snapshot contains a duplicate value.");
			}
			IConfigValue<?> value = knownValues.get(valueData.key());
			if (value == null) {
				throw new IllegalArgumentException("Remote config snapshot contains an unknown value.");
			}
			values.put(value, deserializeValue(value, valueData.serializedValue()));
		}
		return values;
	}

	private static Map<RemoteValueKey, IConfigValue<?>> getKnownValues(IConfigSchema schema) {
		Map<RemoteValueKey, IConfigValue<?>> values = new LinkedHashMap<>();
		for (IConfigCategory category : schema.getCategories()) {
			for (IConfigValue<?> value : category.getConfigValues()) {
				RemoteValueKey key = new RemoteValueKey(category.getName(), value.getEditorInfo().getName());
				if (values.putIfAbsent(key, value) != null) {
					throw new IllegalArgumentException("The local schema has a duplicate config value storage key.");
				}
			}
		}
		return values;
	}

	@SuppressWarnings("unchecked")
	private static <T> T deserializeValue(IConfigValue<?> unresolvedValue, String serializedValue) {
		IConfigValue<T> value = (IConfigValue<T>) unresolvedValue;
		IDeserializeResult<T> result = value.getEditorInfo().getSerializer().deserialize(serializedValue);
		if (!result.getDiagnostics().isEmpty() || result.getResult().isEmpty()) {
			throw new IllegalArgumentException("The server sent an invalid pending config value.");
		}
		return result.getResult().orElseThrow();
	}

	private static List<RemoteValueData> serializeChanges(
		IConfigSchema schema,
		List<ConfigValueChange<?>> changes
	) {
		if (changes.isEmpty()) {
			throw new IllegalArgumentException("A remote config update must not be empty.");
		}
		Map<IConfigValue<?>, RemoteValueKey> knownValues = getKnownValueKeysByIdentity(schema);
		Set<RemoteValueKey> changedKeys = new HashSet<>();
		List<RemoteValueData> serialized = new ArrayList<>(changes.size());
		for (ConfigValueChange<?> change : changes) {
			IConfigValue<?> backingValue = findBackingValue(knownValues.keySet(), change.configValue());
			RemoteValueKey key = Objects.requireNonNull(knownValues.get(backingValue));
			if (!changedKeys.add(key)) {
				throw new IllegalArgumentException("A remote config update contains a duplicate value.");
			}
			serialized.add(new RemoteValueData(key, serializeProposedValue(backingValue, change.value())));
		}
		return List.copyOf(serialized);
	}

	private static Map<IConfigValue<?>, RemoteValueKey> getKnownValueKeysByIdentity(IConfigSchema schema) {
		Map<IConfigValue<?>, RemoteValueKey> values = new IdentityHashMap<>();
		Set<RemoteValueKey> keys = new HashSet<>();
		for (IConfigCategory category : schema.getCategories()) {
			for (IConfigValue<?> value : category.getConfigValues()) {
				RemoteValueKey key = new RemoteValueKey(category.getName(), value.getEditorInfo().getName());
				if (!keys.add(key) || values.put(value, key) != null) {
					throw new IllegalArgumentException("The local schema has duplicate config values.");
				}
			}
		}
		return values;
	}

	private static IConfigValue<?> findBackingValue(
		Collection<IConfigValue<?>> knownValues,
		IConfigScreenValue<?> screenValue
	) {
		Object identityKey = screenValue.getIdentityKey();
		return knownValues.stream()
			.filter(value -> value == identityKey)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
				"Config value does not have a MezzConfig backing value in this schema: " + screenValue.getName()
			));
	}

	@SuppressWarnings("unchecked")
	private static <T> IConfigValue<T> findBackingValue(
		IConfigSchema schema,
		IConfigScreenValue<T> screenValue
	) {
		Object identityKey = screenValue.getIdentityKey();
		for (IConfigCategory category : schema.getCategories()) {
			for (IConfigValue<?> value : category.getConfigValues()) {
				if (value == identityKey) {
					return (IConfigValue<T>) value;
				}
			}
		}
		throw new IllegalArgumentException(
			"Config value does not have a MezzConfig backing value in this schema: " + screenValue.getName()
		);
	}

	@SuppressWarnings("unchecked")
	private static <T> String serializeProposedValue(IConfigValue<?> unresolvedValue, Object proposedValue) {
		IConfigValue<T> value = (IConfigValue<T>) unresolvedValue;
		IConfigValueSerializer<T> serializer = value.getEditorInfo().getSerializer();
		return serializer.serialize((T) proposedValue);
	}

	private <T> Optional<T> getOverlayValue(IConfigSchema schema, IConfigValue<T> value) {
		synchronized (lock) {
			SchemaState state = states.get(schema);
			if (state == null || !state.available() || !state.values().containsKey(value)) {
				return Optional.empty();
			}
			@SuppressWarnings("unchecked")
			T overlayValue = (T) Objects.requireNonNull(state.values().get(value));
			return Optional.of(overlayValue);
		}
	}

	private Runnable addValueListener(IConfigSchema schema, IConfigValue<?> value, Consumer<Object> listener) {
		AtomicBoolean removed = new AtomicBoolean();
		synchronized (lock) {
			valueListeners
				.computeIfAbsent(schema, ignored -> new IdentityHashMap<>())
				.computeIfAbsent(value, ignored -> new ArrayList<>())
				.add(listener);
		}
		return () -> {
			if (!removed.compareAndSet(false, true)) {
				return;
			}
			synchronized (lock) {
				Map<IConfigValue<?>, List<Consumer<Object>>> schemaListeners = valueListeners.get(schema);
				if (schemaListeners == null) {
					return;
				}
				List<Consumer<Object>> listeners = schemaListeners.get(value);
				if (listeners != null) {
					listeners.remove(listener);
					if (listeners.isEmpty()) {
						schemaListeners.remove(value);
					}
				}
				if (schemaListeners.isEmpty()) {
					valueListeners.remove(schema);
				}
			}
		};
	}

	private boolean onBackingValueChanged(IConfigSchema schema) {
		List<ValueNotification> notifications;
		boolean hadState;
		synchronized (lock) {
			SchemaState oldState = states.remove(schema);
			hadState = oldState != null;
			notifications = createNotifications(schema, oldState, null);
		}
		notifyValues(notifications);
		ensureSnapshot(schema);
		return hadState;
	}

	private List<ValueNotification> createNotifications(
		IConfigSchema schema,
		@Nullable SchemaState oldState,
		@Nullable SchemaState newState
	) {
		Map<IConfigValue<?>, List<Consumer<Object>>> schemaListeners = valueListeners.get(schema);
		if (schemaListeners == null) {
			return List.of();
		}
		List<ValueNotification> notifications = new ArrayList<>();
		for (Map.Entry<IConfigValue<?>, List<Consumer<Object>>> entry : schemaListeners.entrySet()) {
			IConfigValue<?> value = entry.getKey();
			Object oldValue = getStateValue(oldState, value);
			Object newValue = getStateValue(newState, value);
			if (!Objects.equals(oldValue, newValue)) {
				for (Consumer<Object> listener : List.copyOf(entry.getValue())) {
					notifications.add(new ValueNotification(listener, newValue));
				}
			}
		}
		return List.copyOf(notifications);
	}

	private static Object getStateValue(@Nullable SchemaState state, IConfigValue<?> value) {
		if (state != null && state.available() && state.values().containsKey(value)) {
			return Objects.requireNonNull(state.values().get(value));
		}
		return value.getEditorInfo().getPendingValue();
	}

	private static void notifyValues(List<ValueNotification> notifications) {
		for (ValueNotification notification : notifications) {
			try {
				notification.listener().accept(notification.value());
			} catch (RuntimeException exception) {
				LOGGER.error("Remote config editor value listener failed.", exception);
			}
		}
	}

	private void updateChannelAvailability(boolean available) {
		boolean lost;
		List<IConfigSchema> schemasToRequest = List.of();
		synchronized (lock) {
			lost = channelAvailable && !available;
			channelAvailable = available;
			if (available) {
				schemasToRequest = wantedSchemas.stream()
					.filter(RemoteConfigEditor::isRemoteSchema)
					.filter(schema -> !states.containsKey(schema))
					.filter(schema -> !hasPendingSnapshot(schema))
					.toList();
			}
		}
		if (lost) {
			handleChannelLoss("The remote config editor channel became unavailable.");
			return;
		}
		for (IConfigSchema schema : schemasToRequest) {
			try {
				requestSnapshot(schema);
			} catch (RuntimeException exception) {
				LOGGER.debug("Unable to refresh a remote config snapshot for {}.", RemoteSchemaKey.from(schema), exception);
			}
		}
	}

	void tick(long nowNanos) {
		responseReassembler.expire(nowNanos);
		List<PendingRequest> expired = new ArrayList<>();
		List<ValueNotification> notifications = new ArrayList<>();
		synchronized (lock) {
			pendingRequests.entrySet().removeIf(entry -> {
				PendingRequest pending = entry.getValue();
				if (nowNanos - pending.createdNanos() >= REQUEST_TIMEOUT_NANOS) {
					expired.add(pending);
					return true;
				}
				return false;
			});
			List<IConfigSchema> inactiveSchemas = states.keySet()
				.stream()
				.filter(schema -> !isRemoteSchema(schema))
				.toList();
			for (IConfigSchema schema : inactiveSchemas) {
				SchemaState oldState = states.remove(schema);
				notifications.addAll(createNotifications(schema, oldState, null));
				wantedSchemas.remove(schema);
			}
		}
		notifyValues(notifications);
		for (PendingRequest pending : expired) {
			pending.future().completeExceptionally(new IllegalStateException(
				"Timed out waiting for a remote config response for " + pending.key() + "."
			));
		}
	}

	private void handleChannelLoss(String reason) {
		List<PendingRequest> pending;
		List<ValueNotification> notifications;
		synchronized (lock) {
			channelAvailable = false;
			pending = List.copyOf(pendingRequests.values());
			pendingRequests.clear();
			notifications = clearStates();
		}
		responseReassembler.clear();
		notifyValues(notifications);
		pending.forEach(request -> request.future().completeExceptionally(new IllegalStateException(reason)));
	}

	private void resetConnection(boolean clearWantedSchemas, String reason) {
		List<PendingRequest> pending;
		List<ValueNotification> notifications;
		synchronized (lock) {
			connected = false;
			channelAvailable = false;
			pending = List.copyOf(pendingRequests.values());
			pendingRequests.clear();
			notifications = clearStates();
			if (clearWantedSchemas) {
				wantedSchemas.clear();
			}
		}
		responseReassembler.clear();
		notifyValues(notifications);
		pending.forEach(request -> request.future().completeExceptionally(new IllegalStateException(reason)));
	}

	private List<ValueNotification> clearStates() {
		List<ValueNotification> notifications = new ArrayList<>();
		for (Map.Entry<IConfigSchema, SchemaState> entry : states.entrySet()) {
			notifications.addAll(createNotifications(entry.getKey(), entry.getValue(), null));
		}
		states.clear();
		return List.copyOf(notifications);
	}

	private static String getResponseError(String error) {
		if (error.isBlank()) {
			return "The server rejected this remote config request.";
		}
		return error;
	}

	private static String getExceptionMessage(RuntimeException exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			return exception.getClass().getSimpleName();
		}
		return message;
	}

	private enum RequestKind {
		SNAPSHOT,
		UPDATE
	}

	private record PendingRequest(
		RemoteSchemaKey key,
		IConfigSchema schema,
		RequestKind kind,
		CompletableFuture<@Nullable Void> future,
		long createdNanos
	) {}

	private record SchemaState(
		boolean available,
		boolean canEdit,
		long revision,
		Map<IConfigValue<?>, Object> values
	) {
		private SchemaState {
			values = new IdentityHashMap<>(values);
		}
	}

	private record ValueNotification(Consumer<Object> listener, Object value) {}

	private static class RemoteConfigScreenValue<T> implements IConfigScreenValue<T> {
		private final RemoteConfigEditor editor;
		private final IConfigSchema schema;
		private final IConfigValue<T> backingValue;
		private final IConfigScreenValue<T> delegate;

		private RemoteConfigScreenValue(
			RemoteConfigEditor editor,
			IConfigSchema schema,
			IConfigValue<T> backingValue,
			IConfigScreenValue<T> delegate
		) {
			this.editor = editor;
			this.schema = schema;
			this.backingValue = backingValue;
			this.delegate = delegate;
		}

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
			return editor.getOverlayValue(schema, backingValue).orElseGet(delegate::getValue);
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
			Runnable removeOverlayListener = editor.addValueListener(
				schema,
				backingValue,
				value -> {
					@SuppressWarnings("unchecked")
					T typedValue = (T) value;
					listener.accept(typedValue);
				}
			);
			Runnable removeBackingListener = delegate.addListener(newValue -> {
				if (!editor.onBackingValueChanged(schema)) {
					listener.accept(newValue);
				}
			});
			AtomicBoolean removed = new AtomicBoolean();
			return () -> {
				if (removed.compareAndSet(false, true)) {
					removeBackingListener.run();
					removeOverlayListener.run();
				}
			};
		}

		@Override
		public ConfigValueApplyMode getApplyMode() {
			return delegate.getApplyMode();
		}

		@Override
		public net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement getRestartRequirement() {
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
			return Optional.of(backingValue);
		}

		@Override
		public String toString() {
			return delegate.toString();
		}
	}

	private static final class LocalizedRemoteValue<T> extends RemoteConfigScreenValue<T> implements IConfigLocalizedValue {
		private final IConfigLocalizedValue localizedValue;

		private LocalizedRemoteValue(
			RemoteConfigEditor editor,
			IConfigSchema schema,
			IConfigValue<T> backingValue,
			IConfigScreenValue<T> delegate,
			IConfigLocalizedValue localizedValue
		) {
			super(editor, schema, backingValue, delegate);
			this.localizedValue = Objects.requireNonNull(localizedValue, "localizedValue");
		}

		@Override
		public Component getLocalizedName() {
			return localizedValue.getLocalizedName();
		}

		@Override
		public Component getLocalizedDescription() {
			return localizedValue.getLocalizedDescription();
		}
	}
}
