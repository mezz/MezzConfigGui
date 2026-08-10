package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.mezzdev.config.gui.input.UserInput;
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

/**
 * Config entry widget for values edited directly as serialized text.
 */
final class TextConfigEntry<T> extends ConfigEntryWidget<T> {
	private static final int VALUE_BOX_WIDTH = 140;
	private static final int VALUE_BOX_HEIGHT = 18;
	private static final int VALUE_TEXT_PADDING = 4;
	private static final int MIN_NAME_WIDTH = 68;
	private static final int MAX_EDIT_TEXT_LENGTH = 512;
	private static final int INVALID_TEXT_COLOR = 0xFFFF7070;

	private final IConfigValueSerializer<T> serializer;
	private ImmutableRect2i valueArea = ImmutableRect2i.EMPTY;

	private boolean editing;
	private String editText = "";

	TextConfigEntry(IConfigScreenValue<T> value, IConfigValueSerializer<T> serializer, ConfigTextures textures) {
		super(value, textures);
		this.serializer = serializer;
	}

	@Override
	public void updateBounds(ImmutableRect2i area) {
		super.updateBounds(area);
		int availableWidth = Math.max(40, area.getWidth() - VALUE_CONTROL_RIGHT_RESERVE - MIN_NAME_WIDTH);
		int width = Math.min(VALUE_BOX_WIDTH, availableWidth);
		valueArea = new ImmutableRect2i(
			area.getX() + area.getWidth() - width - VALUE_CONTROL_RIGHT_RESERVE,
			area.getY() + (area.getHeight() - VALUE_BOX_HEIGHT) / 2,
			width,
			VALUE_BOX_HEIGHT
		);
		recomputeNameArea(area, Math.max(NAME_RIGHT_RESERVE, width + VALUE_CONTROL_RIGHT_RESERVE + 4));
	}

	@Override
	protected void drawContent(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		Font font = Minecraft.getInstance().font;
		ConfigTextures textures = getTextures();
		drawName(guiGraphics);

		boolean hovered = valueArea.contains(mouseX, mouseY);
		drawButtonBackground(guiGraphics, textures, valueArea, true, hovered);

		String displayText = getDisplayText();
		int textColor = getTextColor();
		drawValueText(guiGraphics, font, displayText, textColor);
	}

	private int getTextColor() {
		if (editing && !isValidEditText()) {
			return INVALID_TEXT_COLOR;
		}
		return TEXT_COLOR;
	}

	private String getDisplayText() {
		if (editing) {
			return editText + "_";
		}
		return serializer.serialize(getValue());
	}

	private void drawValueText(GuiGraphics guiGraphics, Font font, String text, int color) {
		ImmutableRect2i textArea = valueArea.cropLeft(VALUE_TEXT_PADDING).cropRight(VALUE_TEXT_PADDING);
		int y = getCenteredTextY(font, textArea);
		String visibleText = getVisibleText(font, text, textArea.getWidth());
		drawText(guiGraphics, font, visibleText, textArea.getX(), y, color);
	}

	private String getVisibleText(Font font, String text, int maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text;
		}
		return font.plainSubstrByWidth(text, maxWidth, true);
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
		if (!valueArea.contains(mouseX, mouseY)) {
			return null;
		}
		if (editing && !isValidEditText()) {
			return createInvalidValueInfo();
		}
		return ConfigValueInfoFactory.createUpdateInfo(configValue, getValue(), hasPendingChange());
	}

	private ConfigInfo createInvalidValueInfo() {
		IDeserializeResult<T> result = serializer.deserialize(editText);
		List<Component> lines = new ArrayList<>();
		for (String error : result.getDiagnostics()) {
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
			editing = false;
			editText = "";
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

	private boolean isValidEditText() {
		return getParsedEditValue().isPresent();
	}

	private Optional<T> getParsedEditValue() {
		IDeserializeResult<T> result = serializer.deserialize(editText);
		if (!result.getDiagnostics().isEmpty()) {
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
		getParsedEditValue()
			.ifPresent(this::setValue);
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
			editing = false;
			editText = "";
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
