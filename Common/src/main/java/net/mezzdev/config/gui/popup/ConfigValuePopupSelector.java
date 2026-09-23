package net.mezzdev.config.gui.popup;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValuePopup;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Adapts config value popup content to the internal popup selector lifecycle.
 */
public final class ConfigValuePopupSelector<T> implements ConfigPopupSelector {
	private final IConfigScreenValue<T> configValue;
	private final IConfigValuePopup<T> popup;
	private final Supplier<ImmutableRect2i> anchorAreaSupplier;
	private final BooleanSupplier pendingChangeSupplier;
	private final Consumer<T> setter;
	private ImmutableRect2i area = ImmutableRect2i.EMPTY;

	public ConfigValuePopupSelector(
		IConfigScreenValue<T> configValue,
		IConfigValuePopup<T> popup,
		Supplier<ImmutableRect2i> anchorAreaSupplier,
		BooleanSupplier pendingChangeSupplier,
		Consumer<T> setter
	) {
		this.configValue = configValue;
		this.popup = popup;
		this.anchorAreaSupplier = anchorAreaSupplier;
		this.pendingChangeSupplier = pendingChangeSupplier;
		this.setter = setter;
	}

	@Override
	public void updateBounds(ImmutableRect2i clipArea) {
		IConfigValuePopup.Size size = popup.getPreferredSize(clipArea.getWidth(), clipArea.getHeight());
		area = ConfigPopupPlacement.placeNearAnchor(anchorAreaSupplier.get(), clipArea, size.width(), size.height());
	}

	@Override
	public ImmutableRect2i getArea() {
		return area;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return area.contains(mouseX, mouseY);
	}

	@Override
	public ConfigInfo getInfo() {
		return new ConfigInfo(ConfigValueLocalization.getName(configValue), ConfigValueLocalization.getDescription(configValue));
	}

	@Override
	@Nullable
	public ConfigInfo getTooltipInfo(double mouseX, double mouseY) {
		return popup.getHoveredValue(toRect2i(area), mouseX, mouseY)
			.map(hoveredValue -> ConfigValueInfoFactory.create(configValue, hoveredValue, pendingChangeSupplier.getAsBoolean()))
			.orElse(null);
	}

	@Override
	public void draw(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
		if (!area.isEmpty()) {
			popup.draw(guiGraphics, toRect2i(area), mouseX, mouseY);
		}
	}

	@Override
	public boolean onMouseClicked(UserInput input) {
		int mouseButton = getMouseButton(input);
		Rect2i popupArea = toRect2i(area);
		Optional<T> clickedValue = popup.getClickedValue(popupArea, input.getMouseX(), input.getMouseY(), mouseButton);
		if (clickedValue.isEmpty()) {
			return false;
		}
		if (!input.isSimulate()) {
			setter.accept(clickedValue.get());
		}
		return true;
	}

	@Override
	public boolean onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return popup.mouseScrolled(toRect2i(area), mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean onMouseDragged(double mouseX, double mouseY, int button) {
		Optional<T> draggedValue = popup.getDraggedValue(toRect2i(area), mouseX, mouseY, button);
		draggedValue.ifPresent(setter);
		return draggedValue.isPresent();
	}

	@Override
	public void onMouseReleased(double mouseX, double mouseY, int button) {
		popup.mouseReleased(toRect2i(area), mouseX, mouseY, button);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		return popup.charTyped(codePoint, modifiers, setter);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return popup.keyPressed(keyCode, scanCode, modifiers, setter);
	}

	@Override
	public boolean closesAfterClick() {
		return popup.closesAfterValueSelected();
	}

	@Override
	public void onOpened() {
		popup.onOpened();
	}

	@Override
	public void onClosed() {
		popup.onClosed();
	}

	private static int getMouseButton(UserInput input) {
		InputConstants.Key key = input.getKey();
		if (key.getType() != InputConstants.Type.MOUSE) {
			return -1;
		}
		return key.getValue();
	}

	private static Rect2i toRect2i(ImmutableRect2i area) {
		return new Rect2i(area.getX(), area.getY(), area.getWidth(), area.getHeight());
	}
}
