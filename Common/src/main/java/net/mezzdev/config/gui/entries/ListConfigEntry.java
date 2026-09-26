package net.mezzdev.config.gui.entries;

import com.mojang.blaze3d.platform.InputConstants;
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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

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
	private static final int ADD_VALUE_TOP_GAP = 2;
	private static final int VALUE_ROW_HORIZONTAL_PADDING = 4;
	private static final int KEY_VALUE_COLUMN_GAP = 4;
	private static final int KEY_VALUE_TEXT_PADDING = 3;
	private static final int COLOR_COMPONENT_SWATCH_SIZE = 18;
	private static final int COLOR_COMPONENT_GAP = 3;
	private static final int MAX_COMPONENT_EDIT_TEXT_LENGTH = 512;
	private static final int ORDERED_ROW_DRAG_FLOAT_Z_OFFSET = 200;
	private static final int ORDERED_ROW_MAX_HORIZONTAL_DRAG_OFFSET = 24;
	private static final List<String> NEW_VALUE_SERIALIZED_SEEDS = List.of(
		"",
		"0",
		"1",
		"false",
		"true",
		"new",
		"0x000000",
		"[]"
	);
	private final List<ListValueRow> valueRows = new ArrayList<>();
	private List<T> availableValues = List.of();
	private final List<T> allValidValues;
	private final IConfigValueSerializer<T> elementSerializer;
	@Nullable
	private final KeyValueElementSerializerAdapter<T> keyValueSerializer;
	private final Consumer<ConfigPopupSelector> valueSelectorOpener;
	private final Runnable layoutUpdater;
	private final boolean ordered;
	private final boolean allowsRemovingValues;
	private final boolean allowsTypedInput;
	private final boolean allowsEditingValues;
	private ImmutableRect2i headerArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i valueGroupArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i addValueRowArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i addValueButtonArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i addValueTextArea = ImmutableRect2i.EMPTY;
	@Nullable
	private DragSession dragSession;
	@Nullable
	private T recentlyMovedValue;
	private int recentlyMovedIndex = -1;
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
		this.allowsEditingValues = allValidValues.isEmpty();
		this.allValidValues = allValidValues
			.map(List::copyOf)
			.orElse(List.of());
		rebuildRows();
	}

	private Optional<T> getInitialAddValue() {
		Optional<T> existingValue = List.of(getValue(), configValue.getDefaultValue()).stream()
			.flatMap(List::stream)
			.filter(Objects::nonNull)
			.filter(elementSerializer::isValid)
			.filter(this::canAddValue)
			.findFirst();
		if (existingValue.isPresent()) {
			return existingValue;
		}
		if (keyValueSerializer != null) {
			List<Object> keys = getInitialComponentValues(keyValueSerializer.getKeySerializer());
			List<Object> values = getInitialComponentValues(keyValueSerializer.getValueSerializer());
			for (Object key : keys) {
				for (Object value : values) {
					Optional<T> entry = keyValueSerializer.createEntry(key, value)
						.filter(this::canAddValue);
					if (entry.isPresent()) {
						return entry;
					}
				}
			}
		}
		for (String serializedSeed : NEW_VALUE_SERIALIZED_SEEDS) {
			Optional<T> value = deserializeNewValueSeed(serializedSeed);
			if (value.isPresent()) {
				return value;
			}
		}
		for (int i = 2; i <= 100; i++) {
			Optional<T> value = deserializeNewValueSeed("new " + i);
			if (value.isPresent()) {
				return value;
			}
		}
		return elementSerializer.getRange()
			.map(range -> range.min())
			.filter(elementSerializer::isValid)
			.filter(this::canAddValue);
	}

	private static List<Object> getInitialComponentValues(IConfigValueSerializer<Object> serializer) {
		List<Object> values = new ArrayList<>();
		serializer.getAllValidValues()
			.ifPresent(allValidValues -> allValidValues.stream()
				.filter(serializer::isValid)
				.forEach(value -> addDistinct(values, value)));
		for (String serializedSeed : NEW_VALUE_SERIALIZED_SEEDS) {
			addDeserializedComponentValue(values, serializer, serializedSeed);
		}
		for (int i = 2; i <= 100; i++) {
			addDeserializedComponentValue(values, serializer, "new " + i);
		}
		serializer.getRange()
			.map(range -> range.min())
			.filter(serializer::isValid)
			.ifPresent(value -> addDistinct(values, value));
		return List.copyOf(values);
	}

	private static void addDeserializedComponentValue(
		List<Object> values,
		IConfigValueSerializer<Object> serializer,
		String serializedValue
	) {
		IDeserializeResult<Object> result = serializer.deserialize(serializedValue);
		if (result.getDiagnostics().isEmpty()) {
			result.getResult()
				.filter(serializer::isValid)
				.ifPresent(value -> addDistinct(values, value));
		}
	}

	private static void addDistinct(List<Object> values, Object value) {
		if (!values.contains(value)) {
			values.add(value);
		}
	}

	private Optional<T> deserializeNewValueSeed(String serializedSeed) {
		IDeserializeResult<T> result = elementSerializer.deserialize(serializedSeed);
		if (!result.getDiagnostics().isEmpty()) {
			return Optional.empty();
		}
		return result.getResult()
			.filter(elementSerializer::isValid)
			.filter(this::canAddValue);
	}

	private void rebuildRows() {
		valueRows.clear();
		List<T> displayedValues = getValue();
		for (int i = 0; i < displayedValues.size(); i++) {
			valueRows.add(new ListValueRow(displayedValues.get(i), i));
		}
		Set<T> current = new HashSet<>(displayedValues);
		availableValues = allValidValues.stream()
			.filter(value -> !current.contains(value))
			.filter(elementSerializer::isValid)
			.filter(this::canAddValue)
			.distinct()
			.toList();
	}

	private boolean showsAddValueRow() {
		return allowsTypedInput || !availableValues.isEmpty();
	}

	@Override
	public int getHeight() {
		if (valueRows.isEmpty() && !showsAddValueRow()) {
			return super.getHeight();
		}
		return super.getHeight() + VALUE_GROUP_TOP_GAP + getValueGroupContentHeight() + VALUE_GROUP_BOTTOM_PADDING;
	}

	private int getValueGroupContentHeight() {
		int entryRowHeight = getEntryRowHeight();
		int height = valueRows.size() * entryRowHeight;
		if (showsAddValueRow()) {
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

	private int getAddValueTopGap() {
		if (showsAddValueRow() && !valueRows.isEmpty()) {
			return ADD_VALUE_TOP_GAP;
		}
		return 0;
	}

	private void updateAddValueBounds(int y) {
		if (!showsAddValueRow() || valueGroupArea.isEmpty()) {
			addValueRowArea = ImmutableRect2i.EMPTY;
			addValueButtonArea = ImmutableRect2i.EMPTY;
			addValueTextArea = ImmutableRect2i.EMPTY;
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
		int textX = addValueRowArea.getX() + VALUE_ROW_HORIZONTAL_PADDING;
		addValueTextArea = new ImmutableRect2i(
			textX,
			addValueRowArea.getY() + 1,
			Math.max(0, addValueButtonArea.getX() - BUTTON_GAP - textX),
			Math.max(0, addValueRowArea.getHeight() - 2)
		);
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
		if (isVisible(headerArea)) {
			drawName(guiGraphics);
		}

		drawValueGroup(guiGraphics, valueGroupArea);
		DragSession dragSession = this.dragSession;
		if (dragSession == null) {
			for (ListValueRow row : getVisibleRows(valueRows, 0)) {
				row.draw(guiGraphics, mouseX, mouseY, false, isRecentlyMoved(row));
			}
		} else {
			dragSession.drawReorderedRows(guiGraphics);
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

	private void drawAddValueRow(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
		if (!showsAddValueRow() || !isVisible(addValueRowArea)) {
			return;
		}
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
		if (componentEditSession != null && componentEditSession.addingValue) {
			drawButtonBackground(guiGraphics, textures, addValueTextArea, true, true);
			int textColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_TEXT);
			if (!componentEditSession.editText.isEmpty() && getEditedEntry().isEmpty()) {
				textColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_INVALID_TEXT);
			}
			ConfigEntryWidget.drawFittedText(
				guiGraphics,
				Minecraft.getInstance().font,
				Component.literal(componentEditSession.editText + "_"),
				addValueTextArea.cropLeft(KEY_VALUE_TEXT_PADDING).cropRight(KEY_VALUE_TEXT_PADDING),
				textColor,
				false
			);
		}
		boolean addHovered = addValueButtonArea.contains(mouseX, mouseY);
		drawButtonBackground(guiGraphics, textures, addValueButtonArea, true, addHovered);
		ConfigButtonIcon.ADD.draw(guiGraphics, addValueButtonArea, true);
	}

	private void drawValueGroup(GuiGraphicsExtractor guiGraphics, ImmutableRect2i groupArea) {
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

	private static void fillRowHover(GuiGraphicsExtractor guiGraphics, ImmutableRect2i rowArea) {
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
		if (showsAddValueRow() && addValueRowArea.contains(mouseX, mouseY)) {
			List<Component> description = List.of();
			if (allowsTypedInput) {
				description = List.of(getAddValueDescription());
			}
			return new ConfigInfo(
				Component.translatable("mezz_config.config.screen.add"),
				description
			);
		}
		return null;
	}

	private Component getAddValueDescription() {
		if (elementSerializer.getRange().isEmpty()) {
			Component description = ConfigValueLocalization.getDescription(configValue);
			if (!description.getString().isBlank()) {
				return description;
			}
		}
		return ConfigNumberInfo.getValidValuesDescription(elementSerializer);
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
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (componentEditSession != null) {
			return keyPressedComponentEdit(keyCode);
		}
		return false;
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		return false;
	}

	private boolean keyPressedComponentEdit(int keyCode) {
		if (ConfigInputUtil.isSelectAll(keyCode)) {
			componentEditSession.replaceOnInput = true;
			return true;
		}
		if (ConfigInputUtil.isPaste(keyCode)) {
			appendComponentEditText(Minecraft.getInstance().keyboardHandler.getClipboard());
			return true;
		}
		if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
			if (getEditedEntry().isPresent()) {
				commitComponentEdit();
			}
			return true;
		}
		if (keyCode == InputConstants.KEY_TAB) {
			commitComponentEdit();
			return true;
		}
		if (keyCode == InputConstants.KEY_ESCAPE) {
			cancelComponentEdit();
			return true;
		}
		if (keyCode == InputConstants.KEY_BACKSPACE && !componentEditSession.editText.isEmpty()) {
			String editText = componentEditSession.editText;
			if (componentEditSession.replaceOnInput) {
				componentEditSession.editText = "";
			} else {
				componentEditSession.editText = editText.substring(0, editText.length() - 1);
			}
			componentEditSession.replaceOnInput = false;
			return true;
		}
		if (keyCode == InputConstants.KEY_DELETE) {
			componentEditSession.editText = "";
			componentEditSession.replaceOnInput = false;
			return true;
		}
		return true;
	}

	private void appendComponentEditCharacter(char codePoint) {
		if (componentEditSession == null ||
			!StringUtil.isAllowedChatCharacter(codePoint)
		) {
			return;
		}
		if (componentEditSession.replaceOnInput) {
			componentEditSession.editText = "";
			componentEditSession.replaceOnInput = false;
		}
		if (componentEditSession.editText.length() >= MAX_COMPONENT_EDIT_TEXT_LENGTH) {
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
		return componentEditSession != null;
	}

	@Override
	public boolean isCapturingTextInput() {
		return componentEditSession != null;
	}

	@Override
	public void unfocus() {
		commitComponentEdit();
	}

	@Override
	protected void onValueChanged() {
		cancelComponentEdit();
		rebuildRows();
		layoutUpdater.run();
	}

	private boolean addValue(T value) {
		clearRecentlyMovedValue();
		List<T> current = new ArrayList<>(getValue());
		current.add(value);
		return setValue(current);
	}

	private boolean onAddValueMouseClicked(UserInput input) {
		if (!showsAddValueRow()) {
			return false;
		}
		if (componentEditSession != null && componentEditSession.addingValue &&
			addValueTextArea.contains(input.getMouseX(), input.getMouseY())
		) {
			return true;
		}
		ImmutableRect2i addArea = addValueRowArea;
		if (allowsTypedInput) {
			addArea = addValueButtonArea;
		}
		if (!addArea.contains(input.getMouseX(), input.getMouseY())) {
			return false;
		}
		if (!input.isSimulate()) {
			commitComponentEdit();
			startAddingValue();
		}
		return true;
	}

	private void startAddingValue() {
		if (!allowsTypedInput) {
			openAddValuePopup();
			return;
		}
		Optional<T> initialValue = getInitialAddValue();
		if (keyValueSerializer == null && initialValue.filter(PackedColor.class::isInstance).isEmpty()) {
			componentEditSession = new ComponentEditSession(-1, false, "", true);
			return;
		}
		if (initialValue.isEmpty() || !addValue(initialValue.get())) {
			return;
		}
		int rowIndex = getValue().size() - 1;
		boolean keyComponent = keyValueSerializer != null;
		if (usesComponentPopup(rowIndex, keyComponent)) {
			openComponentPopup(rowIndex, false, keyComponent);
		} else {
			startComponentEdit(rowIndex, keyComponent);
			componentEditSession.replaceOnInput = true;
		}
	}

	private void openAddValuePopup() {
		if (availableValues.isEmpty()) {
			return;
		}
		AddValueScreenValue addConfigValue = new AddValueScreenValue(availableValues.getFirst());
		ConfigValueSelector<T> popup = new ConfigValueSelector<>(addConfigValue, availableValues, null);
		valueSelectorOpener.accept(new ConfigValuePopupSelector<>(
			addConfigValue,
			popup,
			() -> addValueRowArea,
			this::hasPendingChange,
			addConfigValue::set
		));
	}

	private boolean canAddValue(T value) {
		List<T> current = new ArrayList<>(getValue());
		current.add(value);
		return configValue.getSerializer()
			.isValid(current);
	}

	private boolean onRowValueMouseClicked(ListValueRow row, UserInput input) {
		if (!allowsEditingValues || !isIndexValid(getValue(), row.index)) {
			return false;
		}
		if (row.hexColor != null && row.componentColorSwatchArea.contains(input.getMouseX(), input.getMouseY())) {
			if (!input.isSimulate()) {
				commitComponentEdit();
				openComponentPopup(row.index, false, false);
			}
			return true;
		}
		boolean keyComponent = keyValueSerializer != null && row.keyArea.contains(input.getMouseX(), input.getMouseY());
		Object component = getRowComponent(getValue().get(row.index), keyComponent);
		boolean focusHexInput = false;
		if (component instanceof PackedColor) {
			if (keyComponent) {
				if (!row.keyArea.contains(input.getMouseX(), input.getMouseY())) {
					return false;
				}
			} else {
				focusHexInput = row.componentHexArea.contains(input.getMouseX(), input.getMouseY());
				boolean swatchClicked = row.componentColorSwatchArea.contains(input.getMouseX(), input.getMouseY());
				if (!focusHexInput && !swatchClicked) {
					return false;
				}
			}
		} else if (keyComponent) {
			if (!row.keyArea.contains(input.getMouseX(), input.getMouseY())) {
				return false;
			}
		} else if (!row.componentValueArea.contains(input.getMouseX(), input.getMouseY())) {
			return false;
		}
		if (!input.isSimulate()) {
			commitComponentEdit();
			if (usesComponentPopup(row.index, keyComponent)) {
				openComponentPopup(row.index, focusHexInput, keyComponent);
			} else {
				startComponentEdit(row.index, keyComponent);
			}
		}
		return true;
	}

	private boolean usesComponentPopup(int rowIndex, boolean keyComponent) {
		if (!isIndexValid(getValue(), rowIndex)) {
			return false;
		}
		T entry = getValue().get(rowIndex);
		Object component = getRowComponent(entry, keyComponent);
		return component instanceof PackedColor || getComponentSerializer(keyComponent).getAllValidValues().isPresent();
	}

	private void openComponentPopup(int rowIndex, boolean focusHexInput, boolean keyComponent) {
		if (!isIndexValid(getValue(), rowIndex)) {
			return;
		}
		ListComponentScreenValue componentConfigValue = new ListComponentScreenValue(rowIndex, keyComponent);
		Object component = componentConfigValue.getValue();
		IConfigValuePopup<Object> popup = createComponentPopup(componentConfigValue, component, focusHexInput, rowIndex, keyComponent);
		if (popup == null) {
			return;
		}
		ConfigValuePopupSelector<Object> selector = new ConfigValuePopupSelector<>(
			componentConfigValue,
			popup,
			() -> getComponentArea(rowIndex, focusHexInput, keyComponent),
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
		int rowIndex,
		boolean keyComponent
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
					.filter(value -> getEditedComponent(rowIndex, value, keyComponent).isPresent())
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

	private ImmutableRect2i getComponentArea(int rowIndex, boolean focusHexInput, boolean keyComponent) {
		if (!isIndexValid(valueRows, rowIndex)) {
			return ImmutableRect2i.EMPTY;
		}
		ListValueRow row = valueRows.get(rowIndex);
		if (keyComponent) {
			return row.keyArea;
		}
		if (!row.componentColorSwatchArea.isEmpty()) {
			if (focusHexInput) {
				return row.componentHexArea;
			}
			return row.componentColorSwatchArea;
		}
		return row.componentValueArea;
	}

	private void startComponentEdit(int rowIndex, boolean keyComponent) {
		if (!isIndexValid(getValue(), rowIndex)) {
			return;
		}
		T entry = getValue().get(rowIndex);
		Object component = getRowComponent(entry, keyComponent);
		IConfigValueSerializer<Object> componentSerializer = getComponentSerializer(keyComponent);
		startComponentEdit(rowIndex, keyComponent, componentSerializer.serialize(component));
	}

	private void startComponentEdit(int rowIndex, boolean keyComponent, String editText) {
		componentEditSession = new ComponentEditSession(rowIndex, keyComponent, editText, false);
	}

	private Optional<T> getEditedEntry() {
		if (componentEditSession == null) {
			return Optional.empty();
		}
		if (componentEditSession.addingValue) {
			if (componentEditSession.editText.isEmpty()) {
				return Optional.empty();
			}
		} else if (!isIndexValid(getValue(), componentEditSession.rowIndex)) {
			return Optional.empty();
		}
		Optional<T> editedEntry;
		if (keyValueSerializer == null) {
			IDeserializeResult<T> result = elementSerializer.deserialize(componentEditSession.editText);
			if (!result.getDiagnostics().isEmpty()) {
				return Optional.empty();
			}
			editedEntry = result.getResult().filter(elementSerializer::isValid);
		} else {
			T entry = getValue().get(componentEditSession.rowIndex);
			if (componentEditSession.keyComponent) {
				editedEntry = keyValueSerializer.withSerializedKey(entry, componentEditSession.editText);
			} else {
				editedEntry = keyValueSerializer.withSerializedValue(entry, componentEditSession.editText);
			}
		}
		if (componentEditSession.addingValue) {
			return editedEntry.filter(this::canAddValue);
		}
		return editedEntry.filter(value -> canReplaceEntry(componentEditSession.rowIndex, value));
	}

	private void commitComponentEdit() {
		if (componentEditSession == null) {
			return;
		}
		int rowIndex = componentEditSession.rowIndex;
		boolean addingValue = componentEditSession.addingValue;
		Optional<T> editedEntry = getEditedEntry();
		componentEditSession = null;
		if (addingValue) {
			editedEntry.ifPresent(this::addValue);
		} else {
			editedEntry.ifPresent(entry -> replaceEntry(rowIndex, entry));
		}
	}

	private void cancelComponentEdit() {
		componentEditSession = null;
	}

	private boolean replaceComponent(int rowIndex, Object component, boolean keyComponent) {
		return getEditedComponent(rowIndex, component, keyComponent)
			.map(value -> replaceEntry(rowIndex, value))
			.orElse(false);
	}

	@SuppressWarnings("unchecked")
	private Optional<T> getEditedComponent(int rowIndex, Object component, boolean keyComponent) {
		if (!isIndexValid(getValue(), rowIndex) || !getComponentSerializer(keyComponent).isValid(component)) {
			return Optional.empty();
		}
		Optional<T> editedEntry;
		if (keyValueSerializer == null) {
			editedEntry = Optional.of((T) component);
		} else if (keyComponent) {
			editedEntry = keyValueSerializer.withKey(getValue().get(rowIndex), component);
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

	private boolean hasRowResetButtons() {
		return keyValueSerializer != null && !configValue.getDefaultValue().isEmpty();
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
	private IConfigValueSerializer<Object> getComponentSerializer(boolean keyComponent) {
		if (keyValueSerializer == null) {
			return (IConfigValueSerializer<Object>) elementSerializer;
		}
		if (keyComponent) {
			return keyValueSerializer.getKeySerializer();
		}
		return keyValueSerializer.getValueSerializer();
	}

	private Object getRowComponent(T entry, boolean keyComponent) {
		if (keyValueSerializer == null) {
			return entry;
		}
		if (keyComponent) {
			return keyValueSerializer.getKey(entry);
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
		return row.index == recentlyMovedIndex &&
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
		public Optional<ConfigInputHandler> handleUserInput(@Nullable Screen screen, UserInput input) {
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
				if (onRowValueMouseClicked(row, input)) {
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
		public Optional<ConfigInputHandler> handleUserInput(@Nullable Screen screen, UserInput input) {
			if (active && ConfigInputUtil.isLeftClick(input)) {
				moveToMouse(input.getMouseX(), input.getMouseY());
				commitMove();
			}
			stop();
			return Optional.empty();
		}

		@Override
		public Optional<ConfigInputHandler> handleMouseDragged(
			@Nullable Screen screen,
			double mouseX,
			double mouseY,
			int button,
			double dragX,
			double dragY
		) {
			if (button != InputConstants.MOUSE_BUTTON_LEFT) {
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

		@Override
		public boolean allowsContentAutoScroll() {
			return true;
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
			return active && row.index == sourceIndex;
		}

		public void drawReorderedRows(GuiGraphicsExtractor guiGraphics) {
			// Reordering shifts neighboring rows by one row height, so include that margin.
			for (ListValueRow row : getVisibleRows(valueRows, 1)) {
				if (!isDragging(row)) {
					row.drawDuringDrag(guiGraphics, getVisualArea(row));
				}
			}
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

		public void drawFloatingRow(GuiGraphicsExtractor guiGraphics) {
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
		ImmutableRect2i area = ImmutableRect2i.EMPTY;
		ImmutableRect2i keyArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i componentValueArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i componentColorSwatchArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i componentHexArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i moveUpArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i moveDownArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i resetArea = ImmutableRect2i.EMPTY;
		ImmutableRect2i deleteArea = ImmutableRect2i.EMPTY;

		ListValueRow(T value, int index) {
			this.value = value;
			this.hexColor = HexColorString.parse(getRowComponent(value, false)).orElse(null);
			this.index = index;
		}

		void updateBounds(ImmutableRect2i area) {
			this.area = area;
			int cy = area.getY() + (area.getHeight() - BUTTON_SIZE) / 2;
			moveUpArea = ImmutableRect2i.EMPTY;
			moveDownArea = ImmutableRect2i.EMPTY;
			resetArea = ImmutableRect2i.EMPTY;
			deleteArea = ImmutableRect2i.EMPTY;
			int buttonOffset = 0;
			if (allowsRemovingValues) {
				deleteArea = createButtonArea(area, cy, buttonOffset++);
			}
			if (hasRowResetButtons()) {
				resetArea = createButtonArea(area, cy, buttonOffset++);
			}
			if (ordered) {
				moveDownArea = createButtonArea(area, cy, buttonOffset++);
				moveUpArea = createButtonArea(area, cy, buttonOffset);
			}
			updateComponentBounds(area);
		}

		private void updateComponentBounds(ImmutableRect2i rowArea) {
			if (keyValueSerializer == null) {
				keyArea = ImmutableRect2i.EMPTY;
				componentValueArea = getContentArea(rowArea);
				componentColorSwatchArea = ImmutableRect2i.EMPTY;
				componentHexArea = ImmutableRect2i.EMPTY;
				if (value instanceof PackedColor || hexColor != null) {
					ColorComponentAreas colorAreas = createColorComponentAreas(componentValueArea);
					componentColorSwatchArea = colorAreas.swatchArea();
					componentHexArea = colorAreas.hexArea();
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
			return ordered && index > 0;
		}

		boolean canMoveDown() {
			return ordered && index < valueRows.size() - 1;
		}

		boolean canResetValue() {
			return canResetComponentValue(value);
		}

		boolean isControlMouseOver(double mouseX, double mouseY) {
			return moveUpArea.contains(mouseX, mouseY) ||
				moveDownArea.contains(mouseX, mouseY) ||
				resetArea.contains(mouseX, mouseY) ||
				deleteArea.contains(mouseX, mouseY) ||
				(allowsEditingValues && (keyArea.contains(mouseX, mouseY) ||
					componentColorSwatchArea.contains(mouseX, mouseY) ||
					componentValueArea.contains(mouseX, mouseY)));
		}

		boolean canStartDrag(double mouseX, double mouseY) {
			return ordered &&
				area.contains(mouseX, mouseY) &&
				!isControlMouseOver(mouseX, mouseY);
		}

		private int getControlsWidth() {
			int buttonCount = 0;
			if (ordered) {
				buttonCount += 2;
			}
			if (allowsRemovingValues) {
				buttonCount++;
			}
			if (hasRowResetButtons()) {
				buttonCount++;
			}
			return buttonCount * BUTTON_SIZE + Math.max(0, buttonCount - 1) * BUTTON_GAP;
		}

		void draw(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY, boolean dropTarget, boolean recentlyMoved) {
			draw(guiGraphics, area, mouseX, mouseY, true, false, dropTarget, recentlyMoved);
		}

		void drawDuringDrag(GuiGraphicsExtractor guiGraphics, ImmutableRect2i rowArea) {
			draw(guiGraphics, rowArea, 0, 0, false, false, false, false);
		}

		void drawDragGap(GuiGraphicsExtractor guiGraphics, boolean dropTarget) {
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

		void drawFloating(GuiGraphicsExtractor guiGraphics, int x, int y) {
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
			GuiGraphicsExtractor guiGraphics,
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
				drawSingleValue(guiGraphics, font, textures, rowArea, mouseX, mouseY, drawControls);
			} else {
				drawKeyValue(guiGraphics, font, textures, rowArea, mouseX, mouseY, drawControls);
			}

			if (drawControls) {
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
			}
		}

		private void drawSingleValue(
			GuiGraphicsExtractor guiGraphics,
			Font font,
			ConfigTextures textures,
			ImmutableRect2i rowArea,
			double mouseX,
			double mouseY,
			boolean interactive
		) {
			ImmutableRect2i contentArea = getContentArea(rowArea);
			if (allowsEditingValues && interactive) {
				drawButtonBackground(guiGraphics, textures, contentArea, true, contentArea.contains(mouseX, mouseY));
			}
			ImmutableRect2i paddedContentArea = contentArea
				.cropLeft(KEY_VALUE_TEXT_PADDING)
				.cropRight(KEY_VALUE_TEXT_PADDING);
			int textX = paddedContentArea.getX();
			int iconY = rowArea.getY() + (rowArea.getHeight() - ConfigValueIcon.ICON_SIZE) / 2;
			if (value instanceof PackedColor color) {
				ImmutableRect2i swatch = createColorComponentAreas(paddedContentArea).swatchArea();
				ColorSwatch.draw(guiGraphics, swatch, color);
				textX += swatch.getWidth() + COLOR_COMPONENT_GAP;
			} else if (hexColor != null) {
				ImmutableRect2i swatch = createColorComponentAreas(paddedContentArea).swatchArea();
				ColorSwatch.draw(guiGraphics, swatch, hexColor.color());
				textX += swatch.getWidth() + COLOR_COMPONENT_GAP;
			} else {
				ConfigValueIcon.draw(guiGraphics, elementSerializer, value, textX, iconY);
				textX += ConfigValueIcon.getTextOffset(elementSerializer, value);
			}
			ImmutableRect2i textArea = new ImmutableRect2i(
				textX,
				contentArea.getY(),
				Math.max(0, paddedContentArea.getX() + paddedContentArea.getWidth() - textX),
				contentArea.getHeight()
			);
			Component valueName;
			int textColor = getValueTextColor();
			ComponentEditSession editSession = componentEditSession;
			if (editSession != null && editSession.rowIndex == index) {
				valueName = Component.literal(editSession.editText + "_");
				if (getEditedEntry().isEmpty()) {
					textColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_INVALID_TEXT);
				}
			} else {
				valueName = ConfigValueLocalization.getValueName(elementSerializer, configValue.getLocalizationKey(), value);
			}
			ConfigEntryWidget.drawFittedText(guiGraphics, font, valueName, textArea, textColor, false);
		}

		private void drawKeyValue(
			GuiGraphicsExtractor guiGraphics,
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
			boolean editable = allowsEditingValues && interactive;
			drawKeyValueComponent(
				guiGraphics,
				font,
				textures,
				drawnKeyArea,
				keyValueSerializer.getKeySerializer(),
				keyValueSerializer.getKey(value),
				false,
				editable,
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
			GuiGraphicsExtractor guiGraphics,
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
			if (editSession != null && editSession.rowIndex == index && editSession.keyComponent != valueComponent) {
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
			GuiGraphicsExtractor guiGraphics,
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
			return getConfiguredTextColor();
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
			return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_ROW_BACKGROUND);
		}

		private void drawMovedHighlight(GuiGraphicsExtractor guiGraphics, ImmutableRect2i rowArea) {
			int x = rowArea.getX();
			int y = rowArea.getY();
			int bottom = y + rowArea.getHeight();
			guiGraphics.fill(x, y, x + 2, bottom, ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.LIST_ORDERED_ROW_MOVED_ACCENT));
		}

		private void drawFloatingHighlight(GuiGraphicsExtractor guiGraphics, ImmutableRect2i rowArea) {
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
			GuiGraphicsExtractor guiGraphics,
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

		private void drawDeleteButton(GuiGraphicsExtractor guiGraphics, ConfigTextures textures, double mouseX, double mouseY) {
			boolean deleteHovered = deleteArea.contains(mouseX, mouseY);
			ConfigEntryWidget.drawButtonBackground(guiGraphics, textures, deleteArea, true, deleteHovered);
			ConfigButtonIcon.X.draw(guiGraphics, deleteArea, true);
		}

		private void drawResetButton(GuiGraphicsExtractor guiGraphics, ConfigTextures textures, double mouseX, double mouseY) {
			boolean active = canResetComponentValue(value);
			boolean hovered = active && resetArea.contains(mouseX, mouseY);
			ConfigEntryWidget.drawButtonBackground(guiGraphics, textures, resetArea, active, hovered);
			ConfigResetIcon.draw(guiGraphics, resetArea, active);
		}

	}

	private final class ComponentEditSession {
		private final int rowIndex;
		private final boolean keyComponent;
		private final boolean addingValue;
		private String editText;
		private boolean replaceOnInput;

		private ComponentEditSession(int rowIndex, boolean keyComponent, String editText, boolean addingValue) {
			this.rowIndex = rowIndex;
			this.keyComponent = keyComponent;
			this.editText = editText;
			this.addingValue = addingValue;
		}
	}

	private final class AddValueScreenValue implements IConfigScreenValue<T>, IConfigLocalizedValue {
		private T value;
		private final List<Consumer<T>> listeners = new ArrayList<>();

		private AddValueScreenValue(T value) {
			this.value = value;
		}

		@Override
		public String getName() {
			return configValue.getName();
		}

		@Override
		public String getLocalizationKey() {
			return configValue.getLocalizationKey();
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
		public T getValue() {
			return value;
		}

		@Override
		public T getDefaultValue() {
			return value;
		}

		@Override
		public boolean set(T value) {
			if (!availableValues.contains(value) || !addValue(value)) {
				return false;
			}
			this.value = value;
			for (Consumer<T> listener : List.copyOf(listeners)) {
				listener.accept(value);
			}
			return true;
		}

		@Override
		public Runnable addListener(Consumer<T> listener) {
			listeners.add(listener);
			return () -> listeners.remove(listener);
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
		public IConfigValueSerializer<T> getSerializer() {
			return elementSerializer;
		}
	}

	private final class ListComponentScreenValue implements IConfigScreenValue<Object>, IConfigLocalizedValue {
		private final int rowIndex;
		private final boolean keyComponent;
		private final List<Consumer<Object>> listeners = new ArrayList<>();

		private ListComponentScreenValue(int rowIndex, boolean keyComponent) {
			this.rowIndex = rowIndex;
			this.keyComponent = keyComponent;
		}

		@Override
		public String getName() {
			if (keyComponent) {
				return configValue.getName() + ".key";
			}
			return configValue.getName() + ".value";
		}

		@Override
		public String getLocalizationKey() {
			if (keyComponent) {
				return configValue.getLocalizationKey() + ".key";
			}
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
			if (keyComponent) {
				return getValue();
			}
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
			IConfigValueSerializer<Object> componentSerializer = getComponentSerializer(keyComponent);
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
			if (!replaceComponent(rowIndex, value, keyComponent)) {
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
			return getComponentSerializer(keyComponent);
		}

		private Object getComponent(List<T> entries, int index) {
			if (!isIndexValid(entries, index)) {
				throw new IllegalStateException("List row is no longer available");
			}
			return getRowComponent(entries.get(index), keyComponent);
		}
	}

	private record ListMove<T>(T value, int oldIndex, int newIndex) {
	}

	private record ColorComponentAreas(ImmutableRect2i swatchArea, ImmutableRect2i hexArea) {
	}
}
