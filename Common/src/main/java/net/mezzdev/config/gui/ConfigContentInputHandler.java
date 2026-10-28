package net.mezzdev.config.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.input.InputType;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.popup.ConfigPopupSelector;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Routes input to the page's editors and popup and keeps text focus in sync. */
final class ConfigContentInputHandler {
	private final ConfigScrollState scroll;
	private final ConfigEditorInteraction interaction;
	private final Supplier<@Nullable Screen> parent;
	private final Supplier<ImmutableRect2i> area;
	private final Runnable layout;
	private ConfigInputRouter router = new ConfigInputRouter(List.of());

	ConfigContentInputHandler(ConfigScrollState scroll, ConfigEditorInteraction interaction,
		Supplier<@Nullable Screen> parent, Supplier<ImmutableRect2i> area, Runnable layout) {
		this.scroll = scroll;
		this.interaction = interaction;
		this.parent = parent;
		this.area = area;
		this.layout = layout;
	}

	void setEntries(List<ConfigEntryWidget<?>> entries) {
		List<ConfigInputHandler> handlers = new ArrayList<>();
		handlers.add(interaction.createPopupInputHandler(layout));
		entries.stream().map(entry -> new ConfigEntryInputHandler(entry, area)).forEach(handlers::add);
		router = new ConfigInputRouter(handlers);
	}

	private boolean hasPopup() {
		return interaction.getPopup() != null;
	}

	public boolean mousePressed(double x, double y, int button, int modifiers) {
		if (!hasPopup() && button == InputConstants.MOUSE_BUTTON_LEFT && scroll.startDrag(x, y)) {
			layout.run();
			return true;
		}
		return UserInput.fromVanilla(x, y, button, modifiers, InputType.SIMULATE)
			.map(event -> router.handleUserInput(parent.get(), event)).orElse(false);
	}

	public void mouseReleased(double x, double y, int button, int modifiers) {
		if (scroll.drag(y)) {
			scroll.stopDrag();
			layout.run();
			return;
		}
		UserInput.fromVanilla(x, y, button, modifiers, InputType.EXECUTE).ifPresent(event -> router.handleUserInput(parent.get(), event));
		layout.run();
	}

	public void mouseDragged(double x, double y, int button, double dragX, double dragY) {
		if (scroll.drag(y)) {
			layout.run();
		} else if (router.handleMouseDragged(parent.get(), x, y, button, dragX, dragY) && router.allowsContentAutoScrollForDrag(button)) {
			if (scroll.autoScrollForDrag(y)) {
				layout.run();
				router.handleMouseDragged(parent.get(), x, y, button, dragX, dragY);
			}
		}
	}

	public void mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (!router.handleMouseScrolled(x, y, scrollX, scrollY) && !hasPopup() && area.get().contains(x, y)) {
			scroll.scroll(scrollY);
		}
	}

	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (hasPopup()) {
			interaction.popupKeyPressed(key, scanCode, modifiers);
			return true;
		}
		boolean capturing = interaction.isCapturingKeyboardInput();
		return interaction.keyPressed(key, scanCode, modifiers) || capturing;
	}

	public void keyReleased(int key, int scanCode, int modifiers) {
		interaction.keyReleased(key, scanCode, modifiers);
	}

	public void charTyped(char character, int modifiers) {
		ConfigPopupSelector popup = interaction.getPopup();
		if (popup != null) {
			popup.charTyped(character, modifiers);
		} else {
			interaction.charTyped(character, modifiers);
		}
	}

	public void cancelInput() {
		interaction.flushPendingInput(router);
		scroll.stopDrag();
	}
}
