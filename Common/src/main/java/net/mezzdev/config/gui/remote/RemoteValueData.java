package net.mezzdev.config.gui.remote;

import java.util.Objects;

record RemoteValueData(RemoteValueKey key, String serializedValue) {
	RemoteValueData {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(serializedValue, "serializedValue");
	}
}
