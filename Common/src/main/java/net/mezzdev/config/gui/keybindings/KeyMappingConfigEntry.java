package net.mezzdev.config.gui.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.input.UserInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Config entry widget for editing key mappings and showing binding conflicts.
 */
public final class KeyMappingConfigEntry extends ConfigEntryWidget<KeyMappingValue> {
	private static final int BUTTON_WIDTH = 86;
	private static final int BUTTON_HEIGHT = 22;
	private static final int BUTTON_TEXT_PADDING = 4;
	private static final int CONFLICT_ACCENT_COLOR = 0xFFFFD34D;

	private final IConfigKeyMapping configKeyMapping;
	private ImmutableRect2i buttonArea = ImmutableRect2i.EMPTY;
	private boolean listening = false;
	private String lastPressedModifier = ConfigKeyBinding.UNKNOWN.keyName();
	private String lastPressedKey = ConfigKeyBinding.UNKNOWN.keyName();
	private boolean lastModifierHeldDown = false;
	private boolean lastKeyHeldDown = false;

	public KeyMappingConfigEntry(IConfigValue<KeyMappingValue> value, ConfigTextures textures) {
		super(value, textures);
		this.configKeyMapping = value.getValue().configKeyMapping();
	}

	@Override
	public void updateBounds(ImmutableRect2i area) {
		super.updateBounds(area);
		buttonArea = new ImmutableRect2i(
			area.getX() + area.getWidth() - BUTTON_WIDTH - VALUE_CONTROL_RIGHT_RESERVE,
			area.getY() + (area.getHeight() - BUTTON_HEIGHT) / 2,
			BUTTON_WIDTH,
			BUTTON_HEIGHT
		);
		recomputeNameArea(area, Math.max(NAME_RIGHT_RESERVE, BUTTON_WIDTH + VALUE_CONTROL_RIGHT_RESERVE + 4));
	}

	@Override
	protected void drawContent(GuiGraphics guiGraphics, double mouseX, double mouseY) {
		drawName(guiGraphics);
		boolean hasConflict = !getConflicts().isEmpty();
		if (hasConflict) {
			guiGraphics.fill(
				area.getX() + 1,
				area.getY(),
				area.getX() + 3,
				area.getY() + area.getHeight(),
				CONFLICT_ACCENT_COLOR
			);
		}

		ConfigTextures textures = getTextures();
		boolean hovered = buttonArea.contains(mouseX, mouseY);
		drawButtonBackground(guiGraphics, textures, buttonArea, true, hovered || listening);

		Font font = Minecraft.getInstance().font;
		Component valueText = getButtonText();
		int color = getButtonTextColor(hasConflict, hovered);
		drawFittedText(
			guiGraphics,
			font,
			valueText,
			buttonArea.cropLeft(BUTTON_TEXT_PADDING).cropRight(BUTTON_TEXT_PADDING),
			color,
			true
		);
	}

	private Component getButtonText() {
		Component keyName = configKeyMapping.getValueName(getDisplayValue());
		if (listening) {
			return Component.literal("> ")
				.append(keyName.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE))
				.append(" <")
				.withStyle(ChatFormatting.YELLOW);
		}
		return keyName;
	}

	private int getButtonTextColor(boolean hasConflict, boolean hovered) {
		if (hasConflict) {
			return CONFLICT_ACCENT_COLOR;
		}
		if (listening || hovered) {
			return HOVER_TEXT_COLOR;
		}
		if (getDisplayValue().isUnbound()) {
			return DISABLED_TEXT_COLOR;
		}
		return TEXT_COLOR;
	}

	@Override
	public ConfigInfo getInfo() {
		ConfigInfo info = super.getInfo();
		List<Component> lines = new ArrayList<>(info.lines());
		lines.add(Component.translatable("mezz_config.config.keyMapping.context.info", getValue().context()));
		List<ConfigKeyMappingConflict> conflicts = getConflicts();
		if (!conflicts.isEmpty()) {
			lines.add(getConflictInfo(conflicts).withStyle(ChatFormatting.YELLOW));
		}
		return new ConfigInfo(info.title(), lines);
	}

	@Override
	@Nullable
	public ConfigInfo getTooltipInfo(double mouseX, double mouseY) {
		@Nullable ConfigInfo resetInfo = super.getTooltipInfo(mouseX, mouseY);
		if (resetInfo != null) {
			return resetInfo;
		}
		List<ConfigKeyMappingConflict> conflicts = getConflicts();
		if (area.contains(mouseX, mouseY) && !conflicts.isEmpty()) {
			return getConflictTooltipInfo(conflicts);
		}
		return null;
	}

	@Override
	protected boolean onMouseClicked(UserInput input) {
		if (super.onMouseClicked(input)) {
			stopListening();
			return true;
		}
		if (listening) {
			if (!input.isSimulate()) {
				ConfigKeyModifier modifier = getPendingModifier();
				setBindingValue(new ConfigKeyBinding(input.getKey().getName(), modifier));
				stopListening();
			}
			return true;
		}
		if (buttonArea.contains(input.getMouseX(), input.getMouseY())) {
			if (!input.isSimulate()) {
				startListening();
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!listening) {
			return false;
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			setBindingValue(ConfigKeyBinding.UNKNOWN);
			stopListening();
			return true;
		}
		InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
		String keyName = key.getName();
		ConfigKeyModifier keyModifier = configKeyMapping.getKeyModifier(keyName);
		if (keyModifier != ConfigKeyModifier.NONE) {
			if (!lastPressedModifier.equals(keyName)) {
				lastPressedModifier = keyName;
			}
			lastModifierHeldDown = true;
		} else {
			lastPressedKey = keyName;
			lastKeyHeldDown = true;
			setBindingValue(getPendingValue());
			stopListening();
		}
		return true;
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (!listening) {
			return false;
		}
		InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
		String keyName = key.getName();
		if (lastPressedKey.equals(keyName)) {
			lastKeyHeldDown = false;
		} else if (lastPressedModifier.equals(keyName)) {
			lastModifierHeldDown = false;
		}

		if (!lastKeyHeldDown && !lastModifierHeldDown) {
			setBindingValue(getPendingValue());
			stopListening();
		}
		return true;
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		return listening;
	}

	@Override
	public boolean isCapturingKeyboardInput() {
		return listening;
	}

	@Override
	public void unfocus() {
		stopListening();
	}

	@Override
	protected boolean setValue(KeyMappingValue value) {
		return super.setValue(value.withBinding(value.configKeyMapping().normalize(value.binding())));
	}

	private boolean setBindingValue(ConfigKeyBinding value) {
		return setValue(getValue().withBinding(value));
	}

	private List<ConfigKeyMappingConflict> getConflicts() {
		ConfigKeyBinding value = getBindingValue();
		if (value.isUnbound()) {
			return List.of();
		}
		return configKeyMapping.getConflicts(value);
	}

	private ConfigInfo getConflictTooltipInfo(List<ConfigKeyMappingConflict> conflicts) {
		List<Component> lines = new ArrayList<>();
		for (ConfigKeyMappingConflict conflict : conflicts.stream().limit(3).toList()) {
			lines.add(Component.translatable(
				"mezz_config.config.keyMapping.conflict.binding",
				conflict.name(),
				conflict.binding()
			));
			lines.add(Component.translatable(
				"mezz_config.config.keyMapping.conflict.mod",
				conflict.modName()
			));
			lines.add(Component.translatable(
				"mezz_config.config.keyMapping.conflict.category",
				conflict.category()
			));
		}
		if (conflicts.size() > 3) {
			lines.add(Component.translatable(
				"mezz_config.config.keyMapping.conflict.more",
				conflicts.size() - 3
			));
		}
		return new ConfigInfo(
			Component.translatable("mezz_config.config.keyMapping.conflict.title")
				.withStyle(ChatFormatting.YELLOW),
			lines
		);
	}

	private static MutableComponent getConflictInfo(List<ConfigKeyMappingConflict> conflicts) {
		String conflictNames = conflicts.stream()
			.limit(3)
			.map(conflict -> conflict.name().getString())
			.reduce((first, second) -> first + ", " + second)
			.orElse("");
		if (conflicts.size() > 3) {
			conflictNames += ", ...";
		}
		return Component.translatable("mezz_config.config.keyMapping.conflict", conflictNames);
	}

	private void startListening() {
		listening = true;
		lastPressedModifier = ConfigKeyBinding.UNKNOWN.keyName();
		lastPressedKey = ConfigKeyBinding.UNKNOWN.keyName();
		lastModifierHeldDown = false;
		lastKeyHeldDown = false;
	}

	private void stopListening() {
		listening = false;
		lastPressedModifier = ConfigKeyBinding.UNKNOWN.keyName();
		lastPressedKey = ConfigKeyBinding.UNKNOWN.keyName();
		lastModifierHeldDown = false;
		lastKeyHeldDown = false;
	}

	private ConfigKeyBinding getDisplayValue() {
		if (listening) {
			ConfigKeyBinding pendingValue = getPendingValue();
			if (!pendingValue.isUnbound()) {
				return pendingValue;
			}
		}
		return getBindingValue();
	}

	private ConfigKeyBinding getBindingValue() {
		return getValue().binding();
	}

	private ConfigKeyBinding getPendingValue() {
		if (!lastPressedKey.equals(ConfigKeyBinding.UNKNOWN.keyName())) {
			return new ConfigKeyBinding(lastPressedKey, getPendingModifier());
		}
		if (!lastPressedModifier.equals(ConfigKeyBinding.UNKNOWN.keyName())) {
			return new ConfigKeyBinding(lastPressedModifier, ConfigKeyModifier.NONE);
		}
		return ConfigKeyBinding.UNKNOWN;
	}

	private ConfigKeyModifier getPendingModifier() {
		if (lastPressedModifier.equals(ConfigKeyBinding.UNKNOWN.keyName())) {
			return KeyModifiers.getActive();
		}
		return configKeyMapping.getKeyModifier(lastPressedModifier);
	}
}
