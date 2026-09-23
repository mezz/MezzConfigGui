package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.gui.ConfigInputUtil;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

public final class ConfigKeyBindingUtil {
	private ConfigKeyBindingUtil() {

	}

	public static ConfigKeyBinding create(InputConstants.Key key, ConfigKeyModifier modifier) {
		return new ConfigKeyBinding(key.getName(), modifier);
	}

	public static InputConstants.Key getKey(String keyName) {
		try {
			return InputConstants.getKey(keyName);
		} catch (IllegalArgumentException ignored) {
			return InputConstants.UNKNOWN;
		}
	}

	public static InputConstants.Key getKey(KeyMapping keyMapping) {
		return getKey(keyMapping.saveString());
	}

	public static ConfigKeyBinding normalize(
		ConfigKeyBinding value,
		Function<InputConstants.Key, ConfigKeyModifier> getKeyModifier
	) {
		InputConstants.Key key = getKey(value.keyName());
		if (value.isUnbound() || value.modifier() == getKeyModifier.apply(key)) {
			return new ConfigKeyBinding(value.keyName(), ConfigKeyModifier.NONE);
		}
		return value;
	}

	public static Component getDisplayName(
		ConfigKeyBinding value,
		Function<InputConstants.Key, ConfigKeyModifier> getKeyModifier
	) {
		InputConstants.Key key = getKey(value.keyName());
		if (value.modifier() == ConfigKeyModifier.NONE) {
			ConfigKeyModifier keyModifier = getKeyModifier.apply(key);
			if (keyModifier != ConfigKeyModifier.NONE) {
				return getDisplayName(keyModifier);
			}
		}
		return getCombinedName(value.modifier(), getKeyDisplayName(key));
	}

	public static Component getKeyDisplayName(InputConstants.Key key) {
		if (key.getType() == InputConstants.Type.MOUSE) {
			int value = key.getValue();
			if (value == InputConstants.MOUSE_BUTTON_LEFT) {
				return Component.translatable("mezz_config.key.mouse.left");
			} else if (value == InputConstants.MOUSE_BUTTON_RIGHT) {
				return Component.translatable("mezz_config.key.mouse.right");
			}
		}
		if (key.getType() == InputConstants.Type.KEYBOARD) {
			int value = key.getValue();
			if (ConfigInputUtil.isMac() && (value == ConfigInputUtil.KEY_COMMAND_LEFT || value == ConfigInputUtil.KEY_COMMAND_RIGHT)) {
				return Component.translatable("mezz_config.key.modifier.command");
			}
			return switch (value) {
				case InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT -> Component.translatable("mezz_config.key.modifier.shift");
				case InputConstants.KEY_LALT, InputConstants.KEY_RALT -> Component.translatable("mezz_config.key.modifier.alt");
				case InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL -> Component.translatable("mezz_config.key.modifier.control");
				default -> key.getDisplayName();
			};
		}
		return key.getDisplayName();
	}

	public static ConfigKeyModifier getKeyModifier(InputConstants.Key key) {
		if (!key.getType().equals(InputConstants.Type.KEYBOARD)) {
			return ConfigKeyModifier.NONE;
		}
		int keyCode = key.getValue();
		return switch (keyCode) {
			case InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT -> ConfigKeyModifier.SHIFT;
			case InputConstants.KEY_LALT, InputConstants.KEY_RALT -> ConfigKeyModifier.ALT;
			case InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL -> ConfigKeyModifier.CONTROL_OR_COMMAND;
			case ConfigInputUtil.KEY_COMMAND_LEFT, ConfigInputUtil.KEY_COMMAND_RIGHT -> getMacControlOrCommandModifier();
			default -> ConfigKeyModifier.NONE;
		};
	}

	public static Component getCombinedName(ConfigKeyModifier modifier, Component component) {
		return switch (modifier) {
			case CONTROL_OR_COMMAND -> getControlOrCommandName(component);
			case SHIFT -> Component.translatable("mezz_config.key.combo.shift", component);
			case ALT -> Component.translatable("mezz_config.key.combo.alt", component);
			case NONE -> component;
		};
	}

	private static Component getDisplayName(ConfigKeyModifier modifier) {
		return switch (modifier) {
			case CONTROL_OR_COMMAND -> getControlOrCommandDisplayName();
			case SHIFT -> Component.translatable("mezz_config.key.modifier.shift");
			case ALT -> Component.translatable("mezz_config.key.modifier.alt");
			case NONE -> Component.empty();
		};
	}

	private static ConfigKeyModifier getMacControlOrCommandModifier() {
		if (ConfigInputUtil.isMac()) {
			return ConfigKeyModifier.CONTROL_OR_COMMAND;
		}
		return ConfigKeyModifier.NONE;
	}

	private static Component getControlOrCommandName(Component component) {
		if (ConfigInputUtil.isMac()) {
			return Component.translatable("mezz_config.key.combo.command", component);
		}
		return Component.translatable("mezz_config.key.combo.control", component);
	}

	private static Component getControlOrCommandDisplayName() {
		if (ConfigInputUtil.isMac()) {
			return Component.translatable("mezz_config.key.modifier.command");
		}
		return Component.translatable("mezz_config.key.modifier.control");
	}
}
