package net.mezzdev.config.gui;

import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.model.AppliedConfigValueChange;
import net.mezzdev.config.gui.model.ConfigValueChange;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tracks applied config changes so they can be undone in one batch.
 */
final class AppliedConfigChangeTracker {
	private final Map<Object, AppliedConfigValueChange<?>> changesByValueKey = new IdentityHashMap<>();
	private final List<Object> valueKeysInApplicationOrder = new ArrayList<>();

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
		removeFromApplicationOrder(identityKey);
		if (Objects.equals(oldValue, change.newValue())) {
			changesByValueKey.remove(identityKey);
			return;
		}
		changesByValueKey.put(identityKey, new AppliedConfigValueChange<>(configValue, oldValue, change.newValue()));
		valueKeysInApplicationOrder.add(identityKey);
	}

	private void removeFromApplicationOrder(Object identityKey) {
		for (int i = 0; i < valueKeysInApplicationOrder.size(); i++) {
			if (valueKeysInApplicationOrder.get(i) == identityKey) {
				valueKeysInApplicationOrder.remove(i);
				return;
			}
		}
	}

	public boolean hasChanges() {
		return !changesByValueKey.isEmpty();
	}

	public List<ConfigValueChange<?>> getUndoChanges() {
		List<ConfigValueChange<?>> undoChanges = new ArrayList<>(valueKeysInApplicationOrder.size());
		for (int i = valueKeysInApplicationOrder.size() - 1; i >= 0; i--) {
			Object identityKey = valueKeysInApplicationOrder.get(i);
			AppliedConfigValueChange<?> change = Objects.requireNonNull(changesByValueKey.get(identityKey));
			undoChanges.add(change.toUndoChange());
		}
		return List.copyOf(undoChanges);
	}

}
