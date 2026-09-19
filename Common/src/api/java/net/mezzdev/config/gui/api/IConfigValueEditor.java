package net.mezzdev.config.gui.api;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;

import java.util.Optional;

/**
 * Custom value editor for compact config controls that need specialized rendering or click handling.
 *
 * @param <T> the config value type edited by this control
 *
 * @since 0.1.0
 */
public interface IConfigValueEditor<T> {
	/**
	 * The preferred width of the value control inside a config row.
	 *
	 * @param configValue the config screen value being edited
	 * @param value the currently displayed value, including pending edits
	 * @return the preferred control width
	 *
	 * @since 0.1.0
	 */
	int getControlWidth(IConfigScreenValue<T> configValue, T value);

	/**
	 * The preferred height of the value control inside a config row.
	 *
	 * @param configValue the config screen value being edited
	 * @param value the currently displayed value, including pending edits
	 * @return the preferred control height
	 *
	 * @since 0.1.0
	 */
	int getControlHeight(IConfigScreenValue<T> configValue, T value);

	/**
	 * Draw the value control inside the given row area.
	 *
	 * @param guiGraphics the draw context
	 * @param area the control bounds
	 * @param configValue the config screen value being edited
	 * @param value the currently displayed value, including pending edits
	 * @param hovered true when the mouse is over the control
	 * @param hasPendingChange true when the displayed value differs from the applied value
	 *
	 * @since 0.1.0
	 */
	void draw(
		GuiGraphicsExtractor guiGraphics,
		Rect2i area,
		IConfigScreenValue<T> configValue,
		T value,
		boolean hovered,
		boolean hasPendingChange
	);

	/**
	 * Get info for the current mouse position, or {@link Optional#empty()} to use the standard value info.
	 *
	 * @param area the control bounds
	 * @param configValue the config screen value being edited
	 * @param value the currently displayed value, including pending edits
	 * @param hasPendingChange true when the displayed value differs from the applied value
	 * @param mouseX the current mouse x-coordinate
	 * @param mouseY the current mouse y-coordinate
	 * @return the info to show
	 *
	 * @since 0.1.0
	 */
	Optional<ConfigInfo> getTooltipInfo(
		Rect2i area,
		IConfigScreenValue<T> configValue,
		T value,
		boolean hasPendingChange,
		double mouseX,
		double mouseY
	);

	/**
	 * Create a popup for a mouse click, or {@link Optional#empty()} when this editor does not open a popup.
	 * The config GUI controls popup placement and clipping.
	 *
	 * @param area the control bounds
	 * @param configValue the config screen value being edited
	 * @param value the currently displayed value, including pending edits
	 * @param mouseX the click x-coordinate
	 * @param mouseY the click y-coordinate
	 * @param button the clicked mouse button
	 * @return the popup to open
	 *
	 * @since 0.1.0
	 */
	default Optional<IConfigValuePopup<T>> createPopup(
		Rect2i area,
		IConfigScreenValue<T> configValue,
		T value,
		double mouseX,
		double mouseY,
		int button
	) {
		return Optional.empty();
	}

	/**
	 * Get the value selected by a mouse click, or {@link Optional#empty()} when this editor did not handle the click.
	 *
	 * @param area the control bounds
	 * @param configValue the config screen value being edited
	 * @param value the currently displayed value, including pending edits
	 * @param mouseX the click x-coordinate
	 * @param mouseY the click y-coordinate
	 * @param button the clicked mouse button
	 * @return the clicked value
	 *
	 * @since 0.1.0
	 */
	default Optional<T> getClickedValue(
		Rect2i area,
		IConfigScreenValue<T> configValue,
		T value,
		double mouseX,
		double mouseY,
		int button
	) {
		return Optional.empty();
	}
}
