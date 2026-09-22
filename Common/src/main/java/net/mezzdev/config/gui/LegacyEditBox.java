package net.mezzdev.config.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class LegacyEditBox extends EditBox {
	public LegacyEditBox(Font font, int x, int y, int width, int height, Component label) {
		super(font, x, y, width, height, label);
	}

	public void setHeight(int height) {
		this.height = height;
	}
}
