package net.mezzdev.config.gui.fabric.mixin;

import net.mezzdev.config.gui.quickmenu.QuickMenuManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
abstract class QuickMenuKeyboardMixin {
	// Fabric's 1.21.1 screen API has no character input callback.
	@Inject(
		method = "charTyped",
		at = @At("HEAD"),
		cancellable = true
	)
	private void mezzConfigGui$charTyped(long window, int codePoint, int modifiers, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		if (window == minecraft.getWindow().getWindow() && minecraft.getOverlay() == null &&
			QuickMenuManager.charTyped(codePoint, modifiers)
		) {
			ci.cancel();
		}
	}
}
