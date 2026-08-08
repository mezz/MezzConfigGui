package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.api.value.PackedColor;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.info.ConfigValueIcon;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.popup.ColorPickerPopup;
import net.mezzdev.config.gui.popup.ConfigPopupSelector;
import net.mezzdev.config.gui.popup.ConfigValuePopupSelector;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Config entry for packed integer colors with exact text input and a color-picker swatch.
 */
final class ColorConfigEntry extends ConfigEntryWidget<PackedColor> {
	private static final int VALUE_BOX_WIDTH = 94;
	private static final int CONTROL_HEIGHT = 18;
	private static final int SWATCH_WIDTH = 18;
	private static final int CONTROL_GAP = 2;
	private static final int VALUE_TEXT_PADDING = 4;
	private static final int MIN_NAME_WIDTH = 68;
	private static final int MAX_EDIT_TEXT_LENGTH = 32;
	private static final int INVALID_TEXT_COLOR = 0xFFFF7070;

	private final IConfigValueSerializer<PackedColor> serializer;
	private final Consumer<ConfigPopupSelector> valueSelectorOpener;
	private ImmutableRect2i valueArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i swatchArea = ImmutableRect2i.EMPTY;
	private boolean editing;
	private String editText = "";

	ColorConfigEntry(
		IConfigScreenValue<PackedColor> value,
		IConfigValueSerializer<PackedColor> serializer,
		Consumer<ConfigPopupSelector> valueSelectorOpener,
		ConfigTextures textures
	) {
		super(value, textures);
		this.serializer = serializer;
		this.valueSelectorOpener = valueSelectorOpener;
	}

	@Override
	public void updateBounds(ImmutableRect2i area) {
		super.updateBounds(area);
		int preferredWidth = VALUE_BOX_WIDTH + CONTROL_GAP + SWATCH_WIDTH;
		int availableWidth = Math.max(60, area.getWidth() - VALUE_CONTROL_RIGHT_RESERVE - MIN_NAME_WIDTH);
		int controlWidth = Math.min(preferredWidth, availableWidth);
		int valueWidth = Math.max(40, controlWidth - CONTROL_GAP - SWATCH_WIDTH);
		int y = area.getY() + (area.getHeight() - CONTROL_HEIGHT) / 2;
		int x = area.getX() + area.getWidth() - valueWidth - CONTROL_GAP - SWATCH_WIDTH - VALUE_CONTROL_RIGHT_RESERVE;
		valueArea = new ImmutableRect2i(x, y, valueWidth, CONTROL_HEIGHT);
		swatchArea = new ImmutableRect2i(x + valueWidth + CONTROL_GAP, y, SWATCH_WIDTH, CONTROL_HEIGHT);
		recomputeNameArea(area, Math.max(NAME_RIGHT_RESERVE, controlWidth + VALUE_CONTROL_RIGHT_RESERVE + 4));
	}

	@Override
	protected void drawContent(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		Font font = Minecraft.getInstance().font;
		ConfigTextures textures = getTextures();
		drawName(guiGraphics);

		drawButtonBackground(guiGraphics, textures, valueArea, true, valueArea.contains(mouseX, mouseY));
		drawValueText(guiGraphics, font, getDisplayText(), getTextColor());

		drawButtonBackground(guiGraphics, textures, swatchArea, true, swatchArea.contains(mouseX, mouseY));
		ConfigValueIcon.drawInButton(guiGraphics, configValue, getValue(), swatchArea);
	}

	private String getDisplayText() {
		if (editing) {
			return editText + "_";
		}
		return serializer.serialize(getValue());
	}

	private int getTextColor() {
		if (editing && getParsedEditValue().isEmpty()) {
			return INVALID_TEXT_COLOR;
		}
		return TEXT_COLOR;
	}

	private void drawValueText(GuiGraphics guiGraphics, Font font, String text, int color) {
		ImmutableRect2i textArea = valueArea.cropLeft(VALUE_TEXT_PADDING).cropRight(VALUE_TEXT_PADDING);
		int y = getCenteredTextY(font, textArea);
		String visibleText = text;
		if (font.width(visibleText) > textArea.getWidth()) {
			visibleText = font.plainSubstrByWidth(visibleText, textArea.getWidth(), true);
		}
		drawText(guiGraphics, font, visibleText, textArea.getX(), y, color);
	}

	@Override
	public ConfigInfo getInfo() {
		ConfigInfo info = super.getInfo();
		List<Component> lines = new ArrayList<>(info.lines());
		lines.add(Component.translatable("mezz_config.config.screen.validValues", serializer.getValidValuesDescription()));
		return new ConfigInfo(info.title(), lines);
	}

	@Override
	@Nullable
	public ConfigInfo getTooltipInfo(double mouseX, double mouseY) {
		@Nullable
		ConfigInfo resetInfo = super.getTooltipInfo(mouseX, mouseY);
		if (resetInfo != null) {
			return resetInfo;
		}
		if (swatchArea.contains(mouseX, mouseY)) {
			return new ConfigInfo(
				Component.translatable("mezz_config.config.screen.color.choose"),
				List.of(Component.literal(serializer.serialize(getValue())))
			);
		}
		if (!valueArea.contains(mouseX, mouseY)) {
			return null;
		}
		if (editing && getParsedEditValue().isEmpty()) {
			return createInvalidValueInfo();
		}
		return ConfigValueInfoFactory.createUpdateInfo(configValue, getValue(), hasPendingChange());
	}

	private ConfigInfo createInvalidValueInfo() {
		IDeserializeResult<PackedColor> result = serializer.deserialize(editText);
		List<Component> lines = new ArrayList<>();
		for (String error : result.getErrors()) {
			lines.add(Component.literal(error));
		}
		if (lines.isEmpty()) {
			lines.add(Component.translatable("mezz_config.config.screen.text.invalid.info"));
		}
		lines.add(Component.translatable("mezz_config.config.screen.validValues", serializer.getValidValuesDescription()));
		return new ConfigInfo(Component.translatable("mezz_config.config.screen.text.invalid"), lines);
	}

	@Override
	protected boolean onMouseClicked(UserInput input) {
		if (super.onMouseClicked(input)) {
			cancelEdit();
			return true;
		}
		if (swatchArea.contains(input.getMouseX(), input.getMouseY())) {
			if (!input.isSimulate()) {
				commitEdit();
				ColorPickerPopup popup = new ColorPickerPopup(getValue());
				ConfigValuePopupSelector<PackedColor> selector = new ConfigValuePopupSelector<>(
					configValue,
					popup,
					() -> swatchArea,
					this::hasPendingChange,
					this::setValue
				);
				valueSelectorOpener.accept(selector);
			}
			return true;
		}
		if (valueArea.contains(input.getMouseX(), input.getMouseY())) {
			if (!input.isSimulate()) {
				startEditing();
			}
			return true;
		}
		return false;
	}

	private void startEditing() {
		editing = true;
		editText = serializer.serialize(getValue());
	}

	private Optional<PackedColor> getParsedEditValue() {
		IDeserializeResult<PackedColor> result = serializer.deserialize(editText);
		if (!result.getErrors().isEmpty()) {
			return Optional.empty();
		}
		return result.getResult()
			.filter(serializer::isValid);
	}

	private void commitEdit() {
		if (!editing) {
			return;
		}
		editing = false;
		getParsedEditValue().ifPresent(this::setValue);
		editText = "";
	}

	private void cancelEdit() {
		editing = false;
		editText = "";
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (!editing) {
			return false;
		}
		appendCharacter(codePoint);
		return true;
	}

	private void appendCharacter(char codePoint) {
		if (editText.length() >= MAX_EDIT_TEXT_LENGTH || !StringUtil.isAllowedChatCharacter(codePoint)) {
			return;
		}
		editText += codePoint;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!editing) {
			return false;
		}
		if (Screen.isPaste(keyCode)) {
			appendText(Minecraft.getInstance().keyboardHandler.getClipboard());
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			commitEdit();
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			cancelEdit();
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !editText.isEmpty()) {
			editText = editText.substring(0, editText.length() - 1);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_DELETE) {
			editText = "";
			return true;
		}
		return true;
	}

	private void appendText(String text) {
		for (int i = 0; i < text.length(); i++) {
			appendCharacter(text.charAt(i));
		}
	}

	@Override
	public boolean isCapturingKeyboardInput() {
		return editing;
	}

	@Override
	public void unfocus() {
		commitEdit();
	}
}
