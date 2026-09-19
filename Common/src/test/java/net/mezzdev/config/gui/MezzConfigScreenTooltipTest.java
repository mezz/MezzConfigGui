package net.mezzdev.config.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MezzConfigScreenTooltipTest {
	@Test
	@SuppressWarnings("DataFlowIssue")
	void tooltipSurvivesVanillaRenderPassUntilOverlayPassAndIsConsumedOnce() {
		List<FormattedCharSequence> lines = lines("Config help");
		MezzConfigScreen screen = new TestScreen() {
			@Override
			public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
				setTooltipForNextRenderPass(lines);
			}
		};

		// A null renderer also catches accidental rendering in vanilla's earlier tooltip pass.
		screen.renderWithTooltip(null, 10, 20, 0);
		MezzConfigScreen.DeferredTooltip tooltip = screen.takeDeferredTooltip();
		assertNotNull(tooltip);
		assertEquals(lines, tooltip.lines());
		assertSame(DefaultTooltipPositioner.INSTANCE, tooltip.positioner());
		assertNull(screen.takeDeferredTooltip());
	}

	@Test
	void preservesWidgetTooltipPriorityAndPositioner() {
		MezzConfigScreen screen = new TestScreen();
		List<FormattedCharSequence> focused = lines("Focused widget");
		List<FormattedCharSequence> hovered = lines("Hovered widget");
		ClientTooltipPositioner positioner = (width, height, x, y, tooltipWidth, tooltipHeight) -> new Vector2i(x, y);
		screen.setTooltipForNextRenderPass(focused, DefaultTooltipPositioner.INSTANCE, false);
		screen.setTooltipForNextRenderPass(hovered, positioner, false);
		assertEquals(focused, screen.takeDeferredTooltip().lines());

		screen.setTooltipForNextRenderPass(focused, DefaultTooltipPositioner.INSTANCE, false);
		screen.setTooltipForNextRenderPass(hovered, positioner, true);
		MezzConfigScreen.DeferredTooltip tooltip = screen.takeDeferredTooltip();
		assertNotNull(tooltip);
		assertEquals(hovered, tooltip.lines());
		assertSame(positioner, tooltip.positioner());
	}

	@Test
	void newFrameDropsStaleTooltipWithoutAffectingAnotherScreen() {
		MezzConfigScreen screen = new TestScreen();
		MezzConfigScreen other = new TestScreen();
		screen.setTooltipForNextRenderPass(lines("Old frame"));
		other.setTooltipForNextRenderPass(lines("Other screen"));

		screen.clearTooltipForNextRenderPass();

		assertNull(screen.takeDeferredTooltip());
		assertNotNull(other.takeDeferredTooltip());
	}

	private static List<FormattedCharSequence> lines(String text) {
		return List.of(FormattedCharSequence.forward(text, Style.EMPTY));
	}

	private static class TestScreen extends MezzConfigScreen {
		TestScreen() {
			super(Component.empty());
		}

		@Override
		@Nullable
		public Rect2i getScreenArea() {
			return null;
		}
	}
}
