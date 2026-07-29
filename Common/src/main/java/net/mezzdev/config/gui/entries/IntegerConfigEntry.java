package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.IConfigIntegerValueSerializer;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.gui.textures.ConfigDrawableStatic;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.mezzdev.config.gui.input.UserInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Config entry widget for integer values with editable text and stepper buttons.
 */
final class IntegerConfigEntry extends ConfigEntryWidget<Integer> {

	private static final int BUTTON_SIZE = 18;
	private static final int BUTTON_GAP = 2;
	private static final int ICON_SIZE = 9;
	private static final int VALUE_BOX_WIDTH = 42;
	private static final int VALUE_BOX_HEIGHT = BUTTON_SIZE;
	private static final int VALUE_TEXT_PADDING = 3;
	private static final int CONTROL_WIDTH = VALUE_BOX_WIDTH + BUTTON_GAP + BUTTON_SIZE + BUTTON_GAP + BUTTON_SIZE;
	private static final int NORMAL_STEP = 1;
	private static final int SHIFT_STEP = 10;

	private final IConfigIntegerValueSerializer serializer;
	private ImmutableRect2i valueBoxArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i upArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i downArea = ImmutableRect2i.EMPTY;

	private boolean editing = false;
	private String editText = "";

	IntegerConfigEntry(IConfigValue<Integer> value, IConfigIntegerValueSerializer serializer, ConfigTextures textures) {
		super(value, textures);
		this.serializer = serializer;
	}

	@Override
	public void updateBounds(ImmutableRect2i area) {
		super.updateBounds(area);
		int cy = area.getY() + (area.getHeight() - VALUE_BOX_HEIGHT) / 2;
		int controlX = area.getX() + area.getWidth() - CONTROL_WIDTH - VALUE_CONTROL_RIGHT_RESERVE;

		valueBoxArea = new ImmutableRect2i(
				controlX,
				cy,
				VALUE_BOX_WIDTH,
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
		recomputeNameArea(area, Math.max(NAME_RIGHT_RESERVE, CONTROL_WIDTH + VALUE_CONTROL_RIGHT_RESERVE + 4));
	}

	@Override
	protected void drawContent(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		Font font = Minecraft.getInstance().font;
		ConfigTextures textures = getTextures();
		drawName(guiGraphics);

		boolean valueHovered = valueBoxArea.contains(mouseX, mouseY);
		drawButtonBackground(guiGraphics, textures, valueBoxArea, true, valueHovered);
		int textY = getCenteredTextY(font, valueBoxArea);
		if (editing) {
			String displayText = editText + "_";
			int textColor = TEXT_COLOR;
			if (!editText.isEmpty() && !editText.equals("-")) {
				try {
					int parsed = Integer.parseInt(editText);
					if (parsed < serializer.getMin() || parsed > serializer.getMax()) {
						textColor = 0xFFFF7070;
					}
				} catch (NumberFormatException ignored) {
					textColor = 0xFFFF7070;
				}
			}
			drawRightAlignedText(guiGraphics, font, displayText, textY, textColor);
		} else {
			drawRightAlignedText(guiGraphics, font, getValue().toString(), textY, TEXT_COLOR);
		}

		boolean canUp = getValue() < serializer.getMax();
		boolean upHovered = canUp && upArea.contains(mouseX, mouseY);
		drawButtonBackground(guiGraphics, textures, upArea, canUp, upHovered);
		ConfigDrawableStatic upIcon = textures.getArrowUp();
		int upIx = upArea.getX() + (upArea.getWidth() - ICON_SIZE) / 2;
		int upIy = upArea.getY() + (upArea.getHeight() - ICON_SIZE) / 2;
		if (canUp) {
			upIcon.draw(guiGraphics, upIx, upIy);
		} else {
			guiGraphics.pose().pushPose();
			guiGraphics.setColor(0.3f, 0.3f, 0.3f, 0.5f);
			upIcon.draw(guiGraphics, upIx, upIy);
			guiGraphics.setColor(1f, 1f, 1f, 1f);
			guiGraphics.pose().popPose();
		}

		boolean canDown = getValue() > serializer.getMin();
		boolean downHovered = canDown && downArea.contains(mouseX, mouseY);
		drawButtonBackground(guiGraphics, textures, downArea, canDown, downHovered);
		ConfigDrawableStatic downIcon = textures.getArrowDown();
		int downIx = downArea.getX() + (downArea.getWidth() - ICON_SIZE) / 2;
		int downIy = downArea.getY() + (downArea.getHeight() - ICON_SIZE) / 2;
		if (canDown) {
			downIcon.draw(guiGraphics, downIx, downIy);
		} else {
			guiGraphics.pose().pushPose();
			guiGraphics.setColor(0.3f, 0.3f, 0.3f, 0.5f);
			downIcon.draw(guiGraphics, downIx, downIy);
			guiGraphics.setColor(1f, 1f, 1f, 1f);
			guiGraphics.pose().popPose();
		}
	}

	private void drawRightAlignedText(GuiGraphics guiGraphics, Font font, String text, int y, int color) {
		int minX = valueBoxArea.getX() + VALUE_TEXT_PADDING;
		int maxX = valueBoxArea.getX() + valueBoxArea.getWidth() - VALUE_TEXT_PADDING;
		int x = Math.max(minX, maxX - font.width(text));
		drawText(guiGraphics, font, text, x, y, color);
	}

	@Override
	public ConfigInfo getInfo() {
		ConfigInfo info = super.getInfo();
		List<Component> lines = new ArrayList<>(info.lines());
		lines.add(Component.translatable("mezz_config.config.screen.range", serializer.getMin(), serializer.getMax()));
		lines.add(Component.translatable("mezz_config.config.screen.number.shiftStep", SHIFT_STEP));
		return new ConfigInfo(info.title(), lines);
	}

	@Override
	@Nullable
	public ConfigInfo getTooltipInfo(double mouseX, double mouseY) {
		@Nullable ConfigInfo resetInfo = super.getTooltipInfo(mouseX, mouseY);
		if (resetInfo != null) {
			return resetInfo;
		}
		if (valueBoxArea.contains(mouseX, mouseY)) {
			return ConfigValueInfoFactory.createUpdateInfo(configValue, getValue(), hasPendingChange());
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
		if (getValue() < serializer.getMax() && upArea.contains(input.getMouseX(), input.getMouseY())) {
			if (!input.isSimulate()) {
				commitEdit();
				incrementValue(getStep());
			}
			return true;
		}
		if (getValue() > serializer.getMin() && downArea.contains(input.getMouseX(), input.getMouseY())) {
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
			val = Math.clamp(val, serializer.getMin(), serializer.getMax());
			setValue(val);
		} catch (NumberFormatException ignored) {
		}
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
		if (keyCode == GLFW.GLFW_KEY_UP) {
			commitEdit();
			incrementValue(getStep(modifiers));
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_DOWN) {
			commitEdit();
			incrementValue(-getStep(modifiers));
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ENTER) {
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
		return true;
	}

	private int getStep() {
		return Screen.hasShiftDown() ? SHIFT_STEP : NORMAL_STEP;
	}

	private static int getStep(int modifiers) {
		return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? SHIFT_STEP : NORMAL_STEP;
	}

	private void incrementValue(int increment) {
		int value = (int) Math.clamp((long) getValue() + increment, serializer.getMin(), serializer.getMax());
		setValue(value);
	}

	@Override
	public void unfocus() {
		commitEdit();
	}

}
