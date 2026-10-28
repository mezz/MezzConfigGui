package net.mezzdev.config.gui.fabric.mixin;

import net.mezzdev.config.gui.quickmenu.QuickMenuManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class QuickMenuScreenMixin {
	// Fabric 26.1.2 has no foreground event. Hide the host tooltip just before deferred elements are extracted.
	@Inject(
		method = "extractRenderStateWithTooltipAndSubtitles",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;extractDeferredElements(IIF)V")
	)
	private void mezzConfigGui$hideHostTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		QuickMenuManager.hideHostTooltip(graphics);
	}
}
