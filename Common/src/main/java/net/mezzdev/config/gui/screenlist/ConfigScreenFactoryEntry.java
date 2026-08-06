package net.mezzdev.config.gui.screenlist;

import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * One config screen factory with the metadata needed to display it in the config screen list.
 */
public record ConfigScreenFactoryEntry(
	String modId,
	Component title,
	IConfigScreenFactory factory
) {
	public ConfigScreenFactoryEntry {
		Objects.requireNonNull(modId, "modId");
		Objects.requireNonNull(title, "title");
		Objects.requireNonNull(factory, "factory");
	}
}
