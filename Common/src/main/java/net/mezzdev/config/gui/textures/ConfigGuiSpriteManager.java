package net.mezzdev.config.gui.textures;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

import java.util.Set;

public class ConfigGuiSpriteManager extends TextureAtlasHolder {
	static final String TEXTURE_NAMESPACE = "mezz_config";
	private static final ResourceLocation CONFIG_GUI_TEXTURE_ATLAS_LOCATION = ResourceLocation.fromNamespaceAndPath(TEXTURE_NAMESPACE, "textures/atlas/gui.png");
	private static final ResourceLocation CONFIG_GUI_TEXTURE_ATLAS_ID = ResourceLocation.fromNamespaceAndPath(TEXTURE_NAMESPACE, "gui");

	public ConfigGuiSpriteManager(TextureManager textureManager) {
		super(textureManager, CONFIG_GUI_TEXTURE_ATLAS_LOCATION, CONFIG_GUI_TEXTURE_ATLAS_ID, Set.of(AnimationMetadataSection.SERIALIZER, GuiMetadataSection.TYPE));
	}

	/**
	 * Overridden to make it public.
	 */
	@Override
	public TextureAtlasSprite getSprite(ResourceLocation location) {
		return super.getSprite(location);
	}

	public GuiSpriteScaling getSpriteScaling(TextureAtlasSprite sprite) {
		return getMetadata(sprite).scaling();
	}

	private GuiMetadataSection getMetadata(TextureAtlasSprite sprite) {
		SpriteContents contents = sprite.contents();
		ResourceMetadata metadata = contents.metadata();
		return metadata.getSection(GuiMetadataSection.TYPE)
			.orElse(GuiMetadataSection.DEFAULT);
	}
}
