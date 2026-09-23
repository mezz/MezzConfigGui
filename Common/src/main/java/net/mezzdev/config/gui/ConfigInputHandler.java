package net.mezzdev.config.gui;

import net.mezzdev.config.gui.input.UserInput;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public interface ConfigInputHandler {
	Optional<ConfigInputHandler> handleUserInput(@Nullable Screen screen, UserInput input);

	/**
	 * Called when a mouse is clicked but was handled and canceled by some other mouse handler.
	 */
	default void unfocus() {

	}

	default Optional<ConfigInputHandler> handleMouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
		return Optional.empty();
	}

	default Optional<ConfigInputHandler> handleMouseDragged(
		@Nullable Screen screen,
		double mouseX,
		double mouseY,
		int button,
		double dragX,
		double dragY
	) {
		return Optional.empty();
	}
}
