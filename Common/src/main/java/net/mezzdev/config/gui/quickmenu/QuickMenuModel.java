package net.mezzdev.config.gui.quickmenu;

import net.mezzdev.config.gui.api.IConfigScreenValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.function.Consumer;

/** Settings and customization state shared by the menu controller and view. */
final class QuickMenuModel<T> {
	private final Map<T, IConfigScreenValue<?>> values = new LinkedHashMap<>();
	private final QuickMenuSelection<T> selection;
	private List<T> rows = List.of();
	private boolean editing;

	QuickMenuModel(SequencedMap<T, ? extends IConfigScreenValue<?>> options,
		IConfigScreenValue<List<T>> selection, Consumer<List<T>> saveSelection) {
		this.selection = new QuickMenuSelection<>(List.copyOf(options.sequencedKeySet()), selection, saveSelection);
		options.forEach((id, value) -> values.put(Objects.requireNonNull(id), Objects.requireNonNull(value)));
	}

	QuickMenuSelection<T> selection() {
		return selection;
	}

	List<T> rows() {
		return rows;
	}

	boolean isEditing() {
		return editing;
	}

	IConfigScreenValue<?> value(T id) {
		return values.get(id);
	}

	List<IConfigScreenValue<?>> refreshRows() {
		rows = selection.selected();
		if (editing) {
			rows = selection.editorOrder();
		}
		return rows.stream().map(values::get).toList();
	}

	void toggleEditing() {
		editing = !editing;
	}

	void close() {
		editing = false;
	}
}
