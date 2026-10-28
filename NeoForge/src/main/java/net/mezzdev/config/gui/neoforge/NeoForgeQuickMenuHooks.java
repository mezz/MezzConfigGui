package net.mezzdev.config.gui.neoforge;

import net.mezzdev.config.gui.ConfigInputUtil;
import net.mezzdev.config.gui.quickmenu.QuickMenuManager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Installs quick-menu input and rendering on NeoForge's screen event bus. */
final class NeoForgeQuickMenuHooks {
	private NeoForgeQuickMenuHooks() {}

	static void register() {
		var bus = NeoForge.EVENT_BUS;
		bus.addListener(EventPriority.HIGHEST, (RenderTooltipEvent.Pre event) -> {
			if (QuickMenuManager.shouldHideHostTooltip()) {
				event.setCanceled(true);
			}
		});
		bus.addListener((ScreenEvent.Closing event) -> QuickMenuManager.onScreenRemoved());
		bus.addListener((ClientTickEvent.Post event) -> QuickMenuManager.tick());
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.Render.Pre event) -> QuickMenuManager.prepareFrame());
		bus.addListener(EventPriority.LOWEST, (ScreenEvent.Render.Post event) -> QuickMenuManager.draw(event.getGuiGraphics(), event.getMouseX(), event.getMouseY()));
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.MouseButtonPressed.Pre event) -> {
			if (QuickMenuManager.mousePressed(event.getMouseX(), event.getMouseY(), event.getButton(), ConfigInputUtil.getMouseModifiers())) {
				event.setCanceled(true);
			}
		});
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.MouseButtonReleased.Pre event) -> {
			if (QuickMenuManager.mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton(), ConfigInputUtil.getMouseModifiers())) {
				event.setCanceled(true);
			}
		});
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.MouseDragged.Pre event) -> {
			if (QuickMenuManager.mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(), event.getDragX(), event.getDragY())) {
				event.setCanceled(true);
			}
		});
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.MouseScrolled.Pre event) -> {
			if (QuickMenuManager.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY())) {
				event.setCanceled(true);
			}
		});
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.KeyPressed.Pre event) -> {
			if (QuickMenuManager.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
				event.setCanceled(true);
			}
		});
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.KeyReleased.Pre event) -> {
			if (QuickMenuManager.keyReleased(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
				event.setCanceled(true);
			}
		});
		bus.addListener(EventPriority.HIGHEST, (ScreenEvent.CharacterTyped.Pre event) -> {
			if (QuickMenuManager.charTyped(event.getCodePoint(), event.getModifiers())) {
				event.setCanceled(true);
			}
		});
	}
}
