package net.mezzdev.config.gui;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/** Holds a tooltip until the final loader overlay pass. */
final class ConfigTooltipState<P> {
	@Nullable
	private Tooltip<P> pending;
	void set(List<FormattedCharSequence> lines, P positioner, boolean override) {
		if (pending == null || override)
			pending = new Tooltip<>(List.copyOf(lines), positioner);
	}
	void clear() { pending = null; }
	@Nullable
	Tooltip<P> take() { Tooltip<P> result = pending; pending = null; return result; }
	record Tooltip<P>(List<FormattedCharSequence> lines, P positioner) {}
}
