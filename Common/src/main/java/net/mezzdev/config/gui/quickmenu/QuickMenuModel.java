package net.mezzdev.config.gui.quickmenu;

import net.mezzdev.config.gui.api.IConfigScreenValue;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.function.Consumer;

/** Settings and customization state shared by the menu controller and view. */
final class QuickMenuModel<T> {
	private final Map<T, Row<T>> availableRows;
	private final QuickMenuSelection<T> selection;
	private List<Row<T>> rows = List.of();
	private boolean editing;

	QuickMenuModel(List<Row<T>> options,
		IConfigScreenValue<List<T>> selection, Consumer<List<T>> saveSelection) {
		Map<T, Row<T>> available = new LinkedHashMap<>();
		for (Row<T> row : options) {
			if (available.putIfAbsent(row.id(), row) != null) {
				throw new IllegalArgumentException("Duplicate quick menu option id: " + row.id());
			}
		}
		this.availableRows = Collections.unmodifiableMap(available);
		this.selection = new QuickMenuSelection<>(List.copyOf(available.keySet()), selection, saveSelection);
	}

	QuickMenuSelection<T> selection() {
		return selection;
	}

	List<Row<T>> rows() {
		return rows;
	}

	boolean isEditing() {
		return editing;
	}

	int rowIndex(T id) {
		for (int i = 0; i < rows.size(); i++) {
			if (rows.get(i).id().equals(id)) {
				return i;
			}
		}
		return -1;
	}

	List<IConfigScreenValue<?>> refreshRows() {
		List<T> order = selection.selected();
		if (editing) {
			order = selection.editorOrder();
		}
		rows = order.stream().flatMap(id -> Optional.ofNullable(availableRows.get(id)).stream()).toList();
		return rows.stream().<IConfigScreenValue<?>>map(Row::value).toList();
	}

	void toggleEditing() {
		editing = !editing;
	}

	void close() {
		editing = false;
	}

	record Row<T>(T id, IConfigScreenValue<?> value) {
	}
}
