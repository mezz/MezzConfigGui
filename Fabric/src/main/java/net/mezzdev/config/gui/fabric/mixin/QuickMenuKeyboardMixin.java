package net.mezzdev.config.gui.fabric.mixin;

import net.mezzdev.config.gui.quickmenu.QuickMenuManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
abstract class QuickMenuKeyboardMixin {
	// Route characters here because the Fabric Screen API has no character input event on 1.21.11.
	@Inject(
		method = "charTyped",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;charTyped(Lnet/minecraft/client/input/CharacterEvent;)Z"),
		cancellable = true
	)
	private void mezzConfigGui$routeCharacter(long window, CharacterEvent event, CallbackInfo ci) {
		if (QuickMenuManager.charTyped(event)) {
			ci.cancel();
		}
	}
}
