package net.mezzdev.config.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Base class for MezzConfig screens that exposes their occupied area to optional GUI integrations.
 */
public abstract class MezzConfigScreen extends Screen {
	@Nullable
	private DeferredTooltip deferredTooltip;

	protected MezzConfigScreen(Component title) {
		super(title);
	}

	/**
	 * Returns the part of the screen occupied by this GUI, or {@code null} before it has been laid out.
	 */
	@Nullable
	public abstract Rect2i getScreenArea();

	@Override
	public void setTooltipForNextRenderPass(List<FormattedCharSequence> tooltip, ClientTooltipPositioner positioner, boolean override) {
		if (deferredTooltip == null || override) {
			deferredTooltip = new DeferredTooltip(List.copyOf(tooltip), positioner);
		}
	}

	@Override
	public void clearTooltipForNextRenderPass() {
		super.clearTooltipForNextRenderPass();
		deferredTooltip = null;
	}

	/** Called by each loader after screen overlays, which render after vanilla's tooltip pass. */
	public void renderDeferredTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		DeferredTooltip tooltip = takeDeferredTooltip();
		if (tooltip != null) {
			guiGraphics.renderTooltip(font, tooltip.lines(), tooltip.positioner(), mouseX, mouseY);
		}
	}

	@Nullable
	DeferredTooltip takeDeferredTooltip() {
		DeferredTooltip tooltip = deferredTooltip;
		deferredTooltip = null;
		return tooltip;
	}

	record DeferredTooltip(List<FormattedCharSequence> lines, ClientTooltipPositioner positioner) {
	}
}
