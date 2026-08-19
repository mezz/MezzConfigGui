package net.mezzdev.config.gui.remote;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RemoteConfigResponseChunkPayload(byte[] payload) implements CustomPacketPayload {
	public static final Type<RemoteConfigResponseChunkPayload> TYPE = new Type<>(
		ResourceLocation.fromNamespaceAndPath("mezz_config_gui", "remote_config_response")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, RemoteConfigResponseChunkPayload> STREAM_CODEC = StreamCodec.ofMember(
		RemoteConfigResponseChunkPayload::write,
		RemoteConfigResponseChunkPayload::new
	);

	public RemoteConfigResponseChunkPayload {
		payload = RemoteConfigPayloadChunker.validateAndCopy(payload);
	}

	private RemoteConfigResponseChunkPayload(RegistryFriendlyByteBuf buffer) {
		this(buffer.readByteArray(RemoteConfigPayloadChunker.MAX_NETWORK_PAYLOAD_LENGTH));
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeByteArray(payload);
	}

	byte[] payloadInternal() {
		return payload;
	}

	@Override
	public byte[] payload() {
		return payload.clone();
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
