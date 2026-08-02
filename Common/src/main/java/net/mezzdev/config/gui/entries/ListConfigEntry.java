package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.gui.ConfigInputHandler;
import net.mezzdev.config.gui.ConfigInputUtil;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigListValueEditorOptions;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.info.ConfigValueIcon;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.model.PendingConfigChange;
import net.mezzdev.config.gui.textures.ConfigDrawableStatic;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Config entry widget for ordered list values with add, remove, and reorder controls.
 */
final class ListConfigEntry<T> extends ConfigEntryWidget<List<T>> {

	private static final int ENTRY_ROW_HEIGHT = 20;
	private static final int BUTTON_SIZE = 18;
	private static final int BUTTON_GAP = 2;
	private static final int ROW_BUTTON_COUNT = 3;
	private static final int ROW_MOVE_BUTTON_COUNT = 2;
	private static final int ROW_BUTTONS_WIDTH = BUTTON_SIZE * ROW_BUTTON_COUNT + BUTTON_GAP * (ROW_BUTTON_COUNT - 1);
	private static final int ROW_MOVE_BUTTONS_WIDTH = BUTTON_SIZE * ROW_MOVE_BUTTON_COUNT + BUTTON_GAP * (ROW_MOVE_BUTTON_COUNT - 1);
	private static final int ARROW_ICON_SIZE = 9;
	private static final int VALUE_GROUP_TOP_GAP = 3;
	private static final int VALUE_GROUP_BOTTOM_PADDING = 3;
	private static final int VALUE_GROUP_BORDER_SIZE = 1;
	private static final int UNUSED_VALUE_TOP_GAP = 1;
	private static final int VALUE_ROW_HORIZONTAL_PADDING = 4;
	private static final int ORDERED_ROW_NUMBER_WIDTH = 18;
	private static final int ORDERED_ROW_DRAG_FLOAT_Z_OFFSET = 200;
	private static final int ORDERED_GROUP_BACKGROUND_COLOR = 0x22000000;
	private static final int ORDERED_GROUP_BORDER_DARK_COLOR = 0x90000000;
	private static final int ORDERED_GROUP_BORDER_LIGHT_COLOR = 0x24FFFFFF;
	private static final int ORDERED_ROW_BACKGROUND_COLOR = 0x1E000000;
	private static final int ORDERED_ROW_DRAG_GAP_COLOR = 0x28000000;
	private static final int ORDERED_ROW_DRAG_FLOAT_BACKGROUND_COLOR = 0xFF404A59;
	private static final int ORDERED_ROW_DRAG_FLOAT_SHADOW_COLOR = 0x70000000;
	private static final int ORDERED_ROW_DRAG_FLOAT_BORDER_COLOR = 0xD0D7E6FF;
	private static final int ORDERED_ROW_DRAG_FLOAT_ACCENT_COLOR = 0xFFEAF2FF;
	private static final int ORDERED_ROW_DIVIDER_COLOR = 0x18FFFFFF;
	private static final int UNUSED_ROW_BACKGROUND_COLOR = 0x30000000;
	private static final int UNUSED_ROW_TEXT_COLOR = 0xFF707070;

	private final List<ListValueRow> valueRows = new ArrayList<>();
	private final List<ListValueRow> unusedValueRows = new ArrayList<>();
	private final List<T> allValidValues;
	private final IConfigValueSerializer<T> elementSerializer;
	private final Runnable layoutUpdater;
	private final boolean allowsRemovingValues;
	private ImmutableRect2i valueGroupArea = ImmutableRect2i.EMPTY;
	@Nullable
	private DragSession dragSession;

	ListConfigEntry(
		IConfigScreenValue<List<T>> listValue,
		IConfigListValueEditorSerializer<T> listSerializer,
		Runnable layoutUpdater,
		ConfigTextures textures
	) {
		super(listValue, textures);
		this.layoutUpdater = layoutUpdater;
		this.elementSerializer = listSerializer.getElementSerializer();
		this.allowsRemovingValues = !(listSerializer instanceof IConfigListValueEditorOptions editorOptions) ||
			editorOptions.allowsRemovingValues();
		this.allValidValues = elementSerializer.getAllValidValues()
			.map(List::copyOf)
			.orElse(List.of());
		rebuildRows();
	}

	private void rebuildRows() {
		valueRows.clear();
		unusedValueRows.clear();
		List<T> displayedValues = getValue();
		for (int i = 0; i < displayedValues.size(); i++) {
			valueRows.add(new ListValueRow(displayedValues.get(i), i, true));
		}
		if (shouldShowUnusedValues()) {
			List<T> current = getValue();
			for (T value : allValidValues) {
				if (!current.contains(value)) {
					unusedValueRows.add(new ListValueRow(value, unusedValueRows.size(), false));
				}
			}
		}
	}

	private boolean shouldShowUnusedValues() {
		return !allValidValues.isEmpty();
	}

	@Override
	public int getHeight() {
		if (valueRows.isEmpty() && unusedValueRows.isEmpty()) {
			return super.getHeight();
		}
		int height = super.getHeight() + VALUE_GROUP_TOP_GAP;
		height += valueRows.size() * ENTRY_ROW_HEIGHT;
		height += getUnusedValueTopGap();
		height += unusedValueRows.size() * ENTRY_ROW_HEIGHT;
		height += VALUE_GROUP_BOTTOM_PADDING;
		return height;
	}

	@Override
	public void updateBounds(ImmutableRect2i area) {
		super.updateBounds(new ImmutableRect2i(area.getX(), area.getY(), area.getWidth(), Math.max(getMinimumHeight(), area.getHeight())));
		int headerHeight = super.getHeight();
		super.updateBounds(new ImmutableRect2i(area.getX(), area.getY(), area.getWidth(), headerHeight));
		this.area = area;

		int y = area.getY() + headerHeight + VALUE_GROUP_TOP_GAP;
		int rowCount = valueRows.size() + unusedValueRows.size();
		int unusedValueTopGap = getUnusedValueTopGap();
		if (rowCount > 0) {
			valueGroupArea = new ImmutableRect2i(
				area.getX() + 4,
				y - 1,
				Math.max(0, area.getWidth() - 8),
				rowCount * ENTRY_ROW_HEIGHT + unusedValueTopGap + VALUE_GROUP_BORDER_SIZE * 2
			);
		} else {
			valueGroupArea = ImmutableRect2i.EMPTY;
		}
		for (ListValueRow row : valueRows) {
			row.updateBounds(new ImmutableRect2i(
				valueGroupArea.getX() + VALUE_GROUP_BORDER_SIZE,
				y,
				Math.max(0, valueGroupArea.getWidth() - VALUE_GROUP_BORDER_SIZE * 2),
				ENTRY_ROW_HEIGHT
			));
			y += ENTRY_ROW_HEIGHT;
		}
		y += unusedValueTopGap;
		for (ListValueRow row : unusedValueRows) {
			row.updateBounds(new ImmutableRect2i(
				valueGroupArea.getX() + VALUE_GROUP_BORDER_SIZE,
				y,
				Math.max(0, valueGroupArea.getWidth() - VALUE_GROUP_BORDER_SIZE * 2),
				ENTRY_ROW_HEIGHT
			));
			y += ENTRY_ROW_HEIGHT;
		}
	}

	private int getUnusedValueTopGap() {
		if (!valueRows.isEmpty() && !unusedValueRows.isEmpty()) {
			return UNUSED_VALUE_TOP_GAP;
		}
		return 0;
	}

	@Override
	protected void drawContent(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		drawName(guiGraphics);

		drawValueGroup(guiGraphics, valueGroupArea);
		@Nullable
		DragSession dragSession = this.dragSession;
		for (ListValueRow row : valueRows) {
			if (dragSession != null && dragSession.isDragging(row)) {
				row.drawDragGap(guiGraphics);
			} else {
				row.draw(guiGraphics, mouseX, mouseY);
			}
		}
		for (ListValueRow row : unusedValueRows) {
			row.draw(guiGraphics, mouseX, mouseY);
		}
		if (dragSession != null) {
			guiGraphics.flush();
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0, 0, ORDERED_ROW_DRAG_FLOAT_Z_OFFSET);
			dragSession.drawFloatingRow(guiGraphics);
			guiGraphics.pose().popPose();
			guiGraphics.flush();
		}
	}

	private void drawValueGroup(GuiGraphics guiGraphics, ImmutableRect2i groupArea) {
		if (groupArea.isEmpty()) {
			return;
		}

		int x = groupArea.getX();
		int y = groupArea.getY();
		int right = x + groupArea.getWidth();
		int bottom = y + groupArea.getHeight();

		guiGraphics.fill(x, y, right, bottom, ORDERED_GROUP_BACKGROUND_COLOR);
		guiGraphics.fill(x, y, right, y + 1, ORDERED_GROUP_BORDER_DARK_COLOR);
		guiGraphics.fill(x, y, x + 1, bottom, ORDERED_GROUP_BORDER_DARK_COLOR);
		guiGraphics.fill(right - 1, y, right, bottom, ORDERED_GROUP_BORDER_LIGHT_COLOR);
		guiGraphics.fill(x, bottom - 1, right, bottom, ORDERED_GROUP_BORDER_LIGHT_COLOR);
	}

	@Override
	@Nullable
	public ConfigInfo getTooltipInfo(double mouseX, double mouseY) {
		@Nullable
		ConfigInfo resetInfo = super.getTooltipInfo(mouseX, mouseY);
		if (resetInfo != null) {
			return resetInfo;
		}
		if (dragSession != null) {
			return null;
		}
		for (ListValueRow row : valueRows) {
			if (row.moveUpArea.contains(mouseX, mouseY)) {
				return new ConfigInfo(
					Component.translatable("mezz_config.config.screen.moveUp"),
					List.of()
				);
			}
			if (row.moveDownArea.contains(mouseX, mouseY)) {
				return new ConfigInfo(
					Component.translatable("mezz_config.config.screen.moveDown"),
					List.of()
				);
			}
			if (row.deleteArea.contains(mouseX, mouseY)) {
				return new ConfigInfo(
					Component.translatable("mezz_config.config.screen.remove"),
					List.of()
				);
			}
		}
		for (ListValueRow row : unusedValueRows) {
			if (row.addArea.contains(mouseX, mouseY)) {
				return new ConfigInfo(
					Component.translatable("mezz_config.config.screen.add"),
					List.of()
				);
			}
		}
		return null;
	}

	@Override
	public ConfigInfo getInfo() {
		return ConfigValueInfoFactory.createSharedInfo(configValue, hasPendingChange());
	}

	@Override
	public Optional<PendingConfigChange> getPendingConfigChange() {
		if (!hasPendingChange()) {
			return Optional.empty();
		}
		List<T> oldValue = configValue.getValue();
		List<T> newValue = getValue();
		Component valueChange = getListChangeSummary(oldValue, newValue);
		return Optional.of(PendingConfigChange.createSummary(
			ConfigValueLocalization.getName(configValue),
			valueChange,
			getInfo()
		));
	}

	@Override
	public ConfigInputHandler createInputHandler() {
		return new ListEntryInputHandler();
	}

	@Override
	protected void onValueChanged() {
		rebuildRows();
		layoutUpdater.run();
	}

	private void addValue(T value) {
		List<T> current = new ArrayList<>(getValue());
		current.add(value);
		setValue(current);
	}

	private void moveValue(int index, int offset) {
		List<T> current = new ArrayList<>(getValue());
		int newIndex = index + offset;
		if (index >= 0 && index < current.size() && newIndex >= 0 && newIndex < current.size()) {
			Collections.swap(current, index, newIndex);
			setValue(current);
		}
	}

	private boolean moveValueToIndex(int sourceIndex, int targetIndex) {
		List<T> current = new ArrayList<>(getValue());
		if (!isIndexValid(current, sourceIndex) || !isIndexValid(current, targetIndex) || sourceIndex == targetIndex) {
			return false;
		}
		T value = current.remove(sourceIndex);
		current.add(targetIndex, value);
		return setValue(current);
	}

	private static boolean isIndexValid(List<?> values, int index) {
		return index >= 0 && index < values.size();
	}

	private Component getListChangeSummary(List<T> oldValue, List<T> newValue) {
		return findSingleMove(oldValue, newValue)
			.<Component>map(move -> Component.translatable(
				"mezz_config.config.screen.pendingChanges.list.move",
				ConfigValueLocalization.getValueName(elementSerializer, configValue.getLocalizationKey(), move.value()),
				move.oldIndex() + 1,
				move.newIndex() + 1
			))
			.orElseGet(() -> createListChangeSummary(oldValue, newValue));
	}

	private Component createListChangeSummary(List<T> oldValue, List<T> newValue) {
		if (oldValue.size() != newValue.size()) {
			return Component.translatable(
				"mezz_config.config.screen.pendingChanges.list.sizeChanged",
				oldValue.size(),
				newValue.size()
			);
		}
		return Component.translatable(
			"mezz_config.config.screen.pendingChanges.list.changed",
			countChangedPositions(oldValue, newValue)
		);
	}

	private static <T> Optional<ListMove<T>> findSingleMove(List<T> oldValue, List<T> newValue) {
		if (oldValue.size() != newValue.size()) {
			return Optional.empty();
		}
		int firstChanged = getFirstChangedIndex(oldValue, newValue);
		if (firstChanged < 0) {
			return Optional.empty();
		}
		int lastChanged = getLastChangedIndex(oldValue, newValue);

		T movedDownValue = oldValue.get(firstChanged);
		if (movedDownValue.equals(newValue.get(lastChanged)) && isShiftedUp(oldValue, newValue, firstChanged, lastChanged)) {
			return Optional.of(new ListMove<>(movedDownValue, firstChanged, lastChanged));
		}

		T movedUpValue = oldValue.get(lastChanged);
		if (movedUpValue.equals(newValue.get(firstChanged)) && isShiftedDown(oldValue, newValue, firstChanged, lastChanged)) {
			return Optional.of(new ListMove<>(movedUpValue, lastChanged, firstChanged));
		}

		return Optional.empty();
	}

	private static <T> int getFirstChangedIndex(List<T> oldValue, List<T> newValue) {
		for (int i = 0; i < oldValue.size(); i++) {
			if (!oldValue.get(i).equals(newValue.get(i))) {
				return i;
			}
		}
		return -1;
	}

	private static <T> int getLastChangedIndex(List<T> oldValue, List<T> newValue) {
		for (int i = oldValue.size() - 1; i >= 0; i--) {
			if (!oldValue.get(i).equals(newValue.get(i))) {
				return i;
			}
		}
		return -1;
	}

	private static <T> boolean isShiftedUp(List<T> oldValue, List<T> newValue, int firstChanged, int lastChanged) {
		for (int i = firstChanged; i < lastChanged; i++) {
			if (!oldValue.get(i + 1).equals(newValue.get(i))) {
				return false;
			}
		}
		return true;
	}

	private static <T> boolean isShiftedDown(List<T> oldValue, List<T> newValue, int firstChanged, int lastChanged) {
		for (int i = firstChanged + 1; i <= lastChanged; i++) {
			if (!oldValue.get(i - 1).equals(newValue.get(i))) {
				return false;
			}
		}
		return true;
	}

	private static <T> int countChangedPositions(List<T> oldValue, List<T> newValue) {
		int count = 0;
		for (int i = 0; i < oldValue.size(); i++) {
			if (!oldValue.get(i).equals(newValue.get(i))) {
				count++;
			}
		}
		return count;
	}

	private void removeValue(int index) {
		List<T> current = new ArrayList<>(getValue());
		if (index >= 0 && index < current.size()) {
			current.remove(index);
			setValue(current);
		}
	}

	private class ListEntryInputHandler implements ConfigInputHandler {
		@Override
		public Optional<ConfigInputHandler> handleUserInput(Screen screen, UserInput input) {
			if (!area.contains(input.getMouseX(), input.getMouseY())) {
				return Optional.empty();
			}
			if (!ConfigInputUtil.isLeftClick(input)) {
				return Optional.empty();
			}

			if (onMouseClicked(input)) {
				return Optional.of(this);
			}

			for (ListValueRow row : valueRows) {
				if (row.moveUpArea.contains(input.getMouseX(), input.getMouseY())) {
					if (!row.canMoveUp()) {
						return Optional.empty();
					}
					if (!input.isSimulate()) {
						moveValue(row.index, -1);
					}
					return Optional.of(this);
				}
				if (row.moveDownArea.contains(input.getMouseX(), input.getMouseY())) {
					if (!row.canMoveDown()) {
						return Optional.empty();
					}
					if (!input.isSimulate()) {
						moveValue(row.index, 1);
					}
					return Optional.of(this);
				}
				if (row.deleteArea.contains(input.getMouseX(), input.getMouseY())) {
					if (!input.isSimulate()) {
						removeValue(row.index);
					}
					return Optional.of(this);
				}
			}
			for (ListValueRow row : valueRows) {
				if (row.canStartDrag(input.getMouseX(), input.getMouseY())) {
					DragSession session = new DragSession(row, input.getMouseY(), !input.isSimulate());
					return Optional.of(session);
				}
			}
			for (ListValueRow row : unusedValueRows) {
				if (row.addArea.contains(input.getMouseX(), input.getMouseY())) {
					if (!input.isSimulate()) {
						addValue(row.value);
					}
					return Optional.of(this);
				}
			}

			return Optional.empty();
		}

		@Override
		public void unfocus() {
			dragSession = null;
		}
	}

	private class DragSession implements ConfigInputHandler {
		private int index;
		private final List<Integer> displayIndexes = new ArrayList<>();
		private final double grabOffsetY;
		private double mouseY;
		private boolean active;

		public DragSession(ListValueRow row, double mouseY, boolean active) {
			this.index = row.index;
			this.grabOffsetY = mouseY - row.area.getY();
			this.mouseY = mouseY;
			for (ListValueRow valueRow : valueRows) {
				displayIndexes.add(valueRow.index);
			}
			if (active) {
				start();
			}
		}

		@Override
		public Optional<ConfigInputHandler> handleUserInput(Screen screen, UserInput input) {
			if (active && ConfigInputUtil.isLeftClick(input)) {
				moveToMouseY(input.getMouseY());
			}
			stop();
			return Optional.empty();
		}

		@Override
		public Optional<ConfigInputHandler> handleMouseDragged(
			Screen screen,
			double mouseX,
			double mouseY,
			int button,
			double dragX,
			double dragY
		) {
			if (button != 0) {
				stop();
				return Optional.empty();
			}
			start();
			moveToMouseY(mouseY);
			return Optional.of(this);
		}

		@Override
		public void unfocus() {
			stop();
		}

		private void start() {
			active = true;
			dragSession = this;
		}

		private void stop() {
			active = false;
			if (dragSession == this) {
				dragSession = null;
			}
		}

		private void moveToMouseY(double mouseY) {
			this.mouseY = mouseY;
			int targetIndex = getDragTargetIndex(mouseY);
			if (moveValueToIndex(index, targetIndex)) {
				moveDisplayIndex(index, targetIndex);
				index = targetIndex;
			}
		}

		private void moveDisplayIndex(int sourceIndex, int targetIndex) {
			if (!isIndexValid(displayIndexes, sourceIndex) || !isIndexValid(displayIndexes, targetIndex)) {
				return;
			}
			int displayIndex = displayIndexes.remove(sourceIndex);
			displayIndexes.add(targetIndex, displayIndex);
		}

		private int getDragTargetIndex(double mouseY) {
			if (valueRows.isEmpty()) {
				return index;
			}
			for (ListValueRow row : valueRows) {
				int rowBottom = row.area.getY() + row.area.getHeight();
				if (mouseY < rowBottom) {
					return row.index;
				}
			}
			return valueRows.size() - 1;
		}

		public boolean isDragging(ListValueRow row) {
			return active && row.selected && row.index == index;
		}

		public int getDisplayIndex(ListValueRow row) {
			if (isIndexValid(displayIndexes, row.index)) {
				return displayIndexes.get(row.index);
			}
			return row.index;
		}

		public void drawFloatingRow(GuiGraphics guiGraphics) {
			@Nullable
			ListValueRow row = getDraggingRow();
			if (row != null) {
				row.drawFloating(guiGraphics, getFloatingRowY());
			}
		}

		private int getFloatingRowY() {
			int y = (int) Math.round(mouseY - grabOffsetY);
			if (valueRows.isEmpty()) {
				return y;
			}
			int minY = valueRows.get(0).area.getY();
			int maxY = valueRows.get(valueRows.size() - 1).area.getY();
			return Math.clamp(y, minY, maxY);
		}

		@Nullable
		private ListValueRow getDraggingRow() {
			if (isIndexValid(valueRows, index)) {
				return valueRows.get(index);
			}
			return null;
		}
	}

	private class ListValueRow {
		final T value;
		int index;
		private final boolean selected;
		ImmutableRect2i area = ImmutableRect2i.EMPTY;
		ImmutableRect2i moveUpArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i moveDownArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i deleteArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i addArea = ImmutableRect2i.EMPTY;

		ListValueRow(T value, int index, boolean selected) {
			this.value = value;
			this.index = index;
			this.selected = selected;
		}

		void updateBounds(ImmutableRect2i area) {
			this.area = area;
			int cy = area.getY() + (area.getHeight() - BUTTON_SIZE) / 2;
			addArea = ImmutableRect2i.EMPTY;
			if (selected) {
				deleteArea = ImmutableRect2i.EMPTY;
				if (allowsRemovingValues) {
					deleteArea = createButtonArea(area, cy, 0);
				}
			} else {
				deleteArea = ImmutableRect2i.EMPTY;
				addArea = createButtonArea(area, cy, 0);
			}
			if (selected) {
				int moveButtonOffset = getMoveButtonOffset();
				moveDownArea = createButtonArea(area, cy, moveButtonOffset);
				moveUpArea = createButtonArea(area, cy, moveButtonOffset + 1);
			} else {
				moveDownArea = ImmutableRect2i.EMPTY;
				moveUpArea = ImmutableRect2i.EMPTY;
			}
		}

		private int getMoveButtonOffset() {
			if (allowsRemovingValues) {
				return 1;
			}
			return 0;
		}

		private ImmutableRect2i createButtonArea(ImmutableRect2i area, int y, int buttonsFromRight) {
			int xOffset = (BUTTON_SIZE + BUTTON_GAP) * buttonsFromRight;
			return new ImmutableRect2i(
				area.getX() + area.getWidth() - BUTTON_SIZE - xOffset,
				y,
				BUTTON_SIZE,
				BUTTON_SIZE
			);
		}

		boolean canMoveUp() {
			return selected && index > 0;
		}

		boolean canMoveDown() {
			return selected && index < valueRows.size() - 1;
		}

		boolean isControlMouseOver(double mouseX, double mouseY) {
			return moveUpArea.contains(mouseX, mouseY) ||
				moveDownArea.contains(mouseX, mouseY) ||
				deleteArea.contains(mouseX, mouseY) ||
				addArea.contains(mouseX, mouseY);
		}

		boolean canStartDrag(double mouseX, double mouseY) {
			return selected &&
				area.contains(mouseX, mouseY) &&
				!isControlMouseOver(mouseX, mouseY);
		}

		private int getControlsWidth() {
			if (selected) {
				if (allowsRemovingValues) {
					return ROW_BUTTONS_WIDTH;
				}
				return ROW_MOVE_BUTTONS_WIDTH;
			}
			return BUTTON_SIZE;
		}

		void draw(GuiGraphics guiGraphics, double mouseX, double mouseY) {
			draw(guiGraphics, area, mouseX, mouseY, true, false);
		}

		void drawDragGap(GuiGraphics guiGraphics) {
			int x = area.getX();
			int y = area.getY();
			int right = x + area.getWidth();
			int bottom = y + area.getHeight();
			guiGraphics.fill(x, y, right, bottom, ORDERED_ROW_DRAG_GAP_COLOR);
			if (index > 0) {
				guiGraphics.fill(x, y, right, y + 1, ORDERED_ROW_DIVIDER_COLOR);
			}
		}

		void drawFloating(GuiGraphics guiGraphics, int y) {
			ImmutableRect2i floatingArea = new ImmutableRect2i(area.getX(), y, area.getWidth(), area.getHeight());
			int x = floatingArea.getX();
			int bottom = floatingArea.getY() + floatingArea.getHeight();
			guiGraphics.fill(x, bottom, x + floatingArea.getWidth(), bottom + 2, ORDERED_ROW_DRAG_FLOAT_SHADOW_COLOR);
			draw(guiGraphics, floatingArea, 0, 0, false, true);
		}

		private void draw(
			GuiGraphics guiGraphics,
			ImmutableRect2i rowArea,
			double mouseX,
			double mouseY,
			boolean drawControls,
			boolean floating
		) {
			Font font = Minecraft.getInstance().font;
			ConfigTextures textures = getTextures();

			int backgroundColor = getBackgroundColor(floating);
			guiGraphics.fill(rowArea.getX(), rowArea.getY(),
				rowArea.getX() + rowArea.getWidth(), rowArea.getY() + rowArea.getHeight(),
				backgroundColor);
			if (index > 0) {
				guiGraphics.fill(rowArea.getX(), rowArea.getY(), rowArea.getX() + rowArea.getWidth(), rowArea.getY() + 1, ORDERED_ROW_DIVIDER_COLOR);
			}
			if (floating) {
				drawFloatingHighlight(guiGraphics, rowArea);
			}
			if (selected) {
				drawOrderNumber(guiGraphics, font, rowArea);
			}

			Component valueName = ConfigValueLocalization.getValueName(elementSerializer, configValue.getLocalizationKey(), value);
			int textX = rowArea.getX() + VALUE_ROW_HORIZONTAL_PADDING + ORDERED_ROW_NUMBER_WIDTH;
			int iconY = rowArea.getY() + (rowArea.getHeight() - ConfigValueIcon.ICON_SIZE) / 2;
			ConfigValueIcon.draw(guiGraphics, elementSerializer, value, textX, iconY);
			textX += ConfigValueIcon.getTextOffset(elementSerializer, value);
			ImmutableRect2i textArea = new ImmutableRect2i(
				textX,
				rowArea.getY(),
				Math.max(0, rowArea.getX() + rowArea.getWidth() - textX - getControlsWidth() - 6),
				rowArea.getHeight()
			);
			int textColor = TEXT_COLOR;
			if (!selected) {
				textColor = UNUSED_ROW_TEXT_COLOR;
			}
			ConfigEntryWidget.drawFittedText(guiGraphics, font, valueName, textArea, textColor, false);

			if (drawControls) {
				if (selected) {
					drawMoveButton(guiGraphics, textures, moveUpArea, textures.getArrowUp(), canMoveUp(), mouseX, mouseY);
					drawMoveButton(guiGraphics, textures, moveDownArea, textures.getArrowDown(), canMoveDown(), mouseX, mouseY);
					if (allowsRemovingValues) {
						drawDeleteButton(guiGraphics, textures, font, mouseX, mouseY);
					}
				} else {
					drawAddButton(guiGraphics, textures, font, mouseX, mouseY);
				}
			}
		}

		private int getBackgroundColor(boolean floating) {
			if (floating) {
				return ORDERED_ROW_DRAG_FLOAT_BACKGROUND_COLOR;
			}
			if (selected) {
				return ORDERED_ROW_BACKGROUND_COLOR;
			}
			return UNUSED_ROW_BACKGROUND_COLOR;
		}

		private void drawFloatingHighlight(GuiGraphics guiGraphics, ImmutableRect2i rowArea) {
			int x = rowArea.getX();
			int y = rowArea.getY();
			int right = x + rowArea.getWidth();
			int bottom = y + rowArea.getHeight();
			guiGraphics.fill(x, y, right, y + 1, ORDERED_ROW_DRAG_FLOAT_BORDER_COLOR);
			guiGraphics.fill(x, bottom - 1, right, bottom, ORDERED_ROW_DRAG_FLOAT_BORDER_COLOR);
			guiGraphics.fill(x, y, x + 2, bottom, ORDERED_ROW_DRAG_FLOAT_ACCENT_COLOR);
			guiGraphics.fill(right - 1, y, right, bottom, ORDERED_ROW_DRAG_FLOAT_BORDER_COLOR);
		}

		private void drawOrderNumber(GuiGraphics guiGraphics, Font font, ImmutableRect2i rowArea) {
			String number = Integer.toString(getDisplayIndex() + 1);
			int numberWidth = font.width(number);
			int numberX = rowArea.getX() + VALUE_ROW_HORIZONTAL_PADDING + Math.max(0, (ORDERED_ROW_NUMBER_WIDTH - numberWidth) / 2);
			int numberY = ConfigEntryWidget.getCenteredTextY(font, rowArea);
			ConfigEntryWidget.drawText(guiGraphics, font, number, numberX, numberY, SECONDARY_TEXT_COLOR);
		}

		private int getDisplayIndex() {
			@Nullable
			DragSession dragSession = ListConfigEntry.this.dragSession;
			if (dragSession != null) {
				return dragSession.getDisplayIndex(this);
			}
			return index;
		}

		private void drawMoveButton(
			GuiGraphics guiGraphics,
			ConfigTextures textures,
			ImmutableRect2i buttonArea,
			ConfigDrawableStatic icon,
			boolean active,
			double mouseX,
			double mouseY
		) {
			boolean hovered = active && buttonArea.contains(mouseX, mouseY);
			ConfigEntryWidget.drawButtonBackground(guiGraphics, textures, buttonArea, active, hovered);
			int iconX = buttonArea.getX() + (buttonArea.getWidth() - ARROW_ICON_SIZE) / 2;
			int iconY = buttonArea.getY() + (buttonArea.getHeight() - ARROW_ICON_SIZE) / 2;
			if (active) {
				icon.draw(guiGraphics, iconX, iconY);
				return;
			}

			guiGraphics.pose().pushPose();
			guiGraphics.setColor(0.3f, 0.3f, 0.3f, 0.5f);
			icon.draw(guiGraphics, iconX, iconY);
			guiGraphics.setColor(1f, 1f, 1f, 1f);
			guiGraphics.pose().popPose();
		}

		private void drawDeleteButton(GuiGraphics guiGraphics, ConfigTextures textures, Font font, double mouseX, double mouseY) {
			boolean deleteHovered = deleteArea.contains(mouseX, mouseY);
			ConfigEntryWidget.drawButtonBackground(guiGraphics, textures, deleteArea, true, deleteHovered);
			ConfigEntryWidget.drawCenteredButtonText(guiGraphics, font, "x", deleteArea, getControlTextColor(deleteHovered));
		}

		private void drawAddButton(GuiGraphics guiGraphics, ConfigTextures textures, Font font, double mouseX, double mouseY) {
			boolean addHovered = addArea.contains(mouseX, mouseY);
			ConfigEntryWidget.drawButtonBackground(guiGraphics, textures, addArea, true, addHovered);
			ConfigEntryWidget.drawCenteredButtonText(guiGraphics, font, "+", addArea, getControlTextColor(addHovered));
		}

		private static int getControlTextColor(boolean hovered) {
			if (hovered) {
				return HOVER_TEXT_COLOR;
			}
			return TEXT_COLOR;
		}
	}

	private record ListMove<T>(T value, int oldIndex, int newIndex) {
	}
}
