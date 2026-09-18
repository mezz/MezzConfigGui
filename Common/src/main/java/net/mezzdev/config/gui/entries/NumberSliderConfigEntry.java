package net.mezzdev.config.gui.entries;

import net.mezzdev.config.gui.internal.NumberFormatting;

import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.ConfigInputHandler;
import net.mezzdev.config.gui.ConfigInputUtil;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Config entry widget for bounded number values displayed with a Minecraft-style slider.
 */
final class NumberSliderConfigEntry<T> extends ConfigEntryWidget<T> {
	private static final ResourceLocation SLIDER_SPRITE = ResourceLocation.withDefaultNamespace("widget/slider");
	private static final ResourceLocation SLIDER_HANDLE_SPRITE = ResourceLocation.withDefaultNamespace("widget/slider_handle");
	private static final ResourceLocation SLIDER_HANDLE_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/slider_handle_highlighted");
	private static final int SLIDER_HEIGHT = 20;
	private static final int HANDLE_WIDTH = 8;

	public static <T> Optional<ConfigEntryWidget<T>> create(IConfigScreenValue<T> value, ConfigTextures textures) {
		return value.getSerializer()
			.getRange()
			.flatMap(NumberSliderModel::create)
			.map(model -> new NumberSliderConfigEntry<>(value, model, textures));
	}

	private final IConfigValueSerializer<T> serializer;
	private final NumberSliderModel<T> model;
	private ImmutableRect2i sliderArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i dragSliderArea = ImmutableRect2i.EMPTY;
	private boolean sliding;
	@Nullable
	private T fineValueAnchor;
	private double finePointerAnchor;
	private double normalPointerOffset;

	private NumberSliderConfigEntry(
		IConfigScreenValue<T> value,
		NumberSliderModel<T> model,
		ConfigTextures textures
	) {
		super(value, textures);
		this.serializer = value.getSerializer();
		this.model = model;
	}

	@Override
	public void updateBounds(ImmutableRect2i area) {
		super.updateBounds(area);
		int sliderWidth = getValueColumnWidth(area, HANDLE_WIDTH);
		int sliderX = area.getX() + area.getWidth() - sliderWidth - VALUE_CONTROL_RIGHT_RESERVE;
		int sliderY = area.getY() + (area.getHeight() - SLIDER_HEIGHT) / 2;
		sliderArea = new ImmutableRect2i(sliderX, sliderY, sliderWidth, SLIDER_HEIGHT);
		recomputeNameArea(area, getValueColumnNameRightReserve(area, HANDLE_WIDTH));
	}

	@Override
	protected void drawContent(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		drawName(guiGraphics);
		boolean hovered = sliderArea.contains(mouseX, mouseY);
		guiGraphics.blitSprite(SLIDER_SPRITE, sliderArea.getX(), sliderArea.getY(), sliderArea.getWidth(), sliderArea.getHeight());
		double position = model.getPosition(getValue());
		int handleX = sliderArea.getX() + (int) Math.round(position * (sliderArea.getWidth() - HANDLE_WIDTH));
		ResourceLocation handleSprite = SLIDER_HANDLE_SPRITE;
		if (hovered || sliding) {
			handleSprite = SLIDER_HANDLE_HIGHLIGHTED_SPRITE;
		}
		guiGraphics.blitSprite(handleSprite, handleX, sliderArea.getY(), HANDLE_WIDTH, sliderArea.getHeight());

		Font font = Minecraft.getInstance().font;
		drawCenteredButtonText(guiGraphics, font, getValueName(getValue()), sliderArea, getConfiguredTextColor());
	}

	@Override
	public ConfigInfo getInfo() {
		ConfigInfo info = super.getInfo();
		List<Component> lines = new ArrayList<>(info.lines());
		lines.add(Component.translatable("mezz_config.config.screen.range",
			NumberFormatting.format((Number) model.getRange().min()), NumberFormatting.format((Number) model.getRange().max())));
		lines.add(Component.translatable("mezz_config.config.screen.number.shiftStep", 1));
		lines.add(Component.translatable("mezz_config.config.screen.number.slider.mouseWheel"));
		return new ConfigInfo(info.title(), lines);
	}

	@Override
	@Nullable
	public ConfigInfo getTooltipInfo(double mouseX, double mouseY) {
		@Nullable
		ConfigInfo resetInfo = super.getTooltipInfo(mouseX, mouseY);
		if (resetInfo != null) {
			return resetInfo;
		}
		if (sliderArea.contains(mouseX, mouseY)) {
			return ConfigValueInfoFactory.createUpdateInfo(configValue, getValue(), hasPendingChange());
		}
		return null;
	}

	@Override
	public ConfigInputHandler createInputHandler() {
		return new SliderInputHandler();
	}

	private void updateValueFromMouse(double mouseX) {
		T value;
		if (Screen.hasShiftDown()) {
			if (fineValueAnchor == null) {
				beginFineControl(mouseX);
			}
			long steps = Math.round(mouseX - finePointerAnchor);
			value = model.getFineValue(fineValueAnchor, steps);
		} else {
			if (fineValueAnchor != null) {
				normalPointerOffset = getPointerPosition(getValue()) - mouseX;
				fineValueAnchor = null;
			}
			double position = getSliderPosition(mouseX + normalPointerOffset);
			value = model.getValue(position);
		}
		if (serializer.isValid(value)) {
			setValue(value);
		}
	}

	private double getSliderPosition(double mouseX) {
		ImmutableRect2i interactionArea = getInteractionSliderArea();
		double usableWidth = Math.max(1, interactionArea.getWidth() - HANDLE_WIDTH);
		return (mouseX - interactionArea.getX() - HANDLE_WIDTH / 2.0) / usableWidth;
	}

	private double getPointerPosition(T value) {
		ImmutableRect2i interactionArea = getInteractionSliderArea();
		double usableWidth = Math.max(1, interactionArea.getWidth() - HANDLE_WIDTH);
		return interactionArea.getX() + HANDLE_WIDTH / 2.0 + model.getPosition(value) * usableWidth;
	}

	private ImmutableRect2i getInteractionSliderArea() {
		if (sliding) {
			return dragSliderArea;
		}
		return sliderArea;
	}

	private void beginSliding(double mouseX) {
		sliding = true;
		dragSliderArea = sliderArea;
		fineValueAnchor = null;
		normalPointerOffset = 0.0;
		if (Screen.hasShiftDown()) {
			beginFineControl(mouseX);
		}
	}

	private void beginFineControl(double mouseX) {
		fineValueAnchor = getValue();
		finePointerAnchor = mouseX;
	}

	private void stopSliding() {
		sliding = false;
		dragSliderArea = ImmutableRect2i.EMPTY;
		fineValueAnchor = null;
		normalPointerOffset = 0.0;
	}

	private void scrollValue(double scrollDelta) {
		long steps = getScrollSteps(scrollDelta);
		T value = model.getFineValue(getValue(), steps);
		if (serializer.isValid(value)) {
			setValue(value);
		}
	}

	static long getScrollSteps(double scrollDelta) {
		if (!Double.isFinite(scrollDelta) || scrollDelta == 0.0) {
			return 0;
		}
		long steps = Math.round(scrollDelta);
		if (steps == 0) {
			if (scrollDelta > 0.0) {
				return 1;
			}
			return -1;
		}
		return steps;
	}

	private final class SliderInputHandler implements ConfigInputHandler {
		@Override
		public Optional<ConfigInputHandler> handleUserInput(Screen screen, UserInput input) {
			if (!ConfigInputUtil.isLeftClick(input)) {
				stopSliding();
				return Optional.empty();
			}
			if (input.isSimulate()) {
				if (NumberSliderConfigEntry.this.onMouseClicked(input)) {
					stopSliding();
					return Optional.of(this);
				}
				if (sliderArea.contains(input.getMouseX(), input.getMouseY())) {
					beginSliding(input.getMouseX());
					return Optional.of(this);
				}
				return Optional.empty();
			}
			if (sliding) {
				updateValueFromMouse(input.getMouseX());
				stopSliding();
				return Optional.of(this);
			}
			if (NumberSliderConfigEntry.this.onMouseClicked(input)) {
				return Optional.of(this);
			}
			return Optional.empty();
		}

		@Override
		public Optional<ConfigInputHandler> handleMouseDragged(
			Screen screen,
			double mouseX,
			double mouseY,
			int button,
			double dragX,
			double dragY
		) {
			if (!sliding || button != 0) {
				stopSliding();
				return Optional.empty();
			}
			updateValueFromMouse(mouseX);
			return Optional.of(this);
		}

		@Override
		public Optional<ConfigInputHandler> handleMouseScrolled(
			double mouseX,
			double mouseY,
			double scrollDeltaX,
			double scrollDeltaY
		) {
			if (!sliderArea.contains(mouseX, mouseY)) {
				return Optional.empty();
			}
			double scrollDelta = scrollDeltaY;
			if (scrollDelta == 0.0) {
				scrollDelta = scrollDeltaX;
			}
			if (getScrollSteps(scrollDelta) == 0) {
				return Optional.empty();
			}
			scrollValue(scrollDelta);
			return Optional.of(this);
		}

		@Override
		public void unfocus() {
			stopSliding();
		}
	}
}
