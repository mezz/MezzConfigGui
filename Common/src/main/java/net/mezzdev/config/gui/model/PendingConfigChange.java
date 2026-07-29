package net.mezzdev.config.gui.model;

import net.mezzdev.config.gui.api.ConfigInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Display model for one unapplied config change in the pending changes confirmation screen.
 */
public record PendingConfigChange(Component name, Component valueChange, ConfigInfo tooltipInfo) {
	public static PendingConfigChange create(
		Component name,
		Component oldValue,
		Component newValue,
		ConfigInfo configInfo,
		@Nullable ConfigInfo oldValueInfo,
		@Nullable ConfigInfo newValueInfo
	) {
		return new PendingConfigChange(
			name,
			createValueChange(oldValue, newValue),
			createTooltipInfo(configInfo, oldValue, newValue, oldValueInfo, newValueInfo)
		);
	}

	public static PendingConfigChange createSummary(
		Component name,
		Component valueChange,
		ConfigInfo tooltipInfo
	) {
		return new PendingConfigChange(name, valueChange, tooltipInfo);
	}

	private static Component createValueChange(Component oldValue, Component newValue) {
		return oldValue.copy()
			.append(Component.literal(" -> "))
			.append(newValue);
	}

	private static ConfigInfo createTooltipInfo(
		ConfigInfo configInfo,
		Component oldValue,
		Component newValue,
		@Nullable ConfigInfo oldValueInfo,
		@Nullable ConfigInfo newValueInfo
	) {
		List<Component> lines = new ArrayList<>(configInfo.lines());
		addValueInfo(lines, Component.translatable("mezz_config.config.screen.pendingChanges.previousValue", oldValue), oldValueInfo);
		addValueInfo(lines, Component.translatable("mezz_config.config.screen.pendingChanges.newValue", newValue), newValueInfo);
		return new ConfigInfo(configInfo.title(), lines);
	}

	private static void addValueInfo(List<Component> lines, Component valueLine, @Nullable ConfigInfo valueInfo) {
		lines.add(Component.empty());
		lines.add(valueLine);
		if (valueInfo == null) {
			return;
		}
		lines.add(valueInfo.title());
		lines.addAll(valueInfo.lines());
	}
}
