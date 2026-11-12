package net.mezzdev.config.gui.quickmenu;

import net.mezzdev.config.gui.util.ConfigMath;
import net.mezzdev.config.gui.ConfigContentPage;
import net.mezzdev.config.gui.ConfigGuiColors.GuiColor;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.ConfigInfoPanel;
import net.mezzdev.config.gui.ConfigScreenLayout.ResizeHandle;
import net.mezzdev.config.gui.ConfigScreenResizer;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.quickmenu.QuickMenuReorderDrag.Preview;
import net.mezzdev.config.gui.textures.ConfigButtonIcon;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

import static net.mezzdev.config.gui.quickmenu.QuickMenuLayout.ERROR_HEIGHT;
import static net.mezzdev.config.gui.quickmenu.QuickMenuLayout.PADDING;
import static net.mezzdev.config.gui.quickmenu.QuickMenuLayout.TITLE_HEIGHT;

/** Draws the quick menu, customization controls, and help panel. */
final class QuickMenuView<T> {
	private final Component title;
	private final QuickMenuModel<T> model;
	private final ConfigContentPage page;
	private final QuickMenuLayout layout;
	private final ConfigInfoPanel infoPanel = new ConfigInfoPanel();

	QuickMenuView(Component title, QuickMenuModel<T> model, ConfigContentPage page, QuickMenuLayout layout) {
		this.title = title;
		this.model = model;
		this.page = page;
		this.layout = layout;
	}

	void draw(GuiGraphics graphics, int mouseX, int mouseY, ResizeHandle handle,
		@Nullable Preview<T> reorderDrag, boolean overAnchor, Supplier<@Nullable ConfigInfo> anchorInfo) {
		var font = Minecraft.getInstance().font;
		var textures = ConfigTextures.get();
		graphics.flush();
		textures.getConfigScreenBackground().draw(graphics, layout.area());
		ImmutableRect2i listArea = layout.getListArea();
		fill(graphics, listArea, GuiColor.CONFIG_SCREEN_VALUE_AREA_BACKGROUND);
		ConfigInfo help;
		if (model.isEditing()) {
			help = drawOptions(graphics, mouseX, mouseY, reorderDrag);
			page.drawScrollbar(graphics);
		} else {
			page.draw(graphics, mouseX, mouseY);
			help = page.getInfo(mouseX, mouseY);
		}
		drawInsetBorder(graphics, listArea);
		graphics.flush();
		ImmutableRect2i headerButton = layout.headerButtonArea();
		String editKey = "mezz_config_gui.quickMenu.edit";
		if (model.isEditing()) {
			editKey = "gui.done";
		}
		Component editLabel = Component.translatable(editKey);
		ConfigEntryWidget.drawButtonBackground(graphics, textures, headerButton, true, !page.hasPopup() && headerButton.contains(mouseX, mouseY));
		graphics.flush();
		ConfigEntryWidget.drawCenteredButtonText(graphics, font, editLabel, headerButton, color(GuiColor.CONFIG_ENTRY_TEXT));
		ImmutableRect2i fullConfigButton = layout.fullConfigButtonArea();
		drawButton(graphics, fullConfigButton, ConfigButtonIcon.SCREEN_LIST, true, mouseX, mouseY);
		int titleEnd = fullConfigButton.getX();
		text(graphics, title, new ImmutableRect2i(layout.area().getX() + PADDING, layout.area().getY() + PADDING, titleEnd - layout.area().getX() - PADDING - 4, TITLE_HEIGHT), GuiColor.CONFIG_SCREEN_TITLE_TEXT);
		if (!page.getFailure().isEmpty()) {
			ImmutableRect2i errorArea = new ImmutableRect2i(layout.area().getX() + PADDING, listArea.getY() + listArea.getHeight() + 2, layout.area().getWidth() - PADDING * 2 - 12, ERROR_HEIGHT - 2);
			ConfigEntryWidget.drawFittedText(graphics, font, page.getFailure().get(0), errorArea, color(GuiColor.CONFIG_SCREEN_TITLE_TEXT), true);
		}
		if (model.rows().isEmpty()) {
			text(graphics, Component.translatable("mezz_config_gui.quickMenu.empty"), layout.getRowArea(0), GuiColor.CONFIG_ENTRY_SECONDARY_TEXT);
		}
		if (overAnchor) {
			help = anchorInfo.get();
		} else if (!layout.area().contains(mouseX, mouseY) && !page.isMouseOverPopup(mouseX, mouseY)) {
			help = null;
		} else if (!page.hasPopup() && fullConfigButton.contains(mouseX, mouseY)) {
			help = new ConfigInfo(Component.translatable("mezz_config_gui.quickMenu.fullConfig"),
				Component.translatable("mezz_config_gui.quickMenu.fullConfig.description"));
		} else if (!page.hasPopup() && headerButton.contains(mouseX, mouseY)) {
			String key = "mezz_config_gui.quickMenu.editing";
			if (model.isEditing()) {
				key = "mezz_config_gui.quickMenu.done";
			}
			help = new ConfigInfo(editLabel, Component.translatable(key));
		}
		drawHelp(graphics, help, mouseX, mouseY);
		page.drawPopup(graphics, mouseX, mouseY);
		ConfigScreenResizer.drawResizeHandleHighlight(graphics, layout.area(), handle);
	}

	private @Nullable ConfigInfo drawOptions(GuiGraphics graphics, int mouseX, int mouseY, @Nullable Preview<T> reorderDrag) {
		ImmutableRect2i viewport = layout.editorArea();
		boolean inside = viewport.contains(mouseX, mouseY);
		if (!inside) {
			mouseX = -1;
			mouseY = -1;
		}
		ConfigInfo help = null;
		List<T> selected = model.selection().selected();
		graphics.enableScissor(viewport.getX(), viewport.getY(), viewport.getX() + viewport.getWidth(), viewport.getY() + viewport.getHeight());
		for (int row = 0; row < model.rows().size(); row++) {
			QuickMenuModel.Row<T> option = model.rows().get(row);
			T id = option.id();
			IConfigScreenValue<?> value = option.value();
			if (reorderDrag != null && reorderDrag.active() && reorderDrag.id().equals(id)) {
				continue;
			}
			ImmutableRect2i rowArea = getVisualRowArea(row, reorderDrag);
			if (!rowArea.intersects(viewport)) {
				continue;
			}
			boolean hovered = inside && rowArea.contains(mouseX, mouseY);
			ConfigEntryWidget<?> entry = page.getEntries().get(row);
			ImmutableRect2i original = entry.getArea();
			entry.updateBounds(rowArea);
			try {
				entry.drawSelectionPreview(graphics, mouseX, mouseY, inside && reorderDrag == null, row, selected.contains(id));
				int index = selected.indexOf(id);
				ConfigButtonIcon membershipIcon = ConfigButtonIcon.ADD;
				if (index >= 0) {
					membershipIcon = ConfigButtonIcon.X;
				}
				drawButton(graphics, layout.controlArea(row, 0), membershipIcon, true, mouseX, mouseY);
				if (index >= 0) {
					drawButton(graphics, layout.controlArea(row, 2), ConfigButtonIcon.UP, index > 0, mouseX, mouseY);
					drawButton(graphics, layout.controlArea(row, 1), ConfigButtonIcon.DOWN, index + 1 < selected.size(), mouseX, mouseY);
				}
				if (hovered && reorderDrag == null) {
					help = new ConfigInfo(ConfigValueLocalization.getName(value), ConfigValueLocalization.getDescription(value));
				}
			} finally {
				entry.updateBounds(original);
			}
		}
		if (reorderDrag != null && reorderDrag.active()) {
			drawReorderPreview(graphics, reorderDrag);
		}
		graphics.disableScissor();
		return help;
	}

	private void drawHelp(GuiGraphics graphics, @Nullable ConfigInfo help, int mouseX, int mouseY) {
		var font = Minecraft.getInstance().font;
		ImmutableRect2i infoArea = layout.getInfoArea();
		infoPanel.updateSource(() -> help, font, infoArea.getWidth(), infoArea.contains(mouseX, mouseY));
		layout.requestInfoHeight(infoPanel.getHeight());
		infoPanel.draw(graphics, font, infoArea);
	}

	private ImmutableRect2i getVisualRowArea(int row, @Nullable Preview<T> drag) {
		ImmutableRect2i original = layout.getRowArea(row);
		if (drag == null || !drag.active()) {
			return original;
		}
		int source = model.rowIndex(drag.id());
		int target = drag.targetIndex();
		int offset = 0;
		if (source < target && row > source && row <= target) {
			offset = -layout.getRowArea(source).getHeight();
		} else if (source > target && row >= target && row < source) {
			offset = layout.getRowArea(source).getHeight();
		}
		return new ImmutableRect2i(original.getX(), original.getY() + offset, original.getWidth(), original.getHeight());
	}

	private void drawReorderPreview(GuiGraphics graphics, Preview<T> drag) {
		int source = model.rowIndex(drag.id());
		ImmutableRect2i original = layout.getRowArea(source);
		int target = drag.targetIndex();
		ImmutableRect2i targetArea = layout.getRowArea(target);
		int gapY = targetArea.getY();
		if (source < target) {
			gapY += targetArea.getHeight() - original.getHeight();
		}
		GuiColor gapColor = GuiColor.LIST_ORDERED_ROW_DRAG_GAP;
		if (source != target) {
			gapColor = GuiColor.LIST_ORDERED_ROW_DROP_TARGET;
		}
		ImmutableRect2i gap = new ImmutableRect2i(original.getX(), gapY, original.getWidth(), original.getHeight());
		fill(graphics, gap, gapColor);
		ImmutableRect2i viewport = layout.editorArea();
		int y = ConfigMath.clamp((int) Math.round(drag.mouseY() - drag.grabOffsetY()), viewport.getY(), Math.max(viewport.getY(), viewport.getY() + viewport.getHeight() - original.getHeight()));
		ImmutableRect2i floating = new ImmutableRect2i(original.getX(), y, original.getWidth(), original.getHeight());
		ConfigEntryWidget<?> entry = page.getEntries().get(source);
		entry.updateBounds(floating);
		fill(graphics, floating, GuiColor.LIST_ORDERED_ROW_DRAG_FLOAT_BACKGROUND);
		entry.drawSelectionPreview(graphics, drag.mouseX(), drag.mouseY(), false, -1, true);
		drawButton(graphics, layout.controlArea(source, 0), ConfigButtonIcon.X, true, -1, -1);
		drawButton(graphics, layout.controlArea(source, 2), ConfigButtonIcon.UP, target > 0, -1, -1);
		drawButton(graphics, layout.controlArea(source, 1), ConfigButtonIcon.DOWN, target + 1 < drag.selectionSize(), -1, -1);
		entry.updateBounds(original);
	}

	private static void drawButton(GuiGraphics graphics, ImmutableRect2i area, ConfigButtonIcon icon, boolean active, int x, int y) {
		ConfigEntryWidget.drawButtonBackground(graphics, ConfigTextures.get(), area, active, active && area.contains(x, y));
		graphics.flush();
		icon.draw(graphics, area, active);
	}

	private static void text(GuiGraphics graphics, Component text, ImmutableRect2i area, GuiColor color) {
		var font = Minecraft.getInstance().font;
		String visible = font.plainSubstrByWidth(text.getString(), area.getWidth());
		graphics.drawString(font, visible, area.getX(), area.getY() + (area.getHeight() - font.lineHeight) / 2, color(color), false);
	}

	private static int color(GuiColor color) {
		return ConfigGuiColors.getColor(color);
	}

	private static void fill(GuiGraphics graphics, ImmutableRect2i area, GuiColor color) {
		graphics.fill(area.getX(), area.getY(), area.getX() + area.getWidth(), area.getY() + area.getHeight(), color(color));
	}

	private static void drawInsetBorder(GuiGraphics graphics, ImmutableRect2i area) {
		int x = area.getX(), y = area.getY(), right = x + area.getWidth(), bottom = y + area.getHeight();
		int dark = color(GuiColor.CONFIG_SCREEN_INSET_BORDER_DARK), light = color(GuiColor.CONFIG_SCREEN_INSET_BORDER_LIGHT);
		graphics.fill(x, y, right, y + 1, dark);
		graphics.fill(x, y, x + 1, bottom, dark);
		graphics.fill(x, bottom - 1, right, bottom, light);
		graphics.fill(right - 1, y, right, bottom, light);
	}
}
