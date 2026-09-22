package net.mezzdev.config.gui.entries;

import net.mezzdev.config.gui.ConfigRenderUtil;

import net.mezzdev.config.gui.util.ConfigMath;

import net.mezzdev.config.gui.info.ConfigNumberInfo;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.ConfigListOrdering;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.api.value.color.PackedColor;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.ConfigInputHandler;
import net.mezzdev.config.gui.ConfigInputUtil;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigListValueEditorOptions;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValuePopup;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.info.ColorSwatch;
import net.mezzdev.config.gui.info.ConfigValueIcon;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.model.PendingConfigChange;
import net.mezzdev.config.gui.popup.ColorPickerPopup;
import net.mezzdev.config.gui.popup.ConfigPopupSelector;
import net.mezzdev.config.gui.popup.ConfigValuePopupSelector;
import net.mezzdev.config.gui.popup.ConfigValueSelector;
import net.mezzdev.config.gui.popup.MappedConfigValuePopup;
import net.mezzdev.config.gui.textures.ConfigButtonIcon;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.util.HexColorString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Config entry widget for list values with add, remove, and optional reorder controls.
 */
final class ListConfigEntry<T> extends ConfigEntryWidget<List<T>> {
	private static final Logger LOGGER = LogManager.getLogger();

	private static final int BUTTON_SIZE = 20;
	private static final int BUTTON_GAP = 2;
	private static final int VALUE_GROUP_TOP_GAP = 3;
	private static final int VALUE_GROUP_BOTTOM_PADDING = 3;
	private static final int VALUE_GROUP_BORDER_SIZE = 1;
	private static final int UNUSED_VALUE_TOP_GAP = 1;
	private static final int ADD_VALUE_TOP_GAP = 2;
	private static final int VALUE_ROW_HORIZONTAL_PADDING = 4;
	private static final int ADD_VALUE_TEXT_PADDING = 4;
	private static final int KEY_VALUE_COLUMN_GAP = 4;
	private static final int KEY_VALUE_TEXT_PADDING = 3;
	private static final int COLOR_COMPONENT_SWATCH_SIZE = 18;
	private static final int COLOR_COMPONENT_GAP = 3;
	private static final int MAX_ADD_VALUE_TEXT_LENGTH = 512;
	private static final int ORDERED_ROW_DRAG_FLOAT_Z_OFFSET = 200;
	private static final int ORDERED_ROW_MAX_HORIZONTAL_DRAG_OFFSET = 24;
	private final List<ListValueRow> valueRows = new ArrayList<>();
	private final List<ListValueRow> unusedValueRows = new ArrayList<>();
	private final List<T> allValidValues;
	private final IConfigValueSerializer<T> elementSerializer;
	@Nullable
	private final KeyValueElementSerializerAdapter<T> keyValueSerializer;
	private final Consumer<ConfigPopupSelector> valueSelectorOpener;
	private final Runnable layoutUpdater;
	private final boolean ordered;
	private final boolean allowsRemovingValues;
	private final boolean allowsTypedInput;
	private ImmutableRect2i headerArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i valueGroupArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i addValueRowArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i addValueTextArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i addValueButtonArea = ImmutableRect2i.EMPTY;
	@Nullable
	private DragSession dragSession;
	@Nullable
	private T recentlyMovedValue;
	private int recentlyMovedIndex = -1;
	private boolean addingValue;
	private String addValueText = "";
	@Nullable
	private ComponentEditSession componentEditSession;

	ListConfigEntry(
		IConfigScreenValue<List<T>> listValue,
		IConfigListValueEditorSerializer<T> listSerializer,
		Consumer<ConfigPopupSelector> valueSelectorOpener,
		Runnable layoutUpdater,
		ConfigTextures textures
	) {
		super(listValue, textures);
		this.valueSelectorOpener = valueSelectorOpener;
		this.layoutUpdater = layoutUpdater;
		this.elementSerializer = listSerializer.getElementSerializer();
		this.keyValueSerializer = KeyValueElementSerializerAdapter.create(elementSerializer).orElse(null);
		this.ordered = listSerializer.getOrdering() == ConfigListOrdering.ORDERED;
		boolean configuredAllowsRemovingValues = !(listSerializer instanceof IConfigListValueEditorOptions editorOptions) ||
			editorOptions.allowsRemovingValues();
		this.allowsRemovingValues = configuredAllowsRemovingValues || keyValueSerializer != null;
		Optional<List<T>> allValidValues = elementSerializer.getAllValidValues();
		this.allowsTypedInput = allowsRemovingValues && allValidValues.isEmpty();
		this.allValidValues = allValidValues
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
			Set<T> current = new HashSet<>(displayedValues);
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
		if (valueRows.isEmpty() && unusedValueRows.isEmpty() && !allowsTypedInput) {
			return super.getHeight();
		}
		return super.getHeight() + VALUE_GROUP_TOP_GAP + getValueGroupContentHeight() + VALUE_GROUP_BOTTOM_PADDING;
	}

	private int getValueGroupContentHeight() {
		int entryRowHeight = getEntryRowHeight();
		int height = valueRows.size() * entryRowHeight;
		height += getUnusedValueTopGap();
		height += unusedValueRows.size() * entryRowHeight;
		if (allowsTypedInput) {
			height += getAddValueTopGap();
			height += entryRowHeight;
		}
		return height;
	}

	@Override
	public void updateBounds(ImmutableRect2i area) {
		super.updateBounds(new ImmutableRect2i(area.getX(), area.getY(), area.getWidth(), Math.max(getMinimumHeight(), area.getHeight())));
		int headerHeight = super.getHeight();
		this.headerArea = new ImmutableRect2i(area.getX(), area.getY(), area.getWidth(), headerHeight);
		super.updateBounds(headerArea);
		this.area = area;

		int y = area.getY() + headerHeight + VALUE_GROUP_TOP_GAP;
		int valueGroupContentHeight = getValueGroupContentHeight();
		if (valueGroupContentHeight > 0) {
			valueGroupArea = new ImmutableRect2i(
				area.getX() + 4,
				y - 1,
				Math.max(0, area.getWidth() - 8),
				valueGroupContentHeight + VALUE_GROUP_BORDER_SIZE * 2
			);
		} else {
			valueGroupArea = ImmutableRect2i.EMPTY;
		}
		for (ListValueRow row : valueRows) {
			row.updateBounds(new ImmutableRect2i(
				valueGroupArea.getX() + VALUE_GROUP_BORDER_SIZE,
				y,
				Math.max(0, valueGroupArea.getWidth() - VALUE_GROUP_BORDER_SIZE * 2),
				getEntryRowHeight()
			));
			y += getEntryRowHeight();
		}
		y += getUnusedValueTopGap();
		for (ListValueRow row : unusedValueRows) {
			row.updateBounds(new ImmutableRect2i(
				valueGroupArea.getX() + VALUE_GROUP_BORDER_SIZE,
				y,
				Math.max(0, valueGroupArea.getWidth() - VALUE_GROUP_BORDER_SIZE * 2),
				getEntryRowHeight()
			));
			y += getEntryRowHeight();
		}
		y += getAddValueTopGap();
		updateAddValueBounds(y);
	}

	@Override
	protected ImmutableRect2i getHoverArea() {
		return headerArea;
	}

	private static int getEntryRowHeight() {
		return Math.max(BUTTON_SIZE, ConfigGuiOptions.getRowDensity().getListRowHeight());
	}

	static int getDragRowOffset(int rowIndex, int sourceIndex, int targetIndex, int rowHeight) {
		if (sourceIndex < targetIndex && rowIndex > sourceIndex && rowIndex <= targetIndex) {
			return -rowHeight;
		}
		if (sourceIndex > targetIndex && rowIndex >= targetIndex && rowIndex < sourceIndex) {
			return rowHeight;
		}
		return 0;
	}

	private int getUnusedValueTopGap() {
		if (!valueRows.isEmpty() && !unusedValueRows.isEmpty()) {
			return UNUSED_VALUE_TOP_GAP;
		}
		return 0;
	}

	private int getAddValueTopGap() {
		if (allowsTypedInput && (!valueRows.isEmpty() || !unusedValueRows.isEmpty())) {
			return ADD_VALUE_TOP_GAP;
		}
		return 0;
	}

	private void updateAddValueBounds(int y) {
		if (!allowsTypedInput || valueGroupArea.isEmpty()) {
			addValueRowArea = ImmutableRect2i.EMPTY;
			addValueTextArea = ImmutableRect2i.EMPTY;
			addValueButtonArea = ImmutableRect2i.EMPTY;
			return;
		}
		addValueRowArea = new ImmutableRect2i(
			valueGroupArea.getX() + VALUE_GROUP_BORDER_SIZE,
			y,
			Math.max(0, valueGroupArea.getWidth() - VALUE_GROUP_BORDER_SIZE * 2),
			getEntryRowHeight()
		);
		int cy = addValueRowArea.getY() + (addValueRowArea.getHeight() - BUTTON_SIZE) / 2;
		addValueButtonArea = createButtonArea(addValueRowArea, cy, 0);
		addValueTextArea = new ImmutableRect2i(
			addValueRowArea.getX() + VALUE_ROW_HORIZONTAL_PADDING,
			cy,
			Math.max(0, addValueRowArea.getWidth() - BUTTON_SIZE - BUTTON_GAP - VALUE_ROW_HORIZONTAL_PADDING * 2),
			BUTTON_SIZE
		);
	}

	@Override
	protected void drawContent(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		if (isVisible(headerArea)) {
			drawName(guiGraphics);
		}

		drawValueGroup(guiGraphics, valueGroupArea);
		@Nullable
		DragSession dragSession = this.dragSession;
		if (dragSession == null) {
			for (ListValueRow row : getVisibleRows(valueRows, 0)) {
				row.draw(guiGraphics, mouseX, mouseY, false, isRecentlyMoved(row));
			}
		} else {
			dragSession.drawReorderedRows(guiGraphics);
		}
		for (ListValueRow row : getVisibleRows(unusedValueRows, 0)) {
			row.draw(guiGraphics, mouseX, mouseY, false, false);
		}
		drawAddValueRow(guiGraphics, mouseX, mouseY);
		if (dragSession != null) {
			ConfigRenderUtil.flush(guiGraphics);
			ConfigRenderUtil.pushPose(guiGraphics);
			ConfigRenderUtil.translate(guiGraphics, 0, 0, ORDERED_ROW_DRAG_FLOAT_Z_OFFSET);
			dragSession.drawFloatingRow(guiGraphics);
			ConfigRenderUtil.popPose(guiGraphics);
			ConfigRenderUtil.flush(guiGraphics);
		}
	}

	private List<ListValueRow> getVisibleRows(List<ListValueRow> rows, int overscan) {
		ImmutableRect2i viewport = getViewport();
		if (viewport == null || rows.isEmpty()) {
			return rows;
		}
		RowRange range = getVisibleRowRange(rows.get(0).area.getY(), getEntryRowHeight(), rows.size(), viewport, overscan);
		return rows.subList(range.first(), range.end());
	}

	static RowRange getVisibleRowRange(int firstY, int rowHeight, int rowCount, ImmutableRect2i viewport, int overscan) {
		if (viewport.isEmpty()) {
			return new RowRange(0, 0);
		}
		long first = Math.floorDiv((long) viewport.getY() - firstY, rowHeight) - overscan;
		long end = -Math.floorDiv((long) firstY - viewport.getY() - viewport.getHeight(), rowHeight) + overscan;
		return new RowRange(ConfigMath.clamp(first, 0, rowCount), ConfigMath.clamp(end, 0, rowCount));
	}

	record RowRange(int first, int end) {
	}

	private void drawAddValueRow(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		if (!allowsTypedInput || !isVisible(addValueRowArea)) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		ConfigTextures textures = getTextures();

		guiGraphics.fill(
			addValueRowArea.getX(),
			addValueRowArea.getY(),
			addValueRowArea.getX() + addValueRowArea.getWidth(),
			addValueRowArea.getY() + addValueRowArea.getHeight(),
			ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_UNUSED_ROW_BACKGROUND)
		);
		if (addValueRowArea.contains(mouseX, mouseY)) {
			fillRowHover(guiGraphics, addValueRowArea);
		}
		drawButtonBackground(guiGraphics, textures, addValueTextArea, true, addValueTextArea.contains(mouseX, mouseY));
		String displayText = getAddValueDisplayText();
		int textColor = getAddValueTextColor();
		drawAddValueText(guiGraphics, font, displayText, textColor);

		boolean canAdd = canAddTypedValue();
		boolean addHovered = canAdd && addValueButtonArea.contains(mouseX, mouseY);
		drawButtonBackground(guiGraphics, textures, addValueButtonArea, canAdd, addHovered);
		ConfigButtonIcon.ADD.draw(guiGraphics, addValueButtonArea, canAdd);
	}

	private String getAddValueDisplayText() {
		if (addingValue) {
			return addValueText + "_";
		}
		return elementSerializer.getValidValuesDescription();
	}

	private int getAddValueTextColor() {
		if (addingValue && !addValueText.isEmpty() && !canAddTypedValue()) {
			return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_INVALID_TEXT);
		}
		if (addingValue) {
			return getConfiguredTextColor();
		}
		return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_UNUSED_ROW_TEXT);
	}

	private void drawAddValueText(GuiGraphics guiGraphics, Font font, String text, int color) {
		ImmutableRect2i textArea = addValueTextArea.cropLeft(ADD_VALUE_TEXT_PADDING).cropRight(ADD_VALUE_TEXT_PADDING);
		int y = getCenteredTextY(font, textArea);
		String visibleText = getVisibleText(font, text, textArea.getWidth());
		drawText(guiGraphics, font, visibleText, textArea.getX(), y, color);
	}

	private static String getVisibleText(Font font, String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}
		return font.plainSubstrByWidth(text, maxWidth, true);
	}

	private void drawValueGroup(GuiGraphics guiGraphics, ImmutableRect2i groupArea) {
		if (groupArea.isEmpty()) {
			return;
		}

		int x = groupArea.getX();
		int y = groupArea.getY();
		int right = x + groupArea.getWidth();
		int bottom = y + groupArea.getHeight();

		guiGraphics.fill(x, y, right, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_GROUP_BACKGROUND));
		guiGraphics.fill(x, y, right, y + 1, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_GROUP_BORDER_DARK));
		guiGraphics.fill(x, y, x + 1, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_GROUP_BORDER_DARK));
		guiGraphics.fill(right - 1, y, right, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_GROUP_BORDER_LIGHT));
		guiGraphics.fill(x, bottom - 1, right, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_GROUP_BORDER_LIGHT));
	}

	private static void fillRowHover(GuiGraphics guiGraphics, ImmutableRect2i rowArea) {
		guiGraphics.fill(
			rowArea.getX(),
			rowArea.getY(),
			rowArea.getX() + rowArea.getWidth(),
			rowArea.getY() + rowArea.getHeight(),
			ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ROW_HOVER)
		);
	}

	private static ImmutableRect2i createButtonArea(ImmutableRect2i area, int y, int buttonsFromRight) {
		int xOffset = (BUTTON_SIZE + BUTTON_GAP) * buttonsFromRight;
		return new ImmutableRect2i(
			area.getX() + area.getWidth() - BUTTON_SIZE - xOffset,
			y,
			BUTTON_SIZE,
			BUTTON_SIZE
		);
	}

	private static ColorComponentAreas createColorComponentAreas(ImmutableRect2i area) {
		int swatchSize = Math.min(COLOR_COMPONENT_SWATCH_SIZE, Math.min(area.getWidth(), area.getHeight()));
		int swatchY = area.getY() + (area.getHeight() - swatchSize) / 2;
		ImmutableRect2i swatchArea = new ImmutableRect2i(area.getX(), swatchY, swatchSize, swatchSize);
		int hexX = area.getX() + swatchSize + COLOR_COMPONENT_GAP;
		ImmutableRect2i hexArea = new ImmutableRect2i(
			hexX,
			area.getY(),
			Math.max(0, area.getX() + area.getWidth() - hexX),
			area.getHeight()
		);
		return new ColorComponentAreas(swatchArea, hexArea);
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
			if (row.resetArea.contains(mouseX, mouseY)) {
				return new ConfigInfo(
					Component.translatable("mezz_config.config.screen.reset"),
					List.of(Component.translatable("mezz_config.config.screen.reset.value.info"))
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
		if (allowsTypedInput && addValueTextArea.contains(mouseX, mouseY)) {
			if (addingValue && !addValueText.isEmpty() && !canAddTypedValue()) {
				return createInvalidAddValueInfo();
			}
			return new ConfigInfo(
				Component.translatable("mezz_config.config.screen.add"),
				List.of(ConfigNumberInfo.getValidValuesDescription(elementSerializer))
			);
		}
		if (allowsTypedInput && addValueButtonArea.contains(mouseX, mouseY)) {
			return new ConfigInfo(
				Component.translatable("mezz_config.config.screen.add"),
				List.of(ConfigNumberInfo.getValidValuesDescription(elementSerializer))
			);
		}
		return null;
	}

	private ConfigInfo createInvalidAddValueInfo() {
		IDeserializeResult<T> result = elementSerializer.deserialize(addValueText);
		List<Component> lines = new ArrayList<>();
		for (String error : result.getDiagnostics()) {
			lines.add(Component.literal(error));
		}
		if (lines.isEmpty()) {
			lines.add(Component.translatable("mezz_config.config.screen.text.invalid.info"));
		}
		lines.add(ConfigNumberInfo.getValidValuesDescription(elementSerializer));
		return new ConfigInfo(Component.translatable("mezz_config.config.screen.text.invalid"), lines);
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
	protected boolean onMouseClicked(UserInput input) {
		if (super.onMouseClicked(input)) {
			cancelAddValue();
			cancelComponentEdit();
			return true;
		}
		return false;
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (componentEditSession != null) {
			appendComponentEditCharacter(codePoint);
			return true;
		}
		if (addingValue) {
			appendAddValueCharacter(codePoint);
			return true;
		}
		return false;
	}

	private void appendAddValueCharacter(char codePoint) {
		if (addValueText.length() >= MAX_ADD_VALUE_TEXT_LENGTH || !StringUtil.isAllowedChatCharacter(codePoint)) {
			return;
		}
		addValueText += codePoint;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (componentEditSession != null) {
			return keyPressedComponentEdit(keyCode);
		}
		if (addingValue) {
			return keyPressedAddValue(keyCode);
		}
		return false;
	}

	private boolean keyPressedAddValue(int keyCode) {
		if (ConfigInputUtil.isPaste(keyCode)) {
			appendAddValueText(Minecraft.getInstance().keyboardHandler.getClipboard());
			return true;
		}
		if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER) {
			commitAddValue();
			return true;
		}
		if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
			cancelAddValue();
			return true;
		}
		if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE && !addValueText.isEmpty()) {
			addValueText = addValueText.substring(0, addValueText.length() - 1);
			return true;
		}
		if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_DELETE) {
			addValueText = "";
			return true;
		}
		return true;
	}

	private boolean keyPressedComponentEdit(int keyCode) {
		if (ConfigInputUtil.isPaste(keyCode)) {
			appendComponentEditText(Minecraft.getInstance().keyboardHandler.getClipboard());
			return true;
		}
		if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER) {
			commitComponentEdit();
			return true;
		}
		if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_TAB) {
			commitComponentEdit();
			return true;
		}
		if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
			cancelComponentEdit();
			return true;
		}
		if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE && !componentEditSession.editText.isEmpty()) {
			String editText = componentEditSession.editText;
			componentEditSession.editText = editText.substring(0, editText.length() - 1);
			return true;
		}
		if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_DELETE) {
			componentEditSession.editText = "";
			return true;
		}
		return true;
	}

	private void appendAddValueText(String text) {
		for (int i = 0; i < text.length(); i++) {
			appendAddValueCharacter(text.charAt(i));
		}
	}

	private void appendComponentEditCharacter(char codePoint) {
		if (componentEditSession == null ||
			componentEditSession.editText.length() >= MAX_ADD_VALUE_TEXT_LENGTH ||
			!StringUtil.isAllowedChatCharacter(codePoint)
		) {
			return;
		}
		componentEditSession.editText += codePoint;
	}

	private void appendComponentEditText(String text) {
		for (int i = 0; i < text.length(); i++) {
			appendComponentEditCharacter(text.charAt(i));
		}
	}

	@Override
	public boolean isCapturingKeyboardInput() {
		return addingValue || componentEditSession != null;
	}

	@Override
	public void unfocus() {
		commitAddValue();
		commitComponentEdit();
	}

	@Override
	protected void onValueChanged() {
		cancelComponentEdit();
		rebuildRows();
		layoutUpdater.run();
	}

	private void addValue(T value) {
		clearRecentlyMovedValue();
		List<T> current = new ArrayList<>(getValue());
		current.add(value);
		setValue(current);
	}

	private boolean onAddValueMouseClicked(UserInput input) {
		if (!allowsTypedInput) {
			return false;
		}
		if (addValueTextArea.contains(input.getMouseX(), input.getMouseY())) {
			if (!input.isSimulate()) {
				commitComponentEdit();
				addingValue = true;
			}
			return true;
		}
		if (addValueButtonArea.contains(input.getMouseX(), input.getMouseY())) {
			if (!input.isSimulate()) {
				commitComponentEdit();
				commitAddValue();
			}
			return true;
		}
		if (addingValue && !input.isSimulate()) {
			commitAddValue();
		}
		return false;
	}

	private boolean canAddTypedValue() {
		return getAddValue()
			.isPresent();
	}

	private Optional<T> getAddValue() {
		if (!allowsTypedInput || addValueText.isEmpty()) {
			return Optional.empty();
		}
		IDeserializeResult<T> result = elementSerializer.deserialize(addValueText);
		if (!result.getDiagnostics().isEmpty()) {
			return Optional.empty();
		}
		return result.getResult()
			.filter(elementSerializer::isValid)
			.filter(this::canAddValue);
	}

	private boolean canAddValue(T value) {
		List<T> current = new ArrayList<>(getValue());
		current.add(value);
		return configValue.getSerializer()
			.isValid(current);
	}

	private void commitAddValue() {
		getAddValue()
			.ifPresent(this::addValue);
		cancelAddValue();
	}

	private void cancelAddValue() {
		addingValue = false;
		addValueText = "";
	}

	private boolean onKeyValueComponentMouseClicked(ListValueRow row, UserInput input) {
		if (!row.selected || !isIndexValid(getValue(), row.index)) {
			return false;
		}
		if (row.hexColor != null && row.componentColorSwatchArea.contains(input.getMouseX(), input.getMouseY())) {
			if (!input.isSimulate()) {
				commitAddValue();
				commitComponentEdit();
				openComponentPopup(row.index, false);
			}
			return true;
		}
		if (keyValueSerializer == null) {
			return false;
		}
		Object component = keyValueSerializer.getValue(getValue().get(row.index));
		boolean focusHexInput = false;
		if (component instanceof PackedColor) {
			focusHexInput = row.componentHexArea.contains(input.getMouseX(), input.getMouseY());
			boolean swatchClicked = row.componentColorSwatchArea.contains(input.getMouseX(), input.getMouseY());
			if (!focusHexInput && !swatchClicked) {
				return false;
			}
		} else if (!row.componentValueArea.contains(input.getMouseX(), input.getMouseY())) {
			return false;
		}
		if (!input.isSimulate()) {
			commitAddValue();
			commitComponentEdit();
			if (usesComponentPopup(row.index)) {
				openComponentPopup(row.index, focusHexInput);
			} else {
				startComponentEdit(row.index);
			}
		}
		return true;
	}

	private boolean usesComponentPopup(int rowIndex) {
		if (keyValueSerializer == null || !isIndexValid(getValue(), rowIndex)) {
			return false;
		}
		T entry = getValue().get(rowIndex);
		Object component = keyValueSerializer.getValue(entry);
		return component instanceof PackedColor || getComponentSerializer().getAllValidValues().isPresent();
	}

	private void openComponentPopup(int rowIndex, boolean focusHexInput) {
		if (!isIndexValid(getValue(), rowIndex)) {
			return;
		}
		ListComponentScreenValue componentConfigValue = new ListComponentScreenValue(rowIndex);
		Object component = componentConfigValue.getValue();
		@Nullable
		IConfigValuePopup<Object> popup = createComponentPopup(componentConfigValue, component, focusHexInput, rowIndex);
		if (popup == null) {
			return;
		}
		ConfigValuePopupSelector<Object> selector = new ConfigValuePopupSelector<>(
			componentConfigValue,
			popup,
			() -> getComponentArea(rowIndex, focusHexInput),
			this::hasPendingChange,
			componentConfigValue::set
		);
		valueSelectorOpener.accept(selector);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private IConfigValuePopup<Object> createComponentPopup(
		IConfigScreenValue<Object> componentConfigValue,
		Object component,
		boolean focusHexInput,
		int rowIndex
	) {
		if (component instanceof PackedColor color) {
			return (IConfigValuePopup<Object>) (IConfigValuePopup<?>) new ColorPickerPopup(color, focusHexInput);
		}
		Optional<HexColorString> hexColor = HexColorString.parse(component);
		if (hexColor.isPresent()) {
			HexColorString color = hexColor.get();
			return new MappedConfigValuePopup<>(
				new ColorPickerPopup(color.color(), focusHexInput),
				updated -> color.update(component, updated, componentConfigValue.getSerializer())
					.filter(value -> getEditedComponent(rowIndex, value).isPresent())
			);
		}
		Optional<List<Object>> allValidValues = componentConfigValue.getSerializer().getAllValidValues();
		if (allValidValues.isPresent()) {
			ConfigValueSelector<Object> selector = new ConfigValueSelector<>(
				componentConfigValue,
				List.copyOf(allValidValues.get()),
				component
			);
			if (!selector.isEmpty()) {
				return selector;
			}
		}
		return null;
	}

	private ImmutableRect2i getComponentArea(int rowIndex, boolean focusHexInput) {
		if (!isIndexValid(valueRows, rowIndex)) {
			return ImmutableRect2i.EMPTY;
		}
		ListValueRow row = valueRows.get(rowIndex);
		if (!row.componentColorSwatchArea.isEmpty()) {
			if (focusHexInput) {
				return row.componentHexArea;
			}
			return row.componentColorSwatchArea;
		}
		return row.componentValueArea;
	}

	private void startComponentEdit(int rowIndex) {
		if (keyValueSerializer == null || !isIndexValid(getValue(), rowIndex)) {
			return;
		}
		T entry = getValue().get(rowIndex);
		Object component = keyValueSerializer.getValue(entry);
		IConfigValueSerializer<Object> componentSerializer = getComponentSerializer();
		componentEditSession = new ComponentEditSession(rowIndex, componentSerializer.serialize(component));
	}

	private Optional<T> getEditedEntry() {
		if (componentEditSession == null || keyValueSerializer == null || !isIndexValid(getValue(), componentEditSession.rowIndex)) {
			return Optional.empty();
		}
		T entry = getValue().get(componentEditSession.rowIndex);
		Optional<T> editedEntry = keyValueSerializer.withSerializedValue(entry, componentEditSession.editText);
		return editedEntry.filter(value -> canReplaceEntry(componentEditSession.rowIndex, value));
	}

	private void commitComponentEdit() {
		if (componentEditSession == null) {
			return;
		}
		int rowIndex = componentEditSession.rowIndex;
		Optional<T> editedEntry = getEditedEntry();
		componentEditSession = null;
		editedEntry.ifPresent(entry -> replaceEntry(rowIndex, entry));
	}

	private void cancelComponentEdit() {
		componentEditSession = null;
	}

	private boolean replaceComponent(int rowIndex, Object component) {
		return getEditedComponent(rowIndex, component)
			.map(value -> replaceEntry(rowIndex, value))
			.orElse(false);
	}

	@SuppressWarnings("unchecked")
	private Optional<T> getEditedComponent(int rowIndex, Object component) {
		if (!isIndexValid(getValue(), rowIndex) || !getComponentSerializer().isValid(component)) {
			return Optional.empty();
		}
		Optional<T> editedEntry;
		if (keyValueSerializer == null) {
			editedEntry = Optional.of((T) component);
		} else {
			editedEntry = keyValueSerializer.withValue(getValue().get(rowIndex), component);
		}
		return editedEntry.filter(value -> canReplaceEntry(rowIndex, value));
	}

	private boolean canReplaceEntry(int rowIndex, T entry) {
		List<T> current = new ArrayList<>(getValue());
		if (!isIndexValid(current, rowIndex)) {
			return false;
		}
		current.set(rowIndex, entry);
		return configValue.getSerializer().isValid(current);
	}

	private boolean replaceEntry(int rowIndex, T entry) {
		List<T> current = new ArrayList<>(getValue());
		if (!isIndexValid(current, rowIndex)) {
			return false;
		}
		current.set(rowIndex, entry);
		clearRecentlyMovedValue();
		return setValue(current);
	}

	private Optional<T> getDefaultEntry(T entry) {
		if (keyValueSerializer == null) {
			return Optional.empty();
		}
		Object key = keyValueSerializer.getKey(entry);
		return configValue.getDefaultValue().stream()
			.filter(defaultEntry -> Objects.equals(keyValueSerializer.getKey(defaultEntry), key))
			.findFirst();
	}

	private Optional<Object> getDefaultComponentValue(T entry) {
		if (keyValueSerializer == null) {
			return Optional.empty();
		}
		return getDefaultEntry(entry)
			.map(keyValueSerializer::getValue);
	}

	private boolean canResetComponentValue(T entry) {
		if (keyValueSerializer == null) {
			return false;
		}
		return getDefaultComponentValue(entry)
			.filter(defaultValue -> !Objects.equals(keyValueSerializer.getValue(entry), defaultValue))
			.isPresent();
	}

	boolean resetComponentValue(int rowIndex) {
		if (keyValueSerializer == null || !isIndexValid(getValue(), rowIndex)) {
			return false;
		}
		T entry = getValue().get(rowIndex);
		return getDefaultComponentValue(entry)
			.flatMap(defaultValue -> keyValueSerializer.withValue(entry, defaultValue))
			.filter(defaultEntry -> canReplaceEntry(rowIndex, defaultEntry))
			.map(defaultEntry -> replaceEntry(rowIndex, defaultEntry))
			.orElse(false);
	}

	@SuppressWarnings("unchecked")
	private IConfigValueSerializer<Object> getComponentSerializer() {
		if (keyValueSerializer == null) {
			return (IConfigValueSerializer<Object>) elementSerializer;
		}
		return keyValueSerializer.getValueSerializer();
	}

	private Object getRowComponent(T entry) {
		if (keyValueSerializer == null) {
			return entry;
		}
		return keyValueSerializer.getValue(entry);
	}

	private void moveValue(int index, int offset) {
		if (!ordered) {
			return;
		}
		List<T> current = new ArrayList<>(getValue());
		int newIndex = index + offset;
		if (index >= 0 && index < current.size() && newIndex >= 0 && newIndex < current.size()) {
			T movedValue = current.get(index);
			Collections.swap(current, index, newIndex);
			if (setValue(current)) {
				markRecentlyMovedValue(movedValue, newIndex);
			}
		}
	}

	private boolean moveValueToIndex(int sourceIndex, int targetIndex) {
		if (!ordered) {
			return false;
		}
		List<T> current = new ArrayList<>(getValue());
		if (!isIndexValid(current, sourceIndex) || !isIndexValid(current, targetIndex) || sourceIndex == targetIndex) {
			return false;
		}
		T value = current.remove(sourceIndex);
		current.add(targetIndex, value);
		boolean changed = setValue(current);
		if (changed) {
			markRecentlyMovedValue(value, targetIndex);
		}
		return changed;
	}

	private void markRecentlyMovedValue(T value, int index) {
		recentlyMovedValue = value;
		recentlyMovedIndex = index;
	}

	private void clearRecentlyMovedValue() {
		recentlyMovedValue = null;
		recentlyMovedIndex = -1;
	}

	private boolean isRecentlyMoved(ListValueRow row) {
		return row.selected &&
			row.index == recentlyMovedIndex &&
			recentlyMovedValue != null &&
			recentlyMovedValue.equals(row.value);
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
		clearRecentlyMovedValue();
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
			if (onAddValueMouseClicked(input)) {
				return Optional.of(this);
			}
			for (ListValueRow row : valueRows) {
				if (onKeyValueComponentMouseClicked(row, input)) {
					return Optional.of(this);
				}
			}

			for (ListValueRow row : valueRows) {
				if (row.moveUpArea.contains(input.getMouseX(), input.getMouseY())) {
					if (!row.canMoveUp()) {
						return Optional.empty();
					}
					if (!input.isSimulate()) {
						commitComponentEdit();
						moveValue(row.index, -1);
					}
					return Optional.of(this);
				}
				if (row.moveDownArea.contains(input.getMouseX(), input.getMouseY())) {
					if (!row.canMoveDown()) {
						return Optional.empty();
					}
					if (!input.isSimulate()) {
						commitComponentEdit();
						moveValue(row.index, 1);
					}
					return Optional.of(this);
				}
				if (row.deleteArea.contains(input.getMouseX(), input.getMouseY())) {
					if (!input.isSimulate()) {
						commitComponentEdit();
						removeValue(row.index);
					}
					return Optional.of(this);
				}
				if (row.resetArea.contains(input.getMouseX(), input.getMouseY())) {
					if (!row.canResetValue()) {
						return Optional.empty();
					}
					if (!input.isSimulate()) {
						commitComponentEdit();
						resetComponentValue(row.index);
					}
					return Optional.of(this);
				}
			}
			for (ListValueRow row : valueRows) {
				if (ordered && ConfigGuiOptions.enableDragReordering() && row.canStartDrag(input.getMouseX(), input.getMouseY())) {
					if (!input.isSimulate()) {
						commitComponentEdit();
					}
					DragSession session = new DragSession(row, input.getMouseX(), input.getMouseY(), !input.isSimulate());
					return Optional.of(session);
				}
			}
			for (ListValueRow row : unusedValueRows) {
				if (row.addArea.contains(input.getMouseX(), input.getMouseY())) {
					if (!input.isSimulate()) {
						commitComponentEdit();
						addValue(row.value);
					}
					return Optional.of(this);
				}
			}

			return Optional.empty();
		}

		@Override
		public void unfocus() {
			ListConfigEntry.this.unfocus();
			dragSession = null;
		}
	}

	private class DragSession implements ConfigInputHandler {
		private final int sourceIndex;
		private int targetIndex;
		private final double grabOffsetX;
		private final double grabOffsetY;
		private double mouseX;
		private double mouseY;
		private boolean active;

		public DragSession(ListValueRow row, double mouseX, double mouseY, boolean active) {
			this.sourceIndex = row.index;
			this.targetIndex = row.index;
			this.grabOffsetX = mouseX - row.area.getX();
			this.grabOffsetY = mouseY - row.area.getY();
			this.mouseX = mouseX;
			this.mouseY = mouseY;
			if (active) {
				start();
			}
		}

		@Override
		public Optional<ConfigInputHandler> handleUserInput(Screen screen, UserInput input) {
			if (active && ConfigInputUtil.isLeftClick(input)) {
				moveToMouse(input.getMouseX(), input.getMouseY());
				commitMove();
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
			if (button != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
				stop();
				return Optional.empty();
			}
			start();
			moveToMouse(mouseX, mouseY);
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

		private void moveToMouse(double mouseX, double mouseY) {
			this.mouseX = mouseX;
			this.mouseY = mouseY;
			double draggedCenterY = mouseY - grabOffsetY + getEntryRowHeight() / 2.0;
			targetIndex = getDragTargetIndex(draggedCenterY);
		}

		private void commitMove() {
			moveValueToIndex(sourceIndex, targetIndex);
		}

		private int getDragTargetIndex(double mouseY) {
			if (valueRows.isEmpty()) {
				return sourceIndex;
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
			return active && row.selected && row.index == sourceIndex;
		}

		public void drawReorderedRows(GuiGraphics guiGraphics) {
			// Reordering shifts neighboring rows by one row height, so include that margin.
			for (ListValueRow row : getVisibleRows(valueRows, 1)) {
				if (!isDragging(row)) {
					row.drawDuringDrag(guiGraphics, getVisualArea(row));
				}
			}
			@Nullable
			ListValueRow targetRow = getTargetRow();
			if (targetRow != null) {
				targetRow.drawDragGap(guiGraphics, targetIndex != sourceIndex);
			}
		}

		private ImmutableRect2i getVisualArea(ListValueRow row) {
			int yOffset = getDragRowOffset(row.index, sourceIndex, targetIndex, row.area.getHeight());
			return new ImmutableRect2i(
				row.area.getX(),
				row.area.getY() + yOffset,
				row.area.getWidth(),
				row.area.getHeight()
			);
		}

		public void drawFloatingRow(GuiGraphics guiGraphics) {
			@Nullable
			ListValueRow row = getDraggingRow();
			if (row != null) {
				row.drawFloating(guiGraphics, getFloatingRowX(row), getFloatingRowY());
			}
		}

		private int getFloatingRowX(ListValueRow row) {
			return getDragRowX(row.area.getX(), mouseX, grabOffsetX);
		}

		private int getFloatingRowY() {
			int y = (int) Math.round(mouseY - grabOffsetY);
			if (valueRows.isEmpty()) {
				return y;
			}
			int minY = valueRows.get(0).area.getY();
			int maxY = valueRows.get(valueRows.size() - 1).area.getY();
			return ConfigMath.clamp(y, minY, maxY);
		}

		@Nullable
		private ListValueRow getDraggingRow() {
			if (isIndexValid(valueRows, sourceIndex)) {
				return valueRows.get(sourceIndex);
			}
			return null;
		}

		@Nullable
		private ListValueRow getTargetRow() {
			if (isIndexValid(valueRows, targetIndex)) {
				return valueRows.get(targetIndex);
			}
			return null;
		}
	}

	static int getDragRowX(int originalX, double mouseX, double grabOffsetX) {
		int desiredX = (int) Math.round(mouseX - grabOffsetX);
		return ConfigMath.clamp(
			desiredX,
			originalX - ORDERED_ROW_MAX_HORIZONTAL_DRAG_OFFSET,
			originalX + ORDERED_ROW_MAX_HORIZONTAL_DRAG_OFFSET
		);
	}

	private class ListValueRow {
		final T value;
		@Nullable
		private final HexColorString hexColor;
		int index;
		private final boolean selected;
		ImmutableRect2i area = ImmutableRect2i.EMPTY;
		ImmutableRect2i keyArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i componentValueArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i componentColorSwatchArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i componentHexArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i moveUpArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i moveDownArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i resetArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i deleteArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i addArea = ImmutableRect2i.EMPTY;

		ListValueRow(T value, int index, boolean selected) {
			this.value = value;
			this.hexColor = HexColorString.parse(getRowComponent(value)).orElse(null);
			this.index = index;
			this.selected = selected;
		}

		void updateBounds(ImmutableRect2i area) {
			this.area = area;
			int cy = area.getY() + (area.getHeight() - BUTTON_SIZE) / 2;
			addArea = ImmutableRect2i.EMPTY;
			moveUpArea = ImmutableRect2i.EMPTY;
			moveDownArea = ImmutableRect2i.EMPTY;
			resetArea = ImmutableRect2i.EMPTY;
			deleteArea = ImmutableRect2i.EMPTY;
			if (selected) {
				int buttonOffset = 0;
				if (allowsRemovingValues) {
					deleteArea = createButtonArea(area, cy, buttonOffset++);
				}
				if (getDefaultEntry(value).isPresent()) {
					resetArea = createButtonArea(area, cy, buttonOffset++);
				}
				if (ordered) {
					moveDownArea = createButtonArea(area, cy, buttonOffset++);
					moveUpArea = createButtonArea(area, cy, buttonOffset);
				}
			} else {
				addArea = createButtonArea(area, cy, 0);
			}
			updateComponentBounds(area);
		}

		private void updateComponentBounds(ImmutableRect2i rowArea) {
			if (keyValueSerializer == null) {
				keyArea = ImmutableRect2i.EMPTY;
				componentValueArea = ImmutableRect2i.EMPTY;
				componentColorSwatchArea = ImmutableRect2i.EMPTY;
				componentHexArea = ImmutableRect2i.EMPTY;
				if (hexColor != null) {
					componentColorSwatchArea = createColorComponentAreas(getContentArea(rowArea)).swatchArea();
				}
				return;
			}
			ImmutableRect2i contentArea = getContentArea(rowArea);
			int componentWidth = Math.max(0, (contentArea.getWidth() - KEY_VALUE_COLUMN_GAP) / 2);
			keyArea = new ImmutableRect2i(
				contentArea.getX(),
				contentArea.getY(),
				componentWidth,
				contentArea.getHeight()
			);
			componentValueArea = new ImmutableRect2i(
				keyArea.getX() + keyArea.getWidth() + KEY_VALUE_COLUMN_GAP,
				contentArea.getY(),
				Math.max(0, contentArea.getWidth() - componentWidth - KEY_VALUE_COLUMN_GAP),
				contentArea.getHeight()
			);
			Object component = keyValueSerializer.getValue(value);
			if (component instanceof PackedColor || hexColor != null) {
				ColorComponentAreas colorAreas = createColorComponentAreas(componentValueArea);
				componentColorSwatchArea = colorAreas.swatchArea();
				componentHexArea = colorAreas.hexArea();
			} else {
				componentColorSwatchArea = ImmutableRect2i.EMPTY;
				componentHexArea = ImmutableRect2i.EMPTY;
			}
		}

		private ImmutableRect2i getContentArea(ImmutableRect2i rowArea) {
			int x = rowArea.getX() + VALUE_ROW_HORIZONTAL_PADDING;
			return new ImmutableRect2i(
				x,
				rowArea.getY() + 1,
				Math.max(0, rowArea.getX() + rowArea.getWidth() - x - getControlsWidth() - 6),
				Math.max(0, rowArea.getHeight() - 2)
			);
		}

		boolean canMoveUp() {
			return ordered && selected && index > 0;
		}

		boolean canMoveDown() {
			return ordered && selected && index < valueRows.size() - 1;
		}

		boolean canResetValue() {
			return selected && canResetComponentValue(value);
		}

		boolean isControlMouseOver(double mouseX, double mouseY) {
			return moveUpArea.contains(mouseX, mouseY) ||
				moveDownArea.contains(mouseX, mouseY) ||
				resetArea.contains(mouseX, mouseY) ||
				deleteArea.contains(mouseX, mouseY) ||
				addArea.contains(mouseX, mouseY) ||
				componentColorSwatchArea.contains(mouseX, mouseY) ||
				componentValueArea.contains(mouseX, mouseY);
		}

		boolean canStartDrag(double mouseX, double mouseY) {
			return ordered && selected &&
				area.contains(mouseX, mouseY) &&
				!isControlMouseOver(mouseX, mouseY);
		}

		private int getControlsWidth() {
			if (selected) {
				int buttonCount = 0;
				if (ordered) {
					buttonCount += 2;
				}
				if (allowsRemovingValues) {
					buttonCount++;
				}
				if (getDefaultEntry(value).isPresent()) {
					buttonCount++;
				}
				return buttonCount * BUTTON_SIZE + Math.max(0, buttonCount - 1) * BUTTON_GAP;
			}
			return BUTTON_SIZE;
		}

		void draw(GuiGraphics guiGraphics, double mouseX, double mouseY, boolean dropTarget, boolean recentlyMoved) {
			draw(guiGraphics, area, mouseX, mouseY, true, false, dropTarget, recentlyMoved);
		}

		void drawDuringDrag(GuiGraphics guiGraphics, ImmutableRect2i rowArea) {
			draw(guiGraphics, rowArea, 0, 0, false, false, false, false);
		}

		void drawDragGap(GuiGraphics guiGraphics, boolean dropTarget) {
			if (!isVisible(area)) {
				return;
			}
			int x = area.getX();
			int y = area.getY();
			int right = x + area.getWidth();
			int bottom = y + area.getHeight();
			int color = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_ROW_DRAG_GAP);
			if (dropTarget) {
				color = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_ROW_DROP_TARGET);
			}
			guiGraphics.fill(x, y, right, bottom, color);
			if (index > 0) {
				guiGraphics.fill(x, y, right, y + 1, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_ROW_DIVIDER));
			}
		}

		void drawFloating(GuiGraphics guiGraphics, int x, int y) {
			ImmutableRect2i floatingArea = new ImmutableRect2i(x, y, area.getWidth(), area.getHeight());
			if (!isVisible(new ImmutableRect2i(x, y, area.getWidth(), area.getHeight() + 2))) {
				return;
			}
			int floatingX = floatingArea.getX();
			int bottom = floatingArea.getY() + floatingArea.getHeight();
			guiGraphics.fill(
				floatingX,
				bottom,
				floatingX + floatingArea.getWidth(),
				bottom + 2,
				ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_ROW_DRAG_FLOAT_SHADOW)
			);
			draw(guiGraphics, floatingArea, 0, 0, false, true, false, false);
		}

		private void draw(
			GuiGraphics guiGraphics,
			ImmutableRect2i rowArea,
			double mouseX,
			double mouseY,
			boolean drawControls,
			boolean floating,
			boolean dropTarget,
			boolean recentlyMoved
		) {
			if (!isVisible(rowArea)) {
				return;
			}
			Font font = Minecraft.getInstance().font;
			ConfigTextures textures = getTextures();

			int backgroundColor = getBackgroundColor(floating, dropTarget, recentlyMoved);
			guiGraphics.fill(rowArea.getX(), rowArea.getY(),
				rowArea.getX() + rowArea.getWidth(), rowArea.getY() + rowArea.getHeight(),
				backgroundColor);
			if (drawControls && rowArea.contains(mouseX, mouseY)) {
				fillRowHover(guiGraphics, rowArea);
			}
			if (index > 0) {
				guiGraphics.fill(
					rowArea.getX(),
					rowArea.getY(),
					rowArea.getX() + rowArea.getWidth(),
					rowArea.getY() + 1,
					ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_ROW_DIVIDER)
				);
			}
			if (floating) {
				drawFloatingHighlight(guiGraphics, rowArea);
			} else if (recentlyMoved) {
				drawMovedHighlight(guiGraphics, rowArea);
			}
			if (keyValueSerializer == null) {
				drawSingleValue(guiGraphics, font, rowArea);
			} else {
				drawKeyValue(guiGraphics, font, textures, rowArea, mouseX, mouseY, drawControls);
			}

			if (drawControls) {
				if (selected) {
					if (ordered) {
						drawMoveButton(guiGraphics, textures, moveUpArea, ConfigButtonIcon.UP, canMoveUp(), mouseX, mouseY);
						drawMoveButton(guiGraphics, textures, moveDownArea, ConfigButtonIcon.DOWN, canMoveDown(), mouseX, mouseY);
					}
					if (!resetArea.isEmpty()) {
						drawResetButton(guiGraphics, textures, mouseX, mouseY);
					}
					if (allowsRemovingValues) {
						drawDeleteButton(guiGraphics, textures, mouseX, mouseY);
					}
				} else {
					drawAddButton(guiGraphics, textures, mouseX, mouseY);
				}
			}
		}

		private void drawSingleValue(GuiGraphics guiGraphics, Font font, ImmutableRect2i rowArea) {
			Component valueName = ConfigValueLocalization.getValueName(elementSerializer, configValue.getLocalizationKey(), value);
			ImmutableRect2i contentArea = getContentArea(rowArea);
			int textX = contentArea.getX();
			int iconY = rowArea.getY() + (rowArea.getHeight() - ConfigValueIcon.ICON_SIZE) / 2;
			if (hexColor != null) {
				ImmutableRect2i swatch = createColorComponentAreas(contentArea).swatchArea();
				ColorSwatch.draw(guiGraphics, swatch, hexColor.color());
				textX += swatch.getWidth() + COLOR_COMPONENT_GAP;
			} else {
				ConfigValueIcon.draw(guiGraphics, elementSerializer, value, textX, iconY);
				textX += ConfigValueIcon.getTextOffset(elementSerializer, value);
			}
			ImmutableRect2i textArea = new ImmutableRect2i(
				textX,
				contentArea.getY(),
				Math.max(0, contentArea.getX() + contentArea.getWidth() - textX),
				contentArea.getHeight()
			);
			ConfigEntryWidget.drawFittedText(guiGraphics, font, valueName, textArea, getValueTextColor(), false);
		}

		private void drawKeyValue(
			GuiGraphics guiGraphics,
			Font font,
			ConfigTextures textures,
			ImmutableRect2i rowArea,
			double mouseX,
			double mouseY,
			boolean interactive
		) {
			ImmutableRect2i contentArea = getContentArea(rowArea);
			int componentWidth = Math.max(0, (contentArea.getWidth() - KEY_VALUE_COLUMN_GAP) / 2);
			ImmutableRect2i drawnKeyArea = new ImmutableRect2i(
				contentArea.getX(),
				contentArea.getY(),
				componentWidth,
				contentArea.getHeight()
			);
			ImmutableRect2i drawnValueArea = new ImmutableRect2i(
				drawnKeyArea.getX() + drawnKeyArea.getWidth() + KEY_VALUE_COLUMN_GAP,
				contentArea.getY(),
				Math.max(0, contentArea.getWidth() - componentWidth - KEY_VALUE_COLUMN_GAP),
				contentArea.getHeight()
			);
			boolean editable = selected && interactive;
			drawKeyValueComponent(
				guiGraphics,
				font,
				textures,
				drawnKeyArea,
				keyValueSerializer.getKeySerializer(),
				keyValueSerializer.getKey(value),
				false,
				false,
				mouseX,
				mouseY
			);
			drawKeyValueComponent(
				guiGraphics,
				font,
				textures,
				drawnValueArea,
				keyValueSerializer.getValueSerializer(),
				keyValueSerializer.getValue(value),
				true,
				editable,
				mouseX,
				mouseY
			);
		}

		private void drawKeyValueComponent(
			GuiGraphics guiGraphics,
			Font font,
			ConfigTextures textures,
			ImmutableRect2i componentArea,
			IConfigValueSerializer<Object> componentSerializer,
			Object component,
			boolean valueComponent,
			boolean editable,
			double mouseX,
			double mouseY
		) {
			if (valueComponent && component instanceof PackedColor color) {
				drawColorComponent(guiGraphics, font, textures, componentArea, color, editable, mouseX, mouseY);
				return;
			}
			if (editable) {
				drawButtonBackground(guiGraphics, textures, componentArea, true, componentArea.contains(mouseX, mouseY));
			}
			int textX = componentArea.getX() + KEY_VALUE_TEXT_PADDING;
			int iconY = componentArea.getY() + (componentArea.getHeight() - ConfigValueIcon.ICON_SIZE) / 2;
			if (valueComponent && hexColor != null) {
				ImmutableRect2i swatch = createColorComponentAreas(componentArea).swatchArea();
				ColorSwatch.draw(guiGraphics, swatch, hexColor.color());
				textX += swatch.getWidth() + COLOR_COMPONENT_GAP;
			} else {
				ConfigValueIcon.draw(guiGraphics, componentSerializer, component, textX, iconY);
				textX += ConfigValueIcon.getTextOffset(componentSerializer, component);
			}
			ImmutableRect2i textArea = new ImmutableRect2i(
				textX,
				componentArea.getY(),
				Math.max(0, componentArea.getX() + componentArea.getWidth() - textX - KEY_VALUE_TEXT_PADDING),
				componentArea.getHeight()
			);
			Component text;
			int textColor = getValueTextColor();
			ComponentEditSession editSession = componentEditSession;
			if (editSession != null && editSession.rowIndex == index && valueComponent) {
				text = Component.literal(editSession.editText + "_");
				if (getEditedEntry().isEmpty()) {
					textColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_INVALID_TEXT);
				}
			} else {
				String localizationKeySuffix = ".key";
				if (valueComponent) {
					localizationKeySuffix = ".value";
				}
				text = ConfigValueLocalization.getValueName(
					componentSerializer,
					configValue.getLocalizationKey() + localizationKeySuffix,
					component
				);
			}
			ConfigEntryWidget.drawFittedText(guiGraphics, font, text, textArea, textColor, false);
		}

		private void drawColorComponent(
			GuiGraphics guiGraphics,
			Font font,
			ConfigTextures textures,
			ImmutableRect2i componentArea,
			PackedColor color,
			boolean editable,
			double mouseX,
			double mouseY
		) {
			ColorComponentAreas colorAreas = createColorComponentAreas(componentArea);
			ImmutableRect2i swatchArea = colorAreas.swatchArea();
			ImmutableRect2i hexArea = colorAreas.hexArea();
			ColorSwatch.draw(
				guiGraphics,
				new Rect2i(swatchArea.getX(), swatchArea.getY(), swatchArea.getWidth(), swatchArea.getHeight()),
				color
			);
			if (editable) {
				drawButtonBackground(guiGraphics, textures, hexArea, true, hexArea.contains(mouseX, mouseY));
			}
			ImmutableRect2i textArea = hexArea.cropLeft(KEY_VALUE_TEXT_PADDING).cropRight(KEY_VALUE_TEXT_PADDING);
			ConfigEntryWidget.drawFittedText(
				guiGraphics,
				font,
				Component.literal(ColorSwatch.formatHex(color)),
				textArea,
				getValueTextColor(),
				false
			);
		}

		private int getValueTextColor() {
			if (selected) {
				return getConfiguredTextColor();
			}
			return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_UNUSED_ROW_TEXT);
		}

		private int getBackgroundColor(boolean floating, boolean dropTarget, boolean recentlyMoved) {
			if (floating) {
				return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_ROW_DRAG_FLOAT_BACKGROUND);
			}
			if (dropTarget) {
				return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_ROW_DROP_TARGET);
			}
			if (recentlyMoved) {
				return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_ROW_MOVED_BACKGROUND);
			}
			if (selected) {
				return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_ROW_BACKGROUND);
			}
			return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_UNUSED_ROW_BACKGROUND);
		}

		private void drawMovedHighlight(GuiGraphics guiGraphics, ImmutableRect2i rowArea) {
			int x = rowArea.getX();
			int y = rowArea.getY();
			int bottom = y + rowArea.getHeight();
			guiGraphics.fill(x, y, x + 2, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_ROW_MOVED_ACCENT));
		}

		private void drawFloatingHighlight(GuiGraphics guiGraphics, ImmutableRect2i rowArea) {
			int x = rowArea.getX();
			int y = rowArea.getY();
			int right = x + rowArea.getWidth();
			int bottom = y + rowArea.getHeight();
			int borderColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_ROW_DRAG_FLOAT_BORDER);
			guiGraphics.fill(x, y, right, y + 1, borderColor);
			guiGraphics.fill(x, bottom - 1, right, bottom, borderColor);
			guiGraphics.fill(x, y, x + 2, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_ROW_DRAG_FLOAT_ACCENT));
			guiGraphics.fill(right - 1, y, right, bottom, borderColor);
		}

		private void drawMoveButton(
			GuiGraphics guiGraphics,
			ConfigTextures textures,
			ImmutableRect2i buttonArea,
			ConfigButtonIcon icon,
			boolean active,
			double mouseX,
			double mouseY
		) {
			boolean hovered = active && buttonArea.contains(mouseX, mouseY);
			ConfigEntryWidget.drawButtonBackground(guiGraphics, textures, buttonArea, active, hovered);
			icon.draw(guiGraphics, buttonArea, active);
		}

		private void drawDeleteButton(GuiGraphics guiGraphics, ConfigTextures textures, double mouseX, double mouseY) {
			boolean deleteHovered = deleteArea.contains(mouseX, mouseY);
			ConfigEntryWidget.drawButtonBackground(guiGraphics, textures, deleteArea, true, deleteHovered);
			ConfigButtonIcon.X.draw(guiGraphics, deleteArea, true);
		}

		private void drawResetButton(GuiGraphics guiGraphics, ConfigTextures textures, double mouseX, double mouseY) {
			boolean active = canResetComponentValue(value);
			boolean hovered = active && resetArea.contains(mouseX, mouseY);
			ConfigEntryWidget.drawButtonBackground(guiGraphics, textures, resetArea, active, hovered);
			ConfigResetIcon.draw(guiGraphics, resetArea, active);
		}

		private void drawAddButton(GuiGraphics guiGraphics, ConfigTextures textures, double mouseX, double mouseY) {
			boolean addHovered = addArea.contains(mouseX, mouseY);
			ConfigEntryWidget.drawButtonBackground(guiGraphics, textures, addArea, true, addHovered);
			ConfigButtonIcon.ADD.draw(guiGraphics, addArea, true);
		}

	}

	private final class ComponentEditSession {
		private final int rowIndex;
		private String editText;

		private ComponentEditSession(int rowIndex, String editText) {
			this.rowIndex = rowIndex;
			this.editText = editText;
		}
	}

	private final class ListComponentScreenValue implements IConfigScreenValue<Object>, IConfigLocalizedValue {
		private final int rowIndex;
		private final List<Consumer<Object>> listeners = new ArrayList<>();

		private ListComponentScreenValue(int rowIndex) {
			this.rowIndex = rowIndex;
		}

		@Override
		public String getName() {
			return configValue.getName() + ".value";
		}

		@Override
		public String getLocalizationKey() {
			return configValue.getLocalizationKey() + ".value";
		}

		@Override
		public Component getLocalizedName() {
			return ConfigValueLocalization.getName(configValue);
		}

		@Override
		public Component getLocalizedDescription() {
			return ConfigValueLocalization.getDescription(configValue);
		}

		@Override
		public Object getValue() {
			return getComponent(ListConfigEntry.this.getValue(), rowIndex);
		}

		@Override
		public Object getDefaultValue() {
			if (keyValueSerializer == null && isIndexValid(configValue.getDefaultValue(), rowIndex)) {
				return configValue.getDefaultValue().get(rowIndex);
			}
			if (!isIndexValid(ListConfigEntry.this.getValue(), rowIndex)) {
				return getValue();
			}
			T entry = ListConfigEntry.this.getValue().get(rowIndex);
			return getDefaultComponentValue(entry).orElseGet(this::getValue);
		}

		@Override
		public boolean set(Object value) {
			IConfigValueSerializer<Object> componentSerializer = getComponentSerializer();
			if (!componentSerializer.isValid(value)) {
				throw new IllegalArgumentException(
					"Invalid list component value '%s'. %s".formatted(value, componentSerializer.getValidValuesDescription())
				);
			}
			if (!isIndexValid(ListConfigEntry.this.getValue(), rowIndex)) {
				throw new IllegalStateException("List row is no longer available");
			}
			if (Objects.equals(getValue(), value)) {
				return false;
			}
			if (!replaceComponent(rowIndex, value)) {
				throw new IllegalArgumentException("The list does not accept this component value: " + value);
			}
			Object newValue = getValue();
			for (Consumer<Object> listener : List.copyOf(listeners)) {
				try {
					listener.accept(newValue);
				} catch (RuntimeException exception) {
					LOGGER.error("List component config value listener failed for {}.", getName(), exception);
				}
			}
			return true;
		}

		@Override
		public Runnable addListener(Consumer<Object> listener) {
			Consumer<Object> checkedListener = Objects.requireNonNull(listener, "listener");
			listeners.add(checkedListener);
			return () -> listeners.remove(checkedListener);
		}

		@Override
		public ConfigValueApplyMode getApplyMode() {
			return configValue.getApplyMode();
		}

		@Override
		public ConfigValueRestartRequirement getRestartRequirement() {
			return configValue.getRestartRequirement();
		}

		@Override
		public IConfigValueSerializer<Object> getSerializer() {
			return getComponentSerializer();
		}

		private Object getComponent(List<T> entries, int index) {
			if (!isIndexValid(entries, index)) {
				throw new IllegalStateException("List row is no longer available");
			}
			return getRowComponent(entries.get(index));
		}
	}

	private record ListMove<T>(T value, int oldIndex, int newIndex) {
	}

	private record ColorComponentAreas(ImmutableRect2i swatchArea, ImmutableRect2i hexArea) {
	}
}
