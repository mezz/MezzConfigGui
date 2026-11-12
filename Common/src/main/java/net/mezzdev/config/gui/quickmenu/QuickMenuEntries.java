package net.mezzdev.config.gui.quickmenu;

import net.mezzdev.config.gui.ConfigScreenSchema;
import net.mezzdev.config.gui.ConfigValueCategoryPath;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.api.value.IConfigValue;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Registered entries and their persistent paths, in menu order. */
record QuickMenuEntries(LinkedHashMap<String, IConfigScreenValue<?>> options, List<String> defaults, List<String> additional) {

	static QuickMenuEntries forPaths(ConfigScreenSchema schema, List<String> defaults, List<String> additional) {
		return new Resolver(schema).resolve(defaults, additional);
	}

	private static List<Entry> index(ConfigScreenSchema schema) {
		List<Entry> entries = new ArrayList<>();
		for (var category : schema.getCategories()) {
			for (var value : category.getConfigValues()) {
				String nested = ConfigValueCategoryPath.getCategories(value).stream()
					.map(ConfigValueCategoryPath.Category::name)
					.collect(Collectors.joining("."));
				String categoryPath = category.getName();
				if (!nested.isEmpty()) {
					categoryPath += "." + nested;
				}
				entries.add(new Entry(categoryPath + "." + value.getName(), value));
			}
		}
		return entries;
	}

	static final class Resolver {
		private final List<Entry> entries;

		Resolver(ConfigScreenSchema schema) {
			this.entries = index(schema);
		}

		String path(IConfigValue<?> value) {
			return pathForIdentity(value, value.getEditorInfo().getName());
		}

		String path(IConfigScreenValue<?> value) {
			return pathForIdentity(value.getIdentityKey(), value.getName());
		}

		private String pathForIdentity(Object identityKey, String name) {
			return entries.stream()
				.filter(entry -> entry.value().getIdentityKey() == identityKey)
				.map(Entry::path)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Quick menu value is not registered: " + name));
		}

		QuickMenuEntries resolve(List<String> defaults, List<String> additional) {
			var options = new LinkedHashMap<String, IConfigScreenValue<?>>();
			for (String path : Stream.concat(defaults.stream(), additional.stream()).toList()) {
				if (path.isBlank() || options.containsKey(path)) {
					throw new IllegalArgumentException("Duplicate or empty quick menu setting path: " + path);
				}
				Map<Object, IConfigScreenValue<?>> matches = new IdentityHashMap<>();
				for (Entry entry : entries) {
					if (path.equals(entry.path())) {
						matches.putIfAbsent(entry.value().getIdentityKey(), entry.value());
					}
				}
				if (matches.size() != 1) {
					throw new IllegalArgumentException("Quick menu setting path must match exactly one setting: " + path + " (found " + matches.size() + ")");
				}
				options.put(path, matches.values().iterator().next());
			}
			return new QuickMenuEntries(options, List.copyOf(defaults), List.copyOf(additional));
		}
	}

	private record Entry(String path, IConfigScreenValue<?> value) {}
}
