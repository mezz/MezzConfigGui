package net.mezzdev.config.gui.remote;

import java.util.Objects;

record RemoteValueKey(String categoryName, String valueName) {
	RemoteValueKey {
		Objects.requireNonNull(categoryName, "categoryName");
		Objects.requireNonNull(valueName, "valueName");
	}
}
