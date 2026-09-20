package net.mezzdev.config.gui.forge;

import net.mezzdev.config.gui.remote.RemoteConfigEditor;
import net.mezzdev.config.gui.remote.RemoteConfigEditorServer;
import net.mezzdev.config.gui.remote.RemoteConfigNetworking;
import net.mezzdev.config.gui.remote.RemoteConfigRequestChunkPayload;
import net.mezzdev.config.gui.remote.RemoteConfigResponseChunkPayload;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;

public final class ConfigGuiForgeNetwork {
	private static final int PROTOCOL_VERSION = 1;
	private final Channel<CustomPacketPayload> channel;

	public ConfigGuiForgeNetwork() {
		this.channel = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(ConfigGuiForge.MOD_ID, "remote_config"))
			.networkProtocolVersion(PROTOCOL_VERSION)
			.optional()
			.payloadChannel()
			.play()
			.serverbound()
			.add(RemoteConfigRequestChunkPayload.TYPE, RemoteConfigRequestChunkPayload.STREAM_CODEC, this::handleRequest)
			.clientbound()
			.add(RemoteConfigResponseChunkPayload.TYPE, RemoteConfigResponseChunkPayload.STREAM_CODEC, this::handleResponse)
			.build();
		RemoteConfigNetworking.setServerSender((player, payload) -> {
			if (!channel.isRemotePresent(player.connection.getConnection())) {
				return false;
			}
			Packet<?> packet = NetworkDirection.PLAY_TO_CLIENT.buildPacket(channel, payload);
			player.connection.send(packet);
			return true;
		});
	}

	private void handleRequest(RemoteConfigRequestChunkPayload payload, CustomPayloadEvent.Context context) {
		ServerPlayer player = context.getSender();
		if (player != null) {
			context.setPacketHandled(true);
			RemoteConfigEditorServer.handleRequestChunk(player, payload);
		}
	}

	private void handleResponse(RemoteConfigResponseChunkPayload payload, CustomPayloadEvent.Context context) {
		context.setPacketHandled(true);
		context.enqueueWork(() -> RemoteConfigEditor.handleResponseChunk(payload));
	}

	public Channel<CustomPacketPayload> getChannel() {
		return channel;
	}
}
