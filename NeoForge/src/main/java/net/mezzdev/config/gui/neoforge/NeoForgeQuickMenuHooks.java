package net.mezzdev.config.gui.neoforge;

import net.mezzdev.config.gui.quickmenu.QuickMenuManager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Installs quick-menu input and rendering on NeoForge's screen event bus. */
final class NeoForgeQuickMenuHooks {
	private NeoForgeQuickMenuHooks() {}

	static void register() {
		var bus = NeoForge.EVENT_BUS;
		bus.addListener((ScreenEvent.Closing event) -> QuickMenuManager.onScreenRemoved());
		bus.addListener((ClientTickEvent.Post event) -> QuickMenuManager.tick());
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.Render.Pre event) -> QuickMenuManager.prepareFrame());
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.Render.Foreground event) -> QuickMenuManager.hideHostTooltip(event.getGuiGraphics()));
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.Render.Post event) -> QuickMenuManager.restoreHostClip(event.getGuiGraphics()));
		bus.addListener(EventPriority.LOWEST, (ScreenEvent.Render.Post event) -> QuickMenuManager.draw(event.getGuiGraphics()));
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.MouseButtonPressed.Pre event) -> {
			if (QuickMenuManager.mousePressed(event.getMouseButtonEvent())) {
				event.setCanceled(true);
			}
		});
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.MouseButtonReleased.Pre event) -> {
			if (QuickMenuManager.mouseReleased(event.getMouseButtonEvent())) {
				event.setCanceled(true);
			}
		});
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.MouseDragged.Pre event) -> {
			if (QuickMenuManager.mouseDragged(event.getMouseButtonEvent(), event.getDragX(), event.getDragY())) {
				event.setCanceled(true);
			}
		});
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.MouseScrolled.Pre event) -> {
			if (QuickMenuManager.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY())) {
				event.setCanceled(true);
			}
		});
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.KeyPressed.Pre event) -> {
			if (QuickMenuManager.keyPressed(event.getKeyEvent())) {
				event.setCanceled(true);
			}
		});
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.KeyReleased.Pre event) -> {
			if (QuickMenuManager.keyReleased(event.getKeyEvent())) {
				event.setCanceled(true);
			}
		});
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.CharacterTyped.Pre event) -> {
			if (QuickMenuManager.charTyped(event.getCharacterEvent())) {
				event.setCanceled(true);
			}
		});
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.Preedit.Pre event) -> {
			if (QuickMenuManager.isOpen()) {
				event.setCanceled(true);
			}
		});
	}
}
