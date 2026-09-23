package net.mezzdev.config.gui.model;

import net.mezzdev.config.gui.ConfigScreenCategory;
import net.mezzdev.config.gui.ConfigScreenCategoryGroup;
import net.mezzdev.config.gui.ConfigScreenCategoryNavigationGroup;
import net.mezzdev.config.gui.ConfigValueCategoryPath;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Builds presentation-only subsections after plugins have resolved their categories and values.
 */
final class ConfigCategoryTree {
	private ConfigCategoryTree() {

	}

	static List<Node> create(List<ConfigScreenCategory> categories, int inlineSubsectionLimit) {
		List<Node> result = new ArrayList<>();
		Map<String, List<ConfigScreenCategory>> groupedCategories = new LinkedHashMap<>();
		for (ConfigScreenCategory category : categories) {
			ConfigScreenCategoryNavigationGroup group = getNavigationGroup(category);
			if (group != null) {
				groupedCategories.computeIfAbsent(group.name(), ignored -> new ArrayList<>()).add(category);
			}
		}
		for (ConfigScreenCategory category : categories) {
			ConfigScreenCategoryNavigationGroup grouping = getNavigationGroup(category);
			if (grouping != null) {
				List<ConfigScreenCategory> members = Objects.requireNonNull(groupedCategories.get(grouping.name()));
				if (members.size() > 1) {
					if (members.get(0) == category) {
						MutableNode root = createGroup(grouping, members);
						appendNodes(result, root, category.getGroup(), -1, 0, inlineSubsectionLimit);
					}
					continue;
				}
			}
			appendNodes(result, createRoot(category), category.getGroup(), -1, 0, inlineSubsectionLimit);
		}
		return List.copyOf(result);
	}

	@Nullable
	private static ConfigScreenCategoryNavigationGroup getNavigationGroup(ConfigScreenCategory category) {
		if (category.getGroup() != ConfigScreenCategoryGroup.LOADER_NATIVE || category.getConfigValues().isEmpty()) {
			return null;
		}
		ConfigScreenCategoryNavigationGroup group = category.getNavigationGroup();
		if (group != null && group.title().getString().equals(category.getLocalizedName().getString())) {
			return group;
		}
		return null;
	}

	private static MutableNode createGroup(ConfigScreenCategoryNavigationGroup group, List<ConfigScreenCategory> members) {
		MutableNode root = new MutableNode(group.name(), group.title(), group.description());
		for (ConfigScreenCategory member : members) {
			MutableNode fileRoot = createRoot(member);
			if (!fileRoot.values.isEmpty()) {
				MutableNode settings = new MutableNode(fileRoot.name + "/@values",
					Component.translatableWithFallback("mezz_config.config.screen.sectionSettings", "Settings"), fileRoot.description);
				settings.values.addAll(fileRoot.values);
				root.children.put(settings.name, settings);
			}
			for (MutableNode child : fileRoot.children.values()) {
				Component source = Component.translatableWithFallback("mezz_config.config.screen.configFile", "File: %s", member.getName());
				if (child.description.getString().isBlank()) {
					child.description = source;
				} else {
					child.description = child.description.copy().append("\n\n").append(source);
				}
				root.children.put(child.name, child);
			}
		}
		Map<String, List<MutableNode>> duplicateTitles = new LinkedHashMap<>();
		for (MutableNode child : root.children.values()) {
			duplicateTitles.computeIfAbsent(child.title.getString(), ignored -> new ArrayList<>()).add(child);
		}
		Set<String> usedTitles = new HashSet<>(duplicateTitles.keySet());
		for (List<MutableNode> duplicates : duplicateTitles.values()) {
			if (duplicates.size() > 1) {
				int suffix = 1;
				for (int i = 0; i < duplicates.size(); i++) {
					MutableNode child = duplicates.get(i);
					Component title;
					do {
						title = Component.translatableWithFallback("mezz_config.config.native.numbered.title", "%s (%s)", child.title, suffix++);
					} while (!usedTitles.add(title.getString()));
					child.title = title;
				}
			}
		}
		return root;
	}

	private static MutableNode createRoot(ConfigScreenCategory category) {
		MutableNode root = new MutableNode(category.getName(), category.getLocalizedName(), category.getLocalizedDescription());
		for (IConfigScreenValue<?> value : category.getConfigValues()) {
			MutableNode node = root;
			Optional<ConfigValueCategoryPath> metadata = ConfigValueCategoryPath.getMetadata(value);
			if (metadata.isPresent() && metadata.get().getRootCategoryName().equals(category.getName())) {
				ConfigValueCategoryPath categoryPath = metadata.get();
				for (ConfigValueCategoryPath.Category child : categoryPath.getCategories()) {
					String childName = node.name + "/" + child.name().length() + ":" + child.name();
					node = node.children.computeIfAbsent(child.name(), ignored -> new MutableNode(childName, child.title(), child.description()));
				}
			}
			node.values.add(value);
		}
		return root;
	}

	private static void appendNodes(List<Node> result, MutableNode node, ConfigScreenCategoryGroup group, int parentIndex, int depth, int inlineSubsectionLimit) {
		while (node.values.isEmpty() && node.children.size() == 1) {
			MutableNode child = node.children.values().iterator().next();
			if (!node.title.getString().equalsIgnoreCase(child.title.getString())) {
				break;
			}
			node.values.addAll(child.values);
			if (!child.description.getString().isBlank() && !node.description.equals(child.description)) {
				if (node.description.getString().isBlank()) {
					node.description = child.description;
				} else {
					node.description = node.description.copy().append("\n\n").append(child.description);
				}
			}
			node.children.clear();
			node.children.putAll(child.children);
		}
		int index = result.size();
		List<IConfigScreenValue<?>> values = new ArrayList<>(node.values);
		List<ConfigCategoryWidget.Section> inlineSections = new ArrayList<>();
		List<MutableNode> navigationChildren = new ArrayList<>();
		for (MutableNode child : node.children.values()) {
			if (inlineSubsectionLimit > 0 && child.children.isEmpty() && child.values.size() <= inlineSubsectionLimit) {
				inlineSections.add(new ConfigCategoryWidget.Section(values.size(), child.title, child.description));
				values.addAll(child.values);
			} else {
				navigationChildren.add(child);
			}
		}
		if (!navigationChildren.isEmpty()) {
			ConfigScreenCategory category = new SectionCategory(node.name, node.title, node.description, group, List.of());
			result.add(new Node(category, parentIndex, depth, true, List.of()));
			if (!values.isEmpty()) {
				ConfigScreenCategory settings = new SectionCategory(node.name + "/@values",
					Component.translatableWithFallback("mezz_config.config.screen.sectionSettings", "Settings"), node.description, group, List.copyOf(values));
				result.add(new Node(settings, index, depth + 1, false, List.copyOf(inlineSections)));
			}
		} else {
			ConfigScreenCategory category = new SectionCategory(node.name, node.title, node.description, group, List.copyOf(values));
			result.add(new Node(category, parentIndex, depth, false, List.copyOf(inlineSections)));
		}
		for (MutableNode child : navigationChildren) {
			appendNodes(result, child, group, index, depth + 1, inlineSubsectionLimit);
		}
	}

	record Node(ConfigScreenCategory category, int parentIndex, int depth, boolean hasChildren, List<ConfigCategoryWidget.Section> inlineSections) {
	}

	private static final class MutableNode {
		private final String name;
		private Component title;
		private Component description;
		private final List<IConfigScreenValue<?>> values = new ArrayList<>();
		private final Map<String, MutableNode> children = new LinkedHashMap<>();

		private MutableNode(String name, Component title, Component description) {
			this.name = name;
			this.title = title;
			this.description = description;
		}
	}

	private record SectionCategory(
		String name,
		Component title,
		Component description,
		ConfigScreenCategoryGroup group,
		List<IConfigScreenValue<?>> values
	) implements ConfigScreenCategory {
		@Override
		public ConfigScreenCategoryGroup getGroup() {
			return group;
		}

		@Override
		@Nullable
		public ConfigScreenCategoryNavigationGroup getNavigationGroup() {
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
			return title;
		}

		@Override
		public Component getLocalizedDescription() {
			return description;
		}

		@Override
		public Collection<? extends IConfigScreenValue<?>> getConfigValues() {
			return values;
		}
	}
}
