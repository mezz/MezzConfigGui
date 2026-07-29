package net.mezzdev.config.gui.api;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;

import java.util.Optional;

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
}
