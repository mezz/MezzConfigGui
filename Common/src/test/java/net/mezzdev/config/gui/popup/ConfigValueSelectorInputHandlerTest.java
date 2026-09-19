package net.mezzdev.config.gui.popup;

import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.input.InputType;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigValueSelectorInputHandlerTest {
	private static final ImmutableRect2i POPUP_AREA = new ImmutableRect2i(10, 10, 100, 100);
	private static final ImmutableRect2i CLIP_AREA = new ImmutableRect2i(0, 0, 200, 200);

	@Test
	@SuppressWarnings("DataFlowIssue")
	void releasingOutsideAfterDraggingDoesNotCloseThePopup() {
		DraggingPopupSelector popup = new DraggingPopupSelector();
		AtomicInteger closeCount = new AtomicInteger();
		ConfigValueSelectorInputHandler handler = new ConfigValueSelectorInputHandler(
			() -> popup,
			() -> CLIP_AREA,
			closeCount::incrementAndGet,
			() -> {}
		);
		Screen screen = null;

		assertTrue(handler.handleUserInput(screen, mouseInput(50, 50, InputType.SIMULATE)).isPresent());
		assertTrue(handler.handleMouseDragged(screen, 150, 50, com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 100, 0).isPresent());
		assertTrue(handler.handleUserInput(screen, mouseInput(150, 50, InputType.EXECUTE)).isPresent());

		assertEquals(1, popup.clickCount);
		assertEquals(1, popup.dragCount);
		assertEquals(1, popup.releaseCount);
		assertEquals(0, closeCount.get());

		assertTrue(handler.handleUserInput(screen, mouseInput(150, 50, InputType.SIMULATE)).isPresent());
		assertTrue(handler.handleUserInput(screen, mouseInput(150, 50, InputType.EXECUTE)).isPresent());
		assertEquals(1, closeCount.get());
	}

	private static UserInput mouseInput(double mouseX, double mouseY, InputType inputType) {
		return UserInput.fromVanilla(mouseX, mouseY, com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, inputType).orElseThrow();
	}

	private static final class DraggingPopupSelector implements ConfigPopupSelector {
		private int clickCount;
		private int dragCount;
		private int releaseCount;

		@Override
		public void updateBounds(ImmutableRect2i clipArea) {

		}

		@Override
		public ImmutableRect2i getArea() {
			return POPUP_AREA;
		}

		@Override
		public boolean isMouseOver(double mouseX, double mouseY) {
			return POPUP_AREA.contains(mouseX, mouseY);
		}

		@Override
		public ConfigInfo getInfo() {
			return new ConfigInfo(Component.empty(), Component.empty());
		}

		@Override
		@Nullable
		public ConfigInfo getTooltipInfo(double mouseX, double mouseY) {
			return null;
		}

		@Override
		public void draw(GuiGraphics guiGraphics, double mouseX, double mouseY) {

		}

		@Override
		public boolean onMouseClicked(UserInput input) {
			clickCount++;
			return true;
		}

		@Override
		public boolean onMouseDragged(double mouseX, double mouseY, int button) {
			dragCount++;
			return true;
		}

		@Override
		public void onMouseReleased(double mouseX, double mouseY, int button) {
			releaseCount++;
		}
	}
}
