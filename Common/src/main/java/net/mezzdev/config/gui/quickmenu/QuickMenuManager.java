package net.mezzdev.config.gui.quickmenu;

import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.ConfigGuiColors.GuiColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Routes platform screen events to the active quick menu. */
public final class QuickMenuManager {
	private static @Nullable QuickConfigMenu<?> active;
	private static boolean drawing;
	private static final Map<Integer, QuickConfigMenu<?>> mouseCaptures = new HashMap<>();
	private static final Set<Integer> capturedKeys = new HashSet<>();
	private static final Set<Integer> hostKeys = new HashSet<>();

	private QuickMenuManager() {}

	static void open(QuickConfigMenu<?> menu) {
		if (active != null && active != menu) {
			active.close();
		}
		active = menu;
	}

	static void closed(QuickConfigMenu<?> menu) {
		if (active == menu) {
			active = null;
		}
	}

	public static void onScreenRemoved() {
		if (active != null) {
			active.close();
		}
	}

	public static void tick() {
		if (!Minecraft.getInstance().isWindowActive()) {
			capturedKeys.clear();
			hostKeys.clear();
			if (active != null) {
				active.input().cancelInput();
			}
		}
		prepareFrame();
	}

	public static void prepareFrame() {
		if (active != null) {
			active.refresh();
		}
	}

	public static boolean isOpen() {
		return active != null;
	}

	public static boolean shouldHideHostTooltip() {
		return active != null && !drawing;
	}

	public static void draw(GuiGraphics graphics, int mouseX, int mouseY) {
		QuickConfigMenu<?> menu = active;
		if (menu == null) {
			return;
		}
		// Draw above the host's widgets and any item or text layers already submitted.
		graphics.flush();
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 600);
		drawing = true;
		try {
			graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), ConfigGuiColors.getColor(GuiColor.QUICK_MENU_SCREEN_DIM));
			menu.draw(graphics, mouseX, mouseY);
			graphics.flush();
		} finally {
			drawing = false;
			graphics.pose().popPose();
		}
	}

	public static boolean mousePressed(double x, double y, int button, int modifiers) {
		prepareFrame();
		QuickConfigMenu<?> menu = active;
		if (menu != null && menu.input().mousePressed(x, y, button, modifiers)) {
			mouseCaptures.put(button, menu);
			return true;
		}
		return false;
	}

	public static boolean mouseReleased(double x, double y, int button, int modifiers) {
		prepareFrame();
		QuickConfigMenu<?> capture = mouseCaptures.remove(button);
		if (capture != null) {
			if (capture == active) {
				capture.input().mouseReleased(x, y, button, modifiers);
			}
			return true;
		}
		return active != null;
	}

	public static boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
		prepareFrame();
		if (active == null) {
			return false;
		}
		if (mouseCaptures.get(button) == active) {
			active.input().mouseDragged(x, y, button, dragX, dragY);
		}
		return true;
	}

	public static boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		prepareFrame();
		return active != null && active.input().mouseScrolled(x, y, scrollX, scrollY);
	}

	public static boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		prepareFrame();
		if (active == null) {
			if (capturedKeys.contains(keyCode)) {
				return true;
			}
			hostKeys.add(keyCode);
			return false;
		}
		// Repeats must not transfer a key whose original press went to the host.
		if (!hostKeys.contains(keyCode)) {
			capturedKeys.add(keyCode);
		}
		return active.input().keyPressed(keyCode, scanCode, modifiers);
	}

	public static boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		prepareFrame();
		hostKeys.remove(keyCode);
		boolean captured = capturedKeys.remove(keyCode);
		if (active != null && captured) {
			active.input().keyReleased(keyCode, scanCode, modifiers);
		}
		return captured;
	}

	public static boolean charTyped(int codePoint, int modifiers) {
		prepareFrame();
		return active != null && active.input().charTyped(codePoint, modifiers);
	}
}
