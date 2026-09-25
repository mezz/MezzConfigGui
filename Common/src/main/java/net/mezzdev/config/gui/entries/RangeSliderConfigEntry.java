package net.mezzdev.config.gui.entries;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.ConfigInputHandler;
import net.mezzdev.config.gui.ConfigInputUtil;
import net.mezzdev.config.gui.ConfigRenderUtil;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/** A numeric range shown on one track with independently draggable lower and upper handles. */
final class RangeSliderConfigEntry<T> extends ConfigEntryWidget<ConfigValueRange<T>> {
	private static final Identifier TRACK = Identifier.withDefaultNamespace("widget/slider");
	private static final Identifier HANDLE = Identifier.withDefaultNamespace("widget/slider_handle");
	private static final Identifier HIGHLIGHTED_HANDLE = Identifier.withDefaultNamespace("widget/slider_handle_highlighted");
	private static final int HEIGHT = 20;
	private static final int HANDLE_WIDTH = 8;
	private final RangeSliderModel<T> model;
	private ImmutableRect2i sliderArea = ImmutableRect2i.EMPTY;
	@Nullable
	private RangeSliderDrag<T> dragging;

	RangeSliderConfigEntry(IConfigScreenValue<ConfigValueRange<T>> value, NumberSliderModel<T> numbers, ConfigTextures textures) {
		super(value, textures);
		this.model = new RangeSliderModel<>(numbers);
	}

	@Override
	public void updateBounds(ImmutableRect2i area) {
		super.updateBounds(area);
		int width = getValueColumnWidth(area, HANDLE_WIDTH * 2);
		sliderArea = new ImmutableRect2i(area.getX() + area.getWidth() - width - VALUE_CONTROL_RIGHT_RESERVE,
			area.getY() + (area.getHeight() - HEIGHT) / 2, width, HEIGHT);
		recomputeNameArea(area, getValueColumnNameRightReserve(area, HANDLE_WIDTH * 2));
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
		drawName(graphics);
		ConfigRenderUtil.blitSprite(graphics, TRACK, sliderArea.getX(), sliderArea.getY(), sliderArea.getWidth(), HEIGHT);
		int minX = getHandleX(RangeSliderModel.Handle.MINIMUM);
		int maxX = getHandleX(RangeSliderModel.Handle.MAXIMUM);
		int bandY = sliderArea.getY() + HEIGHT - 5;
		graphics.fill(minX + HANDLE_WIDTH / 2, bandY, maxX + HANDLE_WIDTH / 2, bandY + 2,
			ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_RANGE_FILL));
		RangeSliderModel.Handle highlighted = null;
		if (dragging != null) {
			highlighted = dragging.getHandle();
		}
		if (highlighted == null && isEditable() && sliderArea.contains(mouseX, mouseY)) {
			highlighted = model.getNearestHandle(getValue(), getPosition(sliderArea, mouseX));
		}
		for (RangeSliderModel.Handle handle : RangeSliderModel.Handle.values()) {
			Identifier sprite = HANDLE;
			if (handle == highlighted) {
				sprite = HIGHLIGHTED_HANDLE;
			}
			ConfigRenderUtil.blitSprite(graphics, sprite, getHandleX(handle), sliderArea.getY(), HANDLE_WIDTH, HEIGHT);
		}
		drawCenteredButtonText(graphics, Minecraft.getInstance().font, getValueName(getValue()), sliderArea, getConfiguredTextColor());
	}

	private int getHandleX(RangeSliderModel.Handle handle) {
		return sliderArea.getX() + (int) Math.round(model.getPosition(getValue(), handle) * (sliderArea.getWidth() - HANDLE_WIDTH));
	}

	@Override
	@Nullable
	public ConfigInfo getTooltipInfo(double mouseX, double mouseY) {
		ConfigInfo resetInfo = super.getTooltipInfo(mouseX, mouseY);
		if (resetInfo != null) {
			return resetInfo;
		}
		if (sliderArea.contains(mouseX, mouseY)) {
			return ConfigValueInfoFactory.createUpdateInfo(configValue, getValue(), hasPendingChange());
		}
		return null;
	}

	private static double getPosition(ImmutableRect2i area, double mouseX) {
		return (mouseX - area.getX() - HANDLE_WIDTH / 2.0) / Math.max(1, area.getWidth() - HANDLE_WIDTH);
	}

	private void beginSliding(double mouseX) {
		dragging = new RangeSliderDrag<>(model, getValue(), mouseX, sliderArea.getX() + HANDLE_WIDTH / 2.0,
			sliderArea.getWidth() - HANDLE_WIDTH, HANDLE_WIDTH, ConfigInputUtil.hasShiftDown());
	}

	private void updateFromMouse(double mouseX) {
		if (dragging != null) {
			setValue(dragging.update(getValue(), mouseX, ConfigInputUtil.hasShiftDown()));
		}
	}

	private void stopSliding() {
		dragging = null;
	}

	@Override
	public ConfigInputHandler createInputHandler() {
		return new ConfigInputHandler() {
			@Override
			public Optional<ConfigInputHandler> handleUserInput(@Nullable Screen screen, UserInput input) {
				if (!isEditable() || !ConfigInputUtil.isLeftClick(input)) {
					stopSliding();
					return Optional.empty();
				}
				if (input.isSimulate()) {
					stopSliding();
					if (RangeSliderConfigEntry.this.onMouseClicked(input)) {
						return Optional.of(this);
					}
					if (sliderArea.contains(input.getMouseX(), input.getMouseY())) {
						beginSliding(input.getMouseX());
						return Optional.of(this);
					}
					return Optional.empty();
				}
				if (dragging != null) {
					updateFromMouse(input.getMouseX());
					stopSliding();
					return Optional.of(this);
				}
				if (RangeSliderConfigEntry.this.onMouseClicked(input)) {
					return Optional.of(this);
				}
				return Optional.empty();
			}

			@Override
			public Optional<ConfigInputHandler> handleMouseDragged(@Nullable Screen screen, double mouseX, double mouseY, int button, double dragX, double dragY) {
				if (!isEditable() || dragging == null || button != InputConstants.MOUSE_BUTTON_LEFT) {
					stopSliding();
					return Optional.empty();
				}
				updateFromMouse(mouseX);
				return Optional.of(this);
			}

			@Override
			public Optional<ConfigInputHandler> handleMouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
				long steps = NumberSliderConfigEntry.getScrollSteps(scrollDeltaX);
				if (!isEditable() || scrollDeltaY != 0 || steps == 0 || !sliderArea.contains(mouseX, mouseY)) {
					return Optional.empty();
				}
				RangeSliderModel.Handle handle = model.getNearestHandle(getValue(), getPosition(sliderArea, mouseX));
				setValue(model.step(getValue(), handle, steps));
				return Optional.of(this);
			}

			@Override
			public void unfocus() {
				stopSliding();
			}
		};
	}
}
