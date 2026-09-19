package net.mezzdev.config.gui.textures;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.resources.ResourceLocation;

public final class ConfigGuiSpriteManager extends TextureAtlasHolder {
	static final String TEXTURE_NAMESPACE = "mezz_config";
	public ConfigGuiSpriteManager(TextureManager manager) {
		super(manager, new ResourceLocation(TEXTURE_NAMESPACE, "textures/atlas/gui.png"), new ResourceLocation(TEXTURE_NAMESPACE, "gui"));
	}
	@Override
	public TextureAtlasSprite getSprite(ResourceLocation location) { return super.getSprite(location); }
}
