package net.mezzdev.config.gui.popup;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.ConfigInputHandler;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Routes clicks to an open popup selector and closes it when clicking elsewhere.
 */
public final class ConfigValueSelectorInputHandler implements ConfigInputHandler {
	private final Supplier<@Nullable ConfigPopupSelector> valueSelectorSupplier;
	private final Supplier<ImmutableRect2i> valueSelectorClipAreaSupplier;
	private final Runnable valueSelectorCloser;
	private final Runnable layoutUpdater;
	private boolean valueSelectorWasDragged;

	public ConfigValueSelectorInputHandler(
		Supplier<@Nullable ConfigPopupSelector> valueSelectorSupplier,
		Supplier<ImmutableRect2i> valueSelectorClipAreaSupplier,
		Runnable valueSelectorCloser,
		Runnable layoutUpdater
	) {
		this.valueSelectorSupplier = valueSelectorSupplier;
		this.valueSelectorClipAreaSupplier = valueSelectorClipAreaSupplier;
		this.valueSelectorCloser = valueSelectorCloser;
		this.layoutUpdater = layoutUpdater;
	}

	@Override
	public Optional<ConfigInputHandler> handleUserInput(@Nullable Screen screen, UserInput input) {
		if (input.isSimulate()) {
			valueSelectorWasDragged = false;
		}
		ConfigPopupSelector valueSelector = valueSelectorSupplier.get();
		if (valueSelector == null || input.getKey().getType() != InputConstants.Type.MOUSE) {
			return Optional.empty();
		}
		ImmutableRect2i clipArea = valueSelectorClipAreaSupplier.get();
		valueSelector.updateBounds(clipArea);

		if (valueSelectorWasDragged) {
			valueSelector.onMouseReleased(input.getMouseX(), input.getMouseY(), input.getKey().getValue());
			valueSelectorWasDragged = false;
			return Optional.of(this);
		}

		if (clipArea.contains(input.getMouseX(), input.getMouseY()) && valueSelector.isMouseOver(input.getMouseX(), input.getMouseY())) {
			boolean handled = valueSelector.onMouseClicked(input);
			if (!input.isSimulate()) {
				valueSelector.onMouseReleased(input.getMouseX(), input.getMouseY(), input.getKey().getValue());
			}
			if (handled) {
				if (!input.isSimulate() && valueSelector.closesAfterClick()) {
					closeValueSelector();
					layoutUpdater.run();
				}
				return Optional.of(this);
			}
			return Optional.of(this);
		}

		if (!input.isSimulate()) {
			closeValueSelector();
		}
		return Optional.of(this);
	}

	@Override
	public Optional<ConfigInputHandler> handleMouseScrolled(
		double mouseX,
		double mouseY,
		double scrollDeltaX,
		double scrollDeltaY
	) {
		ConfigPopupSelector valueSelector = valueSelectorSupplier.get();
		if (valueSelector == null) {
			return Optional.empty();
		}
		ImmutableRect2i clipArea = valueSelectorClipAreaSupplier.get();
		valueSelector.updateBounds(clipArea);
		if (!clipArea.contains(mouseX, mouseY) || !valueSelector.isMouseOver(mouseX, mouseY)) {
			return Optional.empty();
		}
		if (valueSelector.onMouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) {
			return Optional.of(this);
		}
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
		ConfigPopupSelector valueSelector = valueSelectorSupplier.get();
		if (valueSelector == null) {
			return Optional.empty();
		}
		ImmutableRect2i clipArea = valueSelectorClipAreaSupplier.get();
		valueSelector.updateBounds(clipArea);
		if (valueSelector.onMouseDragged(mouseX, mouseY, button)) {
			valueSelectorWasDragged = true;
		}
		return Optional.of(this);
	}

	@Override
	public void unfocus() {
		valueSelectorWasDragged = false;
	}

	private void closeValueSelector() {
		valueSelectorWasDragged = false;
		valueSelectorCloser.run();
	}
}
