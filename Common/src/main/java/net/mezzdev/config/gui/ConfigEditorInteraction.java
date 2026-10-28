package net.mezzdev.config.gui;

import org.lwjgl.glfw.GLFW;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.popup.ConfigPopupSelector;
import net.mezzdev.config.gui.popup.ConfigValueSelectorInputHandler;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Popup lifecycle and keyboard input for a host's visible config editors. */
final class ConfigEditorInteraction {
	private final Supplier<List<ConfigEntryWidget<?>>> entries;
	private final Supplier<ImmutableRect2i> popupClipArea;
	private @Nullable ConfigPopupSelector popup;

	ConfigEditorInteraction(Supplier<List<ConfigEntryWidget<?>>> entries, Supplier<ImmutableRect2i> popupClipArea) {
		this.entries = entries;
		this.popupClipArea = popupClipArea;
	}

	void close() {
		closePopup();
	}

	ConfigInputHandler createPopupInputHandler(Runnable updateLayout) {
		return new ConfigValueSelectorInputHandler(this::getPopup, popupClipArea, this::closePopup, updateLayout);
	}

	@Nullable
	ConfigPopupSelector getPopup() {
		return popup;
	}

	void openPopup(ConfigPopupSelector popup) {
		closePopup();
		this.popup = popup;
		updatePopupBounds();
		popup.onOpened();
	}

	private void closePopup() {
		ConfigPopupSelector old = popup;
		popup = null;
		if (old != null) {
			old.onClosed();
		}
	}

	void updatePopupBounds() {
		if (popup != null) {
			popup.updateBounds(popupClipArea.get());
		}
	}

	boolean popupKeyPressed(int key, int scanCode, int modifiers) {
		if (popup == null) {
			return false;
		}
		if (popup.keyPressed(key, scanCode, modifiers)) {
			return true;
		}
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			closePopup();
			return true;
		}
		return false;
	}

	boolean isCapturingKeyboardInput() {
		return forward(ConfigEntryWidget::isCapturingKeyboardInput);
	}

	boolean keyPressed(int key, int scanCode, int modifiers) {
		return forward(entry -> entry.keyPressed(key, scanCode, modifiers));
	}

	boolean keyReleased(int key, int scanCode, int modifiers) {
		return forward(entry -> entry.keyReleased(key, scanCode, modifiers));
	}

	boolean charTyped(char character, int modifiers) {
		return forward(entry -> entry.charTyped(character, modifiers));
	}

	private boolean forward(Predicate<ConfigEntryWidget<?>> handler) {
		return entries.get().stream().filter(ConfigEntryWidget::isEditable).anyMatch(handler);
	}

	void flushPendingInput(ConfigInputRouter input) {
		input.handleGuiChange();
		closePopup();
	}
}
