package net.mezzdev.config.gui.entries;

import net.mezzdev.config.gui.ConfigInputHandler;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.model.ConfigValueChange;
import net.mezzdev.config.gui.model.PendingConfigChange;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Switches a bounded number row between its slider and standard controls while the screen is open.
 */
final class NumberDisplayConfigEntry<T> extends ConfigEntryWidget<T> {
	private final ConfigEntryWidget<T> sliderEntry;
	private final ConfigEntryWidget<T> standardEntry;
	private ConfigEntryWidget<T> activeEntry;
	private boolean subscribedToConfigValue;

	NumberDisplayConfigEntry(
		IConfigScreenValue<T> value,
		ConfigEntryWidget<T> sliderEntry,
		ConfigEntryWidget<T> standardEntry,
		ConfigTextures textures
	) {
		super(value, textures);
		this.sliderEntry = sliderEntry;
		this.standardEntry = standardEntry;
		this.activeEntry = getConfiguredEntry();
	}

	private ConfigEntryWidget<T> getActiveEntry() {
		ConfigEntryWidget<T> configuredEntry = getConfiguredEntry();
		if (activeEntry != configuredEntry) {
			ConfigEntryWidget<T> previousEntry = activeEntry;
			activeEntry = configuredEntry;
			previousEntry.unfocus();
			configuredEntry.copyDisplayedStateFrom(previousEntry);
			if (subscribedToConfigValue) {
				previousEntry.unsubscribeFromConfigValue();
				configuredEntry.subscribeToConfigValue();
			}
		}
		return activeEntry;
	}

	private ConfigEntryWidget<T> getConfiguredEntry() {
		if (ConfigGuiOptions.getNumberDisplayMode() == ConfigGuiOptions.NumberDisplayMode.SLIDER) {
			return sliderEntry;
		}
		return standardEntry;
	}

	@Override
	public void subscribeToConfigValue() {
		if (!subscribedToConfigValue) {
			ConfigEntryWidget<T> activeEntry = getActiveEntry();
			subscribedToConfigValue = true;
			activeEntry.subscribeToConfigValue();
		}
	}

	@Override
	public void unsubscribeFromConfigValue() {
		if (subscribedToConfigValue) {
			subscribedToConfigValue = false;
			activeEntry.unsubscribeFromConfigValue();
		}
	}

	@Override
	public int getHeight() {
		return getActiveEntry().getHeight();
	}

	@Override
	public void setShowSectionPath(boolean showSectionPath) {
		super.setShowSectionPath(showSectionPath);
		sliderEntry.setShowSectionPath(showSectionPath);
		standardEntry.setShowSectionPath(showSectionPath);
	}

	@Override
	public void updateBounds(ImmutableRect2i area) {
		getActiveEntry().updateBounds(area);
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return getActiveEntry().isMouseOver(mouseX, mouseY);
	}

	@Override
	public void resetBounds() {
		sliderEntry.resetBounds();
		standardEntry.resetBounds();
	}

	@Override
	public ConfigInputHandler createInputHandler() {
		ConfigInputHandler sliderInputHandler = sliderEntry.createInputHandler();
		ConfigInputHandler standardInputHandler = standardEntry.createInputHandler();
		return new ConfigInputHandler() {
			@Override
			public Optional<ConfigInputHandler> handleUserInput(@Nullable Screen screen, UserInput input) {
				return getActiveInputHandler().handleUserInput(screen, input);
			}

			@Override
			public void unfocus() {
				sliderInputHandler.unfocus();
				standardInputHandler.unfocus();
			}

			@Override
			public Optional<ConfigInputHandler> handleMouseScrolled(
				double mouseX,
				double mouseY,
				double scrollDeltaX,
				double scrollDeltaY
			) {
				return getActiveInputHandler().handleMouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
			}

			private ConfigInputHandler getActiveInputHandler() {
				if (getActiveEntry() == sliderEntry) {
					return sliderInputHandler;
				}
				return standardInputHandler;
			}
		};
	}

	@Override
	public void setImmediateChangeHandler(Function<ConfigValueChange<?>, Boolean> immediateChangeHandler) {
		sliderEntry.setImmediateChangeHandler(immediateChangeHandler);
		standardEntry.setImmediateChangeHandler(immediateChangeHandler);
	}

	@Override
	public void setEditableSupplier(BooleanSupplier editableSupplier) {
		sliderEntry.setEditableSupplier(editableSupplier);
		standardEntry.setEditableSupplier(editableSupplier);
	}

	@Override
	public boolean isEditable() {
		return getActiveEntry().isEditable();
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		return getActiveEntry().charTyped(codePoint, modifiers);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return getActiveEntry().keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		return getActiveEntry().keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean isCapturingKeyboardInput() {
		return getActiveEntry().isCapturingKeyboardInput();
	}

	@Override
	public boolean isCapturingTextInput() {
		return getActiveEntry().isCapturingTextInput();
	}

	@Override
	public void unfocus() {
		getActiveEntry().unfocus();
	}

	@Override
	public void draw(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY, boolean allowHover) {
		getActiveEntry().draw(guiGraphics, mouseX, mouseY, allowHover);
	}

	@Override
	public void draw(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY, boolean allowHover, int rowIndex) {
		getActiveEntry().draw(guiGraphics, mouseX, mouseY, allowHover, rowIndex);
	}

	@Override
	public boolean isModified() {
		return getActiveEntry().isModified();
	}

	@Override
	public boolean hasPendingChange() {
		return getActiveEntry().hasPendingChange();
	}

	@Override
	public Optional<ConfigValueChange<T>> getPendingChange() {
		return getActiveEntry().getPendingChange();
	}

	@Override
	public ImmutableRect2i getArea() {
		return getActiveEntry().getArea();
	}

	@Override
	public Optional<PendingConfigChange> getPendingConfigChange() {
		return getActiveEntry().getPendingConfigChange();
	}

	@Override
	public void discardPendingChange() {
		sliderEntry.discardPendingChange();
		standardEntry.discardPendingChange();
	}

	@Override
	public ConfigInfo getInfo() {
		return getActiveEntry().getInfo();
	}

	@Override
	public ConfigInfo getInfo(double mouseX, double mouseY) {
		return getActiveEntry().getInfo(mouseX, mouseY);
	}

	@Override
	@Nullable
	public ConfigInfo getTooltipInfo(double mouseX, double mouseY) {
		return getActiveEntry().getTooltipInfo(mouseX, mouseY);
	}

	@Override
	public void resetToDefault() {
		getActiveEntry().resetToDefault();
	}

	Class<?> getActiveEntryType() {
		return getActiveEntry().getClass();
	}

	@Override
	protected void drawContent(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
		throw new UnsupportedOperationException("NumberDisplayConfigEntry delegates drawing to its active entry.");
	}
}
