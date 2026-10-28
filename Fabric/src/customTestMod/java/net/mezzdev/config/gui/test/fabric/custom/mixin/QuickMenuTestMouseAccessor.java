package net.mezzdev.config.gui.test.fabric.custom.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface QuickMenuTestMouseAccessor {
	@Invoker("onPress")
	void mezzConfigGui$press(long window, int button, int action, int modifiers);

	@Invoker("onMove")
	void mezzConfigGui$move(long window, double x, double y);

	@Accessor("activeButton")
	int mezzConfigGui$getActiveButton();
}
