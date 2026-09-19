package net.mezzdev.config.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base class for MezzConfig screens that exposes their occupied area to optional GUI integrations.
 */
public abstract class MezzConfigScreen extends ConfigScreenBase {
	private final ConfigTooltipState<ClientTooltipPositioner> tooltipState = new ConfigTooltipState<>();

	protected MezzConfigScreen(Component title) {
		super(title);
	}

	/**
	 * Returns the part of the screen occupied by this GUI, or {@code null} before it has been laid out.
	 */
	@Nullable
	public abstract Rect2i getScreenArea();

	public void setTooltipForNextRenderPass(List<FormattedCharSequence> tooltip) {
		setTooltipForNextRenderPass(tooltip, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, true);
	}

	public void setTooltipForNextRenderPass(List<FormattedCharSequence> tooltip, ClientTooltipPositioner positioner, boolean override) {
		tooltipState.set(tooltip, positioner, override);
	}

	public void clearTooltipForNextRenderPass() {
		tooltipState.clear();
	}

	/** Called by each loader after screen overlays, which render after vanilla's tooltip pass. */
	public void renderDeferredTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		ConfigTooltipState.Tooltip<ClientTooltipPositioner> tooltip = tooltipState.take();
		if (tooltip != null) {
			ConfigRenderUtil.renderTooltip(guiGraphics, font, tooltip.lines(), tooltip.positioner(), mouseX, mouseY);
		}
	}

}
