package net.mezzdev.config.gui.remote;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteConfigPayloadTest {
	private static final RemoteSchemaKey SCHEMA_KEY = new RemoteSchemaKey("test", "server.ini");
	private static final List<RemoteValueData> VALUES = List.of(
		new RemoteValueData(new RemoteValueKey("general", "enabled"), "true"),
		new RemoteValueData(new RemoteValueKey("general", "label"), "日本語")
	);

	@Test
	void roundTripsEveryLogicalMessage() {
		List<RemoteConfigMessage> messages = List.of(
			new RemoteConfigMessage.SnapshotRequest(1, SCHEMA_KEY),
			new RemoteConfigMessage.SnapshotResponse(2, SCHEMA_KEY, true, false, "read only", 3, VALUES),
			new RemoteConfigMessage.UpdateRequest(4, SCHEMA_KEY, 3, VALUES),
			new RemoteConfigMessage.UpdateResponse(5, SCHEMA_KEY, false, true, "stale", 4, VALUES)
		);

		for (RemoteConfigMessage message : messages) {
			assertEquals(message, RemoteConfigPayloadCodec.decode(RemoteConfigPayloadCodec.encode(message)));
		}
	}

	@Test
	void rejectsTrailingDataInvalidUtf8AndOversizedValues() {
		byte[] valid = RemoteConfigPayloadCodec.encode(new RemoteConfigMessage.SnapshotRequest(1, SCHEMA_KEY));
		byte[] trailing = Arrays.copyOf(valid, valid.length + 1);
		assertThrows(IllegalArgumentException.class, () -> RemoteConfigPayloadCodec.decode(trailing));

		byte[] invalidUtf8 = {
			1,
			0, 0, 0, 0, 0, 0, 0, 1,
			0, 0, 0, 1,
			(byte) 0x80
		};
		assertThrows(IllegalArgumentException.class, () -> RemoteConfigPayloadCodec.decode(invalidUtf8));

		String oversized = "x".repeat(RemoteConfigPayloadCodec.MAX_SERIALIZED_VALUE_BYTES + 1);
		RemoteConfigMessage.UpdateRequest request = new RemoteConfigMessage.UpdateRequest(
			1,
			SCHEMA_KEY,
			0,
			List.of(new RemoteValueData(new RemoteValueKey("general", "value"), oversized))
		);
		assertThrows(IllegalArgumentException.class, () -> RemoteConfigPayloadCodec.encode(request));

		String oversizedMultibyte = "\u0800".repeat(RemoteConfigPayloadCodec.MAX_SERIALIZED_VALUE_BYTES / 3 + 1);
		RemoteConfigMessage.UpdateRequest multibyteRequest = new RemoteConfigMessage.UpdateRequest(
			2,
			SCHEMA_KEY,
			0,
			List.of(new RemoteValueData(new RemoteValueKey("general", "value"), oversizedMultibyte))
		);
		assertThrows(IllegalArgumentException.class, () -> RemoteConfigPayloadCodec.encode(multibyteRequest));
	}

	@Test
	void boundsEncodingAcrossIndividuallyValidValues() {
		String maximumValue = "x".repeat(RemoteConfigPayloadCodec.MAX_SERIALIZED_VALUE_BYTES);
		List<RemoteValueData> values = List.of(
			new RemoteValueData(new RemoteValueKey("general", "first"), maximumValue),
			new RemoteValueData(new RemoteValueKey("general", "second"), maximumValue),
			new RemoteValueData(new RemoteValueKey("general", "third"), maximumValue),
			new RemoteValueData(new RemoteValueKey("general", "fourth"), maximumValue)
		);
		RemoteConfigMessage.UpdateRequest request = new RemoteConfigMessage.UpdateRequest(1, SCHEMA_KEY, 0, values);

		assertThrows(IllegalArgumentException.class, () -> RemoteConfigPayloadCodec.encode(request));
	}

	@Test
	void reassemblesOutOfOrderAndCleansUpAfterDuplicateOrExpiry() {
		byte[] data = new byte[RemoteConfigPayloadChunker.MAX_CHUNK_DATA_LENGTH * 2 + 17];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) i;
		}
		List<byte[]> chunks = new ArrayList<>(RemoteConfigPayloadChunker.split(data, 10));
		Collections.reverse(chunks);
		RemoteConfigPayloadReassembler reassembler = new RemoteConfigPayloadReassembler();
		byte[] result = null;
		for (byte[] chunk : chunks) {
			result = reassembler.accept(chunk, 0).orElse(result);
		}

		assertArrayEquals(data, result);
		assertTrue(reassembler.isEmpty());

		byte[] first = RemoteConfigPayloadChunker.split(data, 11).getFirst();
		reassembler.accept(first, 0);
		assertThrows(IllegalArgumentException.class, () -> reassembler.accept(first, 0));
		assertTrue(reassembler.isEmpty());

		reassembler.accept(RemoteConfigPayloadChunker.split(data, 12).getFirst(), 0);
		assertEquals(1, reassembler.expire(RemoteConfigPayloadReassembler.INCOMPLETE_MESSAGE_TIMEOUT.toNanos()));
		assertEquals(0, reassembler.pendingBytes());
	}

	@Test
	void boundsBufferedFragmentsAndCompletePayloads() {
		byte[] maximum = new byte[RemoteConfigPayloadChunker.MAX_REASSEMBLED_PAYLOAD_LENGTH];
		RemoteConfigPayloadReassembler reassembler = new RemoteConfigPayloadReassembler();
		reassembler.accept(RemoteConfigPayloadChunker.split(maximum, 20).getFirst(), 0);
		reassembler.accept(RemoteConfigPayloadChunker.split(maximum, 21).getFirst(), 0);
		assertThrows(
			IllegalArgumentException.class,
			() -> reassembler.accept(RemoteConfigPayloadChunker.split(maximum, 22).getFirst(), 0)
		);
		assertEquals(RemoteConfigPayloadReassembler.MAX_PENDING_BYTES, reassembler.pendingBytes());

		byte[] oversized = new byte[RemoteConfigPayloadChunker.MAX_REASSEMBLED_PAYLOAD_LENGTH + 1];
		assertThrows(IllegalArgumentException.class, () -> RemoteConfigPayloadChunker.split(oversized));
	}
}
