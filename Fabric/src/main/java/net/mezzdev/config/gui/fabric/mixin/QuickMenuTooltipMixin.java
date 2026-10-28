package net.mezzdev.config.gui.fabric.mixin;

import net.mezzdev.config.gui.quickmenu.QuickMenuManager;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
abstract class QuickMenuTooltipMixin {
	@Inject(
		method = "renderTooltipInternal",
		at = @At("HEAD"),
		cancellable = true
	)
	private void mezzConfigGui$hideHostTooltip(CallbackInfo ci) {
		if (QuickMenuManager.shouldHideHostTooltip()) {
			ci.cancel();
		}
	}
}
