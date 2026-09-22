package net.mezzdev.config.gui.entries;

import net.mezzdev.config.gui.ConfigInputUtil;

import net.mezzdev.config.gui.util.ConfigMath;

import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.info.ConfigNumberInfo;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.textures.ConfigButtonIcon;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.mezzdev.config.gui.api.LegacyGuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Config entry widget for integer values with editable text and stepper buttons.
 */
final class IntegerConfigEntry extends ConfigEntryWidget<Integer> {

	private static final int BUTTON_SIZE = 18;
	private static final int BUTTON_GAP = 2;
	private static final int MIN_VALUE_BOX_WIDTH = 38;
	private static final int VALUE_BOX_HEIGHT = BUTTON_SIZE;
	private static final int VALUE_TEXT_PADDING = 3;
	private static final int STEPPER_BUTTONS_WIDTH = BUTTON_GAP + BUTTON_SIZE + BUTTON_GAP + BUTTON_SIZE;
	private static final int MIN_CONTROL_WIDTH = MIN_VALUE_BOX_WIDTH + STEPPER_BUTTONS_WIDTH;
	private static final int NORMAL_STEP = 1;
	private static final int SHIFT_STEP = 10;

	private final int min;
	private final int max;
	private ImmutableRect2i valueBoxArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i upArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i downArea = ImmutableRect2i.EMPTY;

	private boolean editing = false;
	private String editText = "";

	IntegerConfigEntry(
		IConfigScreenValue<Integer> value,
		ConfigValueRange<Integer> range,
		ConfigTextures textures
	) {
		super(value, textures);
		this.min = range.min();
		this.max = range.max();
	}

	@Override
	public void updateBounds(ImmutableRect2i area) {
		super.updateBounds(area);
		int cy = area.getY() + (area.getHeight() - VALUE_BOX_HEIGHT) / 2;
		int controlWidth = getValueColumnWidth(area, MIN_CONTROL_WIDTH);
		int valueBoxWidth = controlWidth - STEPPER_BUTTONS_WIDTH;
		int controlX = area.getX() + area.getWidth() - controlWidth - VALUE_CONTROL_RIGHT_RESERVE;

		valueBoxArea = new ImmutableRect2i(
			controlX,
			cy,
			valueBoxWidth,
			VALUE_BOX_HEIGHT
		);
		upArea = new ImmutableRect2i(
			valueBoxArea.getX() + valueBoxArea.getWidth() + BUTTON_GAP,
			cy,
			BUTTON_SIZE,
			BUTTON_SIZE
		);
		downArea = new ImmutableRect2i(
			upArea.getX() + upArea.getWidth() + BUTTON_GAP,
			cy,
			BUTTON_SIZE,
			BUTTON_SIZE
		);
		recomputeNameArea(area, getValueColumnNameRightReserve(area, MIN_CONTROL_WIDTH));
	}

	@Override
	protected void drawContent(LegacyGuiGraphics guiGraphics, double mouseX, double mouseY) {
		Font font = Minecraft.getInstance().font;
		ConfigTextures textures = getTextures();
		drawName(guiGraphics);

		boolean valueHovered = valueBoxArea.contains(mouseX, mouseY);
		drawButtonBackground(guiGraphics, textures, valueBoxArea, true, valueHovered);
		int textY = getCenteredTextY(font, valueBoxArea);
		if (editing) {
			String displayText = editText + "_";
			int textColor = getConfiguredTextColor();
			if (!editText.isEmpty() && !editText.equals("-")) {
				try {
					int parsed = Integer.parseInt(editText);
					if (parsed < min || parsed > max) {
						textColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_INVALID_TEXT);
					}
				} catch (NumberFormatException ignored) {
					textColor = ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_INVALID_TEXT);
				}
			}
			drawRightAlignedText(guiGraphics, font, displayText, textY, textColor);
		} else {
			drawFittedText(guiGraphics, font, ConfigValueLocalization.getValueName(configValue, getValue()),
				valueBoxArea.cropLeft(VALUE_TEXT_PADDING).cropRight(VALUE_TEXT_PADDING), getConfiguredTextColor(), true);
		}

		boolean canUp = getValue() < max;
		boolean upHovered = canUp && upArea.contains(mouseX, mouseY);
		drawButtonBackground(guiGraphics, textures, upArea, canUp, upHovered);
		ConfigButtonIcon.UP.draw(guiGraphics, upArea, canUp);

		boolean canDown = getValue() > min;
		boolean downHovered = canDown && downArea.contains(mouseX, mouseY);
		drawButtonBackground(guiGraphics, textures, downArea, canDown, downHovered);
		ConfigButtonIcon.DOWN.draw(guiGraphics, downArea, canDown);
	}

	private void drawRightAlignedText(LegacyGuiGraphics guiGraphics, Font font, String text, int y, int color) {
		int minX = valueBoxArea.getX() + VALUE_TEXT_PADDING;
		int maxX = valueBoxArea.getX() + valueBoxArea.getWidth() - VALUE_TEXT_PADDING;
		text = font.plainSubstrByWidth(text, Math.max(0, maxX - minX), true);
		int x = Math.max(minX, maxX - font.width(text));
		drawText(guiGraphics, font, text, x, y, color);
	}

	@Override
	public ConfigInfo getInfo() {
		ConfigInfo info = super.getInfo();
		List<Component> lines = new ArrayList<>(info.lines());
		lines.add(ConfigNumberInfo.getRange(new ConfigValueRange<>(min, max)));
		lines.add(Component.translatable("mezz_config.config.screen.number.shiftStep", SHIFT_STEP));
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
		if (valueBoxArea.contains(mouseX, mouseY)) {
			ConfigInfo updateInfo = ConfigValueInfoFactory.createUpdateInfo(configValue, getValue(), hasPendingChange());
			if (updateInfo != null) {
				return updateInfo;
			}
			return new ConfigInfo(ConfigValueLocalization.getValueName(configValue, getValue()), List.of());
		}
		return null;
	}

	@Override
	protected boolean onMouseClicked(UserInput input) {
		if (super.onMouseClicked(input)) {
			editing = false;
			editText = "";
			return true;
		}
		if (valueBoxArea.contains(input.getMouseX(), input.getMouseY())) {
			if (!input.isSimulate()) {
				startEditing();
			}
			return true;
		}
		if (editing && !input.isSimulate()) {
			commitEdit();
		}
		if (getValue() < max && upArea.contains(input.getMouseX(), input.getMouseY())) {
			if (!input.isSimulate()) {
				commitEdit();
				incrementValue(getStep());
			}
			return true;
		}
		if (getValue() > min && downArea.contains(input.getMouseX(), input.getMouseY())) {
			if (!input.isSimulate()) {
				commitEdit();
				incrementValue(-getStep());
			}
			return true;
		}
		return false;
	}

	private void startEditing() {
		editing = true;
		editText = getValue().toString();
	}

	private void commitEdit() {
		if (!editing) {
			return;
		}
		editing = false;
		try {
			int val = Integer.parseInt(editText.trim());
			val = ConfigMath.clamp(val, min, max);
			setValue(val);
		} catch (NumberFormatException ignored) {}
		editText = "";
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (!editing) {
			return false;
		}
		if (codePoint == '-' && editText.isEmpty()) {
			editText = "-";
			return true;
		}
		if (Character.isDigit(codePoint) && editText.length() < 10) {
			editText += codePoint;
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!editing) {
			return false;
		}
		if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_UP) {
			commitEdit();
			incrementValue(getStep(modifiers));
			return true;
		}
		if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_DOWN) {
			commitEdit();
			incrementValue(-getStep(modifiers));
			return true;
		}
		if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN) {
			commitEdit();
			return true;
		}
		if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
			editing = false;
			editText = "";
			return true;
		}
		if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE && !editText.isEmpty()) {
			editText = editText.substring(0, editText.length() - 1);
			return true;
		}
		return true;
	}

	private int getStep() {
		if (ConfigInputUtil.hasShiftDown()) {
			return SHIFT_STEP;
		}
		return NORMAL_STEP;
	}

	private static int getStep(int modifiers) {
		if ((modifiers & net.mezzdev.config.gui.ConfigInputUtil.SHIFT_MODIFIER) != 0) {
			return SHIFT_STEP;
		}
		return NORMAL_STEP;
	}

	private void incrementValue(int increment) {
		int value = (int) ConfigMath.clamp((long) getValue() + increment, min, max);
		setValue(value);
	}

	@Override
	public void unfocus() {
		commitEdit();
	}

}
