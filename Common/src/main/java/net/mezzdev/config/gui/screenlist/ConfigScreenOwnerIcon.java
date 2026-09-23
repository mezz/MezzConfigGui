package net.mezzdev.config.gui.screenlist;

import net.mezzdev.config.gui.ConfigRenderUtil;

import com.mojang.blaze3d.platform.NativeImage;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Lazily loads and draws a config screen owner's mod icon.
 */
public final class ConfigScreenOwnerIcon {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final String MINECRAFT_MOD_ID = "minecraft";
	private static final int ITEM_ICON_SIZE = 16;

	private final String modId;
	private final Component displayName;
	@Nullable
	private final Path iconPath;
	@Nullable
	private LoadedIcon loadedIcon;
	private boolean loadAttempted;

	public ConfigScreenOwnerIcon(String modId, Component displayName, @Nullable Path iconPath) {
		this.modId = Objects.requireNonNull(modId, "modId");
		this.displayName = Objects.requireNonNull(displayName, "displayName");
		this.iconPath = iconPath;
	}

	public void draw(GuiGraphics guiGraphics, Font font, ImmutableRect2i iconArea) {
		Optional<LoadedIcon> icon = getLoadedIcon();
		if (icon.isPresent()) {
			icon.get().draw(guiGraphics, iconArea);
			return;
		}
		drawPlaceholder(guiGraphics, font, iconArea);
	}

	private Optional<LoadedIcon> getLoadedIcon() {
		if (loadedIcon != null) {
			return Optional.of(loadedIcon);
		}
		if (!loadAttempted) {
			loadAttempted = true;
			loadedIcon = loadIcon().orElse(null);
		}
		return Optional.ofNullable(loadedIcon);
	}

	private Optional<LoadedIcon> loadIcon() {
		Path iconPath = this.iconPath;
		if (iconPath == null) {
			return Optional.empty();
		}
		try (InputStream inputStream = Files.newInputStream(iconPath)) {
			NativeImage image = NativeImage.read(inputStream);
			int imageWidth = image.getWidth();
			int imageHeight = image.getHeight();
			Identifier location = ConfigRenderUtil.registerIcon(image);
			return Optional.of(new LoadedIcon(location, imageWidth, imageHeight));
		} catch (IOException | RuntimeException e) {
			LOGGER.debug("Failed to load config screen icon for mod id: {}, path: {}", modId, iconPath, e);
			return Optional.empty();
		}
	}

	private void drawPlaceholder(GuiGraphics guiGraphics, Font font, ImmutableRect2i iconArea) {
		if (MINECRAFT_MOD_ID.equals(modId)) {
			drawMinecraftIcon(guiGraphics, iconArea);
			return;
		}
		int color = getPlaceholderColor(modId);
		guiGraphics.fill(iconArea.getX(), iconArea.getY(), iconArea.getX() + iconArea.getWidth(), iconArea.getY() + iconArea.getHeight(), color);
		drawInsetBorder(guiGraphics, iconArea);
		String initials = getInitials(displayName, modId);
		float scale = Math.min(1.0F, (float) Math.max(1, iconArea.getWidth() - 4) / Math.max(1, font.width(initials)));
		ConfigRenderUtil.pushPose(guiGraphics);
		ConfigRenderUtil.translate(guiGraphics, iconArea.getX() + iconArea.getWidth() / 2, iconArea.getY() + Math.round((iconArea.getHeight() - font.lineHeight * scale) / 2.0F), 0);
		ConfigRenderUtil.scale(guiGraphics, scale, scale, 1.0F);
		guiGraphics.drawCenteredString(
			font,
			initials,
			0,
			0,
			ConfigEntryWidget.getConfiguredTextColor()
		);
		ConfigRenderUtil.popPose(guiGraphics);
	}

	private static void drawMinecraftIcon(GuiGraphics guiGraphics, ImmutableRect2i iconArea) {
		drawIconBorder(guiGraphics, iconArea);
		ConfigRenderUtil.pushPose(guiGraphics);
		float scale = (float) iconArea.getWidth() / ITEM_ICON_SIZE;
		ConfigRenderUtil.translate(guiGraphics, iconArea.getX(), iconArea.getY(), 0);
		ConfigRenderUtil.scale(guiGraphics, scale, scale, 1.0F);
		guiGraphics.renderFakeItem(MinecraftIconHolder.ICON, 0, 0);
		ConfigRenderUtil.popPose(guiGraphics);
	}

	private static final class MinecraftIconHolder {
		private static final ItemStack ICON = new ItemStack(Blocks.GRASS_BLOCK);

		private MinecraftIconHolder() {

		}
	}

	private static int getPlaceholderColor(String modId) {
		int hash = modId.hashCode();
		int red = 0x40 + (hash & 0x3F);
		int green = 0x40 + ((hash >> 8) & 0x3F);
		int blue = 0x40 + ((hash >> 16) & 0x3F);
		return 0xFF000000 | red << 16 | green << 8 | blue;
	}

	static String getInitials(Component displayName, String modId) {
		List<String> words = getNameWords(displayName.getString());
		if (words.isEmpty()) {
			words = getNameWords(modId);
		}
		if (words.isEmpty()) {
			return "?";
		}
		StringBuilder initials = new StringBuilder();
		if (words.size() > 1) {
			words.stream().limit(3)
				.mapToInt(word -> word.codePointAt(0))
				.map(Character::toUpperCase)
				.forEach(initials::appendCodePoint);
		} else {
			words.get(0).codePoints().limit(3)
				.map(Character::toUpperCase)
				.forEach(initials::appendCodePoint);
		}
		return initials.toString();
	}

	private static List<String> getNameWords(String name) {
		String spacedName = name.replaceAll("([\\p{Ll}\\p{N}])(\\p{Lu})", "$1 $2");
		return Arrays.stream(spacedName.split("[^\\p{L}\\p{N}]+"))
			.filter(word -> !word.isEmpty())
			.toList();
	}

	private static void drawInsetBorder(GuiGraphics guiGraphics, ImmutableRect2i area) {
		int x = area.getX();
		int y = area.getY();
		int right = x + area.getWidth();
		int bottom = y + area.getHeight();
		guiGraphics.fill(x, y, right, y + 1, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_INSET_BORDER_DARK));
		guiGraphics.fill(x, y, x + 1, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_INSET_BORDER_DARK));
		guiGraphics.fill(x, bottom - 1, right, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_INSET_BORDER_LIGHT));
		guiGraphics.fill(right - 1, y, right, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_INSET_BORDER_LIGHT));
	}

	private static void drawIconBorder(GuiGraphics guiGraphics, ImmutableRect2i iconArea) {
		guiGraphics.fill(
			iconArea.getX() - 1,
			iconArea.getY() - 1,
			iconArea.getX() + iconArea.getWidth() + 1,
			iconArea.getY() + iconArea.getHeight() + 1,
			ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SCREEN_LIST_ICON_BORDER)
		);
	}

	private record LoadedIcon(
		Identifier location,
		int width,
		int height
	) {
		private LoadedIcon {
			Objects.requireNonNull(location, "location");
		}

		public void draw(GuiGraphics guiGraphics, ImmutableRect2i iconArea) {
			drawIconBorder(guiGraphics, iconArea);
			ImmutableRect2i fittedIconArea = getFittedIconArea(iconArea);
			ConfigRenderUtil.blit(guiGraphics,
				location,
				fittedIconArea.getX(),
				fittedIconArea.getY(),
				fittedIconArea.getWidth(),
				fittedIconArea.getHeight(),
				0.0F,
				0.0F,
				width,
				height,
				width,
				height
			);
		}

		private ImmutableRect2i getFittedIconArea(ImmutableRect2i iconArea) {
			int fittedWidth = iconArea.getWidth();
			int fittedHeight = Math.max(1, fittedWidth * height / width);
			if (fittedHeight > iconArea.getHeight()) {
				fittedHeight = iconArea.getHeight();
				fittedWidth = Math.max(1, fittedHeight * width / height);
			}
			return new ImmutableRect2i(
				iconArea.getX() + (iconArea.getWidth() - fittedWidth) / 2,
				iconArea.getY() + (iconArea.getHeight() - fittedHeight) / 2,
				fittedWidth,
				fittedHeight
			);
		}
	}
}
