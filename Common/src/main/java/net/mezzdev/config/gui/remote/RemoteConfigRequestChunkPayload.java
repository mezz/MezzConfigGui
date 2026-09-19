package net.mezzdev.config.gui.remote;

import net.minecraft.resources.ResourceLocation;

public record RemoteConfigRequestChunkPayload(byte[] payload) {
	public static final ResourceLocation ID = new ResourceLocation("mezz_config_gui", "remote_config_request");
	public static final int MAX_NETWORK_PAYLOAD_LENGTH = RemoteConfigPayloadChunker.MAX_NETWORK_PAYLOAD_LENGTH;
	public RemoteConfigRequestChunkPayload { payload = RemoteConfigPayloadChunker.validateAndCopy(payload); }
	byte[] payloadInternal() { return payload; }
	@Override
	public byte[] payload() { return payload.clone(); }
}
