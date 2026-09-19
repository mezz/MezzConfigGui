package net.mezzdev.config.gui.textures;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.resources.ResourceLocation;

public final class ConfigGuiSpriteManager extends TextureAtlasHolder {
	static final String TEXTURE_NAMESPACE = "mezz_config";
	public ConfigGuiSpriteManager(TextureManager manager) {
		super(manager, new ResourceLocation(TEXTURE_NAMESPACE, "textures/atlas/gui.png"), "mezz_config/atlas/gui");
	}
	@Override
	protected java.util.stream.Stream<ResourceLocation> getResourcesToLoad() {
		return java.util.stream.Stream.of("button_disabled", "button_enabled", "button_highlight", "button_pressed", "button_pressed_highlight", "checkbox", "checkbox_highlight", "gui_background", "icons/add", "icons/arrow_down", "icons/arrow_up", "icons/button_down", "icons/button_up", "icons/check", "icons/reset", "icons/screen_list", "icons/x", "mod_tab_selected", "mod_tab_unselected", "scrollbar_background", "scrollbar_marker", "search_background").map(name -> new ResourceLocation(TEXTURE_NAMESPACE, name));
	}
	@Override
	public TextureAtlasSprite getSprite(ResourceLocation location) { return super.getSprite(location); }
}
