package net.mezzdev.config.gui.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.event.Event;
import net.mezzdev.config.gui.ConfigInputUtil;
import net.mezzdev.config.gui.quickmenu.QuickMenuManager;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

/** Installs quick-menu input and rendering in Fabric's screen event phases. */
final class FabricQuickMenuHooks {
	private static final ResourceLocation INPUT_PHASE = new ResourceLocation("mezz_config_gui", "quick_menu_input");
	private static final ResourceLocation RENDER_PHASE = new ResourceLocation("mezz_config_gui", "quick_menu_render");

	private FabricQuickMenuHooks() {}

	static void register() {
		ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> register(screen));
		ClientTickEvents.END_CLIENT_TICK.register(client -> QuickMenuManager.tick());
	}

	private static void register(Screen screen) {
		ScreenEvents.remove(screen).register(removed -> QuickMenuManager.onScreenRemoved());
		ScreenEvents.beforeRender(screen).register((host, graphics, x, y, delta) -> QuickMenuManager.prepareFrame());
		var afterRender = ScreenEvents.afterRender(screen);
		afterRender.addPhaseOrdering(Event.DEFAULT_PHASE, RENDER_PHASE);
		afterRender.register(RENDER_PHASE, (host, graphics, x, y, delta) -> QuickMenuManager.draw(graphics, x, y));

		beforeOtherHandlers(ScreenMouseEvents.allowMouseClick(screen)).register(INPUT_PHASE, (host, x, y, button) -> !QuickMenuManager.mousePressed(x, y, button, ConfigInputUtil.getMouseModifiers()));
		beforeOtherHandlers(ScreenMouseEvents.allowMouseRelease(screen)).register(INPUT_PHASE, (host, x, y, button) -> !QuickMenuManager.mouseReleased(x, y, button, ConfigInputUtil.getMouseModifiers()));
		beforeOtherHandlers(ScreenMouseEvents.allowMouseScroll(screen)).register(INPUT_PHASE, (host, x, y, scrollX, scrollY) -> !QuickMenuManager.mouseScrolled(x, y, scrollX, scrollY));
		beforeOtherHandlers(ScreenKeyboardEvents.allowKeyPress(screen)).register(INPUT_PHASE, (host, key, scanCode, modifiers) -> !QuickMenuManager.keyPressed(key, scanCode, modifiers));
		beforeOtherHandlers(ScreenKeyboardEvents.allowKeyRelease(screen)).register(INPUT_PHASE, (host, key, scanCode, modifiers) -> !QuickMenuManager.keyReleased(key, scanCode, modifiers));
	}

	private static <T> Event<T> beforeOtherHandlers(Event<T> event) {
		event.addPhaseOrdering(INPUT_PHASE, Event.DEFAULT_PHASE);
		return event;
	}
}
