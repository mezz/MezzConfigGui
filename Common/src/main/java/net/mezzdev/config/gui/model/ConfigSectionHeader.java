package net.mezzdev.config.gui.model;

import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.ConfigInputHandler;
import net.mezzdev.config.gui.ConfigInputUtil;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.util.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A collapsible heading above a section's nested values, with its description available on hover.
 */
public final class ConfigSectionHeader implements ConfigInputHandler {
	private static final int HORIZONTAL_PADDING = 6;
	private static final int DISCLOSURE_WIDTH = 12;
	private static final int TOP_PADDING = 8;
	private static final int BOTTOM_PADDING = 5;

	private final Component title;
	private final Supplier<ConfigInfo> info;
	private final Runnable layoutUpdater;
	private ImmutableRect2i area = ImmutableRect2i.EMPTY;
	private List<FormattedCharSequence> lines = List.of();
	private boolean collapsed;
	private int contentBottom;
	private int cachedWidth = -1;
	private int cachedHeight;

	public ConfigSectionHeader(Component title, Component description) {
		this(title, description, () -> {});
	}

	public ConfigSectionHeader(Component title, Component description, Runnable layoutUpdater) {
		this(title, () -> new ConfigInfo(title, description), layoutUpdater);
	}

	public ConfigSectionHeader(Component title, Supplier<ConfigInfo> info, Runnable layoutUpdater) {
		this.title = StringUtil.stripStyling(title);
		this.info = info;
		this.layoutUpdater = layoutUpdater;
	}

	public int updateBounds(int x, int y, int width) {
		if (width != cachedWidth) {
			Font font = Minecraft.getInstance().font;
			lines = font.split(title, Math.max(1, width - 2 * HORIZONTAL_PADDING - DISCLOSURE_WIDTH));
			cachedWidth = width;
			cachedHeight = Math.max(1, lines.size()) * font.lineHeight + TOP_PADDING + BOTTOM_PADDING;
		}
		area = new ImmutableRect2i(x, y, width, cachedHeight);
		contentBottom = y + cachedHeight;
		return cachedHeight;
	}

	public void setContentBottom(int contentBottom) {
		this.contentBottom = Math.max(area.getY() + area.getHeight(), contentBottom);
	}

	public boolean isCollapsed() {
		return collapsed;
	}

	public void resetBounds() {
		area = ImmutableRect2i.EMPTY;
		contentBottom = 0;
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return area.contains(mouseX, mouseY);
	}

	public ConfigInfo getInfo() {
		return info.get();
	}

	public void draw(GuiGraphics guiGraphics, ImmutableRect2i viewport) {
		ImmutableRect2i railArea = new ImmutableRect2i(
			area.getX() + 1,
			area.getY() + area.getHeight(),
			2,
			Math.max(0, contentBottom - area.getY() - area.getHeight())
		);
		boolean drawHeader = area.intersects(viewport);
		if (!drawHeader && !railArea.intersects(viewport)) {
			return;
		}
		int x = area.getX();
		int y = area.getY();
		int right = x + area.getWidth();
		int bottom = y + area.getHeight();
		guiGraphics.fill(x + 1, bottom, x + 3, contentBottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SECTION_HEADER_DIVIDER));
		if (!drawHeader) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		guiGraphics.fill(x + 1, y + TOP_PADDING / 2, right - 1, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SECTION_HEADER_BACKGROUND));
		guiGraphics.fill(x + 1, bottom - 1, right - 1, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SECTION_HEADER_DIVIDER));
		String disclosure = "▼";
		if (collapsed) {
			disclosure = "▶";
		}
		int disclosureY = y + TOP_PADDING;
		guiGraphics.drawString(font, disclosure, x + HORIZONTAL_PADDING, disclosureY, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SECTION_HEADER_TEXT), false);
		int textY = y + TOP_PADDING;
		for (FormattedCharSequence line : lines) {
			guiGraphics.drawString(font, line, x + HORIZONTAL_PADDING + DISCLOSURE_WIDTH, textY, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.SECTION_HEADER_TEXT), false);
			textY += font.lineHeight;
		}
	}

	@Override
	public Optional<ConfigInputHandler> handleUserInput(@Nullable Screen screen, UserInput input) {
		if (!area.contains(input.getMouseX(), input.getMouseY()) || !ConfigInputUtil.isLeftClick(input)) {
			return Optional.empty();
		}
		if (!input.isSimulate()) {
			collapsed = !collapsed;
			layoutUpdater.run();
		}
		return Optional.of(this);
	}
}
