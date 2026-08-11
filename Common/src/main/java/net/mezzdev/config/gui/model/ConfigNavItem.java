package net.mezzdev.config.gui.model;

import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.util.Pair;
import net.mezzdev.config.gui.util.StringUtil;
import net.mezzdev.config.gui.ConfigInputUtil;
import net.mezzdev.config.gui.ConfigScreenLayout;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.ConfigInputHandler;
import net.mezzdev.config.gui.input.UserInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * Navigation item that selects a category and displays its hover info.
 */
public final class ConfigNavItem implements ConfigInputHandler {
	private static final int TEXT_LEFT_PADDING = 6;
	private static final int ACTIVE_TEXT_LEFT_PADDING = 8;
	private static final int TEXT_RIGHT_PADDING = 6;
	private static final int TEXT_VERTICAL_PADDING = 5;
	private static final int MAX_TEXT_LINES = 2;
	private static final int ACTIVE_ACCENT_WIDTH = 2;
	private static final int BACKGROUND_COLOR = 0x33000000;
	private static final int HOVER_BACKGROUND_COLOR = 0x22FFFFFF;
	private static final int ACTIVE_BACKGROUND_COLOR = 0x66313A46;
	private static final int ACTIVE_ACCENT_COLOR = 0xFF5E9AD6;
	private static final int DIVIDER_COLOR = 0x2AFFFFFF;
	private static final int TEXT_COLOR = 0xFF303030;
	private static final int HOVER_TEXT_COLOR = 0xFF202020;
	private static final int ACTIVE_TEXT_COLOR = 0xFF18202A;

	private final Component fullName;
	private final int categoryIndex;
	private final ConfigCategoryWidget categoryWidget;
	private final Supplier<ImmutableRect2i> navAreaSupplier;
	private final IntConsumer categorySelector;

	private ImmutableRect2i area = ImmutableRect2i.EMPTY;
	private ImmutableRect2i hoverArea = ImmutableRect2i.EMPTY;
	private List<FormattedCharSequence> visibleNameLines = List.of(FormattedCharSequence.EMPTY);
	private int cachedHeight = ConfigScreenLayout.NAV_ITEM_HEIGHT;

	public ConfigNavItem(
		Component displayName,
		int categoryIndex,
		ConfigCategoryWidget categoryWidget,
		Supplier<ImmutableRect2i> navAreaSupplier,
		IntConsumer categorySelector
	) {
		this.fullName = StringUtil.stripStyling(displayName);
		this.categoryIndex = categoryIndex;
		this.categoryWidget = categoryWidget;
		this.navAreaSupplier = navAreaSupplier;
		this.categorySelector = categorySelector;
	}

	public int calculateHeight(int availableWidth) {
		Font font = Minecraft.getInstance().font;
		int textWidth = Math.max(0, availableWidth - ACTIVE_TEXT_LEFT_PADDING - TEXT_RIGHT_PADDING);
		Pair<List<FormattedText>, Boolean> splitLines = StringUtil.splitLines(font, List.of(fullName), textWidth, MAX_TEXT_LINES);
		visibleNameLines = Language.getInstance().getVisualOrder(splitLines.first());
		int textHeight = visibleNameLines.size() * font.lineHeight;
		cachedHeight = Math.max(ConfigScreenLayout.NAV_ITEM_HEIGHT, textHeight + TEXT_VERTICAL_PADDING);
		return cachedHeight;
	}

	public void updateBounds(ImmutableRect2i area, int hoverHeight) {
		this.area = area;
		this.hoverArea = new ImmutableRect2i(area.getX(), area.getY(), area.getWidth(), hoverHeight);
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return hoverArea.contains(mouseX, mouseY);
	}

	public void draw(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean active) {
		Font font = Minecraft.getInstance().font;
		ImmutableRect2i navArea = navAreaSupplier.get();
		boolean hovered = isMouseOver(mouseX, mouseY) && navArea.contains(mouseX, mouseY);

		int x = area.getX();
		int y = area.getY();
		int right = area.getX() + area.getWidth();
		int bottom = area.getY() + area.getHeight();
		int hoverBottom = hoverArea.getY() + hoverArea.getHeight();

		guiGraphics.fill(x, y, right, bottom, getBackgroundColor(active));
		if (hovered) {
			guiGraphics.fill(x, y, right, hoverBottom, HOVER_BACKGROUND_COLOR);
		}
		if (active) {
			guiGraphics.fill(x, y, x + ACTIVE_ACCENT_WIDTH, bottom, ACTIVE_ACCENT_COLOR);
		}
		guiGraphics.fill(x, bottom - 1, right, bottom, DIVIDER_COLOR);

		int textColor = getTextColor(active, hovered);
		int textX = area.getX() + getTextLeftPadding(active);
		int textHeight = visibleNameLines.size() * font.lineHeight;
		int textY = area.getY() + Math.round((area.getHeight() - textHeight) / 2.0f);
		for (FormattedCharSequence visibleNameLine : visibleNameLines) {
			guiGraphics.drawString(font, visibleNameLine, textX, textY, textColor, false);
			textY += font.lineHeight;
		}
	}

	private static int getBackgroundColor(boolean active) {
		if (active) {
			return ACTIVE_BACKGROUND_COLOR;
		}
		return BACKGROUND_COLOR;
	}

	private static int getTextColor(boolean active, boolean hovered) {
		if (active) {
			return ACTIVE_TEXT_COLOR;
		}
		if (hovered) {
			return HOVER_TEXT_COLOR;
		}
		return TEXT_COLOR;
	}

	private static int getTextLeftPadding(boolean active) {
		if (active) {
			return ACTIVE_TEXT_LEFT_PADDING;
		}
		return TEXT_LEFT_PADDING;
	}

	public ConfigInfo getInfo() {
		return categoryWidget.getInfo();
	}

	@Override
	public Optional<ConfigInputHandler> handleUserInput(Screen screen, UserInput input) {
		ImmutableRect2i navArea = navAreaSupplier.get();
		if (navArea.contains(input.getMouseX(), input.getMouseY())
			&& isMouseOver(input.getMouseX(), input.getMouseY())
			&& ConfigInputUtil.isLeftClick(input)
		) {
			if (!input.isSimulate()) {
				categorySelector.accept(categoryIndex);
			}
			return Optional.of(this);
		}
		return Optional.empty();
	}
}
