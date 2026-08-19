package net.mezzdev.config.gui.remote;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

final class RemoteConfigPayloadReassembler {
	static final int MAX_CONCURRENT_MESSAGES = 4;
	static final int MAX_PENDING_BYTES = 2 * RemoteConfigPayloadChunker.MAX_REASSEMBLED_PAYLOAD_LENGTH;
	static final Duration INCOMPLETE_MESSAGE_TIMEOUT = Duration.ofSeconds(10);
	private static final long INCOMPLETE_MESSAGE_TIMEOUT_NANOS = INCOMPLETE_MESSAGE_TIMEOUT.toNanos();

	private final Map<Long, Assembly> assemblies = new HashMap<>();
	private int pendingBytes;

	synchronized Optional<byte[]> accept(byte[] payload) {
		return accept(payload, System.nanoTime());
	}

	synchronized Optional<byte[]> accept(byte[] payload, long nowNanos) {
		expire(nowNanos);
		RemoteConfigPayloadChunker.Fragment fragment = RemoteConfigPayloadChunker.parse(payload);
		Assembly assembly = assemblies.get(fragment.messageId());
		if (assembly == null) {
			if (assemblies.size() >= MAX_CONCURRENT_MESSAGES) {
				throw new IllegalArgumentException("Too many incomplete remote config messages.");
			}
			if (fragment.totalLength() > MAX_PENDING_BYTES - pendingBytes) {
				throw new IllegalArgumentException("Incomplete remote config messages exceed the pending byte limit.");
			}
			assembly = new Assembly(fragment, nowNanos);
			assemblies.put(fragment.messageId(), assembly);
			pendingBytes += fragment.totalLength();
		} else if (!assembly.metadataMatches(fragment)) {
			remove(fragment.messageId());
			throw new IllegalArgumentException("Remote config fragments with the same message id have inconsistent metadata.");
		}

		try {
			if (!assembly.accept(fragment)) {
				return Optional.empty();
			}
			byte[] result = assembly.data();
			remove(fragment.messageId());
			return Optional.of(result);
		} catch (RuntimeException exception) {
			remove(fragment.messageId());
			throw exception;
		}
	}

	synchronized void clear() {
		assemblies.clear();
		pendingBytes = 0;
	}

	synchronized int expire(long nowNanos) {
		int expired = 0;
		Iterator<Map.Entry<Long, Assembly>> iterator = assemblies.entrySet().iterator();
		while (iterator.hasNext()) {
			Assembly assembly = iterator.next().getValue();
			if (nowNanos - assembly.createdNanos() >= INCOMPLETE_MESSAGE_TIMEOUT_NANOS) {
				pendingBytes -= assembly.data().length;
				iterator.remove();
				expired++;
			}
		}
		return expired;
	}

	synchronized Optional<Duration> getTimeUntilNextExpiration(long nowNanos) {
		return assemblies.values()
			.stream()
			.mapToLong(Assembly::createdNanos)
			.min()
			.stream()
			.mapToObj(createdNanos -> {
				long elapsedNanos = nowNanos - createdNanos;
				long remainingNanos = Math.max(0, INCOMPLETE_MESSAGE_TIMEOUT_NANOS - elapsedNanos);
				return Duration.ofNanos(remainingNanos);
			})
			.findFirst();
	}

	synchronized boolean isEmpty() {
		return assemblies.isEmpty();
	}

	synchronized int pendingMessageCount() {
		return assemblies.size();
	}

	synchronized int pendingBytes() {
		return pendingBytes;
	}

	private void remove(long messageId) {
		Assembly removed = assemblies.remove(messageId);
		if (removed != null) {
			pendingBytes -= removed.data().length;
		}
	}

	private static final class Assembly {
		private final int fragmentCount;
		private final long createdNanos;
		private final byte[] data;
		private final boolean[] receivedFragments;
		private int receivedCount;

		private Assembly(RemoteConfigPayloadChunker.Fragment firstFragment, long createdNanos) {
			this.fragmentCount = firstFragment.fragmentCount();
			this.createdNanos = createdNanos;
			this.data = new byte[firstFragment.totalLength()];
			this.receivedFragments = new boolean[fragmentCount];
		}

		private boolean metadataMatches(RemoteConfigPayloadChunker.Fragment fragment) {
			return fragment.fragmentCount() == fragmentCount && fragment.totalLength() == data.length;
		}

		private boolean accept(RemoteConfigPayloadChunker.Fragment fragment) {
			int fragmentIndex = fragment.fragmentIndex();
			if (receivedFragments[fragmentIndex]) {
				throw new IllegalArgumentException("Received a duplicate remote config fragment: " + fragmentIndex);
			}
			fragment.copyContentTo(data);
			receivedFragments[fragmentIndex] = true;
			receivedCount++;
			return receivedCount == fragmentCount;
		}

		private long createdNanos() {
			return createdNanos;
		}

		private byte[] data() {
			return data;
		}
	}
}
