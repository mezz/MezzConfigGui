package net.mezzdev.config.gui.popup;

import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValuePopup;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.info.ConfigValueIcon;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
	private static final int BACKGROUND_COLOR = 0xF0101218;
	private static final int ROW_BACKGROUND_COLOR = 0xAA1A1D24;
	private static final int ROW_HOVER_COLOR = 0xFF313A46;
	private static final int BORDER_DARK_COLOR = 0xE0000000;
	private static final int BORDER_LIGHT_COLOR = 0x45FFFFFF;
	private static final int DIVIDER_COLOR = 0x22FFFFFF;

	private final IConfigScreenValue<T> configValue;
	private final List<ValueEntry> valueEntries;

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
		return Math.clamp(width + 12, MIN_ENTRY_WIDTH, MAX_ENTRY_WIDTH);
	}

	@Override
	public int getHeight() {
		if (valueEntries.isEmpty()) {
			return 0;
		}
		return valueEntries.size() * ENTRY_HEIGHT + BORDER_SIZE * 2;
	}

	@Override
	public Optional<T> getHoveredValue(Rect2i area, double mouseX, double mouseY) {
		for (int i = 0; i < valueEntries.size(); i++) {
			ValueEntry entry = valueEntries.get(i);
			if (getValueArea(area, i).contains(mouseX, mouseY)) {
				return Optional.of(entry.value);
			}
		}
		return Optional.empty();
	}

	@Override
	public void draw(GuiGraphics guiGraphics, Rect2i area, double mouseX, double mouseY) {
		if (isEmpty(area)) {
			return;
		}

		Font font = Minecraft.getInstance().font;
		drawBackground(guiGraphics, area);
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
	}

	public boolean isEmpty() {
		return valueEntries.isEmpty();
	}

	private static void drawBackground(GuiGraphics guiGraphics, Rect2i area) {
		int x = area.getX();
		int y = area.getY();
		int right = x + area.getWidth();
		int bottom = y + area.getHeight();

		guiGraphics.fill(x, y, right, bottom, BACKGROUND_COLOR);
		guiGraphics.fill(x, y, right, y + 1, BORDER_DARK_COLOR);
		guiGraphics.fill(x, y, x + 1, bottom, BORDER_DARK_COLOR);
		guiGraphics.fill(right - 1, y, right, bottom, BORDER_LIGHT_COLOR);
		guiGraphics.fill(x, bottom - 1, right, bottom, BORDER_LIGHT_COLOR);
	}

	private static void drawEntryBackground(GuiGraphics guiGraphics, ImmutableRect2i valueArea, boolean hovered, boolean drawDivider) {
		int x = valueArea.getX();
		int y = valueArea.getY();
		int right = x + valueArea.getWidth();
		int bottom = y + valueArea.getHeight();

		guiGraphics.fill(x, y, right, bottom, getBackgroundColor(hovered));
		if (drawDivider) {
			guiGraphics.fill(x, y, right, y + 1, DIVIDER_COLOR);
		}
	}

	private static int getTextColor(boolean hovered) {
		if (hovered) {
			return ConfigEntryWidget.HOVER_TEXT_COLOR;
		}
		return ConfigEntryWidget.TEXT_COLOR;
	}

	private static int getBackgroundColor(boolean hovered) {
		if (hovered) {
			return ROW_HOVER_COLOR;
		}
		return ROW_BACKGROUND_COLOR;
	}

	private static ImmutableRect2i getValueArea(Rect2i area, int index) {
		return new ImmutableRect2i(
			area.getX() + BORDER_SIZE,
			area.getY() + BORDER_SIZE + index * ENTRY_HEIGHT,
			Math.max(0, area.getWidth() - BORDER_SIZE * 2),
			ENTRY_HEIGHT
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
