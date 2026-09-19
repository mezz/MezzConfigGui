package net.mezzdev.config.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/** Keeps the shared screen handlers behind Minecraft's event-object API. */
public abstract class ConfigScreenBase extends Screen {
	private boolean doubleClick;
	private int mouseModifiers;

	protected ConfigScreenBase(Component title) {
		super(title);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		return keyPressed(event.key(), event.scancode(), event.modifiers());
	}

	public boolean keyPressed(int key, int scanCode, int modifiers) {
		return super.keyPressed(new KeyEvent(key, scanCode, modifiers));
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		return keyReleased(event.key(), event.scancode(), event.modifiers());
	}

	public boolean keyReleased(int key, int scanCode, int modifiers) {
		return super.keyReleased(new KeyEvent(key, scanCode, modifiers));
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		boolean handled = false;
		for (char character : Character.toChars(event.codepoint())) {
			handled |= charTyped(character, 0);
		}
		return handled;
	}

	public boolean charTyped(char character, int modifiers) {
		return super.charTyped(new CharacterEvent(character));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		this.doubleClick = doubleClick;
		this.mouseModifiers = event.modifiers();
		try {
			return mouseClicked(event.x(), event.y(), event.button());
		} finally {
			this.doubleClick = false;
			this.mouseModifiers = 0;
		}
	}

	public boolean mouseClicked(double x, double y, int button) {
		return super.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(button, mouseModifiers)), doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		this.mouseModifiers = event.modifiers();
		try {
			return mouseReleased(event.x(), event.y(), event.button());
		} finally {
			this.mouseModifiers = 0;
		}
	}

	public boolean mouseReleased(double x, double y, int button) {
		return super.mouseReleased(new MouseButtonEvent(x, y, new MouseButtonInfo(button, mouseModifiers)));
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		this.mouseModifiers = event.modifiers();
		try {
			return mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
		} finally {
			this.mouseModifiers = 0;
		}
	}

	public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
		return super.mouseDragged(new MouseButtonEvent(x, y, new MouseButtonInfo(button, mouseModifiers)), dragX, dragY);
	}
}
