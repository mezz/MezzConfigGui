package net.mezzdev.config.gui;

import net.mezzdev.config.gui.input.IMouseOverable;
import net.mezzdev.config.gui.input.UserInput;
import net.minecraft.client.gui.screens.Screen;

import java.util.Optional;

public final class SameConfigElementInputHandler implements ConfigInputHandler {
	private final ConfigInputHandler handler;
	private final IMouseOverable mouseOverable;

	public SameConfigElementInputHandler(ConfigInputHandler handler, IMouseOverable mouseOverable) {
		this.handler = handler;
		this.mouseOverable = mouseOverable;
	}

	@Override
	public Optional<ConfigInputHandler> handleUserInput(Screen screen, UserInput input) {
		double mouseX = input.getMouseX();
		double mouseY = input.getMouseY();
		if (mouseOverable.isMouseOver(mouseX, mouseY)) {
			return handler.handleUserInput(screen, input)
				.map(handled -> this);
		}
		return Optional.empty();
	}

	@Override
	public void unfocus() {
		handler.unfocus();
	}

	@Override
	public Optional<ConfigInputHandler> handleMouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
		if (mouseOverable.isMouseOver(mouseX, mouseY)) {
			return handler.handleMouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
		}
		return Optional.empty();
	}
}
