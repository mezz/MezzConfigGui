package net.mezzdev.config.gui.quickmenu;

import com.mojang.blaze3d.platform.cursor.CursorType;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.ConfigGuiColors.GuiColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Routes platform screen events to the active quick menu. */
public final class QuickMenuManager {
	private static @Nullable QuickConfigMenu<?> active;
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

	public static void hideHostTooltip(GuiGraphics graphics) {
		if (active != null) {
			graphics.deferredTooltip = null;
		}
	}

	public static void draw(GuiGraphics hostGraphics) {
		QuickConfigMenu<?> menu = active;
		if (menu == null) {
			return;
		}
		var minecraft = Minecraft.getInstance();
		int x = (int) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
		int y = (int) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
		// Share the final render layers, with independent tooltip and text-hover state.
		GuiGraphics graphics = new GuiGraphics(minecraft, hostGraphics.guiRenderState, x, y) {
			@Override
			public void requestCursor(CursorType cursor) {
				super.requestCursor(cursor);
				hostGraphics.requestCursor(cursor);
			}
		};
		graphics.nextStratum();
		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), ConfigGuiColors.getColor(GuiColor.QUICK_MENU_SCREEN_DIM));
		menu.draw(graphics, x, y);
		graphics.renderDeferredElements();
	}

	public static boolean mousePressed(MouseButtonEvent event) {
		prepareFrame();
		QuickConfigMenu<?> menu = active;
		if (menu != null && menu.input().mousePressed(event)) {
			mouseCaptures.put(event.button(), menu);
			return true;
		}
		return false;
	}

	public static boolean mouseReleased(MouseButtonEvent event) {
		prepareFrame();
		QuickConfigMenu<?> capture = mouseCaptures.remove(event.button());
		if (capture != null) {
			if (capture == active) {
				capture.input().mouseReleased(event);
			}
			return true;
		}
		return active != null;
	}

	public static boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		prepareFrame();
		if (active == null) {
			return false;
		}
		if (mouseCaptures.get(event.button()) == active) {
			active.input().mouseDragged(event, dragX, dragY);
		}
		return true;
	}

	public static boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		prepareFrame();
		return active != null && active.input().mouseScrolled(x, y, scrollX, scrollY);
	}

	public static boolean keyPressed(KeyEvent event) {
		prepareFrame();
		if (active == null) {
			if (capturedKeys.contains(event.key())) {
				return true;
			}
			hostKeys.add(event.key());
			return false;
		}
		// Repeats must not transfer a key whose original press went to the host.
		if (!hostKeys.contains(event.key())) {
			capturedKeys.add(event.key());
		}
		return active.input().keyPressed(event);
	}

	public static boolean keyReleased(KeyEvent event) {
		prepareFrame();
		hostKeys.remove(event.key());
		boolean captured = capturedKeys.remove(event.key());
		if (active != null && captured) {
			active.input().keyReleased(event);
		}
		return captured;
	}

	public static boolean charTyped(CharacterEvent event) {
		prepareFrame();
		return active != null && active.input().charTyped(event);
	}
}
