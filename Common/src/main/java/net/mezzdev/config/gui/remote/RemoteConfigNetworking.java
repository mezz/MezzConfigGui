package net.mezzdev.config.gui.remote;

import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class RemoteConfigNetworking {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Object SERVER_SEND_LOCK = new Object();
	private static final Object CLIENT_SEND_LOCK = new Object();
	private static volatile @Nullable BiPredicate<ServerPlayer, RemoteConfigResponseChunkPayload> serverSender;
	private static volatile @Nullable Predicate<RemoteConfigRequestChunkPayload> clientSender;

	private RemoteConfigNetworking() {

	}

	public static void setServerSender(BiPredicate<ServerPlayer, RemoteConfigResponseChunkPayload> serverSender) {
		RemoteConfigNetworking.serverSender = Objects.requireNonNull(serverSender, "serverSender");
	}

	public static void setClientSender(Predicate<RemoteConfigRequestChunkPayload> clientSender) {
		RemoteConfigNetworking.clientSender = Objects.requireNonNull(clientSender, "clientSender");
	}

	static boolean sendToPlayer(ServerPlayer player, RemoteConfigMessage message) {
		BiPredicate<ServerPlayer, RemoteConfigResponseChunkPayload> sender = serverSender;
		if (sender == null) {
			return false;
		}
		try {
			List<byte[]> chunks = RemoteConfigPayloadChunker.split(RemoteConfigPayloadCodec.encode(message));
			synchronized (SERVER_SEND_LOCK) {
				for (byte[] chunk : chunks) {
					if (!sender.test(player, new RemoteConfigResponseChunkPayload(chunk))) {
						return false;
					}
				}
			}
			return true;
		} catch (RuntimeException exception) {
			LOGGER.error("Failed to send remote config editor response to {}.", player.getGameProfile().getName(), exception);
			return false;
		}
	}

	static boolean sendToServer(RemoteConfigMessage message) {
		Predicate<RemoteConfigRequestChunkPayload> sender = clientSender;
		if (sender == null) {
			return false;
		}
		try {
			List<byte[]> chunks = RemoteConfigPayloadChunker.split(RemoteConfigPayloadCodec.encode(message));
			synchronized (CLIENT_SEND_LOCK) {
				for (byte[] chunk : chunks) {
					if (!sender.test(new RemoteConfigRequestChunkPayload(chunk))) {
						return false;
					}
				}
			}
			return true;
		} catch (RuntimeException exception) {
			LOGGER.error("Failed to send remote config editor request.", exception);
			return false;
		}
	}
}
