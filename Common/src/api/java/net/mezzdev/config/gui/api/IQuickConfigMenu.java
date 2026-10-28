package net.mezzdev.config.gui.api;

import org.jetbrains.annotations.ApiStatus;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.Rect2i;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import java.util.function.BooleanSupplier;

/**
 * A modal menu for editing selected config values over the current Minecraft screen.
 * MezzConfigGui draws it above the host screen with a dimmed backdrop and handles its input and lifecycle.
 * Host controls and shortcuts are blocked while open.
 * Clicking outside the menu and its editor popups closes it without activating the host.
 * All methods must be called on the client thread.
 *
 * @see IQuickConfigMenuFactory
 * @since 0.7.0
 */
@ApiStatus.NonExtendable
public interface IQuickConfigMenu {
	/**
	 * Open the quick menu in the current screen, replacing any other quick menu.
	 * The same menu can be reopened after closing. An empty selection still allows choosing options with Customize.
	 * Opening fails when there is no host screen, the host is a MezzConfigGui config screen, the menu is unavailable,
	 * the anchor has nonpositive dimensions, or there is insufficient space to display the menu.
	 *
	 * @param button the control to open beside
	 * @return true if opened; false if the quick menu cannot be displayed
	 * @since 0.7.0
	 */
	default boolean open(AbstractWidget button) {
		return open(() -> new Rect2i(button.getX(), button.getY(), button.getWidth(), button.getHeight()),
			() -> button.visible && button.active);
	}

	/**
	 * Opens beside a custom control. Bounds use GUI-scaled screen coordinates and are read while open.
	 *
	 * @param anchor current control bounds; nonpositive dimensions close the menu
	 * @param available whether the control is available; false closes the menu
	 * @return whether the menu opened
	 * @since 0.7.0
	 */
	default boolean open(Supplier<Rect2i> anchor, BooleanSupplier available) {
		return open(anchor, available, () -> {}, () -> null);
	}

	/**
	 * Opens beside a custom control that remains usable while the menu is open.
	 *
	 * @param anchor current control bounds in GUI-scaled coordinates
	 * @param available whether the control is available
	 * @param onAnchorClick action to run after closing and saving when the control is clicked
	 * @param anchorInfo help shown when hovering the control, or {@code null} for no help
	 * @return whether the menu opened
	 * @since 0.7.0
	 */
	boolean open(Supplier<Rect2i> anchor, BooleanSupplier available, Runnable onAnchorClick, Supplier<@Nullable ConfigInfo> anchorInfo);

	/**
	 * Close the quick menu and apply its pending edits, including selection changes.
	 * This also happens when the menu closes after an outside click, screen change, or replacement by another menu.
	 * If saving fails, pending edits are retained for retry when the same menu is reopened and closed.
	 *
	 * @since 0.7.0
	 */
	void close();

	/**
	 * Whether this quick menu is open.
	 *
	 * @since 0.7.0
	 */
	boolean isOpen();
}
