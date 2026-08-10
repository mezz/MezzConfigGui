package net.mezzdev.config.gui;

import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.model.AppliedConfigValueChange;
import net.mezzdev.config.gui.model.ConfigValueChange;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tracks applied config changes so they can be undone in one batch.
 */
final class AppliedConfigChangeTracker {
	private final Map<Object, AppliedConfigValueChange<?>> changesByValueKey = new IdentityHashMap<>();

	public void add(AppliedConfigValueChange<?> change) {
		addTyped(change);
	}

	@SuppressWarnings("unchecked")
	private <T> void addTyped(AppliedConfigValueChange<T> change) {
		IConfigScreenValue<T> configValue = change.configValue();
		Object identityKey = configValue.getIdentityKey();
		AppliedConfigValueChange<T> existing = (AppliedConfigValueChange<T>) changesByValueKey.get(identityKey);
		T oldValue = change.oldValue();
		if (existing != null) {
			oldValue = existing.oldValue();
		}
		if (Objects.equals(oldValue, change.newValue())) {
			changesByValueKey.remove(identityKey);
			return;
		}
		changesByValueKey.put(identityKey, new AppliedConfigValueChange<>(configValue, oldValue, change.newValue()));
	}

	public boolean hasChanges() {
		return !changesByValueKey.isEmpty();
	}

	public List<ConfigValueChange<?>> getUndoChanges() {
		return changesByValueKey.values()
			.stream()
			.<ConfigValueChange<?>>map(AppliedConfigValueChange::toUndoChange)
			.toList();
	}

	public void clear() {
		changesByValue.clear();
	}
}
