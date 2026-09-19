package net.mezzdev.config.gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
public final class ConfigWidgets {
	private ConfigWidgets() {}
	public static Button button(Component label, Button.OnPress action, int x, int y, int width, int height, Component tooltip) {
		return new Button(x, y, width, height, label, action, new Button.OnTooltip() {
			@Override
			public void onTooltip(Button button, com.mojang.blaze3d.vertex.PoseStack pose, int mouseX, int mouseY) {
				if (Minecraft.getInstance().screen instanceof MezzConfigScreen screen)
					screen.setTooltipForNextRenderPass(Minecraft.getInstance().font.split(tooltip, 280));
			}
			@Override
			public void narrateTooltip(java.util.function.Consumer<Component> narration) { narration.accept(tooltip); }
		});
	}
}
