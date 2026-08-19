package net.mezzdev.config.gui.remote;

import net.mezzdev.config.api.Configs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RemoteConfigEditorServer {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Map<UUID, RemoteConfigPayloadReassembler> REQUEST_REASSEMBLERS = new ConcurrentHashMap<>();
	private static volatile @Nullable MinecraftServer activeServer;
	private static volatile @Nullable RemoteConfigRequestHandler requestHandler;

	private RemoteConfigEditorServer() {

	}

	public static synchronized void onServerStarted(MinecraftServer server) {
		closeRequestHandler();
		REQUEST_REASSEMBLERS.clear();
		activeServer = server;
		requestHandler = new RemoteConfigRequestHandler(Configs::getSchemas);
	}

	public static synchronized void onServerStopped(MinecraftServer server) {
		if (activeServer != server) {
			return;
		}
		activeServer = null;
		REQUEST_REASSEMBLERS.clear();
		closeRequestHandler();
	}

	public static void onPlayerDisconnect(ServerPlayer player) {
		RemoteConfigPayloadReassembler reassembler = REQUEST_REASSEMBLERS.remove(player.getUUID());
		if (reassembler != null) {
			reassembler.clear();
		}
	}

	public static void onServerTick(MinecraftServer server) {
		if (activeServer != server) {
			return;
		}
		long nowNanos = System.nanoTime();
		REQUEST_REASSEMBLERS.entrySet().removeIf(entry -> {
			RemoteConfigPayloadReassembler reassembler = entry.getValue();
			reassembler.expire(nowNanos);
			return reassembler.isEmpty();
		});
	}

	public static void handleRequestChunk(ServerPlayer player, RemoteConfigRequestChunkPayload chunk) {
		UUID playerId = player.getUUID();
		RemoteConfigPayloadReassembler reassembler = REQUEST_REASSEMBLERS.computeIfAbsent(
			playerId,
			ignored -> new RemoteConfigPayloadReassembler()
		);
		try {
			reassembler.accept(chunk.payloadInternal())
				.map(RemoteConfigPayloadCodec::decode)
				.ifPresent(message -> scheduleRequest(player, message));
		} catch (RuntimeException exception) {
			LOGGER.warn(
				"Rejected malformed remote config fragment from {}: {}",
				player.getGameProfile().getName(),
				getExceptionMessage(exception)
			);
			LOGGER.debug("Malformed remote config fragment details.", exception);
		} finally {
			if (reassembler.isEmpty()) {
				REQUEST_REASSEMBLERS.remove(playerId, reassembler);
			}
		}
	}

	private static void scheduleRequest(ServerPlayer player, RemoteConfigMessage message) {
		if (!(message instanceof RemoteConfigMessage.SnapshotRequest) &&
			!(message instanceof RemoteConfigMessage.UpdateRequest)
		) {
			LOGGER.warn("Rejected a remote config response sent in the serverbound channel by {}.", player.getGameProfile().getName());
			return;
		}
		MinecraftServer server = player.getServer();
		RemoteConfigRequestHandler handler = requestHandler;
		if (server == null || server != activeServer || handler == null) {
			return;
		}
		Runnable task = () -> handleRequest(server, handler, player, message);
		if (server.isSameThread()) {
			task.run();
		} else {
			server.execute(task);
		}
	}

	private static void handleRequest(
		MinecraftServer server,
		RemoteConfigRequestHandler handler,
		ServerPlayer player,
		RemoteConfigMessage message
	) {
		if (activeServer != server || requestHandler != handler ||
			server.getPlayerList().getPlayer(player.getUUID()) != player
		) {
			return;
		}
		boolean canEdit = hasEditPermission(server, player);
		RemoteConfigMessage response = switch (message) {
			case RemoteConfigMessage.SnapshotRequest request -> handler.handleSnapshot(request, canEdit);
			case RemoteConfigMessage.UpdateRequest request -> handler.handleUpdate(request, canEdit);
			default -> throw new IllegalArgumentException("Unexpected serverbound remote config message.");
		};
		sendResponse(player, response);
	}

	private static boolean hasEditPermission(MinecraftServer server, ServerPlayer player) {
		return player.createCommandSourceStack().hasPermission(server.getOperatorUserPermissionLevel());
	}

	private static void sendResponse(ServerPlayer player, RemoteConfigMessage response) {
		try {
			RemoteConfigPayloadCodec.encode(response);
			RemoteConfigNetworking.sendToPlayer(player, response);
		} catch (RuntimeException exception) {
			LOGGER.error("Failed to create remote config response for {}.", response.schemaKey(), exception);
			RemoteConfigMessage fallback = createUnavailableFallback(response);
			RemoteConfigNetworking.sendToPlayer(player, fallback);
		}
	}

	private static RemoteConfigMessage createUnavailableFallback(RemoteConfigMessage response) {
		String error = "This server config is not available for remote editing.";
		return switch (response) {
			case RemoteConfigMessage.SnapshotResponse snapshot -> new RemoteConfigMessage.SnapshotResponse(
				snapshot.requestId(),
				snapshot.schemaKey(),
				false,
				false,
				error,
				0,
				java.util.List.of()
			);
			case RemoteConfigMessage.UpdateResponse update -> new RemoteConfigMessage.UpdateResponse(
				update.requestId(),
				update.schemaKey(),
				false,
				false,
				error,
				0,
				java.util.List.of()
			);
			default -> throw new IllegalArgumentException("A request cannot be sent as a remote config response.");
		};
	}

	private static synchronized void closeRequestHandler() {
		RemoteConfigRequestHandler handler = requestHandler;
		requestHandler = null;
		if (handler != null) {
			handler.close();
		}
	}

	private static String getExceptionMessage(RuntimeException exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			return exception.getClass().getSimpleName();
		}
		return message;
	}
}
