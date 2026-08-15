package net.mezzdev.config.gui;

import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.mezzdev.config.gui.screenlist.ConfigScreenListEntry;
import net.mezzdev.config.gui.screenlist.ConfigScreenListScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Shared navigation targets available to config screens created from one config screen registry.
 */
public final class ConfigScreenNavigation {
	@Nullable
	private IConfigScreenFactory screenListFactory;
	private List<ConfigScreenListEntry> screenListEntries = List.of();

	public void setScreenListFactory(IConfigScreenFactory screenListFactory) {
		this.screenListFactory = Objects.requireNonNull(screenListFactory, "screenListFactory");
	}

	public void setScreenListEntries(List<ConfigScreenListEntry> screenListEntries) {
		this.screenListEntries = List.copyOf(screenListEntries);
	}

	public List<ConfigScreenListEntry> getScreenListEntries() {
		return screenListEntries;
	}

	public boolean canOpenScreenList(@Nullable Screen parent) {
		return parent instanceof ConfigScreenListScreen ||
			screenListFactory != null;
	}

	public Screen createScreenList(@Nullable Screen parent) {
		if (parent instanceof ConfigScreenListScreen screenListScreen) {
			return screenListScreen;
		}
		@Nullable
		IConfigScreenFactory screenListFactory = this.screenListFactory;
		if (screenListFactory == null) {
			throw new IllegalStateException("No config screen list factory has been registered.");
		}
		return screenListFactory.create(parent);
	}
}
