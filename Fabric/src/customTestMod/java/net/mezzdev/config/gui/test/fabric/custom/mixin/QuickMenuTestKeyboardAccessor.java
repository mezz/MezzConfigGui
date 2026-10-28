package net.mezzdev.config.gui.test.fabric.custom.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface QuickMenuTestKeyboardAccessor {
	@Invoker("charTyped")
	void mezzConfigGui$type(long window, int codePoint, int modifiers);
}
