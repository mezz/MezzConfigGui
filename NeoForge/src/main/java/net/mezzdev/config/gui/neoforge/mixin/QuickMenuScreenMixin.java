package net.mezzdev.config.gui.neoforge.mixin;

import net.mezzdev.config.gui.quickmenu.QuickMenuManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class QuickMenuScreenMixin {
	// Clear the host tooltip before its deferred render pass so the quick menu owns tooltips.
	@Inject(
		method = "renderWithTooltipAndSubtitles",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderDeferredElements()V")
	)
	private void mezzConfigGui$hideHostTooltip(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		QuickMenuManager.hideHostTooltip(graphics);
	}
}
