package net.mezzdev.config.gui.popup;

import net.mezzdev.config.gui.api.IConfigValuePopup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Adapts every popup interaction to a validated backing value without changing its config type.
 */
public final class MappedConfigValuePopup<S, T> implements IConfigValuePopup<T> {
	private final IConfigValuePopup<S> delegate;
	private final Function<S, Optional<T>> mapper;

	public MappedConfigValuePopup(IConfigValuePopup<S> delegate, Function<S, Optional<T>> mapper) {
		this.delegate = delegate;
		this.mapper = mapper;
	}

	@Override
	public int getWidth() {
		return delegate.getWidth();
	}

	@Override
	public int getHeight() {
		return delegate.getHeight();
	}

	@Override
	public Size getPreferredSize(int availableWidth, int availableHeight) {
		return delegate.getPreferredSize(availableWidth, availableHeight);
	}

	@Override
	public Optional<T> getHoveredValue(Rect2i area, double mouseX, double mouseY) {
		return delegate.getHoveredValue(area, mouseX, mouseY).flatMap(mapper);
	}

	@Override
	public Optional<T> getClickedValue(Rect2i area, double mouseX, double mouseY, int button) {
		return delegate.getClickedValue(area, mouseX, mouseY, button).flatMap(mapper);
	}

	@Override
	public Optional<T> getDraggedValue(Rect2i area, double mouseX, double mouseY, int button) {
		return delegate.getDraggedValue(area, mouseX, mouseY, button).flatMap(mapper);
	}

	@Override
	public void draw(GuiGraphicsExtractor guiGraphics, Rect2i area, double mouseX, double mouseY) {
		delegate.draw(guiGraphics, area, mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(Rect2i area, double mouseX, double mouseY, double scrollX, double scrollY) {
		return delegate.mouseScrolled(area, mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void mouseReleased(Rect2i area, double mouseX, double mouseY, int button) {
		delegate.mouseReleased(area, mouseX, mouseY, button);
	}

	@Override
	public void onOpened() {
		delegate.onOpened();
	}

	@Override
	public void onClosed() {
		delegate.onClosed();
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers, Consumer<T> valueConsumer) {
		return delegate.charTyped(codePoint, modifiers, value -> mapper.apply(value).ifPresent(valueConsumer));
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers, Consumer<T> valueConsumer) {
		return delegate.keyPressed(keyCode, scanCode, modifiers, value -> mapper.apply(value).ifPresent(valueConsumer));
	}

	@Override
	public boolean closesAfterValueSelected() {
		return delegate.closesAfterValueSelected();
	}
}
