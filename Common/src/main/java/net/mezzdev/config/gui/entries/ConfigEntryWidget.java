package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.ConfigValueAccess;
import net.mezzdev.config.gui.ConfigValueCategoryPath;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.model.ConfigValueChange;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.util.Pair;
import net.mezzdev.config.gui.util.StringUtil;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.mezzdev.config.gui.info.ConfigServerInfo;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.model.PendingConfigChange;
import net.mezzdev.config.gui.ConfigInputHandler;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.SameConfigElementInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base widget for one editable config value row, including reset and pending-change handling.
 */
public abstract class ConfigEntryWidget<T> {

	protected static final int NAME_RIGHT_RESERVE = 95;
	private static final int NAME_LEFT_PADDING = 5;
	public static final float TEXT_SCALE = 1.0f;
	static final int PREFERRED_VALUE_CONTROL_WIDTH = 110;
	private static final int MIN_VALUE_NAME_WIDTH = 80;
	private static final int VALUE_CONTROL_GAP = 4;
	private static final int RESET_BUTTON_SIZE = 18;
	private static final int RESET_BUTTON_RIGHT_PADDING = 2;
	protected static final int VALUE_CONTROL_RIGHT_RESERVE = RESET_BUTTON_SIZE + RESET_BUTTON_RIGHT_PADDING + 2;
	private static final int BUTTON_TEXT_PADDING = 3;
	private static final int FITTED_TEXT_VERTICAL_PADDING = 2;

	public static int getConfiguredTextColor() {
		return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_TEXT);
	}

	public static int getConfiguredHoverTextColor() {
		return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_HOVER_TEXT);
	}

	public static int getConfiguredDisabledTextColor() {
		return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_DISABLED_TEXT);
	}

	public static void drawText(GuiGraphicsExtractor guiGraphics, Font font, FormattedCharSequence text, int x, int y, int color) {
		guiGraphics.text(font, text, x, y, color, false);
	}

	public static void drawText(GuiGraphicsExtractor guiGraphics, Font font, Component text, int x, int y, int color) {
		guiGraphics.text(font, text, x, y, color, false);
	}

	public static void drawText(GuiGraphicsExtractor guiGraphics, Font font, String text, int x, int y, int color) {
		guiGraphics.text(font, text, x, y, color, false);
	}

	public static boolean drawCenteredButtonText(GuiGraphicsExtractor guiGraphics, Font font, Component text, ImmutableRect2i area, int color) {
		ImmutableRect2i textArea = area.cropLeft(BUTTON_TEXT_PADDING).cropRight(BUTTON_TEXT_PADDING);
		return drawFittedText(guiGraphics, font, text, textArea, color, true);
	}

	public static boolean drawCenteredButtonText(GuiGraphicsExtractor guiGraphics, Font font, String text, ImmutableRect2i area, int color) {
		return drawCenteredButtonText(guiGraphics, font, Component.literal(text), area, color);
	}

	public static boolean drawFittedText(
		GuiGraphicsExtractor guiGraphics,
		Font font,
		Component text,
		ImmutableRect2i area,
		int color,
		boolean centered
	) {
		int fittedTextHeight = area.getHeight() - FITTED_TEXT_VERTICAL_PADDING * 2;
		int maxLines = Math.max(1, (fittedTextHeight + 1) / font.lineHeight);
		Pair<List<FormattedText>, Boolean> splitLines = StringUtil.splitLines(font, List.of(text), area.getWidth(), maxLines);
		List<FormattedCharSequence> visibleLines = Language.getInstance().getVisualOrder(splitLines.first());
		int textHeight = getTextBlockHeight(font, visibleLines.size());
		int y = area.getY() + Math.round((area.getHeight() - textHeight) / 2.0f);
		for (FormattedCharSequence line : visibleLines) {
			int x = area.getX();
			if (centered) {
				x += Math.round((area.getWidth() - font.width(line)) / 2.0f);
			}
			drawText(guiGraphics, font, line, x, y, color);
			y += font.lineHeight;
		}
		return splitLines.second();
	}

	private static int getTextBlockHeight(Font font, int lineCount) {
		if (lineCount <= 0) {
			return 0;
		}
		return lineCount * font.lineHeight - 1;
	}

	public static int getCenteredTextY(Font font, ImmutableRect2i area) {
		return area.getY() + Math.round((area.getHeight() - font.lineHeight) / 2.0f);
	}

	public static int getMinimumHeight() {
		Font font = Minecraft.getInstance().font;
		ConfigGuiOptions.RowDensity rowDensity = ConfigGuiOptions.getRowDensity();
		int textHeight = getScaledLineHeight(font) * rowDensity.getMinimumNameLines();
		return Math.max(RESET_BUTTON_SIZE + 2, textHeight + rowDensity.getNameVerticalPadding());
	}

	private static int getScaledLineHeight(Font font) {
		return (int) (font.lineHeight * TEXT_SCALE);
	}

	public static void drawButtonBackground(
		GuiGraphicsExtractor guiGraphics,
		ConfigTextures textures,
		ImmutableRect2i area,
		boolean active,
		boolean hovered
	) {
		guiGraphics.fill(
			area.getX(),
			area.getY(),
			area.getX() + area.getWidth(),
			area.getY() + area.getHeight(),
			ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_BUTTON_UNDERLAY)
		);
		textures.getButtonForState(false, active, hovered).draw(guiGraphics, area);
	}

	protected final IConfigScreenValue<T> configValue;
	private final ConfigTextures textures;
	private final Component fullName;
	private boolean showSectionPath;
	private final Consumer<Runnable> clientThreadDispatcher;
	private T value;
	private T lastKnownConfigValue;
	private boolean subscribedToConfigValue;
	private long configValueListenerGeneration;
	private final AtomicLong configValueChangeSequence = new AtomicLong();
	@Nullable
	private Runnable removeConfigValueListener;
	private Function<ConfigValueChange<?>, Boolean> immediateChangeHandler = change -> false;
	private BooleanSupplier editableSupplier = () -> true;
	private Supplier<List<Component>> accessDescriptions = List::of;

	protected List<FormattedCharSequence> nameLines = List.of();

	protected ImmutableRect2i area = ImmutableRect2i.EMPTY;
	ImmutableRect2i nameArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i resetArea = ImmutableRect2i.EMPTY;
	@Nullable
	private ImmutableRect2i viewport;

	protected ConfigEntryWidget(IConfigScreenValue<T> configValue, ConfigTextures textures) {
		this(configValue, textures, ConfigEntryWidget::runOnClientThread);
	}

	protected ConfigEntryWidget(
		IConfigScreenValue<T> configValue,
		ConfigTextures textures,
		Consumer<Runnable> clientThreadDispatcher
	) {
		this.configValue = configValue;
		this.textures = textures;
		this.clientThreadDispatcher = Objects.requireNonNull(clientThreadDispatcher, "clientThreadDispatcher");
		this.fullName = StringUtil.stripStyling(ConfigValueLocalization.getName(configValue));
		this.value = configValue.getValue();
		this.lastKnownConfigValue = this.value;
	}

	public void subscribeToConfigValue() {
		if (subscribedToConfigValue) {
			return;
		}
		subscribedToConfigValue = true;
		long listenerGeneration = ++configValueListenerGeneration;
		syncFromConfigValue();
		try {
			removeConfigValueListener = Objects.requireNonNull(
				configValue.addListener(newValue -> dispatchConfigValueChanged(listenerGeneration, newValue)),
				"config value listener removal callback"
			);
		} catch (RuntimeException exception) {
			subscribedToConfigValue = false;
			configValueListenerGeneration++;
			throw exception;
		}
		syncFromConfigValue();
	}

	public void unsubscribeFromConfigValue() {
		if (!subscribedToConfigValue) {
			return;
		}
		subscribedToConfigValue = false;
		configValueListenerGeneration++;
		Runnable removeConfigValueListener = this.removeConfigValueListener;
		this.removeConfigValueListener = null;
		if (removeConfigValueListener != null) {
			removeConfigValueListener.run();
		}
	}

	private void dispatchConfigValueChanged(long listenerGeneration, T newValue) {
		long changeSequence = configValueChangeSequence.incrementAndGet();
		clientThreadDispatcher.accept(() -> {
			if (subscribedToConfigValue &&
				configValueListenerGeneration == listenerGeneration &&
				configValueChangeSequence.get() == changeSequence
			) {
				onConfigValueChanged(newValue);
			}
		});
	}

	private void syncFromConfigValue() {
		configValueChangeSequence.incrementAndGet();
		onConfigValueChanged(configValue.getValue());
	}

	private static void runOnClientThread(Runnable task) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.isSameThread()) {
			task.run();
		} else {
			minecraft.execute(task);
		}
	}

	private void onConfigValueChanged(T newValue) {
		boolean hasPendingChange = !Objects.equals(value, lastKnownConfigValue);
		lastKnownConfigValue = newValue;
		if (!hasPendingChange && !Objects.equals(value, newValue)) {
			value = newValue;
			onValueChanged();
		}
	}

	public int getHeight() {
		if (nameLines.isEmpty()) {
			return getMinimumHeight();
		}
		Font font = Minecraft.getInstance().font;
		int scaledLineHeight = getScaledLineHeight(font);
		return Math.max(getMinimumHeight(), nameLines.size() * scaledLineHeight + ConfigGuiOptions.getRowDensity().getNameVerticalPadding());
	}

	public void updateBounds(ImmutableRect2i area) {
		this.area = area;
		updateNameLayout(area, NAME_RIGHT_RESERVE);
		this.resetArea = new ImmutableRect2i(
			area.getX() + area.getWidth() - RESET_BUTTON_SIZE - RESET_BUTTON_RIGHT_PADDING,
			area.getY() + (area.getHeight() - RESET_BUTTON_SIZE) / 2,
			RESET_BUTTON_SIZE,
			RESET_BUTTON_SIZE
		);
	}

	protected static int getValueColumnWidth(ImmutableRect2i area, int minimumControlWidth) {
		return getValueColumnWidth(area.getWidth(), minimumControlWidth);
	}

	static int getValueColumnWidth(int areaWidth, int minimumControlWidth) {
		int availableWidth = areaWidth - VALUE_CONTROL_RIGHT_RESERVE - VALUE_CONTROL_GAP - NAME_LEFT_PADDING - MIN_VALUE_NAME_WIDTH;
		return Math.min(PREFERRED_VALUE_CONTROL_WIDTH, Math.max(minimumControlWidth, availableWidth));
	}

	protected static int getValueColumnNameRightReserve(ImmutableRect2i area, int minimumControlWidth) {
		int valueColumnWidth = getValueColumnWidth(area, minimumControlWidth);
		return valueColumnWidth + VALUE_CONTROL_RIGHT_RESERVE + VALUE_CONTROL_GAP;
	}

	protected void recomputeNameArea(ImmutableRect2i area, int rightReserve) {
		updateNameLayout(area, rightReserve);
	}

	private void updateNameLayout(ImmutableRect2i area, int rightReserve) {
		Font font = Minecraft.getInstance().font;
		int nameColWidth = Math.max(40, area.getWidth() - rightReserve - NAME_LEFT_PADDING);
		int wrapWidth = (int) (nameColWidth / TEXT_SCALE);
		nameLines = font.split(getDisplayName(), wrapWidth);
		int scaledLineHeight = getScaledLineHeight(font);
		int textHeight = nameLines.size() * scaledLineHeight;
		this.nameArea = new ImmutableRect2i(
			area.getX() + NAME_LEFT_PADDING,
			area.getY() + Math.round((area.getHeight() - textHeight) / 2.0f),
			nameColWidth,
			textHeight
		);
	}

	public boolean isMouseOver(double mouseX, double mouseY) {
		return area.contains(mouseX, mouseY);
	}

	public void resetBounds() {
		this.area = ImmutableRect2i.EMPTY;
		this.nameArea = ImmutableRect2i.EMPTY;
		this.resetArea = ImmutableRect2i.EMPTY;
	}

	public ConfigInputHandler createInputHandler() {
		return new EntryWidgetInputHandler();
	}

	public void setImmediateChangeHandler(Function<ConfigValueChange<?>, Boolean> immediateChangeHandler) {
		this.immediateChangeHandler = Objects.requireNonNull(immediateChangeHandler, "immediateChangeHandler");
	}

	public void setEditableSupplier(BooleanSupplier editableSupplier) {
		this.editableSupplier = Objects.requireNonNull(editableSupplier, "editableSupplier");
	}

	public void setAccessDescriptions(Supplier<List<Component>> accessDescriptions) {
		this.accessDescriptions = Objects.requireNonNull(accessDescriptions, "accessDescriptions");
	}

	public ConfigInfo getInfoWithAccess(double mouseX, double mouseY) {
		return ConfigServerInfo.add(getInfo(mouseX, mouseY), accessDescriptions.get());
	}

	public boolean isEditable() {
		return editableSupplier.getAsBoolean() && ConfigValueAccess.isEditable(configValue);
	}

	protected boolean onMouseClicked(UserInput input) {
		if (isEditable() && isModified() && resetArea.contains(input.getMouseX(), input.getMouseY())) {
			if (!input.isSimulate()) {
				resetToDefault();
			}
			return true;
		}
		return false;
	}

	public boolean charTyped(char codePoint, int modifiers) {
		return false;
	}

	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return false;
	}

	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		return false;
	}

	public boolean isCapturingKeyboardInput() {
		return false;
	}

	public void unfocus() {

	}

	public void draw(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY, boolean allowHover) {
		draw(guiGraphics, mouseX, mouseY, allowHover, -1);
	}

	public final void draw(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY, boolean allowHover, int rowIndex, ImmutableRect2i viewport) {
		if (!getArea().intersects(viewport)) {
			return;
		}
		ImmutableRect2i previousViewport = this.viewport;
		this.viewport = viewport;
		try {
			draw(guiGraphics, mouseX, mouseY, allowHover, rowIndex);
		} finally {
			this.viewport = previousViewport;
		}
	}

	@Nullable
	protected final ImmutableRect2i getViewport() {
		return viewport;
	}

	protected final boolean isVisible(ImmutableRect2i bounds) {
		return !bounds.isEmpty() && (viewport == null || bounds.intersects(viewport));
	}

	public void draw(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY, boolean allowHover, int rowIndex) {
		drawRowStripe(guiGraphics, rowIndex);
		ImmutableRect2i hoverArea = getHoverArea();
		if (allowHover && hoverArea.contains(mouseX, mouseY)) {
			guiGraphics.fill(
				hoverArea.getX() + 1,
				hoverArea.getY(),
				hoverArea.getX() + hoverArea.getWidth() - 1,
				hoverArea.getY() + hoverArea.getHeight(),
				ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_ROW_HOVER)
			);
		}
		if (hasPendingChange()) {
			guiGraphics.fill(
				area.getX() + 1,
				area.getY(),
				area.getX() + area.getWidth() - 1,
				area.getY() + area.getHeight(),
				ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_PENDING_BACKGROUND)
			);
			guiGraphics.fill(
				area.getX() + 1,
				area.getY(),
				area.getX() + 3,
				area.getY() + area.getHeight(),
				ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_PENDING_ACCENT)
			);
		}
		double drawMouseX = Double.NaN;
		double drawMouseY = Double.NaN;
		if (allowHover) {
			drawMouseX = mouseX;
			drawMouseY = mouseY;
		}
		drawContent(guiGraphics, drawMouseX, drawMouseY);
		if (isVisible(resetArea)) {
			drawResetButton(guiGraphics, drawMouseX, drawMouseY);
		}
		if (!isEditable()) {
			guiGraphics.fill(
				area.getX() + 1,
				area.getY(),
				area.getX() + area.getWidth() - 1,
				area.getY() + area.getHeight(),
				ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_READ_ONLY_OVERLAY)
			);
		}
	}

	protected ImmutableRect2i getHoverArea() {
		return area;
	}

	private void drawRowStripe(GuiGraphicsExtractor guiGraphics, int rowIndex) {
		if (rowIndex < 0 || !ConfigGuiOptions.showRowStriping()) {
			return;
		}
		int color = getRowStripeColor(rowIndex);
		guiGraphics.fill(
			area.getX() + 1,
			area.getY(),
			area.getX() + area.getWidth() - 1,
			area.getY() + area.getHeight(),
			color
		);
	}

	private static int getRowStripeColor(int rowIndex) {
		if (rowIndex % 2 == 0) {
			return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_ROW_STRIPE_LIGHT);
		}
		return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_ENTRY_ROW_STRIPE_DARK);
	}

	private void drawResetButton(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
		boolean active = isEditable() && isModified();
		boolean hovered = active && resetArea.contains(mouseX, mouseY);
		drawButtonBackground(guiGraphics, textures, resetArea, active, hovered);
		ConfigResetIcon.draw(guiGraphics, resetArea, active);
	}

	protected ConfigTextures getTextures() {
		return textures;
	}

	public boolean isModified() {
		return !value.equals(configValue.getDefaultValue());
	}

	public boolean hasPendingChange() {
		T configValue = this.configValue.getValue();
		if (Objects.equals(value, configValue)) {
			lastKnownConfigValue = configValue;
			return false;
		}
		return true;
	}

	public Optional<ConfigValueChange<T>> getPendingChange() {
		if (hasPendingChange()) {
			return Optional.of(new ConfigValueChange<>(configValue, value));
		}
		return Optional.empty();
	}

	public IConfigScreenValue<T> getConfigValue() {
		return configValue;
	}

	public Component getFullName() {
		return fullName;
	}

	public void setShowSectionPath(boolean showSectionPath) {
		this.showSectionPath = showSectionPath;
	}

	public Component getDisplayName() {
		if (showSectionPath) {
			return ConfigValueCategoryPath.getContextualName(configValue, fullName);
		}
		return fullName;
	}

	public ImmutableRect2i getArea() {
		return area;
	}

	public Optional<PendingConfigChange> getPendingConfigChange() {
		if (hasPendingChange()) {
			T oldValue = configValue.getValue();
			T newValue = value;
			return Optional.of(PendingConfigChange.create(
				ConfigValueCategoryPath.getContextualName(configValue, fullName),
				getValueName(oldValue),
				getValueName(newValue),
				getInfo(),
				ConfigValueInfoFactory.createPendingChangeValueInfo(configValue, oldValue),
				ConfigValueInfoFactory.createPendingChangeValueInfo(configValue, newValue)
			));
		}
		return Optional.empty();
	}

	public void discardPendingChange() {
		T configValue = this.configValue.getValue();
		lastKnownConfigValue = configValue;
		if (!Objects.equals(value, configValue)) {
			value = configValue;
			onValueChanged();
		}
	}

	public ConfigInfo getInfo() {
		return new ConfigInfo(ConfigValueLocalization.getName(configValue), ConfigValueLocalization.getDescription(configValue));
	}

	protected Component getValueName(T value) {
		return ConfigValueLocalization.getValueName(configValue, value);
	}

	public ConfigInfo getInfo(double mouseX, double mouseY) {
		return getInfo();
	}

	@Nullable
	public ConfigInfo getTooltipInfo(double mouseX, double mouseY) {
		if (resetArea.contains(mouseX, mouseY)) {
			return ConfigValueInfoFactory.createResetInfo(configValue);
		}
		return null;
	}

	protected final void drawName(GuiGraphicsExtractor guiGraphics) {
		Font font = Minecraft.getInstance().font;
		int scaledLineHeight = getScaledLineHeight(font);
		int y = nameArea.getY();
		for (FormattedCharSequence line : nameLines) {
			drawText(
				guiGraphics,
				font,
				line,
				nameArea.getX(),
				y,
				getConfiguredTextColor()
			);
			y += scaledLineHeight;
		}
	}

	public void resetToDefault() {
		setValue(configValue.getDefaultValue());
	}

	protected T getValue() {
		return value;
	}

	final void copyDisplayedStateFrom(ConfigEntryWidget<T> source) {
		boolean valueChanged = !Objects.equals(value, source.value);
		value = source.value;
		lastKnownConfigValue = source.lastKnownConfigValue;
		if (valueChanged) {
			onValueChanged();
		}
	}

	protected boolean setValue(T value) {
		if (!isEditable() || !configValue.getSerializer().isValid(value) || this.value.equals(value)) {
			return false;
		}
		if (appliesImmediately()) {
			T previousDisplayedValue = this.value;
			boolean succeeded = immediateChangeHandler.apply(new ConfigValueChange<>(configValue, value));
			if (!succeeded) {
				boolean displayedValueChanged = !Objects.equals(this.value, previousDisplayedValue);
				this.value = previousDisplayedValue;
				this.lastKnownConfigValue = previousDisplayedValue;
				if (displayedValueChanged) {
					onValueChanged();
				}
				return false;
			}
			configValueChangeSequence.incrementAndGet();
			T storedValue = configValue.getValue();
			this.lastKnownConfigValue = storedValue;
			boolean displayedValueChanged = !Objects.equals(this.value, storedValue);
			this.value = storedValue;
			if (displayedValueChanged) {
				onValueChanged();
			}
			return Objects.equals(storedValue, value) && !Objects.equals(previousDisplayedValue, storedValue);
		}
		this.value = value;
		onValueChanged();
		return true;
	}

	private boolean appliesImmediately() {
		return configValue.getApplyMode() == ConfigValueApplyMode.IMMEDIATE &&
			configValue.getRestartRequirement() == ConfigValueRestartRequirement.NONE;
	}

	protected void onValueChanged() {

	}

	protected abstract void drawContent(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY);

	private class EntryWidgetInputHandler implements ConfigInputHandler {
		@Override
		public Optional<ConfigInputHandler> handleUserInput(@Nullable Screen screen, UserInput input) {
			if (!isEditable()) {
				return Optional.empty();
			}
			if (onMouseClicked(input)) {
				return Optional.of(new SameConfigElementInputHandler(this, area::contains));
			}
			return Optional.empty();
		}

		@Override
		public void unfocus() {
			ConfigEntryWidget.this.unfocus();
		}
	}
}
