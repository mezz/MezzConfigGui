package net.mezzdev.config.gui.textures;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

/** Reads the same GUI metadata used by newer Minecraft versions. */
record LegacySpriteScaling(String type, int width, int height, int left, int right, int top, int bottom) {
	private static final LegacySpriteScaling STRETCH = new LegacySpriteScaling("stretch", 1, 1, 0, 0, 0, 0);
	private static final Map<TextureAtlasSprite, LegacySpriteScaling> CACHE = new WeakHashMap<>();
	private static final MetadataSectionSerializer<LegacySpriteScaling> SERIALIZER = new MetadataSectionSerializer<>() {
		@Override
		public String getMetadataSectionName() { return "gui"; }
		@Override
		public LegacySpriteScaling fromJson(JsonObject metadata) {
			if (!metadata.has("scaling"))
				return STRETCH;
			JsonObject scaling = metadata.getAsJsonObject("scaling");
			String type = scaling.get("type").getAsString();
			if (type.equals("stretch"))
				return STRETCH;
			int width = scaling.get("width").getAsInt();
			int height = scaling.get("height").getAsInt();
			if (type.equals("tile"))
				return new LegacySpriteScaling(type, width, height, 0, 0, 0, 0);
			JsonElement border = scaling.get("border");
			if (border.isJsonPrimitive()) {
				int size = border.getAsInt();
				return new LegacySpriteScaling(type, width, height, size, size, size, size);
			}
			JsonObject edges = border.getAsJsonObject();
			return new LegacySpriteScaling(type, width, height, edges.get("left").getAsInt(), edges.get("right").getAsInt(), edges.get("top").getAsInt(), edges.get("bottom").getAsInt());
		}
	};
	LegacySpriteScaling {
		if (width <= 0 || height <= 0 || left < 0 || right < 0 || top < 0 || bottom < 0 || left + right >= width || top + bottom >= height) {
			throw new IllegalArgumentException("Invalid GUI sprite scaling");
		}
	}
	static LegacySpriteScaling get(TextureAtlasSprite sprite) {
		return CACHE.computeIfAbsent(sprite, LegacySpriteScaling::load);
	}
	private static LegacySpriteScaling load(TextureAtlasSprite sprite) {
		ResourceLocation id = sprite.contents().name();
		ResourceLocation texture = new ResourceLocation(id.getNamespace(), "textures/mezz_config/atlas/gui/" + id.getPath() + ".png");
		try {
			var resource = Minecraft.getInstance().getResourceManager().getResource(texture);
			if (resource.isPresent())
				return resource.get().metadata().getSection(SERIALIZER).orElse(STRETCH);
		} catch (IOException | RuntimeException exception) {
			org.apache.logging.log4j.LogManager.getLogger().warn("Failed to read GUI sprite metadata for {}", texture, exception);
		}
		return STRETCH;
	}
}
