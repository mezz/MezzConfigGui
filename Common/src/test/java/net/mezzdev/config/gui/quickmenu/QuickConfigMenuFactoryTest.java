package net.mezzdev.config.gui.quickmenu;

import net.mezzdev.config.file.ConfigFileWatcherSettings;
import net.mezzdev.config.file.ConfigManager;
import net.mezzdev.config.gui.ConfigScreenSchema;
import net.mezzdev.config.gui.ConfigScreenCategory;
import net.mezzdev.config.gui.ConfigScreenCategoryGroup;
import net.mezzdev.config.gui.ConfigScreenCategoryNavigationGroup;
import net.mezzdev.config.gui.ConfigScreenValues;
import net.mezzdev.config.gui.ConfigValueCategoryPath;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IQuickConfigMenuBuilder;
import net.mezzdev.config.gui.api.IConfigValueEditorMetadata;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import net.mezzdev.config.schema.ConfigSchemaBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuickConfigMenuFactoryTest {
	@Test
	void resolvesOrderedSettingsAndRejectsMissingDuplicateOrAmbiguousPaths(@TempDir Path directory) throws Exception {
		var disabled = ConfigFileWatcherSettings.clientDefaults().withEnabled(false);
		var manager = new ConfigManager("Quick menu paths", disabled, disabled);
		try {
			var builder = new ConfigSchemaBuilder(directory.resolve("first.ini"), "test", manager);
			var category = builder.addCategory("display");
			var labels = category.addBoolean("labels", true).build();
			category.addBoolean("columns", false).build();
			var schema = ConfigScreenSchema.from(builder.build());
			var options = QuickMenuEntries.forPaths(schema, List.of("display.labels"), List.of("display.columns"));
			assertEquals(List.of("display.labels", "display.columns"), List.copyOf(options.options().keySet()));
			assertSame(labels, options.options().get("display.labels").getIdentityKey());
			assertThrows(IllegalArgumentException.class, () -> QuickMenuEntries.forPaths(schema, List.of("display.missing"), List.of()));
			assertThrows(IllegalArgumentException.class, () -> QuickMenuEntries.forPaths(schema, List.of("display.labels"), List.of("display.labels")));

			var second = new ConfigSchemaBuilder(directory.resolve("second.ini"), "test", manager);
			second.addCategory("display").addBoolean("labels", false).build();
			var secondSchema = ConfigScreenSchema.from(second.build());
			ConfigScreenSchema merged = () -> Stream.concat(schema.getCategories().stream(), secondSchema.getCategories().stream()).toList();
			assertThrows(IllegalArgumentException.class, () -> QuickMenuEntries.forPaths(merged, List.of("display.labels"), List.of()));
		} finally {
			((AutoCloseable) manager.getSaveScheduler()).close();
		}
	}

	@Test
	void mixedReferencesKeepRegisteredDecoratorsAndMigrateSavedPaths(@TempDir Path directory) throws Exception {
		var disabled = ConfigFileWatcherSettings.clientDefaults().withEnabled(false);
		var manager = new ConfigManager("Quick menu references", disabled, disabled);
		try {
			var builder = new ConfigSchemaBuilder(directory.resolve("first.ini"), "test", manager);
			var category = builder.addCategory("storage");
			var labels = category.addBoolean("labels", true).build();
			var columns = category.addBoolean("columns", true).build();
			builder.build();
			var adapted = ConfigScreenValues.create(labels);
			var registered = ConfigValueCategoryPath.withCategoryPath(
				IConfigScreenValue.withApplyMode(adapted, ConfigValueApplyMode.IMMEDIATE), "display",
				List.of(new ConfigValueCategoryPath.Category("nested", Component.literal("Nested"), Component.empty())));
			var runtime = new RuntimeValue(new Object(), adapted.getEditorMetadata());
			var extraRuntime = new RuntimeValue(new Object(), adapted.getEditorMetadata());
			var runtimeReference = new RuntimeValue(runtime.getIdentityKey(), adapted.getEditorMetadata());
			var decoratedRuntime = IConfigScreenValue.withApplyMode(runtime, ConfigValueApplyMode.IMMEDIATE);
			ConfigScreenSchema schema = () -> List.of(
				new Category("display", List.of(registered, decoratedRuntime, ConfigScreenValues.create(columns))),
				new Category("more", List.of(extraRuntime)));
			var entries = resolve(schema, menu -> menu
				.addDefault(labels).addDefault(runtimeReference)
				.addAdditional(extraRuntime).addAdditional(columns));
			assertEquals(List.of("display.nested.labels", "display.runtime"), entries.defaults());
			assertEquals(List.of("more.runtime", "display.columns"), entries.additional());
			assertEquals(List.of("display.nested.labels", "display.runtime", "more.runtime", "display.columns"), List.copyOf(entries.options().keySet()));
			assertSame(registered, entries.options().get("display.nested.labels"));
			assertSame(decoratedRuntime, entries.options().get("display.runtime"));
			assertSame(extraRuntime, entries.options().get("more.runtime"));
			assertEquals(ConfigValueApplyMode.IMMEDIATE, entries.options().get("display.runtime").getApplyMode());
			assertEquals(ConfigValueApplyMode.IMMEDIATE, entries.options().get("display.nested.labels").getApplyMode());

			// Resolve the registered adapter so menu edits notify its listeners.
			var notifications = new ArrayList<Boolean>();
			runtime.addListener(notifications::add);
			@SuppressWarnings("unchecked")
			var resolvedRuntime = (IConfigScreenValue<Boolean>) entries.options().get("display.runtime");
			resolvedRuntime.set(true);
			assertEquals(List.of(true), notifications);
			assertEquals(false, runtimeReference.getValue());

			var defaults = List.of("display.nested.labels");
			var additional = List.of("display.runtime");
			Path oldDirectory = directory.resolve("old");
			var oldPreferences = new QuickMenuPreferences(oldDirectory, "example", defaults, additional);
			for (List<String> selection : List.of(List.of("display.runtime", "display.nested.labels"), List.<String>of())) {
				oldPreferences.set(selection);
				Path reload = Files.createTempDirectory(directory, "reload-");
				try (var files = Files.list(oldDirectory)) {
					for (Path file : files.toList()) {
						Files.copy(file, reload.resolve(file.getFileName()));
					}
				}
				// Recreate all identities as on a new launch, retaining only registered names on disk.
				var nextBuilder = new ConfigSchemaBuilder(reload.resolve("next.ini"), "test", manager);
				var nextLabels = nextBuilder.addCategory("storage").addBoolean("labels", true).build();
				nextBuilder.build();
				var nextRegistered = ConfigValueCategoryPath.copyTo(registered, ConfigScreenValues.create(nextLabels));
				var nextRuntime = new RuntimeValue(new Object(), adapted.getEditorMetadata());
				ConfigScreenSchema nextSchema = () -> List.of(
					new Category("display", List.of(nextRegistered, nextRuntime)),
					new Category("other", List.of(nextRegistered, nextRuntime)));
				var nextEntries = resolve(nextSchema, menu -> menu.addDefault(nextLabels).addAdditional(nextRuntime));
				var preferences = new QuickMenuPreferences(reload, "example", nextEntries.defaults(), nextEntries.additional());
				assertEquals(selection, preferences.getValue());
			}

			assertThrows(IllegalArgumentException.class, () -> resolve(schema, menu -> menu.addDefault(labels).addAdditional("display.nested.labels")));
			assertThrows(IllegalArgumentException.class, () -> resolve(schema, menu -> menu.addDefault(labels).addAdditional(adapted)));
			assertThrows(IllegalArgumentException.class, () -> resolve(schema, menu -> menu.addDefault(runtime).addAdditional(runtimeReference)));
			assertThrows(IllegalArgumentException.class, () -> resolve(schema, menu -> menu.addDefault(
				new RuntimeValue(new Object(), adapted.getEditorMetadata()))));
			var unregisteredBuilder = new ConfigSchemaBuilder(directory.resolve("unregistered.ini"), "test", manager);
			var unregistered = unregisteredBuilder.addCategory("storage").addBoolean("labels", true).build();
			unregisteredBuilder.build();
			assertThrows(IllegalArgumentException.class, () -> resolve(schema, menu -> menu.addDefault(unregistered)));
			ConfigScreenSchema collision = () -> List.of(new Category("display", List.of(runtime, extraRuntime)));
			assertThrows(IllegalArgumentException.class, () -> resolve(collision, menu -> menu.addDefault("display.runtime")));
			assertThrows(IllegalArgumentException.class, () -> resolve(collision, menu -> menu.addDefault(runtime)));

		} finally {
			((AutoCloseable) manager.getSaveScheduler()).close();
		}
	}

	@Test
	void referencesUseFirstRegisteredEntryWhenValuesAppearInMultipleCategories(@TempDir Path directory) throws Exception {
		var disabled = ConfigFileWatcherSettings.clientDefaults().withEnabled(false);
		var manager = new ConfigManager("Quick menu aliases", disabled, disabled);
		try {
			var builder = new ConfigSchemaBuilder(directory.resolve("config.ini"), "test", manager);
			var labels = builder.addCategory("storage").addBoolean("labels", true).build();
			builder.build();
			var adapted = ConfigScreenValues.create(labels);
			var registered = IConfigScreenValue.withApplyMode(adapted, ConfigValueApplyMode.IMMEDIATE);
			var runtime = new RuntimeValue(new Object(), adapted.getEditorMetadata());
			var runtimeReference = new RuntimeValue(runtime.getIdentityKey(), adapted.getEditorMetadata());
			var registeredRuntime = IConfigScreenValue.withApplyMode(runtime, ConfigValueApplyMode.IMMEDIATE);
			ConfigScreenSchema schema = () -> List.of(
				new Category("display", List.of(registered, registeredRuntime)),
				new Category("other", List.of(adapted, runtimeReference)),
				new Category("display", List.of(adapted, runtimeReference)));

			var entries = resolve(schema, menu -> menu.addDefault(labels).addAdditional(runtimeReference));
			assertEquals(List.of("display.labels"), entries.defaults());
			assertEquals(List.of("display.runtime"), entries.additional());
			assertSame(registered, entries.options().get("display.labels"));
			assertSame(registeredRuntime, entries.options().get("display.runtime"));
			assertSame(registered, resolve(schema, menu -> menu.addDefault(adapted)).options().get("display.labels"));
			assertSame(adapted, resolve(schema, menu -> menu.addDefault("other.labels")).options().get("other.labels"));

			var notifications = new ArrayList<Boolean>();
			runtime.addListener(notifications::add);
			@SuppressWarnings("unchecked")
			var resolvedRuntime = (IConfigScreenValue<Boolean>) entries.options().get("display.runtime");
			resolvedRuntime.set(true);
			assertEquals(List.of(true), notifications);
			assertEquals(false, runtimeReference.getValue());
		} finally {
			((AutoCloseable) manager.getSaveScheduler()).close();
		}
	}

	private static QuickMenuEntries resolve(ConfigScreenSchema schema, Consumer<IQuickConfigMenuBuilder> configure) {
		var builder = new QuickConfigMenuBuilder(() -> schema, entries -> {
			throw new AssertionError("Menu construction requires a Minecraft client");
		});
		configure.accept(builder);
		return builder.resolve();
	}

	private record Category(String name, List<IConfigScreenValue<?>> values) implements ConfigScreenCategory {
		@Override
		public ConfigScreenCategoryGroup getGroup() {
			return ConfigScreenCategoryGroup.MOD_OWNED;
		}

		@Override
		public @Nullable ConfigScreenCategoryNavigationGroup getNavigationGroup() {
			return null;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getLocalizationKey() {
			return name;
		}

		@Override
		public Component getLocalizedName() {
			return Component.literal(name);
		}

		@Override
		public Component getLocalizedDescription() {
			return Component.empty();
		}

		@Override
		public List<IConfigScreenValue<?>> getConfigValues() {
			return values;
		}
	}

	private static final class RuntimeValue implements IConfigScreenValue<Boolean> {
		private final Object key;
		private final IConfigValueEditorMetadata<Boolean> metadata;
		private final AtomicBoolean enabled = new AtomicBoolean();
		private final List<Consumer<Boolean>> listeners = new ArrayList<>();

		private RuntimeValue(Object key, IConfigValueEditorMetadata<Boolean> metadata) {
			this.key = key;
			this.metadata = metadata;
		}

		@Override
		public Object getIdentityKey() {
			return key;
		}

		@Override
		public String getName() {
			return "runtime";
		}

		@Override
		public String getLocalizationKey() {
			return "test.runtime";
		}

		@Override
		public Boolean getValue() {
			return enabled.get();
		}

		@Override
		public Boolean getDefaultValue() {
			return false;
		}

		@Override
		public boolean set(Boolean value) {
			boolean previous = enabled.getAndSet(value);
			listeners.forEach(listener -> listener.accept(value));
			return previous != value;
		}

		@Override
		public Runnable addListener(Consumer<Boolean> listener) {
			listeners.add(listener);
			return () -> listeners.remove(listener);
		}

		@Override
		public IConfigValueEditorMetadata<Boolean> getEditorMetadata() {
			return metadata;
		}
	}

}
