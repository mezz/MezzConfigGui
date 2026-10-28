package net.mezzdev.config.gui.quickmenu;

import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.gui.model.ConfigValueChange;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** The selection shared by the quick menu and config screen. */
final class QuickMenuSelection<T> {
	private final List<T> available;
	private final IConfigScreenValue<List<T>> value;
	private final Consumer<List<T>> save;
	private @Nullable List<T> pending;

	QuickMenuSelection(List<T> available, IConfigScreenValue<List<T>> value, Consumer<List<T>> save) {
		if (value.getRestartRequirement() != ConfigValueRestartRequirement.NONE) {
			throw new IllegalArgumentException("The quick menu selection must not require a restart: " + value.getName());
		}
		this.available = List.copyOf(available);
		this.value = value;
		this.save = save;
	}

	List<T> selected() {
		List<T> selected = pending;
		if (selected == null) {
			selected = value.getValue();
		}
		return selected.stream().filter(available::contains).distinct().toList();
	}

	Optional<ConfigValueChange<?>> getPendingChange() {
		if (pending != null && !pending.equals(value.getValue())) {
			return Optional.of(new ConfigValueChange<>(value, pending));
		}
		return Optional.empty();
	}

	void saved() {
		pending = null;
	}

	private void update(List<T> selected) {
		List<T> next = List.copyOf(selected);
		if (value.getApplyMode() == ConfigValueApplyMode.IMMEDIATE) {
			save.accept(next);
		} else if (next.equals(value.getValue())) {
			pending = null;
		} else {
			pending = next;
		}
	}

	List<T> editorOrder() {
		List<T> result = new ArrayList<>(selected());
		available.stream().filter(id -> !result.contains(id)).forEach(result::add);
		return List.copyOf(result);
	}

	void toggleMembership(T id) {
		if (!available.contains(id)) {
			return;
		}
		List<T> result = new ArrayList<>(selected());
		if (!result.remove(id)) {
			result.add(id);
		}
		update(result);
	}

	void move(T id, int direction) {
		List<T> result = new ArrayList<>(selected());
		int from = result.indexOf(id);
		int to = from + direction;
		if (from >= 0 && to >= 0 && to < result.size()) {
			result.remove(from);
			result.add(to, id);
			update(result);
		}
	}
}
