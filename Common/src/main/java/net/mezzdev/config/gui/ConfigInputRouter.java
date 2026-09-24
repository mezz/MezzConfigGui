package net.mezzdev.config.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.input.UserInput;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ConfigInputRouter {
	private final List<ConfigInputHandler> inputHandlers;
	private final Map<InputConstants.Key, ConfigInputHandler> pending = new HashMap<>();

	public ConfigInputRouter(List<ConfigInputHandler> inputHandlers) {
		this.inputHandlers = List.copyOf(inputHandlers);
	}

	public boolean handleUserInput(@Nullable Screen screen, UserInput input) {
		return switch (input.getInputType()) {
			case IMMEDIATE -> handleImmediateClick(screen, input);
			case SIMULATE -> handleSimulateClick(screen, input);
			case EXECUTE -> handleExecuteClick(screen, input);
		};
	}

	private boolean handleImmediateClick(@Nullable Screen screen, UserInput input) {
		pending.remove(input.getKey());
		return handleInput(screen, input).isPresent();
	}

	private boolean handleSimulateClick(@Nullable Screen screen, UserInput input) {
		pending.remove(input.getKey());
		return handleInput(screen, input)
			.map(callback -> {
				pending.put(input.getKey(), callback);
				return true;
			})
			.orElse(false);
	}

	private boolean handleExecuteClick(@Nullable Screen screen, UserInput input) {
		return Optional.ofNullable(pending.remove(input.getKey()))
			.flatMap(inputHandler -> inputHandler.handleUserInput(screen, input))
			.isPresent();
	}

	private Optional<ConfigInputHandler> handleInput(@Nullable Screen screen, UserInput input) {
		Optional<ConfigInputHandler> firstHandled = Optional.empty();
		for (ConfigInputHandler inputHandler : inputHandlers) {
			if (firstHandled.isEmpty()) {
				firstHandled = inputHandler.handleUserInput(screen, input);
				if (firstHandled.isEmpty()) {
					inputHandler.unfocus();
				}
			} else {
				inputHandler.unfocus();
			}
		}
		return firstHandled;
	}

	public void handleGuiChange() {
		for (ConfigInputHandler inputHandler : inputHandlers) {
			inputHandler.unfocus();
		}
		pending.clear();
	}

	public boolean handleMouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
		return inputHandlers.stream()
			.flatMap(inputHandler -> inputHandler.handleMouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY).stream())
			.findFirst()
			.isPresent();
	}

	public boolean handleMouseDragged(@Nullable Screen screen, double mouseX, double mouseY, int button, double dragX, double dragY) {
		InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(button);
		ConfigInputHandler inputHandler = pending.get(key);
		if (inputHandler == null) {
			return false;
		}
		return inputHandler.handleMouseDragged(screen, mouseX, mouseY, button, dragX, dragY)
			.map(callback -> {
				pending.put(key, callback);
				return true;
			})
			.orElse(false);
	}

	public boolean allowsContentAutoScrollForDrag(int button) {
		InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(button);
		ConfigInputHandler inputHandler = pending.get(key);
		return inputHandler != null && inputHandler.allowsContentAutoScroll();
	}
}
