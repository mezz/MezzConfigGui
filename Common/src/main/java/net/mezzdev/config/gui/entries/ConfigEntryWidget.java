package net.mezzdev.config.gui.entries;

import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.model.AppliedConfigValueChange;
import net.mezzdev.config.gui.model.ConfigValueChange;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.util.Pair;
import net.mezzdev.config.gui.util.StringUtil;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.model.PendingConfigChange;
import net.mezzdev.config.gui.ConfigInputHandler;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.SameConfigElementInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Base widget for one editable config value row, including reset and pending-change handling.
 */
public abstract class ConfigEntryWidget<T> {

	public static final int TEXT_COLOR = 0xFFFFFFFF;
	public static final int SECONDARY_TEXT_COLOR = 0xFFE0E0E0;
	public static final int HOVER_TEXT_COLOR = 0xFFFFFFFF;
	public static final int DISABLED_TEXT_COLOR = 0xFFA0A0A0;

	protected static final int NAME_RIGHT_RESERVE = 95;
	private static final int NAME_LEFT_PADDING = 5;
	public static final float TEXT_SCALE = 1.0f;
	private static final int RESET_BUTTON_SIZE = 18;
	private static final int RESET_BUTTON_RIGHT_PADDING = 2;
	protected static final int VALUE_CONTROL_RIGHT_RESERVE = RESET_BUTTON_SIZE + RESET_BUTTON_RIGHT_PADDING + 2;
	private static final int ROW_STRIPE_LIGHT_COLOR = 0x08FFFFFF;
	private static final int ROW_STRIPE_DARK_COLOR = 0x08000000;
	private static final int ROW_HOVER_COLOR = 0x18FFFFFF;
	private static final int PENDING_BACKGROUND_COLOR = 0x302F5F8E;
	private static final int PENDING_ACCENT_COLOR = 0xFF5E9AD6;
	private static final int BUTTON_UNDERLAY_COLOR = 0xFF111216;
	private static final int BUTTON_TEXT_PADDING = 3;

	public static void drawText(GuiGraphics guiGraphics, Font font, FormattedCharSequence text, int x, int y, int color) {
		guiGraphics.drawString(font, text, x, y, color, false);
	}

	public static void drawText(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int color) {
		guiGraphics.drawString(font, text, x, y, color, false);
	}

	public static void drawText(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color) {
		guiGraphics.drawString(font, text, x, y, color, false);
	}

	public static boolean drawCenteredButtonText(GuiGraphics guiGraphics, Font font, Component text, ImmutableRect2i area, int color) {
		ImmutableRect2i textArea = area.cropLeft(BUTTON_TEXT_PADDING).cropRight(BUTTON_TEXT_PADDING);
		return drawFittedText(guiGraphics, font, text, textArea, color, true);
	}

	public static boolean drawCenteredButtonText(GuiGraphics guiGraphics, Font font, String text, ImmutableRect2i area, int color) {
		return drawCenteredButtonText(guiGraphics, font, Component.literal(text), area, color);
	}

	public static boolean drawFittedText(
		GuiGraphics guiGraphics,
		Font font,
		Component text,
		ImmutableRect2i area,
		int color,
		boolean centered
	) {
		int maxLines = Math.max(1, area.getHeight() / font.lineHeight);
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
		GuiGraphics guiGraphics,
		ConfigTextures textures,
		ImmutableRect2i area,
		boolean active,
		boolean hovered
	) {
		guiGraphics.fill(area.getX(), area.getY(), area.getX() + area.getWidth(), area.getY() + area.getHeight(), BUTTON_UNDERLAY_COLOR);
		textures.getButtonForState(false, active, hovered).draw(guiGraphics, area);
	}

	protected final IConfigScreenValue<T> configValue;
	private final ConfigTextures textures;
	private final Component fullName;
	private T value;
	private Consumer<AppliedConfigValueChange<?>> appliedChangeListener = change -> {};

	protected List<FormattedCharSequence> nameLines = List.of();

	protected ImmutableRect2i area = ImmutableRect2i.EMPTY;
	ImmutableRect2i nameArea = ImmutableRect2i.EMPTY;
	private ImmutableRect2i resetArea = ImmutableRect2i.EMPTY;

	protected ConfigEntryWidget(IConfigScreenValue<T> configValue, ConfigTextures textures) {
		this.configValue = configValue;
		this.textures = textures;
		this.fullName = StringUtil.stripStyling(ConfigValueLocalization.getName(configValue));
		this.value = configValue.getValue();
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

	protected void recomputeNameArea(ImmutableRect2i area, int rightReserve) {
		updateNameLayout(area, rightReserve);
	}

	private void updateNameLayout(ImmutableRect2i area, int rightReserve) {
		Font font = Minecraft.getInstance().font;
		int nameColWidth = Math.max(40, area.getWidth() - rightReserve - NAME_LEFT_PADDING);
		int wrapWidth = (int) (nameColWidth / TEXT_SCALE);
		nameLines = font.split(fullName, wrapWidth);
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

	public void setAppliedChangeListener(Consumer<AppliedConfigValueChange<?>> appliedChangeListener) {
		this.appliedChangeListener = Objects.requireNonNull(appliedChangeListener, "appliedChangeListener");
	}

	protected boolean onMouseClicked(UserInput input) {
		if (isModified() && resetArea.contains(input.getMouseX(), input.getMouseY())) {
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

	public void draw(GuiGraphics guiGraphics, double mouseX, double mouseY, boolean allowHover) {
		draw(guiGraphics, mouseX, mouseY, allowHover, -1);
	}

	public void draw(GuiGraphics guiGraphics, double mouseX, double mouseY, boolean allowHover, int rowIndex) {
		drawRowStripe(guiGraphics, rowIndex);
		if (allowHover && area.contains(mouseX, mouseY)) {
			guiGraphics.fill(
				area.getX() + 1,
				area.getY(),
				area.getX() + area.getWidth() - 1,
				area.getY() + area.getHeight(),
				ROW_HOVER_COLOR
			);
		}
		if (hasPendingChange()) {
			guiGraphics.fill(
				area.getX() + 1,
				area.getY(),
				area.getX() + area.getWidth() - 1,
				area.getY() + area.getHeight(),
				PENDING_BACKGROUND_COLOR
			);
			guiGraphics.fill(
				area.getX() + 1,
				area.getY(),
				area.getX() + 3,
				area.getY() + area.getHeight(),
				PENDING_ACCENT_COLOR
			);
		}
		double drawMouseX = Double.NaN;
		double drawMouseY = Double.NaN;
		if (allowHover) {
			drawMouseX = mouseX;
			drawMouseY = mouseY;
		}
		drawContent(guiGraphics, drawMouseX, drawMouseY);
		drawResetButton(guiGraphics, drawMouseX, drawMouseY);
	}

	private void drawRowStripe(GuiGraphics guiGraphics, int rowIndex) {
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
			return ROW_STRIPE_LIGHT_COLOR;
		}
		return ROW_STRIPE_DARK_COLOR;
	}

	private void drawResetButton(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		boolean active = isModified();
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
		return !value.equals(configValue.getValue());
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

	public ImmutableRect2i getArea() {
		return area;
	}

	public Optional<PendingConfigChange> getPendingConfigChange() {
		if (hasPendingChange()) {
			T oldValue = configValue.getValue();
			T newValue = value;
			return Optional.of(PendingConfigChange.create(
				ConfigValueLocalization.getName(configValue),
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
		setValue(configValue.getValue());
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

	protected final void drawName(GuiGraphics guiGraphics) {
		Font font = Minecraft.getInstance().font;
		int scaledLineHeight = getScaledLineHeight(font);
		int y = nameArea.getY();
		for (FormattedCharSequence line : nameLines) {
			drawText(guiGraphics, font, line, nameArea.getX(), y, TEXT_COLOR);
			y += scaledLineHeight;
		}
	}

	public void resetToDefault() {
		setValue(configValue.getDefaultValue());
	}

	protected T getValue() {
		return value;
	}

	protected boolean setValue(T value) {
		if (!configValue.getSerializer().isValid(value) || this.value.equals(value)) {
			return false;
		}
		this.value = value;
		if (appliesImmediately()) {
			T oldValue = configValue.getValue();
			boolean changed = configValue.set(value);
			if (!changed && !configValue.getValue().equals(value)) {
				this.value = configValue.getValue();
				return false;
			}
			if (changed) {
				appliedChangeListener.accept(new AppliedConfigValueChange<>(configValue, oldValue, configValue.getValue()));
			}
		}
		onValueChanged();
		return true;
	}

	private boolean appliesImmediately() {
		return configValue.getApplyMode() == ConfigValueApplyMode.IMMEDIATE &&
			!configValue.requiresRestart();
	}

	protected void onValueChanged() {

	}

	protected abstract void drawContent(GuiGraphics guiGraphics, double mouseX, double mouseY);

	private class EntryWidgetInputHandler implements ConfigInputHandler {
		@Override
		public Optional<ConfigInputHandler> handleUserInput(Screen screen, UserInput input) {
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
