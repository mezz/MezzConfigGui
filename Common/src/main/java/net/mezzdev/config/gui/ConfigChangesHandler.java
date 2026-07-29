package net.mezzdev.config.gui;

import net.mezzdev.config.api.value.ConfigValueChange;
import net.mezzdev.config.api.value.ConfigValueUpdateType;

import java.util.List;

@FunctionalInterface
interface ConfigChangesHandler {
	ConfigValueUpdateType applyChanges(List<ConfigValueChange<?>> changes);
}
