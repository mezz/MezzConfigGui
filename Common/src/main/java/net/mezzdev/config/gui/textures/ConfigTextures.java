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

	private final ConfigScalableDrawable modTabSelected;
	private final ConfigScalableDrawable modTabUnselected;
	private final ConfigDrawableStatic arrowUp;
	private final ConfigDrawableStatic arrowDown;
	private final ConfigDrawableStatic checkbox;
	private final ConfigDrawableStatic checkboxHighlight;

	private ConfigTextures(ConfigGuiSpriteManager guiSpriteManager) {
		this.guiSpriteManager = guiSpriteManager;

		this.buttonDisabled = createScalableGuiSprite("button_disabled");
		this.buttonEnabled = createScalableGuiSprite("button_enabled");
		this.buttonHighlight = createScalableGuiSprite("button_highlight");
		this.buttonPressed = createScalableGuiSprite("button_pressed");
		this.buttonPressedHighlight = createScalableGuiSprite("button_pressed_highlight");
		this.configScreenBackground = createScalableGuiSprite("gui_background");
		this.searchBackground = createScalableGuiSprite("search_background");
		this.scrollbarBackground = createScalableGuiSprite("scrollbar_background");
		this.scrollbarMarker = createScalableGuiSprite("scrollbar_marker");

		this.modTabSelected = createScalableGuiSprite("mod_tab_selected");
		this.modTabUnselected = createScalableGuiSprite("mod_tab_unselected");
		// These sprites include padding; enlarge them so the visible arrows match JEI navigation icons.
		this.arrowUp = createGuiSprite("icons/arrow_up", 13, 13);
		this.arrowDown = createGuiSprite("icons/arrow_down", 13, 13);
		this.checkbox = createGuiSprite("checkbox", 18, 18);
		this.checkboxHighlight = createGuiSprite("checkbox_highlight", 18, 18);
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

	public ConfigScalableDrawable getModTab(boolean selected) {
		if (selected) {
			return modTabSelected;
		}
		return modTabUnselected;
	}

	public ConfigDrawableStatic getArrowUp() {
		return arrowUp;
	}

	public ConfigDrawableStatic getArrowDown() {
		return arrowDown;
	}

	ConfigDrawableStatic getCheckbox(boolean hovered) {
		if (hovered) {
			return checkboxHighlight;
		}
		return checkbox;
	}
}
