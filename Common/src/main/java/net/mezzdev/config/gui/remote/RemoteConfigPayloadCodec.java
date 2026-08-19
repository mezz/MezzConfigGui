package net.mezzdev.config.gui.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class RemoteConfigPayloadCodec {
	static final int MAX_VALUE_COUNT = 4_096;
	static final int MAX_MOD_ID_BYTES = 128;
	static final int MAX_SCHEMA_ID_BYTES = 512;
	static final int MAX_CATEGORY_NAME_BYTES = 128;
	static final int MAX_VALUE_NAME_BYTES = 128;
	static final int MAX_SERIALIZED_VALUE_BYTES = 256 * 1024;
	static final int MAX_ERROR_MESSAGE_BYTES = 4 * 1024;

	private static final int SNAPSHOT_REQUEST = 1;
	private static final int SNAPSHOT_RESPONSE = 2;
	private static final int UPDATE_REQUEST = 3;
	private static final int UPDATE_RESPONSE = 4;

	private RemoteConfigPayloadCodec() {

	}

	static byte[] encode(RemoteConfigMessage message) {
		return encode(output -> {
			switch (message) {
				case RemoteConfigMessage.SnapshotRequest request -> {
					output.writeByte(SNAPSHOT_REQUEST);
					writeHeader(output, request);
				}
				case RemoteConfigMessage.SnapshotResponse response -> {
					output.writeByte(SNAPSHOT_RESPONSE);
					writeHeader(output, response);
					writeBoolean(output, response.available());
					writeBoolean(output, response.canEdit());
					writeString(output, response.error(), MAX_ERROR_MESSAGE_BYTES, "error message");
					output.writeLong(response.revision());
					writeValues(output, response.pendingValues());
				}
				case RemoteConfigMessage.UpdateRequest request -> {
					output.writeByte(UPDATE_REQUEST);
					writeHeader(output, request);
					output.writeLong(request.expectedRevision());
					writeValues(output, request.proposedValues());
				}
				case RemoteConfigMessage.UpdateResponse response -> {
					output.writeByte(UPDATE_RESPONSE);
					writeHeader(output, response);
					writeBoolean(output, response.accepted());
					writeBoolean(output, response.canEdit());
					writeString(output, response.error(), MAX_ERROR_MESSAGE_BYTES, "error message");
					output.writeLong(response.revision());
					writeValues(output, response.pendingValues());
				}
			}
		});
	}

	static RemoteConfigMessage decode(byte[] data) {
		return decode(data, input -> {
			int messageType = input.readUnsignedByte();
			long requestId = input.readLong();
			RemoteSchemaKey schemaKey = readSchemaKey(input);
			return switch (messageType) {
				case SNAPSHOT_REQUEST -> new RemoteConfigMessage.SnapshotRequest(requestId, schemaKey);
				case SNAPSHOT_RESPONSE -> new RemoteConfigMessage.SnapshotResponse(
					requestId,
					schemaKey,
					readBoolean(input, "available"),
					readBoolean(input, "canEdit"),
					readString(input, MAX_ERROR_MESSAGE_BYTES, "error message"),
					input.readLong(),
					readValues(input)
				);
				case UPDATE_REQUEST -> new RemoteConfigMessage.UpdateRequest(
					requestId,
					schemaKey,
					input.readLong(),
					readValues(input)
				);
				case UPDATE_RESPONSE -> new RemoteConfigMessage.UpdateResponse(
					requestId,
					schemaKey,
					readBoolean(input, "accepted"),
					readBoolean(input, "canEdit"),
					readString(input, MAX_ERROR_MESSAGE_BYTES, "error message"),
					input.readLong(),
					readValues(input)
				);
				default -> throw new IllegalArgumentException("Unknown remote config message type: " + messageType);
			};
		});
	}

	private static byte[] encode(IoConsumer<DataOutputStream> encoder) {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (DataOutputStream output = new DataOutputStream(bytes)) {
				encoder.accept(output);
			}
			byte[] encoded = bytes.toByteArray();
			if (encoded.length > RemoteConfigPayloadChunker.MAX_REASSEMBLED_PAYLOAD_LENGTH) {
				throw new IllegalArgumentException("Remote config payload exceeds the maximum length of " +
					RemoteConfigPayloadChunker.MAX_REASSEMBLED_PAYLOAD_LENGTH + " bytes: " + encoded.length);
			}
			return encoded;
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to encode a remote config payload.", exception);
		}
	}

	private static <T> T decode(byte[] data, IoFunction<DataInputStream, T> decoder) {
		if (data.length > RemoteConfigPayloadChunker.MAX_REASSEMBLED_PAYLOAD_LENGTH) {
			throw new IllegalArgumentException("Remote config payload exceeds the maximum length of " +
				RemoteConfigPayloadChunker.MAX_REASSEMBLED_PAYLOAD_LENGTH + " bytes: " + data.length);
		}
		try {
			DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
			T result = decoder.apply(input);
			if (input.available() != 0) {
				throw new IllegalArgumentException("Remote config payload has trailing data.");
			}
			return result;
		} catch (IOException exception) {
			throw new IllegalArgumentException("Invalid remote config payload.", exception);
		}
	}

	private static void writeHeader(DataOutputStream output, RemoteConfigMessage message) throws IOException {
		output.writeLong(message.requestId());
		writeSchemaKey(output, message.schemaKey());
	}

	private static void writeSchemaKey(DataOutputStream output, RemoteSchemaKey key) throws IOException {
		writeString(output, key.modId(), MAX_MOD_ID_BYTES, "mod id");
		writeString(output, key.schemaId(), MAX_SCHEMA_ID_BYTES, "schema id");
	}

	private static RemoteSchemaKey readSchemaKey(DataInputStream input) throws IOException {
		return new RemoteSchemaKey(
			readString(input, MAX_MOD_ID_BYTES, "mod id"),
			readString(input, MAX_SCHEMA_ID_BYTES, "schema id")
		);
	}

	private static void writeValues(DataOutputStream output, List<RemoteValueData> values) throws IOException {
		if (values.size() > MAX_VALUE_COUNT) {
			throw new IllegalArgumentException("Too many remote config values: " + values.size());
		}
		output.writeInt(values.size());
		for (RemoteValueData value : values) {
			writeString(output, value.key().categoryName(), MAX_CATEGORY_NAME_BYTES, "category name");
			writeString(output, value.key().valueName(), MAX_VALUE_NAME_BYTES, "value name");
			writeString(output, value.serializedValue(), MAX_SERIALIZED_VALUE_BYTES, "serialized value");
		}
	}

	private static List<RemoteValueData> readValues(DataInputStream input) throws IOException {
		int size = input.readInt();
		if (size < 0 || size > MAX_VALUE_COUNT || size > input.available() / (Integer.BYTES * 3)) {
			throw new IllegalArgumentException("Invalid remote config value count: " + size);
		}
		List<RemoteValueData> values = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			String categoryName = readString(input, MAX_CATEGORY_NAME_BYTES, "category name");
			String valueName = readString(input, MAX_VALUE_NAME_BYTES, "value name");
			String serializedValue = readString(input, MAX_SERIALIZED_VALUE_BYTES, "serialized value");
			values.add(new RemoteValueData(new RemoteValueKey(categoryName, valueName), serializedValue));
		}
		return List.copyOf(values);
	}

	private static void writeBoolean(DataOutputStream output, boolean value) throws IOException {
		if (value) {
			output.writeByte(1);
		} else {
			output.writeByte(0);
		}
	}

	private static boolean readBoolean(DataInputStream input, String fieldName) throws IOException {
		int value = input.readUnsignedByte();
		if (value != 0 && value != 1) {
			throw new IllegalArgumentException("Invalid remote config boolean for " + fieldName + ": " + value);
		}
		return value == 1;
	}

	private static void writeString(
		DataOutputStream output,
		String value,
		int maxEncodedLength,
		String fieldName
	) throws IOException {
		byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
		if (encoded.length > maxEncodedLength) {
			throw new IllegalArgumentException("Remote config " + fieldName + " exceeds " + maxEncodedLength + " UTF-8 bytes.");
		}
		output.writeInt(encoded.length);
		output.write(encoded);
	}

	private static String readString(DataInputStream input, int maxEncodedLength, String fieldName) throws IOException {
		int length = input.readInt();
		if (length < 0 || length > maxEncodedLength) {
			throw new IllegalArgumentException("Invalid remote config " + fieldName + " length: " + length);
		}
		if (length > input.available()) {
			throw new EOFException("Truncated remote config " + fieldName + ".");
		}
		byte[] encoded = input.readNBytes(length);
		try {
			return StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT)
				.decode(ByteBuffer.wrap(encoded))
				.toString();
		} catch (CharacterCodingException exception) {
			throw new IllegalArgumentException("Remote config payload contains invalid UTF-8.", exception);
		}
	}

	@FunctionalInterface
	private interface IoConsumer<T> {
		void accept(T value) throws IOException;
	}

	@FunctionalInterface
	private interface IoFunction<T, R> {
		R apply(T value) throws IOException;
	}
}
