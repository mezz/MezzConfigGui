package net.mezzdev.config.gui.popup;

import net.mezzdev.config.gui.util.ConfigMath;

import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValuePopup;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.info.ConfigValueIcon;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Generic popup content for choosing one value from a finite list.
 */
public final class ConfigValueSelector<T> implements IConfigValuePopup<T> {

	private static final int ENTRY_HEIGHT = 20;
	private static final int BORDER_SIZE = 1;
	private static final int MIN_ENTRY_WIDTH = 50;
	private static final int MAX_ENTRY_WIDTH = 160;
	private static final int TEXT_PADDING = 4;
	private static final int SCROLLBAR_WIDTH = 3;

	private final IConfigScreenValue<T> configValue;
	private final List<ValueEntry> valueEntries;
	private int scrollOffset;

	public ConfigValueSelector(IConfigScreenValue<T> configValue, List<T> allValues, @Nullable T currentValue) {
		this.configValue = configValue;
		this.valueEntries = allValues.stream()
			.filter(value -> !value.equals(currentValue))
			.map(value -> new ValueEntry(value, getValueName(value)))
			.toList();
	}

	@Override
	public int getWidth() {
		Font font = Minecraft.getInstance().font;
		int width = valueEntries.stream()
			.mapToInt(entry -> {
				int textWidth = (int) (font.width(entry.label) * ConfigEntryWidget.TEXT_SCALE);
				return textWidth + ConfigValueIcon.getTextOffset(configValue, entry.value);
			})
			.max().orElse(MIN_ENTRY_WIDTH);
		return ConfigMath.clamp(width + 12, MIN_ENTRY_WIDTH, MAX_ENTRY_WIDTH);
	}

	@Override
	public int getHeight() {
		if (valueEntries.isEmpty()) {
			return 0;
		}
		return valueEntries.size() * ENTRY_HEIGHT + BORDER_SIZE * 2;
	}

	@Override
	public Size getPreferredSize(int availableWidth, int availableHeight) {
		return new Size(
			Math.min(getWidth(), Math.max(0, availableWidth)),
			Math.min(getHeight(), Math.max(0, availableHeight))
		);
	}

	@Override
	public Optional<T> getHoveredValue(Rect2i area, double mouseX, double mouseY) {
		ImmutableRect2i contentArea = getContentArea(area);
		if (!contentArea.contains(mouseX, mouseY)) {
			return Optional.empty();
		}
		clampScrollOffset(contentArea);
		for (int i = 0; i < valueEntries.size(); i++) {
			ValueEntry entry = valueEntries.get(i);
			if (getValueArea(area, i).contains(mouseX, mouseY)) {
				return Optional.of(entry.value);
			}
		}
		return Optional.empty();
	}

	@Override
	public void draw(GuiGraphicsExtractor guiGraphics, Rect2i area, double mouseX, double mouseY) {
		if (isEmpty(area)) {
			return;
		}

		Font font = Minecraft.getInstance().font;
		drawBackground(guiGraphics, area);
		ImmutableRect2i contentArea = getContentArea(area);
		clampScrollOffset(contentArea);
		guiGraphics.enableScissor(
			contentArea.getX(),
			contentArea.getY(),
			contentArea.getX() + contentArea.getWidth(),
			contentArea.getY() + contentArea.getHeight()
		);
		for (int i = 0; i < valueEntries.size(); i++) {
			ValueEntry entry = valueEntries.get(i);
			ImmutableRect2i valueArea = getValueArea(area, i);
			boolean hovered = valueArea.contains(mouseX, mouseY);
			drawEntryBackground(guiGraphics, valueArea, hovered, i > 0);
			int contentX = valueArea.getX() + TEXT_PADDING;
			int iconY = valueArea.getY() + (valueArea.getHeight() - ConfigValueIcon.ICON_SIZE) / 2;
			ConfigValueIcon.draw(guiGraphics, configValue, entry.value, contentX, iconY);
			int textX = contentX + ConfigValueIcon.getTextOffset(configValue, entry.value);
			ImmutableRect2i textArea = new ImmutableRect2i(
				textX,
				valueArea.getY(),
				Math.max(0, valueArea.getX() + valueArea.getWidth() - textX - TEXT_PADDING),
				valueArea.getHeight()
			);
			ConfigEntryWidget.drawFittedText(guiGraphics, font, entry.label, textArea, getTextColor(hovered), false);
		}
		guiGraphics.disableScissor();
		drawScrollbar(guiGraphics, contentArea);
	}

	@Override
	public boolean mouseScrolled(Rect2i area, double mouseX, double mouseY, double scrollX, double scrollY) {
		ImmutableRect2i contentArea = getContentArea(area);
		int maxScroll = getMaxScroll(contentArea);
		if (!contentArea.contains(mouseX, mouseY) || maxScroll <= 0 || scrollY == 0.0) {
			return false;
		}
		int scrollAmount = Math.max(1, (int) Math.round(Math.abs(scrollY))) * ENTRY_HEIGHT;
		if (scrollY > 0.0) {
			scrollOffset = Math.max(0, scrollOffset - scrollAmount);
		} else {
			scrollOffset = Math.min(maxScroll, scrollOffset + scrollAmount);
		}
		return true;
	}

	public boolean isEmpty() {
		return valueEntries.isEmpty();
	}

	private static void drawBackground(GuiGraphicsExtractor guiGraphics, Rect2i area) {
		int x = area.getX();
		int y = area.getY();
		int right = x + area.getWidth();
		int bottom = y + area.getHeight();

		guiGraphics.fill(x, y, right, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.VALUE_SELECTOR_BACKGROUND));
		guiGraphics.fill(x, y, right, y + 1, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.VALUE_SELECTOR_BORDER_DARK));
		guiGraphics.fill(x, y, x + 1, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.VALUE_SELECTOR_BORDER_DARK));
		guiGraphics.fill(right - 1, y, right, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.VALUE_SELECTOR_BORDER_LIGHT));
		guiGraphics.fill(x, bottom - 1, right, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.VALUE_SELECTOR_BORDER_LIGHT));
	}

	private static void drawEntryBackground(GuiGraphicsExtractor guiGraphics, ImmutableRect2i valueArea, boolean hovered, boolean drawDivider) {
		int x = valueArea.getX();
		int y = valueArea.getY();
		int right = x + valueArea.getWidth();
		int bottom = y + valueArea.getHeight();

		guiGraphics.fill(x, y, right, bottom, getBackgroundColor(hovered));
		if (drawDivider) {
			guiGraphics.fill(x, y, right, y + 1, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.VALUE_SELECTOR_DIVIDER));
		}
	}

	private static int getTextColor(boolean hovered) {
		if (hovered) {
			return ConfigEntryWidget.getConfiguredHoverTextColor();
		}
		return ConfigEntryWidget.getConfiguredTextColor();
	}

	private static int getBackgroundColor(boolean hovered) {
		if (hovered) {
			return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.VALUE_SELECTOR_ROW_HOVER);
		}
		return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.VALUE_SELECTOR_ROW_BACKGROUND);
	}

	private ImmutableRect2i getValueArea(Rect2i area, int index) {
		return new ImmutableRect2i(
			area.getX() + BORDER_SIZE,
			area.getY() + BORDER_SIZE + index * ENTRY_HEIGHT - scrollOffset,
			Math.max(0, area.getWidth() - BORDER_SIZE * 2),
			ENTRY_HEIGHT
		);
	}

	private static ImmutableRect2i getContentArea(Rect2i area) {
		return new ImmutableRect2i(
			area.getX() + BORDER_SIZE,
			area.getY() + BORDER_SIZE,
			Math.max(0, area.getWidth() - BORDER_SIZE * 2),
			Math.max(0, area.getHeight() - BORDER_SIZE * 2)
		);
	}

	private int getMaxScroll(ImmutableRect2i contentArea) {
		return Math.max(0, valueEntries.size() * ENTRY_HEIGHT - contentArea.getHeight());
	}

	private void clampScrollOffset(ImmutableRect2i contentArea) {
		scrollOffset = ConfigMath.clamp(scrollOffset, 0, getMaxScroll(contentArea));
	}

	private void drawScrollbar(GuiGraphicsExtractor guiGraphics, ImmutableRect2i contentArea) {
		int contentHeight = valueEntries.size() * ENTRY_HEIGHT;
		if (contentArea.isEmpty() || contentHeight <= contentArea.getHeight()) {
			return;
		}
		int scrollbarHeight = Math.max(ENTRY_HEIGHT / 2, contentArea.getHeight() * contentArea.getHeight() / contentHeight);
		int scrollbarTravel = contentArea.getHeight() - scrollbarHeight;
		int scrollbarY = contentArea.getY() + scrollbarTravel * scrollOffset / getMaxScroll(contentArea);
		int scrollbarX = contentArea.getX() + contentArea.getWidth() - SCROLLBAR_WIDTH;
		guiGraphics.fill(
			scrollbarX,
			scrollbarY,
			scrollbarX + SCROLLBAR_WIDTH,
			scrollbarY + scrollbarHeight,
			ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.VALUE_SELECTOR_SCROLLBAR)
		);
	}

	private static boolean isEmpty(Rect2i area) {
		return area.getWidth() <= 0 || area.getHeight() <= 0;
	}

	private class ValueEntry {
		final T value;
		final Component label;

		ValueEntry(T value, Component label) {
			this.value = value;
			this.label = label;
		}
	}

	private Component getValueName(T value) {
		return ConfigValueLocalization.getValueName(configValue, value);
	}
}
