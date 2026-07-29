package net.mezzdev.config.gui.entries;

import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Renders the reset-to-default icon used in config value rows.
 */
final class ConfigResetIcon {
	private static final ItemStack RESET_ICON = new ItemStack(Items.CLOCK);
	private static final int ICON_SIZE = 16;

	private ConfigResetIcon() {

	}

	public static void draw(GuiGraphics guiGraphics, ImmutableRect2i buttonArea, boolean active) {
		int iconX = buttonArea.getX() + Math.round((buttonArea.getWidth() - ICON_SIZE) / 2.0f);
		int iconY = buttonArea.getY() + Math.round((buttonArea.getHeight() - ICON_SIZE) / 2.0f);
		if (active) {
			guiGraphics.renderFakeItem(RESET_ICON, iconX, iconY);
			return;
		}

		guiGraphics.pose().pushPose();
		guiGraphics.setColor(0.3f, 0.3f, 0.3f, 0.5f);
		guiGraphics.renderFakeItem(RESET_ICON, iconX, iconY);
		guiGraphics.setColor(1f, 1f, 1f, 1f);
		guiGraphics.pose().popPose();
	}
}
