package net.mezzdev.config.gui.textures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class ConfigTextures {
	@Nullable
	private static ConfigTextures textures;

	private final ConfigGuiSpriteManager guiSpriteManager;

	private final ConfigScalableDrawable buttonDisabled;
	private final ConfigScalableDrawable buttonEnabled;
	private final ConfigScalableDrawable buttonHighlight;
	private final ConfigScalableDrawable buttonPressed;
	private final ConfigScalableDrawable buttonPressedHighlight;
	private final ConfigScalableDrawable configScreenBackground;
	private final ConfigScalableDrawable searchBackground;
	private final ConfigScalableDrawable scrollbarBackground;
	private final ConfigScalableDrawable scrollbarMarker;

	private final ConfigDrawableStatic arrowUp;
	private final ConfigDrawableStatic arrowDown;

	private ConfigTextures(ConfigGuiSpriteManager guiSpriteManager) {
		this.guiSpriteManager = guiSpriteManager;

		this.buttonDisabled = createScalableGuiSprite("button_disabled_v2");
		this.buttonEnabled = createScalableGuiSprite("button_enabled_v2");
		this.buttonHighlight = createScalableGuiSprite("button_highlight_v2");
		this.buttonPressed = createScalableGuiSprite("button_pressed_v2");
		this.buttonPressedHighlight = createScalableGuiSprite("button_pressed_highlight_v2");
		this.configScreenBackground = createScalableGuiSprite("gui_background_v2");
		this.searchBackground = createScalableGuiSprite("search_background_v2");
		this.scrollbarBackground = createScalableGuiSprite("scrollbar_background_v2");
		this.scrollbarMarker = createScalableGuiSprite("scrollbar_marker_v2");

		this.arrowUp = createGuiSprite("icons/arrow_up", 9, 9);
		this.arrowDown = createGuiSprite("icons/arrow_down", 9, 9);
	}

	public static ConfigTextures get() {
		if (textures == null) {
			Minecraft minecraft = Minecraft.getInstance();
			TextureManager textureManager = minecraft.getTextureManager();
			ConfigGuiSpriteManager spriteManager = new ConfigGuiSpriteManager(textureManager);
			textures = new ConfigTextures(spriteManager);
		}
		return textures;
	}

	private static ResourceLocation createSprite(String name) {
		return ResourceLocation.fromNamespaceAndPath(ConfigGuiSpriteManager.TEXTURE_NAMESPACE, name);
	}

	private ConfigDrawableStatic createGuiSprite(String name, int width, int height) {
		ResourceLocation location = createSprite(name);
		return new ConfigDrawableStatic(() -> guiSpriteManager.getSprite(location), width, height);
	}

	private ConfigScalableDrawable createScalableGuiSprite(String name) {
		ResourceLocation location = createSprite(name);
		return new ConfigScalableDrawable(() -> guiSpriteManager.getSprite(location));
	}

	public ConfigGuiSpriteManager getGuiSpriteManager() {
		return guiSpriteManager;
	}

	public ConfigScalableDrawable getButtonForState(boolean pressed, boolean enabled, boolean hovered) {
		if (!enabled) {
			return buttonDisabled;
		}

		if (hovered) {
			if (pressed) {
				return buttonPressedHighlight;
			}
			return buttonHighlight;
		}
		if (pressed) {
			return buttonPressed;
		}
		return buttonEnabled;
	}

	public ConfigScalableDrawable getConfigScreenBackground() {
		return configScreenBackground;
	}

	public ConfigScalableDrawable getSearchBackground() {
		return searchBackground;
	}

	public ConfigScalableDrawable getScrollbarMarker() {
		return scrollbarMarker;
	}

	public ConfigScalableDrawable getScrollbarBackground() {
		return scrollbarBackground;
	}

	public ConfigDrawableStatic getArrowUp() {
		return arrowUp;
	}

	public ConfigDrawableStatic getArrowDown() {
		return arrowDown;
	}
}
