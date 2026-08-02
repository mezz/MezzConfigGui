package net.mezzdev.config.gui;

import net.mezzdev.config.gui.model.ConfigValueChange;

import java.util.List;

@FunctionalInterface
interface ConfigChangesHandler {
	boolean applyChanges(List<ConfigValueChange<?>> changes);
}
