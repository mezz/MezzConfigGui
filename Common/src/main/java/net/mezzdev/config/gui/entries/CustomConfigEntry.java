package net.mezzdev.config.gui.entries;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditor;
import net.mezzdev.config.gui.api.IConfigValuePopup;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.popup.ConfigPopupSelector;
import net.mezzdev.config.gui.popup.ConfigValuePopupSelector;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Config entry widget backed by a custom value editor registered through the config GUI API.
 */
final class CustomConfigEntry<T> extends ConfigEntryWidget<T> {
	private static final int MIN_CONTROL_WIDTH = 18;
	private static final int MIN_CONTROL_HEIGHT = 18;
	private static final int MIN_NAME_WIDTH = 68;

	private final IConfigValueEditor<T> editor;
	private final Consumer<ConfigPopupSelector> valueSelectorOpener;
	private ImmutableRect2i valueArea = ImmutableRect2i.EMPTY;

	CustomConfigEntry(
		IConfigScreenValue<T> value,
		IConfigValueEditor<T> editor,
		Consumer<ConfigPopupSelector> valueSelectorOpener,
		ConfigTextures textures
	) {
		super(value, textures);
		this.editor = editor;
		this.valueSelectorOpener = valueSelectorOpener;
	}

	@Override
	public void updateBounds(ImmutableRect2i area) {
		super.updateBounds(area);
		T value = getValue();
		int preferredWidth = Math.max(MIN_CONTROL_WIDTH, editor.getControlWidth(configValue, value));
		int preferredHeight = Math.max(MIN_CONTROL_HEIGHT, editor.getControlHeight(configValue, value));
		int availableWidth = Math.max(MIN_CONTROL_WIDTH, area.getWidth() - VALUE_CONTROL_RIGHT_RESERVE - MIN_NAME_WIDTH);
		int totalWidth = Math.min(preferredWidth, availableWidth);
		int totalHeight = Math.min(preferredHeight, area.getHeight());
		valueArea = new ImmutableRect2i(
			area.getX() + area.getWidth() - totalWidth - VALUE_CONTROL_RIGHT_RESERVE,
			area.getY() + (area.getHeight() - totalHeight) / 2,
			totalWidth,
			totalHeight
		);
		recomputeNameArea(area, Math.max(NAME_RIGHT_RESERVE, totalWidth + VALUE_CONTROL_RIGHT_RESERVE + 4));
	}

	@Override
	protected void drawContent(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		drawName(guiGraphics);

		boolean hovered = valueArea.contains(mouseX, mouseY);
		drawButtonBackground(guiGraphics, getTextures(), valueArea, true, hovered);
		editor.draw(guiGraphics, toRect2i(valueArea), configValue, getValue(), hovered, hasPendingChange());
	}

	@Override
	@Nullable
	public ConfigInfo getTooltipInfo(double mouseX, double mouseY) {
		@Nullable
		ConfigInfo resetInfo = super.getTooltipInfo(mouseX, mouseY);
		if (resetInfo != null) {
			return resetInfo;
		}
		if (valueArea.contains(mouseX, mouseY)) {
			Optional<ConfigInfo> tooltipInfo = editor.getTooltipInfo(toRect2i(valueArea), configValue, getValue(), hasPendingChange(), mouseX, mouseY);
			if (tooltipInfo.isPresent()) {
				return tooltipInfo.get();
			}
			return ConfigValueInfoFactory.create(configValue, getValue(), hasPendingChange());
		}
		return null;
	}

	@Override
	protected boolean onMouseClicked(UserInput input) {
		if (super.onMouseClicked(input)) {
			return true;
		}
		if (valueArea.contains(input.getMouseX(), input.getMouseY())) {
			Rect2i rect = toRect2i(valueArea);
			int button = getMouseButton(input);
			Optional<IConfigValuePopup<T>> popup = editor.createPopup(
				rect,
				configValue,
				getValue(),
				input.getMouseX(),
				input.getMouseY(),
				button
			);
			if (popup.isPresent()) {
				if (!input.isSimulate()) {
					ConfigValuePopupSelector<T> selector = new ConfigValuePopupSelector<>(
						configValue,
						popup.get(),
						() -> valueArea,
						this::hasPendingChange,
						this::setValue
					);
					valueSelectorOpener.accept(selector);
				}
				return true;
			}
			Optional<T> clickedValue = editor.getClickedValue(
				rect,
				configValue,
				getValue(),
				input.getMouseX(),
				input.getMouseY(),
				button
			);
			if (clickedValue.isEmpty()) {
				return false;
			}
			if (!input.isSimulate()) {
				setValue(clickedValue.get());
			}
			return true;
		}
		return false;
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
