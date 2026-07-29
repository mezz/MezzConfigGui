package net.mezzdev.config.gui.api;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

/**
 * Information shown in the config screen info panel or tooltip area.
 *
 * @param title the title line
 * @param lines the detail lines
 *
 * @since 0.1.0
 */
public record ConfigInfo(Component title, List<Component> lines) {
	public ConfigInfo {
		title = Objects.requireNonNull(title, "title");
		lines = List.copyOf(lines);
	}

	/**
	 * Create config info with one detail line.
	 *
	 * @param title the title line
	 * @param line the detail line
	 *
	 * @since 0.1.0
	 */
	public ConfigInfo(Component title, Component line) {
		this(title, List.of(line));
	}
}
