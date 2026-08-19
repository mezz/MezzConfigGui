package net.mezzdev.config.gui.remote;

import java.util.List;
import java.util.Objects;

sealed interface RemoteConfigMessage {
	long requestId();

	RemoteSchemaKey schemaKey();

	record SnapshotRequest(
		long requestId,
		RemoteSchemaKey schemaKey
	) implements RemoteConfigMessage {
		public SnapshotRequest {
			validateRequest(requestId, schemaKey);
		}
	}

	record SnapshotResponse(
		long requestId,
		RemoteSchemaKey schemaKey,
		boolean available,
		boolean canEdit,
		String error,
		long revision,
		List<RemoteValueData> pendingValues
	) implements RemoteConfigMessage {
		public SnapshotResponse {
			pendingValues = List.copyOf(pendingValues);
			validateResponse(requestId, schemaKey, error, revision, pendingValues);
		}
	}

	record UpdateRequest(
		long requestId,
		RemoteSchemaKey schemaKey,
		long expectedRevision,
		List<RemoteValueData> proposedValues
	) implements RemoteConfigMessage {
		public UpdateRequest {
			validateRequest(requestId, schemaKey);
			if (expectedRevision < 0) {
				throw new IllegalArgumentException("expectedRevision must not be negative.");
			}
			proposedValues = List.copyOf(proposedValues);
		}
	}

	record UpdateResponse(
		long requestId,
		RemoteSchemaKey schemaKey,
		boolean accepted,
		boolean canEdit,
		String error,
		long revision,
		List<RemoteValueData> pendingValues
	) implements RemoteConfigMessage {
		public UpdateResponse {
			pendingValues = List.copyOf(pendingValues);
			validateResponse(requestId, schemaKey, error, revision, pendingValues);
		}
	}

	private static void validateRequest(long requestId, RemoteSchemaKey schemaKey) {
		if (requestId <= 0) {
			throw new IllegalArgumentException("requestId must be positive.");
		}
		Objects.requireNonNull(schemaKey, "schemaKey");
	}

	private static void validateResponse(
		long requestId,
		RemoteSchemaKey schemaKey,
		String error,
		long revision,
		List<RemoteValueData> pendingValues
	) {
		validateRequest(requestId, schemaKey);
		Objects.requireNonNull(error, "error");
		if (revision < 0) {
			throw new IllegalArgumentException("revision must not be negative.");
		}
		Objects.requireNonNull(pendingValues, "pendingValues");
	}
}
