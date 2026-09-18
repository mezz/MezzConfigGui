package net.mezzdev.config.gui.model;

import net.mezzdev.config.gui.ConfigScreenCategory;
import net.mezzdev.config.gui.ConfigScreenCategoryGroup;
import net.mezzdev.config.gui.ConfigValueSections;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds presentation-only subsections after plugins have resolved their categories and values.
 */
final class ConfigCategoryTree {
	private ConfigCategoryTree() {

	}

	static List<Node> create(List<ConfigScreenCategory> categories, int inlineSubsectionLimit) {
		List<Node> result = new ArrayList<>();
		for (ConfigScreenCategory category : categories) {
			MutableNode root = new MutableNode(category.getName(), category.getLocalizedName(), category.getLocalizedDescription());
			for (IConfigScreenValue<?> value : category.getConfigValues()) {
				MutableNode node = root;
				if (value.getIdentityKey() instanceof ConfigValueSections sections &&
					sections.getSectionCategoryName().equals(category.getName())
				) {
					for (ConfigValueSections.Section section : sections.getSections()) {
						String childName = node.name + "/" + section.name().length() + ":" + section.name();
						node = node.children.computeIfAbsent(section.name(), ignored -> new MutableNode(childName, section.title(), section.description()));
					}
				}
				node.values.add(value);
			}
			appendNodes(result, root, category.getGroup(), -1, 0, inlineSubsectionLimit);
		}
		return List.copyOf(result);
	}

	private static void appendNodes(List<Node> result, MutableNode node, ConfigScreenCategoryGroup group, int parentIndex, int depth, int inlineSubsectionLimit) {
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
		ConfigScreenCategory category = new SectionCategory(node.name, node.title, node.description, group, List.copyOf(values));
		result.add(new Node(category, parentIndex, depth, !navigationChildren.isEmpty(), List.copyOf(inlineSections)));
		for (MutableNode child : navigationChildren) {
			appendNodes(result, child, group, index, depth + 1, inlineSubsectionLimit);
		}
	}

	record Node(ConfigScreenCategory category, int parentIndex, int depth, boolean hasChildren, List<ConfigCategoryWidget.Section> inlineSections) {
	}

	private static final class MutableNode {
		private final String name;
		private final Component title;
		private final Component description;
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
