package net.mezzdev.config.gui.quickmenu;

import org.lwjgl.glfw.GLFW;
import net.mezzdev.config.gui.ConfigContentPage;
import net.mezzdev.config.gui.ConfigScreenLayout.ResizeHandle;
import net.mezzdev.config.gui.ConfigScrollState;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.quickmenu.QuickMenuReorderDrag.Preview;
import net.mezzdev.config.gui.quickmenu.QuickMenuTarget.Action;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Routes mouse and keyboard input and owns gestures until release or cancellation. */
final class QuickMenuInputHandler<T> {
	private final QuickConfigMenu<T> menu;
	private final QuickMenuModel<T> model;
	private final ConfigContentPage page;
	private final QuickMenuLayout layout;
	private final ConfigScrollState scroll;
	private @Nullable ResizeDrag resizeDrag;
	private @Nullable QuickMenuReorderDrag<T> reorderDrag;
	private @Nullable QuickMenuTarget<T> pressed;
	private int pressedButton = -1;

	QuickMenuInputHandler(QuickConfigMenu<T> menu, QuickMenuModel<T> model, ConfigContentPage page, QuickMenuLayout layout) {
		this.menu = menu;
		this.model = model;
		this.page = page;
		this.layout = layout;
		this.scroll = page.getScrollState();
	}

	void validateSelection() {
		if (reorderDrag != null && !reorderDrag.selection().equals(model.selection().selected())) {
			cancelInput();
		}
	}

	void stepScroll() {
		if (model.isEditing() && reorderDrag != null && reorderDrag.isActive()) {
			scroll.autoScrollForDrag(reorderDrag.mouseY());
		}
		page.stepScroll();
	}

	@Nullable
	Preview<T> reorderPreview() {
		if (reorderDrag == null) {
			return null;
		}
		return reorderDrag.preview(layout);
	}

	ResizeHandle resizeHandleAt(double x, double y) {
		if (resizeDrag != null) {
			return resizeDrag.handle();
		}
		return layout.getResizeHandle(x, y);
	}

	public boolean mousePressed(double mouseX, double mouseY, int button, int modifiers) {
		menu.refresh();
		pressed = null;
		reorderDrag = null;
		pressedButton = -1;
		if (!menu.isOpen()) {
			return false;
		}
		pressedButton = button;
		ResizeHandle handle = layout.getResizeHandle(mouseX, mouseY);
		if (!model.isEditing() && page.hasPopup() && (layout.area().contains(mouseX, mouseY) || page.isMouseOverPopup(mouseX, mouseY))) {
			page.mousePressed(mouseX, mouseY, button, modifiers);
			pressed = new QuickMenuTarget<>(null, Action.PAGE);
		} else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && handle != ResizeHandle.NONE) {
			page.cancelInput();
			resizeDrag = new ResizeDrag(handle, layout.area(), mouseX, mouseY);
			pressed = new QuickMenuTarget<>(null, Action.RESIZE);
		} else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && menu.isMouseOverAnchor(mouseX, mouseY)) {
			pressed = new QuickMenuTarget<>(null, Action.ANCHOR);
		} else if (!layout.area().contains(mouseX, mouseY)) {
			pressed = new QuickMenuTarget<>(null, Action.CLOSE);
		} else if (model.isEditing() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && scroll.startDrag(mouseX, mouseY)) {
			pressed = new QuickMenuTarget<>(null, Action.SCROLL);
		} else if (!model.isEditing() && layout.getListArea().contains(mouseX, mouseY)) {
			page.mousePressed(mouseX, mouseY, button, modifiers);
			pressed = new QuickMenuTarget<>(null, Action.PAGE);
		} else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			pressed = targetAt(mouseX, mouseY);
			if (ConfigGuiOptions.enableDragReordering() && pressed != null && pressed.id() != null && pressed.action() != Action.MEMBERSHIP && model.selection().selected().contains(pressed.id())) {
				reorderDrag = new QuickMenuReorderDrag<>(pressed.id(), model.selection().selected(), layout.getRowArea(model.rows().indexOf(pressed.id())), mouseX, mouseY);
			}
		}
		return true;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button, int modifiers) {
		if (button != pressedButton) {
			return false;
		}
		menu.refresh();
		QuickMenuTarget<T> target = pressed;
		QuickMenuReorderDrag<T> drag = reorderDrag;
		pressed = null;
		reorderDrag = null;
		pressedButton = -1;
		if (!menu.isOpen()) {
			return true;
		}
		if (drag != null && drag.isActive()) {
			drag.moveTo(mouseX, mouseY);
			model.selection().move(drag.id(), drag.targetIndex(layout) - drag.selection().indexOf(drag.id()));
			menu.refresh();
			return true;
		}
		if (target != null && target.action() == Action.RESIZE) {
			resizeTo(mouseX, mouseY);
			resizeDrag = null;
			return true;
		}
		if (target != null && target.action() == Action.SCROLL) {
			scroll.drag(mouseY);
			scroll.stopDrag();
			return true;
		}
		if (target != null && target.action() == Action.PAGE) {
			page.mouseReleased(mouseX, mouseY, button, modifiers);
			return true;
		}
		if (target != null && (target.action() == Action.CLOSE || target.equals(targetAt(mouseX, mouseY)))) {
			menu.activate(target);
		}
		return true;
	}

	private @Nullable QuickMenuTarget<T> targetAt(double x, double y) {
		if (menu.isMouseOverAnchor(x, y)) {
			return new QuickMenuTarget<>(null, Action.ANCHOR);
		}
		if (layout.fullConfigButtonArea().contains(x, y)) {
			return new QuickMenuTarget<>(null, Action.FULL_CONFIG);
		}
		if (layout.headerButtonArea().contains(x, y)) {
			return new QuickMenuTarget<>(null, Action.EDIT);
		}
		if (!model.isEditing() || !layout.editorArea().contains(x, y)) {
			return null;
		}
		List<T> selected = model.selection().selected();
		for (int row = layout.firstVisibleRow(); row < layout.lastVisibleRow(); row++) {
			T id = model.rows().get(row);
			if (!layout.getRowArea(row).contains(x, y)) {
				continue;
			}
			if (layout.controlArea(row, 0).contains(x, y)) {
				return new QuickMenuTarget<>(id, Action.MEMBERSHIP);
			}
			int index = selected.indexOf(id);
			if (index >= 0) {
				if (index > 0 && layout.controlArea(row, 2).contains(x, y)) {
					return new QuickMenuTarget<>(id, Action.UP);
				}
				if (index + 1 < selected.size() && layout.controlArea(row, 1).contains(x, y)) {
					return new QuickMenuTarget<>(id, Action.DOWN);
				}
				return new QuickMenuTarget<>(id, Action.REORDER);
			}
		}
		return null;
	}

	public boolean mouseScrolled(double x, double y, double scrollX, double amount) {
		menu.refresh();
		if (!menu.isOpen()) {
			return false;
		}
		if (!model.isEditing()) {
			page.mouseScrolled(x, y, scrollX, amount);
		} else if (layout.getListArea().insetBy(1).contains(x, y)) {
			scroll.scroll(amount);
		}
		return true;
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!menu.isOpen()) {
			return false;
		}
		if (!model.isEditing() && page.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			menu.close();
		}
		return true;
	}

	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (!menu.isOpen()) {
			return false;
		}
		if (!model.isEditing()) {
			page.keyReleased(keyCode, scanCode, modifiers);
		}
		return true;
	}

	public boolean charTyped(int codePoint, int modifiers) {
		if (!menu.isOpen()) {
			return false;
		}
		if (!model.isEditing()) {
			for (char character : Character.toChars(codePoint)) {
				page.charTyped(character, modifiers);
			}
		}
		return true;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (!menu.isOpen() || button != pressedButton) {
			return false;
		}
		if (reorderDrag != null) {
			reorderDrag.moveTo(mouseX, mouseY);
		} else if (resizeDrag != null) {
			resizeTo(mouseX, mouseY);
		} else if (pressed != null && pressed.action() == Action.SCROLL) {
			scroll.drag(mouseY);
		} else if (!model.isEditing() && pressed != null && pressed.action() == Action.PAGE) {
			page.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		}
		return true;
	}

	public void cancelInput() {
		resizeDrag = null;
		reorderDrag = null;
		pressed = null;
		pressedButton = -1;
		page.cancelInput();
	}

	private void resizeTo(double x, double y) {
		if (resizeDrag != null) {
			layout.resize(resizeDrag.handle(), resizeDrag.area(), resizeDrag.x(), resizeDrag.y(), x, y);
			menu.refresh();
		}
	}

	private record ResizeDrag(ResizeHandle handle, ImmutableRect2i area, double x, double y) {}
}
