package net.mezzdev.config.gui.input;

import net.mezzdev.config.gui.ConfigInputUtil;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

import java.util.Optional;

public class UserInput {
	public static UserInput fromVanilla(int keyCode, int scanCode, int modifiers, InputType inputType) {
		InputConstants.Key input = ConfigInputUtil.getKey(keyCode, scanCode);
		return new UserInput(input, getCurrentMouseX(), getCurrentMouseY(), modifiers, inputType);
	}

	public static Optional<UserInput> fromVanilla(double mouseX, double mouseY, int mouseButton, InputType inputType) {
		if (mouseButton < 0) {
			return Optional.empty();
		}
		InputConstants.Key input = InputConstants.Type.MOUSE.getOrCreate(mouseButton);
		UserInput userInput = new UserInput(input, mouseX, mouseY, 0, inputType);
		return Optional.of(userInput);
	}

	private static double getCurrentMouseX() {
		Minecraft minecraft = Minecraft.getInstance();
		MouseHandler mouseHelper = minecraft.mouseHandler;
		double scale = (double) minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth();
		return mouseHelper.xpos() * scale;
	}

	private static double getCurrentMouseY() {
		Minecraft minecraft = Minecraft.getInstance();
		MouseHandler mouseHelper = minecraft.mouseHandler;
		double scale = (double) minecraft.getWindow().getGuiScaledHeight() / (double) minecraft.getWindow().getScreenHeight();
		return mouseHelper.ypos() * scale;
	}

	private final InputConstants.Key key;
	private final double mouseX;
	private final double mouseY;
	private final int modifiers;
	private final InputType inputType;

	private UserInput(InputConstants.Key key, double mouseX, double mouseY, int modifiers, InputType inputType) {
		this.key = key;
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.modifiers = modifiers;
		this.inputType = inputType;
	}

	public InputConstants.Key getKey() {
		return key;
	}

	public double getMouseX() {
		return mouseX;
	}

	public double getMouseY() {
		return mouseY;
	}

	public InputType getInputType() {
		return inputType;
	}

	public boolean isSimulate() {
		return inputType == InputType.SIMULATE;
	}

	public boolean is(KeyMapping keyMapping) {
		if (keyMapping.isUnbound()) {
			return false;
		}
		return ConfigInputUtil.matches(keyMapping, key, modifiers);
	}
}
