package net.mezzdev.config.gui.popup;

import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.ConfigInputHandler;
import net.mezzdev.config.gui.ConfigInputUtil;
import net.mezzdev.config.gui.input.UserInput;
import net.minecraft.client.gui.screens.Screen;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Routes clicks to an open popup selector and closes it when clicking elsewhere.
 */
public final class ConfigValueSelectorInputHandler implements ConfigInputHandler {
	private final Supplier<ConfigPopupSelector> valueSelectorSupplier;
	private final Supplier<ImmutableRect2i> valueSelectorClipAreaSupplier;
	private final Runnable valueSelectorCloser;
	private final Runnable layoutUpdater;

	public ConfigValueSelectorInputHandler(
		Supplier<ConfigPopupSelector> valueSelectorSupplier,
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
	public Optional<ConfigInputHandler> handleUserInput(Screen screen, UserInput input) {
		ConfigPopupSelector valueSelector = valueSelectorSupplier.get();
		if (valueSelector == null || !ConfigInputUtil.isLeftClick(input)) {
			return Optional.empty();
		}
		ImmutableRect2i clipArea = valueSelectorClipAreaSupplier.get();
		valueSelector.updateBounds(clipArea);

		if (clipArea.contains(input.getMouseX(), input.getMouseY()) && valueSelector.isMouseOver(input.getMouseX(), input.getMouseY())) {
			if (valueSelector.onMouseClicked(input)) {
				if (!input.isSimulate()) {
					closeValueSelector();
					layoutUpdater.run();
				}
				return Optional.of(this);
			}
			return Optional.empty();
		}

		if (!input.isSimulate()) {
			closeValueSelector();
		}
		return Optional.of(this);
	}

	private void closeValueSelector() {
		valueSelectorCloser.run();
	}
}
