package net.mezzdev.config.gui.forge;

import net.mezzdev.config.gui.ConfigInputUtil;
import net.mezzdev.config.gui.quickmenu.QuickMenuManager;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;

/** Installs quick-menu input and rendering on Forge's screen event bus. */
final class ForgeQuickMenuHooks {
	private ForgeQuickMenuHooks() {}

	static void register() {
		var bus = MinecraftForge.EVENT_BUS;
		bus.addListener(EventPriority.HIGHEST, (RenderTooltipEvent.Pre event) -> {
			if (QuickMenuManager.shouldHideHostTooltip()) {
				event.setCanceled(true);
			}
		});
		bus.addListener((ScreenEvent.Closing event) -> QuickMenuManager.onScreenRemoved());
		bus.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				QuickMenuManager.tick();
			}
		});
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
			if (QuickMenuManager.mouseScrolled(event.getMouseX(), event.getMouseY(), 0.0, event.getScrollDelta())) {
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
