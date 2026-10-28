package net.mezzdev.config.gui;

import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/** Restricts a value editor's input to its visible content area. */
final class ConfigEntryInputHandler implements ConfigInputHandler {
	private final ConfigEntryWidget<?> entry;
	private final ConfigInputHandler input;
	private final Supplier<ImmutableRect2i> viewport;

	ConfigEntryInputHandler(ConfigEntryWidget<?> entry, Supplier<ImmutableRect2i> viewport) {
		this.entry = entry;
		this.input = entry.createInputHandler();
		this.viewport = viewport;
	}

	@Override
	public Optional<ConfigInputHandler> handleUserInput(@Nullable Screen screen, UserInput event) {
		if (entry.isEditable() && !entry.getArea().isEmpty() && viewport.get().contains(event.getMouseX(), event.getMouseY()) &&
			entry.isMouseOver(event.getMouseX(), event.getMouseY())
		) {
			return input.handleUserInput(screen, event);
		}
		return Optional.empty();
	}

	@Override
	public void unfocus() {
		input.unfocus();
	}

	@Override
	public Optional<ConfigInputHandler> handleMouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (entry.isEditable() && !entry.getArea().isEmpty() && viewport.get().contains(x, y)) {
			return input.handleMouseScrolled(x, y, scrollX, scrollY);
		}
		return Optional.empty();
	}
}
