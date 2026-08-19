package net.mezzdev.config.gui.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.mezzdev.config.gui.remote.RemoteConfigEditorServer;
import net.mezzdev.config.gui.remote.RemoteConfigNetworking;
import net.mezzdev.config.gui.remote.RemoteConfigRequestChunkPayload;
import net.mezzdev.config.gui.remote.RemoteConfigResponseChunkPayload;

/**
 * Fabric common entry point and optional remote config editor networking registration.
 */
public final class ConfigGuiFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playC2S().register(
			RemoteConfigRequestChunkPayload.TYPE,
			RemoteConfigRequestChunkPayload.STREAM_CODEC
		);
		PayloadTypeRegistry.playS2C().register(
			RemoteConfigResponseChunkPayload.TYPE,
			RemoteConfigResponseChunkPayload.STREAM_CODEC
		);
		ServerPlayNetworking.registerGlobalReceiver(
			RemoteConfigRequestChunkPayload.TYPE,
			(payload, context) -> RemoteConfigEditorServer.handleRequestChunk(context.player(), payload)
		);
		RemoteConfigNetworking.setServerSender((player, payload) -> {
			if (!ServerPlayNetworking.canSend(player, payload.type())) {
				return false;
			}
			ServerPlayNetworking.send(player, payload);
			return true;
		});
		ServerLifecycleEvents.SERVER_STARTED.register(RemoteConfigEditorServer::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPED.register(RemoteConfigEditorServer::onServerStopped);
		ServerTickEvents.END_SERVER_TICK.register(RemoteConfigEditorServer::onServerTick);
		ServerPlayConnectionEvents.DISCONNECT.register(
			(handler, server) -> RemoteConfigEditorServer.onPlayerDisconnect(handler.player)
		);
	}
}
