package net.mezzdev.config.gui;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
public final class LegacyEditBox extends net.minecraft.client.gui.components.EditBox {
	private final Font hintFont;
	@Nullable
	private Component hint;
	public LegacyEditBox(Font font, int x, int y, int width, int height, Component label) { super(font, x, y, width, height, label); hintFont = font; }
	public void setHint(Component hint) { this.hint = hint; }
	public int getX() { return x; }
	public void setY(int y) { this.y = y; }
	public void setHeight(int height) { this.height = height; }
	@Override
	public void setFocused(boolean focused) { super.setFocused(focused); }
	@Override
	public void renderButton(PoseStack pose, int mouseX, int mouseY, float tick) {
		super.renderButton(pose, mouseX, mouseY, tick);
		if (hint != null && getValue().isEmpty() && !isFocused())
			hintFont.drawShadow(pose, net.minecraft.locale.Language.getInstance().getVisualOrder(hintFont.substrByWidth(hint, getWidth())), x, y, 0x808080);
	}
}
