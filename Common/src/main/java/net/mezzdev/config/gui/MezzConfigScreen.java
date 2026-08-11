package net.mezzdev.config.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for MezzConfig screens that exposes their occupied area to optional GUI integrations.
 */
public abstract class MezzConfigScreen extends Screen {
	protected MezzConfigScreen(Component title) {
		super(title);
	}

	/**
	 * Returns the part of the screen occupied by this GUI, or {@code null} before it has been laid out.
	 */
	@Nullable
	public abstract Rect2i getScreenArea();
}
