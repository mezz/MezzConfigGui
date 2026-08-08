package net.mezzdev.config.gui.api;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Popup content opened by a custom config value editor.
 * <p>
 * The config GUI places the popup near the editor control and keeps it inside the visible config area.
 * Implementations only need to provide their preferred size and handle drawing and hit testing inside the
 * provided popup bounds.
 *
 * @param <T> the config value type selected by this popup
 *
 * @since 0.1.0
 */
public interface IConfigValuePopup<T> {
	/**
	 * The preferred popup width.
	 *
	 * @return the preferred popup width
	 *
	 * @since 0.1.0
	 */
	int getWidth();

	/**
	 * The preferred popup height.
	 *
	 * @return the preferred popup height
	 *
	 * @since 0.1.0
	 */
	int getHeight();

	/**
	 * Get the value currently under the mouse, or {@link Optional#empty()} when hovering no value.
	 *
	 * @param area the popup bounds
	 * @param mouseX the current mouse x-coordinate
	 * @param mouseY the current mouse y-coordinate
	 * @return the hovered value
	 *
	 * @since 0.1.0
	 */
	Optional<T> getHoveredValue(Rect2i area, double mouseX, double mouseY);

	/**
	 * Draw the popup.
	 *
	 * @param guiGraphics the draw context
	 * @param area the popup bounds
	 * @param mouseX the current mouse x-coordinate
	 * @param mouseY the current mouse y-coordinate
	 *
	 * @since 0.1.0
	 */
	void draw(GuiGraphics guiGraphics, Rect2i area, double mouseX, double mouseY);

	/**
	 * Get the value selected by a mouse click, or {@link Optional#empty()} when the click did not select a value.
	 * <p>
	 * By default, a left-click selects the hovered value.
	 *
	 * @param area the popup bounds
	 * @param mouseX the click x-coordinate
	 * @param mouseY the click y-coordinate
	 * @param button the clicked mouse button
	 * @return the clicked value
	 *
	 * @since 0.1.0
	 */
	default Optional<T> getClickedValue(Rect2i area, double mouseX, double mouseY, int button) {
		if (button != 0) {
			return Optional.empty();
		}
		return getHoveredValue(area, mouseX, mouseY);
	}

	/**
	 * Get the value selected while dragging the mouse, or {@link Optional#empty()} when this popup does not handle
	 * dragging.
	 *
	 * @param area the popup bounds
	 * @param mouseX the current mouse x-coordinate
	 * @param mouseY the current mouse y-coordinate
	 * @param button the held mouse button
	 * @return the value selected by the drag
	 *
	 * @since 0.1.0
	 */
	default Optional<T> getDraggedValue(Rect2i area, double mouseX, double mouseY, int button) {
		return Optional.empty();
	}

	/**
	 * Handle a typed character while this popup has an active text field.
	 *
	 * @param codePoint the typed character
	 * @param modifiers the active keyboard modifiers
	 * @param valueConsumer receives a new value when the edit becomes valid
	 * @return true when the popup handled the character
	 *
	 * @since 0.1.0
	 */
	default boolean charTyped(char codePoint, int modifiers, Consumer<T> valueConsumer) {
		return false;
	}

	/**
	 * Handle a key press while this popup has an active text field.
	 *
	 * @param keyCode the key code
	 * @param scanCode the platform scan code
	 * @param modifiers the active keyboard modifiers
	 * @param valueConsumer receives a new value when the edit becomes valid
	 * @return true when the popup handled the key press
	 *
	 * @since 0.1.0
	 */
	default boolean keyPressed(int keyCode, int scanCode, int modifiers, Consumer<T> valueConsumer) {
		return false;
	}

	/**
	 * Whether the popup should close after it selects a value.
	 * <p>
	 * Return {@code false} for interactive controls such as color pickers that support several adjustments before the
	 * user clicks outside the popup.
	 *
	 * @return true to close after selecting a value
	 *
	 * @since 0.1.0
	 */
	default boolean closesAfterValueSelected() {
		return true;
	}
}
