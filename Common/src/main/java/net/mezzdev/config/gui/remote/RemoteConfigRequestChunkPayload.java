package net.mezzdev.config.gui.remote;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RemoteConfigRequestChunkPayload(byte[] payload) implements CustomPacketPayload {
	public static final Type<RemoteConfigRequestChunkPayload> TYPE = new Type<>(
		ResourceLocation.fromNamespaceAndPath("mezz_config_gui", "remote_config_request")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, RemoteConfigRequestChunkPayload> STREAM_CODEC = StreamCodec.ofMember(
		RemoteConfigRequestChunkPayload::write,
		RemoteConfigRequestChunkPayload::new
	);

	public RemoteConfigRequestChunkPayload {
		payload = RemoteConfigPayloadChunker.validateAndCopy(payload);
	}

	private RemoteConfigRequestChunkPayload(RegistryFriendlyByteBuf buffer) {
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
