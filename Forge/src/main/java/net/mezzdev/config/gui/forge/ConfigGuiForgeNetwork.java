package net.mezzdev.config.gui.forge;

import net.mezzdev.config.gui.remote.RemoteConfigEditor;
import net.mezzdev.config.gui.remote.RemoteConfigEditorServer;
import net.mezzdev.config.gui.remote.RemoteConfigNetworking;
import net.mezzdev.config.gui.remote.RemoteConfigRequestChunkPayload;
import net.mezzdev.config.gui.remote.RemoteConfigResponseChunkPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ConfigGuiForgeNetwork {
	private static final String PROTOCOL_VERSION = "1";
	private final SimpleChannel channel;

	public ConfigGuiForgeNetwork() {
		channel = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(ConfigGuiForge.MOD_ID, "remote_config"),
			() -> PROTOCOL_VERSION,
			ConfigGuiForgeNetwork::acceptsVersion,
			ConfigGuiForgeNetwork::acceptsVersion
		);
		channel.messageBuilder(RemoteConfigRequestChunkPayload.class, 0, NetworkDirection.PLAY_TO_SERVER)
			.encoder((payload, buffer) -> buffer.writeByteArray(payload.payload()))
			.decoder(buffer -> new RemoteConfigRequestChunkPayload(buffer.readByteArray(RemoteConfigRequestChunkPayload.MAX_NETWORK_PAYLOAD_LENGTH)))
			.consumerMainThread((payload, context) -> {
				ServerPlayer player = context.get().getSender();
				if (player != null)
					RemoteConfigEditorServer.handleRequestChunk(player, payload);
			})
			.add();
		channel.messageBuilder(RemoteConfigResponseChunkPayload.class, 1, NetworkDirection.PLAY_TO_CLIENT)
			.encoder((payload, buffer) -> buffer.writeByteArray(payload.payload()))
			.decoder(buffer -> new RemoteConfigResponseChunkPayload(buffer.readByteArray(RemoteConfigResponseChunkPayload.MAX_NETWORK_PAYLOAD_LENGTH)))
			.consumerMainThread((payload, context) -> RemoteConfigEditor.handleResponseChunk(payload))
			.add();
		RemoteConfigNetworking.setServerSender((player, payload) -> {
			if (!channel.isRemotePresent(player.connection.connection))
				return false;
			channel.send(PacketDistributor.PLAYER.with(() -> player), payload);
			return true;
		});
	}

	private static boolean acceptsVersion(String version) {
		return PROTOCOL_VERSION.equals(version) || NetworkRegistry.ABSENT.equals(version) || NetworkRegistry.ACCEPTVANILLA.equals(version);
	}

	public SimpleChannel getChannel() { return channel; }
}
