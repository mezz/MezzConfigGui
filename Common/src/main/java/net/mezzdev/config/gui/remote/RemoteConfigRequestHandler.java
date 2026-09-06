package net.mezzdev.config.gui.remote;

import net.mezzdev.config.api.schema.ConfigSchemaType;
import net.mezzdev.config.api.schema.update.IConfigBatchUpdater;
import net.mezzdev.config.api.schema.category.IConfigCategory;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

final class RemoteConfigRequestHandler implements AutoCloseable {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final String SCHEMA_UNAVAILABLE = "This server config is not available for remote editing.";
	private static final String PERMISSION_REQUIRED = "Server operator permission is required.";
	private static final String STALE_REVISION = "This server config changed; the editor was refreshed.";
	private static final String INVALID_UPDATE = "The server rejected this config update.";

	private final Supplier<? extends Collection<? extends IConfigSchema>> schemasSupplier;
	private final Map<IConfigSchema, RevisionState> revisions = new IdentityHashMap<>();

	RemoteConfigRequestHandler(Supplier<? extends Collection<? extends IConfigSchema>> schemasSupplier) {
		this.schemasSupplier = Objects.requireNonNull(schemasSupplier, "schemasSupplier");
	}

	synchronized RemoteConfigMessage.SnapshotResponse handleSnapshot(
		RemoteConfigMessage.SnapshotRequest request,
		boolean canEdit
	) {
		try {
			IConfigSchema schema = resolveSchema(request.schemaKey());
			if (!canEdit) {
				return readOnlySnapshot(request);
			}
			RevisionState revision = getRevisionState(schema);
			return new RemoteConfigMessage.SnapshotResponse(
				request.requestId(),
				request.schemaKey(),
				true,
				canEdit,
				"",
				revision.revision(),
				serializeSnapshot(schema)
			);
		} catch (RequestRejectedException exception) {
			LOGGER.debug("Rejected remote config snapshot request for {}: {}", request.schemaKey(), exception.getMessage());
			return unavailableSnapshot(request, exception.userMessage());
		} catch (RuntimeException exception) {
			LOGGER.error("Failed to create remote config snapshot for {}.", request.schemaKey(), exception);
			return unavailableSnapshot(request, SCHEMA_UNAVAILABLE);
		}
	}

	synchronized RemoteConfigMessage.UpdateResponse handleUpdate(
		RemoteConfigMessage.UpdateRequest request,
		boolean canEdit
	) {
		IConfigSchema schema;
		try {
			schema = resolveSchema(request.schemaKey());
		} catch (RequestRejectedException exception) {
			LOGGER.debug("Rejected remote config update request for {}: {}", request.schemaKey(), exception.getMessage());
			return unavailableUpdate(request, exception.userMessage());
		} catch (RuntimeException exception) {
			LOGGER.error("Failed to resolve remote config update schema {}.", request.schemaKey(), exception);
			return unavailableUpdate(request, SCHEMA_UNAVAILABLE);
		}

		if (!canEdit) {
			return permissionDeniedUpdate(request);
		}
		RevisionState revision = getRevisionState(schema);
		if (request.expectedRevision() != revision.revision()) {
			return rejectedUpdate(request, schema, revision, STALE_REVISION);
		}

		List<ResolvedUpdate<?>> updates;
		List<RemoteValueData> projectedSnapshot;
		try {
			updates = resolveUpdates(schema, request.proposedValues());
			projectedSnapshot = serializeProjectedSnapshot(schema, updates);
			RemoteConfigPayloadCodec.encode(new RemoteConfigMessage.UpdateResponse(
				request.requestId(),
				request.schemaKey(),
				true,
				true,
				"",
				revision.revision(),
				projectedSnapshot
			));
		} catch (RuntimeException exception) {
			LOGGER.debug("Rejected remote config update validation for {}.", request.schemaKey(), exception);
			return rejectedUpdate(request, schema, revision, INVALID_UPDATE);
		}

		try {
			schema.batchUpdate(updater -> updates.forEach(update -> queueUpdate(updater, update)));
		} catch (RuntimeException exception) {
			LOGGER.debug("Rejected remote config update for {}.", request.schemaKey(), exception);
			return rejectedUpdate(request, schema, revision, INVALID_UPDATE);
		}

		List<RemoteValueData> appliedSnapshot;
		try {
			appliedSnapshot = serializeSnapshot(schema);
		} catch (RuntimeException exception) {
			LOGGER.error("Failed to serialize an applied remote config update for {}.", request.schemaKey(), exception);
			appliedSnapshot = projectedSnapshot;
		}
		return new RemoteConfigMessage.UpdateResponse(
			request.requestId(),
			request.schemaKey(),
			true,
			true,
			"",
			revision.revision(),
			appliedSnapshot
		);
	}

	private RemoteConfigMessage.UpdateResponse rejectedUpdate(
		RemoteConfigMessage.UpdateRequest request,
		IConfigSchema schema,
		RevisionState revision,
		String error
	) {
		try {
			return new RemoteConfigMessage.UpdateResponse(
				request.requestId(),
				request.schemaKey(),
				false,
				true,
				error,
				revision.revision(),
				serializeSnapshot(schema)
			);
		} catch (RuntimeException exception) {
			LOGGER.error("Failed to serialize rejected remote config update response for {}.", request.schemaKey(), exception);
			return unavailableUpdate(request, error);
		}
	}

	private static RemoteConfigMessage.SnapshotResponse readOnlySnapshot(
		RemoteConfigMessage.SnapshotRequest request
	) {
		return new RemoteConfigMessage.SnapshotResponse(
			request.requestId(),
			request.schemaKey(),
			true,
			false,
			PERMISSION_REQUIRED,
			0,
			List.of()
		);
	}

	private static RemoteConfigMessage.UpdateResponse permissionDeniedUpdate(
		RemoteConfigMessage.UpdateRequest request
	) {
		return new RemoteConfigMessage.UpdateResponse(
			request.requestId(),
			request.schemaKey(),
			false,
			false,
			PERMISSION_REQUIRED,
			0,
			List.of()
		);
	}

	private static RemoteConfigMessage.SnapshotResponse unavailableSnapshot(
		RemoteConfigMessage.SnapshotRequest request,
		String error
	) {
		return new RemoteConfigMessage.SnapshotResponse(
			request.requestId(),
			request.schemaKey(),
			false,
			false,
			error,
			0,
			List.of()
		);
	}

	private static RemoteConfigMessage.UpdateResponse unavailableUpdate(
		RemoteConfigMessage.UpdateRequest request,
		String error
	) {
		return new RemoteConfigMessage.UpdateResponse(
			request.requestId(),
			request.schemaKey(),
			false,
			false,
			error,
			0,
			List.of()
		);
	}

	private IConfigSchema resolveSchema(RemoteSchemaKey key) {
		List<? extends IConfigSchema> matches = schemasSupplier.get()
			.stream()
			.filter(schema -> schema.getType() == ConfigSchemaType.SERVER)
			.filter(schema -> schema.getModId().equals(key.modId()))
			.filter(schema -> schema.getId().equals(key.schemaId()))
			.toList();
		if (matches.size() != 1) {
			throw new RequestRejectedException(
				SCHEMA_UNAVAILABLE,
				"Expected one server schema match but found " + matches.size() + "."
			);
		}
		IConfigSchema schema = matches.getFirst();
		if (!schema.isActive() || schema.getPath().isEmpty()) {
			throw new RequestRejectedException(SCHEMA_UNAVAILABLE, "The server schema is not locally authoritative and active.");
		}
		return schema;
	}

	private RevisionState getRevisionState(IConfigSchema schema) {
		return revisions.computeIfAbsent(schema, key -> {
			RevisionState state = new RevisionState();
			state.setRemoveListener(schema.addPendingBatchListener(changes -> state.increment()));
			return state;
		});
	}

	private static List<RemoteValueData> serializeSnapshot(IConfigSchema schema) {
		List<RemoteValueData> snapshot = new ArrayList<>();
		Set<RemoteValueKey> keys = new HashSet<>();
		for (IConfigCategory category : schema.getCategories()) {
			for (IConfigValue<?> value : category.getConfigValues()) {
				RemoteValueKey key = new RemoteValueKey(category.getName(), value.getEditorInfo().getName());
				if (!keys.add(key)) {
					throw new IllegalStateException("Duplicate config value storage key: " + key);
				}
				snapshot.add(new RemoteValueData(key, serializePendingValue(value)));
			}
		}
		if (snapshot.size() > RemoteConfigPayloadCodec.MAX_VALUE_COUNT) {
			throw new IllegalStateException("The schema has too many values for remote editing.");
		}
		return List.copyOf(snapshot);
	}

	private static <T> String serializePendingValue(IConfigValue<T> value) {
		return value.getEditorInfo().getSerializer().serialize(value.getEditorInfo().getPendingValue());
	}

	private static List<RemoteValueData> serializeProjectedSnapshot(
		IConfigSchema schema,
		List<ResolvedUpdate<?>> updates
	) {
		Map<IConfigValue<?>, ResolvedUpdate<?>> updatesByValue = new IdentityHashMap<>();
		updates.forEach(update -> updatesByValue.put(update.value(), update));
		List<RemoteValueData> snapshot = new ArrayList<>();
		Set<RemoteValueKey> keys = new HashSet<>();
		for (IConfigCategory category : schema.getCategories()) {
			for (IConfigValue<?> value : category.getConfigValues()) {
				RemoteValueKey key = new RemoteValueKey(category.getName(), value.getEditorInfo().getName());
				if (!keys.add(key)) {
					throw new IllegalStateException("Duplicate config value storage key: " + key);
				}
				ResolvedUpdate<?> update = updatesByValue.get(value);
				String serializedValue;
				if (update == null) {
					serializedValue = serializePendingValue(value);
				} else {
					serializedValue = serializeProposedValue(update);
				}
				snapshot.add(new RemoteValueData(key, serializedValue));
			}
		}
		return List.copyOf(snapshot);
	}

	private static <T> String serializeProposedValue(ResolvedUpdate<T> update) {
		return update.value().getEditorInfo().getSerializer().serialize(update.proposedValue());
	}

	private static List<ResolvedUpdate<?>> resolveUpdates(
		IConfigSchema schema,
		List<RemoteValueData> proposedValues
	) {
		if (proposedValues.isEmpty()) {
			throw new IllegalArgumentException("A remote config update must not be empty.");
		}
		Set<RemoteValueKey> requestedKeys = new HashSet<>();
		List<ResolvedUpdate<?>> updates = new ArrayList<>(proposedValues.size());
		for (RemoteValueData proposedValue : proposedValues) {
			if (!requestedKeys.add(proposedValue.key())) {
				throw new IllegalArgumentException("Duplicate config value in update: " + proposedValue.key());
			}
			IConfigValue<?> value = resolveValue(schema, proposedValue.key());
			updates.add(deserializeUpdate(value, proposedValue.serializedValue()));
		}
		return List.copyOf(updates);
	}

	private static IConfigValue<?> resolveValue(IConfigSchema schema, RemoteValueKey key) {
		List<? extends IConfigCategory> categories = schema.getCategories()
			.stream()
			.filter(category -> category.getName().equals(key.categoryName()))
			.toList();
		if (categories.size() != 1) {
			throw new IllegalArgumentException("Unknown or duplicate config category: " + key.categoryName());
		}
		List<? extends IConfigValue<?>> values = categories.getFirst()
			.getConfigValues()
			.stream()
			.filter(value -> value.getEditorInfo().getName().equals(key.valueName()))
			.toList();
		if (values.size() != 1) {
			throw new IllegalArgumentException("Unknown or duplicate config value: " + key.valueName());
		}
		return values.getFirst();
	}

	@SuppressWarnings("unchecked")
	private static <T> ResolvedUpdate<T> deserializeUpdate(IConfigValue<?> unresolvedValue, String serializedValue) {
		IConfigValue<T> value = (IConfigValue<T>) unresolvedValue;
		IConfigValueSerializer<T> serializer = value.getEditorInfo().getSerializer();
		IDeserializeResult<T> result = serializer.deserialize(serializedValue);
		if (!result.getDiagnostics().isEmpty() || result.getResult().isEmpty()) {
			throw new IllegalArgumentException("Invalid serialized config value: " + value.getEditorInfo().getName());
		}
		return new ResolvedUpdate<>(value, result.getResult().orElseThrow());
	}

	private static <T> void queueUpdate(IConfigBatchUpdater updater, ResolvedUpdate<T> update) {
		updater.set(update.value(), update.proposedValue());
	}

	@Override
	public synchronized void close() {
		revisions.values().forEach(RevisionState::removeListener);
		revisions.clear();
	}

	private record ResolvedUpdate<T>(IConfigValue<T> value, T proposedValue) {}

	private static final class RevisionState {
		private long revision;
		private @Nullable Runnable removeListener;

		private synchronized long revision() {
			return revision;
		}

		private synchronized void increment() {
			if (revision != Long.MAX_VALUE) {
				revision++;
			}
		}

		private void setRemoveListener(Runnable removeListener) {
			this.removeListener = Objects.requireNonNull(removeListener, "removeListener");
		}

		private void removeListener() {
			if (removeListener != null) {
				removeListener.run();
				removeListener = null;
			}
		}
	}

	private static final class RequestRejectedException extends RuntimeException {
		private final String userMessage;

		private RequestRejectedException(String userMessage, String detailedMessage) {
			super(detailedMessage);
			this.userMessage = userMessage;
		}

		private String userMessage() {
			return userMessage;
		}
	}
}
