package net.mezzdev.config.gui.info;

import net.mezzdev.config.api.value.ConfigValueUpdateType;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

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
	public static <T> ConfigInfo create(IConfigValue<T> configValue, T value, boolean hasPendingChange) {
		return getValueTooltipDescription(configValue, value)
			.map(description -> appendUpdateInfo(configValue, new ConfigInfo(description, List.of()), hasPendingChange))
			.orElseGet(() -> createUpdateInfo(configValue, value, hasPendingChange));
	}

	public static ConfigInfo createBooleanToggle(IConfigValue<Boolean> configValue, boolean value, boolean hasPendingChange) {
		Component description = getValueDescription(configValue, value);
		Component action = Component.translatableWithFallback(
			"mezz_config.config.value.boolean.clickTo",
			"Click to: %s",
			getValueDescription(configValue, !value)
		);
		return appendUpdateInfo(configValue, new ConfigInfo(description, action), hasPendingChange);
	}

	public static ConfigInfo createSharedInfo(IConfigValue<?> configValue, boolean hasPendingChange) {
		ConfigInfo info = new ConfigInfo(configValue.getLocalizedName(), configValue.getLocalizedDescription());
		return appendUpdateInfo(configValue, info, hasPendingChange);
	}

	public static ConfigInfo createResetInfo(IConfigValue<?> configValue) {
		ConfigInfo info = new ConfigInfo(
			Component.translatable("mezz_config.config.screen.reset"),
			Component.translatable("mezz_config.config.screen.reset.value.info")
		);
		return appendUpdateInfo(configValue, info, false);
	}

	@Nullable
	public static <T> ConfigInfo createUpdateInfo(IConfigValue<T> configValue, T value, boolean hasPendingChange) {
		return getUpdateInfo(configValue, hasPendingChange)
			.map(updateInfo -> new ConfigInfo(getValueName(configValue, value), updateInfo))
			.orElse(null);
	}

	@Nullable
	public static <T> ConfigInfo createPendingChangeValueInfo(IConfigValue<T> configValue, T value) {
		return getSpecificValueDescription(configValue, value)
			.map(description -> new ConfigInfo(description, List.of()))
			.orElse(null);
	}

	private static ConfigInfo appendUpdateInfo(IConfigValue<?> configValue, ConfigInfo info, boolean hasPendingChange) {
		Optional<Component> updateInfo = getUpdateInfo(configValue, hasPendingChange);
		if (updateInfo.isEmpty()) {
			return info;
		}
		List<Component> lines = new ArrayList<>(info.lines());
		lines.add(updateInfo.get());
		return new ConfigInfo(info.title(), lines);
	}

	private static Optional<Component> getUpdateInfo(IConfigValue<?> configValue, boolean hasPendingChange) {
		ConfigValueUpdateType updateType = configValue.getUpdateType();
		return switch (updateType) {
			case IMMEDIATE -> Optional.empty();
			case ON_APPLY -> Optional.of(Component.translatable(hasPendingChange ? "mezz_config.config.screen.update.onApply.pending" : "mezz_config.config.screen.update.onApply.info"));
			case RESTART -> Optional.of(Component.translatable(hasPendingChange ? "mezz_config.config.screen.update.restart.pending" : "mezz_config.config.screen.update.restart.info"));
		};
	}

	private static <T> Component getValueDescription(IConfigValue<T> configValue, T value) {
		return getSpecificValueDescription(configValue, value)
			.orElseGet(() -> getValueName(configValue, value));
	}

	private static <T> Optional<Component> getValueTooltipDescription(IConfigValue<T> configValue, T value) {
		Optional<Component> valueDescription = getSpecificValueDescription(configValue, value);
		if (valueDescription.isPresent()) {
			return valueDescription;
		}
		if (configValue.getSerializer().getAllValidValues().isPresent()) {
			return Optional.empty();
		}
		return Optional.of(configValue.getLocalizedDescription());
	}

	private static <T> Component getValueName(IConfigValue<T> configValue, T value) {
		return configValue.getSerializer()
			.getLocalizedValueName(configValue.getLocalizationKey(), value);
	}

	private static <T> Optional<Component> getSpecificValueDescription(IConfigValue<T> configValue, T value) {
		return configValue.getSerializer()
			.getLocalizedValueDescription(configValue.getLocalizationKey(), value);
	}
}
