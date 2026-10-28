package net.mezzdev.config.gui.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mezzdev.config.gui.quickmenu.QuickMenuManager;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
abstract class QuickMenuMouseMixin {
	// Fabric's 1.21.1 screen API has no drag callback.
	@WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(DDIDD)Z"))
	private boolean mezzConfigGui$mouseDragged(Screen screen, double x, double y, int button, double dragX, double dragY, Operation<Boolean> original) {
		return QuickMenuManager.mouseDragged(x, y, button, dragX, dragY) || original.call(screen, x, y, button, dragX, dragY);
	}
}
