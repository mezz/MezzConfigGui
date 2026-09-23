package net.mezzdev.config.gui.info;

import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditorSerializer;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds localized info-panel and tooltip content for config values and pending updates.
 */
public final class ConfigValueInfoFactory {
	private ConfigValueInfoFactory() {
	}

	@Nullable
	public static <T> ConfigInfo create(IConfigScreenValue<T> configValue, T value, boolean hasPendingChange) {
		return getValueTooltipDescription(configValue, value)
			.map(description -> appendUpdateInfo(configValue, new ConfigInfo(description, List.of()), hasPendingChange))
			.orElseGet(() -> createUpdateInfo(configValue, value, hasPendingChange));
	}

	public static ConfigInfo createBooleanToggle(IConfigScreenValue<Boolean> configValue, boolean value, boolean hasPendingChange) {
		Component description = getValueDescription(configValue, value);
		Component action = getBooleanToggleAction(value);
		return appendUpdateInfo(configValue, new ConfigInfo(description, action), hasPendingChange);
	}

	static Component getBooleanToggleAction(boolean value) {
		if (value) {
			return net.mezzdev.config.gui.internal.LegacyTranslations.translatableWithFallback(
				"mezz_config.config.value.boolean.action.disable",
				"Click to disable this setting."
			);
		}
		return net.mezzdev.config.gui.internal.LegacyTranslations.translatableWithFallback(
			"mezz_config.config.value.boolean.action.enable",
			"Click to enable this setting."
		);
	}

	public static ConfigInfo createSharedInfo(IConfigScreenValue<?> configValue, boolean hasPendingChange) {
		ConfigInfo info = new ConfigInfo(ConfigValueLocalization.getName(configValue), ConfigValueLocalization.getDescription(configValue));
		return appendUpdateInfo(configValue, info, hasPendingChange);
	}

	public static ConfigInfo createResetInfo(IConfigScreenValue<?> configValue) {
		ConfigInfo info = new ConfigInfo(
			Component.translatable("mezz_config.config.screen.reset"),
			Component.translatable("mezz_config.config.screen.reset.value.info")
		);
		return appendUpdateInfo(configValue, info, false);
	}

	@Nullable
	public static <T> ConfigInfo createUpdateInfo(IConfigScreenValue<T> configValue, T value, boolean hasPendingChange) {
		return getUpdateInfo(configValue, hasPendingChange)
			.map(updateInfo -> new ConfigInfo(getValueName(configValue, value), updateInfo))
			.orElse(null);
	}

	@Nullable
	public static <T> ConfigInfo createPendingChangeValueInfo(IConfigScreenValue<T> configValue, T value) {
		return getSpecificValueDescription(configValue, value)
			.map(description -> new ConfigInfo(description, List.of()))
			.orElse(null);
	}

	private static ConfigInfo appendUpdateInfo(IConfigScreenValue<?> configValue, ConfigInfo info, boolean hasPendingChange) {
		Optional<Component> updateInfo = getUpdateInfo(configValue, hasPendingChange);
		if (updateInfo.isEmpty() && !ConfigGuiOptions.showAdvancedValueDetails()) {
			return info;
		}
		List<Component> lines = new ArrayList<>(info.lines());
		updateInfo.ifPresent(lines::add);
		appendAdvancedInfo(configValue, lines);
		return new ConfigInfo(info.title(), lines);
	}

	private static void appendAdvancedInfo(IConfigScreenValue<?> configValue, List<Component> lines) {
		if (!ConfigGuiOptions.showAdvancedValueDetails()) {
			return;
		}
		lines.add(Component.literal("Name: " + configValue.getName()));
		lines.add(Component.literal("Apply mode: " + configValue.getApplyMode()));
		lines.add(Component.literal("Restart requirement: " + configValue.getRestartRequirement()));
		lines.add(Component.literal("Serializer: " + configValue.getSerializer().getClass().getSimpleName()));
		if (configValue.getSerializer() instanceof IConfigValueEditorSerializer<?> editorSerializer) {
			lines.add(Component.literal("Editor: " + editorSerializer.getEditorType().getUid()));
		}
	}

	private static Optional<Component> getUpdateInfo(IConfigScreenValue<?> configValue, boolean hasPendingChange) {
		ConfigValueRestartRequirement restartRequirement = configValue.getRestartRequirement();
		if (restartRequirement == ConfigValueRestartRequirement.WORLD_RESTART) {
			return Optional.of(getUpdateInfoComponent(
				hasPendingChange,
				"mezz_config.config.screen.update.worldRestart.pending",
				"mezz_config.config.screen.update.worldRestart.info"
			));
		}
		if (restartRequirement == ConfigValueRestartRequirement.GAME_RESTART) {
			return Optional.of(getUpdateInfoComponent(
				hasPendingChange,
				"mezz_config.config.screen.update.gameRestart.pending",
				"mezz_config.config.screen.update.gameRestart.info"
			));
		}
		if (configValue.getApplyMode() == ConfigValueApplyMode.ON_APPLY) {
			return Optional.of(getUpdateInfoComponent(
				hasPendingChange,
				"mezz_config.config.screen.update.onApply.pending",
				"mezz_config.config.screen.update.onApply.info"
			));
		}
		return Optional.empty();
	}

	private static Component getUpdateInfoComponent(boolean hasPendingChange, String pendingKey, String infoKey) {
		if (hasPendingChange) {
			return Component.translatable(pendingKey);
		}
		return Component.translatable(infoKey);
	}

	private static <T> Component getValueDescription(IConfigScreenValue<T> configValue, T value) {
		return getSpecificValueDescription(configValue, value)
			.orElseGet(() -> getValueName(configValue, value));
	}

	private static <T> Optional<Component> getValueTooltipDescription(IConfigScreenValue<T> configValue, T value) {
		Optional<Component> valueDescription = getSpecificValueDescription(configValue, value);
		if (valueDescription.isPresent()) {
			return valueDescription;
		}
		if (configValue.getSerializer().getAllValidValues().isPresent()) {
			return Optional.empty();
		}
		return Optional.of(ConfigValueLocalization.getDescription(configValue));
	}

	private static <T> Component getValueName(IConfigScreenValue<T> configValue, T value) {
		return ConfigValueLocalization.getValueName(configValue, value);
	}

	private static <T> Optional<Component> getSpecificValueDescription(IConfigScreenValue<T> configValue, T value) {
		return ConfigValueLocalization.getValueDescription(configValue, value);
	}
}
