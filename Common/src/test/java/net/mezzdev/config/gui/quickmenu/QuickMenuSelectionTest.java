package net.mezzdev.config.gui.quickmenu;

import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.editor.ConfigValueEditMode;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.file.ConfigFileWatcherSettings;
import net.mezzdev.config.file.ConfigManager;
import net.mezzdev.config.file.ConfigSerializer;
import net.mezzdev.config.gui.ConfigScreenValues;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.schema.ConfigSchema;
import net.mezzdev.config.schema.ConfigSchemaBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuickMenuSelectionTest {
	@ParameterizedTest
	@EnumSource(ConfigValueEditMode.class)
	void inlineEditsFullConfigAndReloadShareOneOrderedSelection(ConfigValueEditMode editMode, @TempDir Path directory) throws Exception {
		Path file = directory.resolve("selection.ini");
		try (Fixture fixture = create(file, editMode)) {
			var fullConfig = ConfigScreenValues.create(fixture.value());
			var inline = selection(List.of(Choice.values()), fullConfig);
			inline.move(Choice.SECOND, -1);
			inline.toggleMembership(Choice.THIRD);
			inline.toggleMembership(Choice.FIRST);
			assertEquals(List.of(Choice.SECOND, Choice.THIRD), inline.selected());
			if (editMode == ConfigValueEditMode.BATCH) {
				assertEquals(List.of(Choice.FIRST, Choice.SECOND), fullConfig.getValue());
			}
			savePending(inline);
			assertEquals(List.of(Choice.SECOND, Choice.THIRD), fullConfig.getValue());
			fullConfig.set(List.of(Choice.THIRD));
			assertEquals(List.of(Choice.THIRD), inline.selected());
			inline.toggleMembership(Choice.THIRD);
			savePending(inline);
			assertEquals(List.of(), fullConfig.getValue());
			assertEquals(List.of(Choice.values()), inline.editorOrder());
			inline.toggleMembership(Choice.SECOND);
			savePending(inline);
			assertEquals(List.of(Choice.SECOND), fullConfig.getValue());
		}
		try (Fixture fixture = create(file, editMode)) {
			var inline = selection(List.of(Choice.values()), ConfigScreenValues.create(fixture.value()));
			assertEquals(List.of(Choice.SECOND), inline.selected());
			Files.writeString(file, "[menu]\nselection = " + fixture.value().getEditorInfo().getSerializer().serialize(List.of(Choice.THIRD, Choice.FIRST)) + "\n");
			ConfigSerializer.load(file, fixture.schema().getCategories());
			assertEquals(List.of(Choice.THIRD, Choice.FIRST), inline.selected());
		}
	}

	@Test
	void preservesOrderAndDoesNotRewriteUnknownOrDuplicateChoicesUntilEdited(@TempDir Path directory) throws Exception {
		try (Fixture fixture = create(directory.resolve("selection.ini"))) {
			var value = ConfigScreenValues.create(fixture.value());
			value.set(List.of(Choice.THIRD, Choice.SECOND, Choice.SECOND, Choice.FIRST));
			var inline = selection(List.of(Choice.FIRST, Choice.SECOND), value);
			assertEquals(List.of(Choice.SECOND, Choice.FIRST), inline.selected());
			assertEquals(List.of(Choice.THIRD, Choice.SECOND, Choice.SECOND, Choice.FIRST), value.getValue());
			inline.move(Choice.FIRST, -1);
			savePending(inline);
			assertEquals(List.of(Choice.FIRST, Choice.SECOND), value.getValue());
		}
	}

	@Test
	void refusesRestartRequiredSelections(@TempDir Path directory) throws Exception {
		try (Fixture fixture = create(directory.resolve("selection.ini"))) {
			var value = ConfigScreenValues.create(fixture.value());
			assertEquals(ConfigValueApplyMode.ON_APPLY, value.getApplyMode());
			assertThrows(IllegalArgumentException.class, () -> selection(List.of(Choice.values()),
				IConfigScreenValue.withRestartRequirement(value, ConfigValueRestartRequirement.GAME_RESTART)));
		}
	}

	@Test
	void rowsKeepRegisteredValuesWhenSavedChoicesContainUnknownOrDuplicateIds(@TempDir Path directory) throws Exception {
		var disabled = ConfigFileWatcherSettings.clientDefaults().withEnabled(false);
		var manager = new ConfigManager("Quick menu row bindings", disabled, disabled);
		try {
			var builder = new ConfigSchemaBuilder(directory.resolve("menu.ini"), "test", manager);
			var category = builder.addCategory("menu");
			var first = ConfigScreenValues.create(category.addBoolean("first", true).build());
			var second = ConfigScreenValues.create(category.addBoolean("second", false).build());
			var selection = ConfigScreenValues.create(category.addEnumList("selection", List.of(Choice.FIRST), Choice.class).build());
			builder.build();
			var options = new ArrayList<QuickMenuModel.Row<Choice>>();
			options.add(new QuickMenuModel.Row<>(Choice.FIRST, first));
			options.add(new QuickMenuModel.Row<>(Choice.SECOND, second));
			var model = new QuickMenuModel<>(options, selection, selection::set);
			assertThrows(IllegalArgumentException.class, () -> new QuickMenuModel<>(List.of(options.get(0), options.get(0)), selection, selection::set));
			options.clear();
			selection.set(List.of(Choice.THIRD, Choice.SECOND, Choice.SECOND));

			assertEquals(List.of(second), model.refreshRows());
			assertEquals(List.of(new QuickMenuModel.Row<>(Choice.SECOND, second)), model.rows());
			model.toggleEditing();
			assertEquals(List.of(second, first), model.refreshRows());
			assertEquals(List.of(new QuickMenuModel.Row<>(Choice.SECOND, second), new QuickMenuModel.Row<>(Choice.FIRST, first)), model.rows());
			selection.set(List.of(Choice.FIRST));
			assertEquals(List.of(first, second), model.refreshRows());
			model.close();
			assertEquals(List.of(first), model.refreshRows());
		} finally {
			((AutoCloseable) manager.getSaveScheduler()).close();
		}
	}

	private static <T> QuickMenuSelection<T> selection(List<T> available, IConfigScreenValue<List<T>> value) {
		return new QuickMenuSelection<>(available, value, value::set);
	}

	private static void savePending(QuickMenuSelection<?> selection) {
		selection.getPendingChange().ifPresent(change -> change.apply());
		selection.saved();
	}

	private static Fixture create(Path file) {
		return create(file, ConfigValueEditMode.BATCH);
	}

	private static Fixture create(Path file, ConfigValueEditMode editMode) {
		var disabled = ConfigFileWatcherSettings.clientDefaults().withEnabled(false);
		var manager = new ConfigManager("Quick menu persistence test", disabled, disabled);
		var builder = new ConfigSchemaBuilder(file, "test", manager);
		var value = builder.addCategory("menu").addEnumList("selection", List.of(Choice.FIRST, Choice.SECOND), Choice.class)
			.setEditMode(editMode).build();
		return new Fixture(value, builder.build(), manager);
	}

	private enum Choice { FIRST, SECOND, THIRD }
	private record Fixture(IConfigValue<List<Choice>> value, ConfigSchema schema, ConfigManager manager) implements AutoCloseable {
		@Override
		public void close() throws Exception {
			((AutoCloseable) manager.getSaveScheduler()).close();
		}
	}
}
