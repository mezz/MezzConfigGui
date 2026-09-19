package net.mezzdev.config.gui;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MezzConfigScreenTooltipTest {
	@Test
	void tooltipIsSnapshottedAndConsumedExactlyOnceByTheOverlayPass() {
		ConfigTooltipState<String> state = new ConfigTooltipState<>();
		List<FormattedCharSequence> lines = new ArrayList<>(lines("Config help"));
		state.set(lines, "default", true);
		lines.clear();
		var tooltip = state.take();
		assertNotNull(tooltip);
		assertEquals(1, tooltip.lines().size());
		assertEquals("default", tooltip.positioner());
		assertNull(state.take());
	}
	@Test
	void preservesWidgetTooltipPriorityAndPositioner() {
		ConfigTooltipState<String> state = new ConfigTooltipState<>();
		var focused = lines("Focused widget");
		var hovered = lines("Hovered widget");
		state.set(focused, "default", false);
		state.set(hovered, "custom", false);
		assertEquals(focused, state.take().lines());
		state.set(focused, "default", false);
		state.set(hovered, "custom", true);
		var tooltip = state.take();
		assertNotNull(tooltip);
		assertEquals(hovered, tooltip.lines());
		assertEquals("custom", tooltip.positioner());
	}
	@Test
	void newFrameDropsStaleTooltipWithoutAffectingAnotherScreen() {
		ConfigTooltipState<String> state = new ConfigTooltipState<>();
		ConfigTooltipState<String> other = new ConfigTooltipState<>();
		state.set(lines("Old frame"), "default", true);
		other.set(lines("Other screen"), "default", true);
		state.clear();
		assertNull(state.take());
		assertNotNull(other.take());
	}
	private static List<FormattedCharSequence> lines(String text) { return List.of(FormattedCharSequence.forward(text, Style.EMPTY)); }
}
