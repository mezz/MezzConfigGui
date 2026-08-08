package net.mezzdev.config.gui.popup;

import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.input.UserInput;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * Internal lifecycle wrapper for popup content that edits a config value.
 */
public interface ConfigPopupSelector {
	void updateBounds(ImmutableRect2i clipArea);

	ImmutableRect2i getArea();

	boolean isMouseOver(double mouseX, double mouseY);

	ConfigInfo getInfo();

	@Nullable
	ConfigInfo getTooltipInfo(double mouseX, double mouseY);

	void draw(GuiGraphics guiGraphics, double mouseX, double mouseY);

	boolean onMouseClicked(UserInput input);

	default boolean onMouseDragged(double mouseX, double mouseY, int button) {
		return false;
	}

	default boolean charTyped(char codePoint, int modifiers) {
		return false;
	}

	default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return false;
	}

	default boolean closesAfterClick() {
		return true;
	}
}
