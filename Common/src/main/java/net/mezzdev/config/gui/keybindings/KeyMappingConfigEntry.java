package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.gui.ConfigInputUtil;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.ConfigGuiColors;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.input.UserInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.mezzdev.config.gui.api.LegacyGuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Config entry widget for editing key mappings and showing binding conflicts.
 */
public final class KeyMappingConfigEntry extends ConfigEntryWidget<KeyMappingValue> {
	private static final int MIN_BUTTON_WIDTH = 40;
	private static final int BUTTON_HEIGHT = 22;
	private static final int BUTTON_TEXT_PADDING = 4;
	private final IConfigKeyMapping configKeyMapping;
	private ImmutableRect2i buttonArea = ImmutableRect2i.EMPTY;
	private boolean listening = false;
	private String lastPressedModifier = ConfigKeyBinding.UNKNOWN.keyName();
	private String lastPressedKey = ConfigKeyBinding.UNKNOWN.keyName();
	private boolean lastModifierHeldDown = false;
	private boolean lastKeyHeldDown = false;

	public KeyMappingConfigEntry(IConfigScreenValue<KeyMappingValue> value, ConfigTextures textures) {
		super(value, textures);
		this.configKeyMapping = value.getValue().configKeyMapping();
	}

	@Override
	public void updateBounds(ImmutableRect2i area) {
		super.updateBounds(area);
		int buttonWidth = getValueColumnWidth(area, MIN_BUTTON_WIDTH);
		buttonArea = new ImmutableRect2i(
			area.getX() + area.getWidth() - buttonWidth - VALUE_CONTROL_RIGHT_RESERVE,
			area.getY() + (area.getHeight() - BUTTON_HEIGHT) / 2,
			buttonWidth,
			BUTTON_HEIGHT
		);
		recomputeNameArea(area, getValueColumnNameRightReserve(area, MIN_BUTTON_WIDTH));
	}

	@Override
	protected void drawContent(LegacyGuiGraphics guiGraphics, double mouseX, double mouseY) {
		drawName(guiGraphics);
		boolean hasConflict = !getConflicts().isEmpty();
		if (hasConflict) {
			guiGraphics.fill(
				area.getX() + 1,
				area.getY(),
				area.getX() + 3,
				area.getY() + area.getHeight(),
				ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.KEY_MAPPING_CONFLICT_ACCENT)
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
			MutableComponent listeningText = Component.literal("> ")
				.append(withColor(keyName.copy().withStyle(ChatFormatting.UNDERLINE), ConfigGuiColors.GuiColor.KEY_MAPPING_LISTENING_KEY_TEXT))
				.append(" <");
			return withColor(listeningText, ConfigGuiColors.GuiColor.KEY_MAPPING_LISTENING_TEXT);
		}
		return keyName;
	}

	private int getButtonTextColor(boolean hasConflict, boolean hovered) {
		if (hasConflict) {
			return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.KEY_MAPPING_CONFLICT_ACCENT);
		}
		if (listening || hovered) {
			return getConfiguredHoverTextColor();
		}
		if (getDisplayValue().isUnbound()) {
			return getConfiguredDisabledTextColor();
		}
		return getConfiguredTextColor();
	}

	@Override
	public ConfigInfo getInfo() {
		ConfigInfo info = super.getInfo();
		List<Component> lines = new ArrayList<>();
		for (Component line : info.lines()) {
			if (!line.getString().isBlank()) {
				lines.add(line);
			}
		}
		Component context = getValue().context();
		if (!context.getString().isBlank()) {
			lines.add(Component.translatable("mezz_config.config.keyMapping.context.info", context));
		}
		List<ConfigKeyMappingConflict> conflicts = getConflicts();
		if (!conflicts.isEmpty()) {
			lines.add(withColor(getConflictInfo(conflicts), ConfigGuiColors.GuiColor.KEY_MAPPING_CONFLICT_INFO_TEXT));
		}
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
		if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
			setBindingValue(ConfigKeyBinding.UNKNOWN);
			stopListening();
			return true;
		}
		InputConstants.Key key = ConfigInputUtil.getKey(keyCode, scanCode);
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
		InputConstants.Key key = ConfigInputUtil.getKey(keyCode, scanCode);
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
		if (!ConfigGuiOptions.showKeyConflictDetails()) {
			return List.of();
		}
		ConfigKeyBinding value = getBindingValue();
		if (value.isUnbound()) {
			return List.of();
		}
		return configKeyMapping.getConflicts(value);
	}

	static ConfigInfo getConflictTooltipInfo(List<ConfigKeyMappingConflict> conflicts) {
		List<Component> lines = new ArrayList<>();
		for (ConfigKeyMappingConflict conflict : conflicts.stream().limit(3).toList()) {
			if (!lines.isEmpty()) {
				lines.add(Component.empty());
			}
			lines.add(Component.literal("• ").append(Component.translatable(
				"mezz_config.config.keyMapping.conflict.binding",
				conflict.name(),
				conflict.binding()
			))
				.withStyle(ChatFormatting.BOLD));
			lines.add(Component.literal("  ").append(Component.translatable(
				"mezz_config.config.keyMapping.conflict.mod",
				conflict.modName()
			)));
			lines.add(Component.literal("  ").append(Component.translatable(
				"mezz_config.config.keyMapping.conflict.category",
				conflict.category()
			)));
		}
		if (conflicts.size() > 3) {
			lines.add(Component.empty());
			lines.add(Component.translatable(
				"mezz_config.config.keyMapping.conflict.more",
				conflicts.size() - 3
			));
		}
		return new ConfigInfo(
			withColor(
				Component.translatable("mezz_config.config.keyMapping.conflict.title"),
				ConfigGuiColors.GuiColor.KEY_MAPPING_CONFLICT_INFO_TEXT
			),
			lines
		);
	}

	private static MutableComponent withColor(MutableComponent component, ConfigGuiColors.GuiColor color) {
		int rgb = ConfigGuiColors.getColor(color) & 0xFFFFFF;
		return component.setStyle(component.getStyle().withColor(rgb));
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
