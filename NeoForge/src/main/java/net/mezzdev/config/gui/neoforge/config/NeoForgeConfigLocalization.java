package net.mezzdev.config.gui.neoforge.config;

import net.mezzdev.config.gui.ConfigScreenCategoryNavigationGroup;
import net.mezzdev.config.gui.ConfigValueCategoryPath;
import net.mezzdev.config.gui.util.ConfigNameUtil;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class NeoForgeConfigLocalization {
	private NeoForgeConfigLocalization() {

	}

	public static Component getScreenTitle(ModContainer modContainer) {
		String modId = modContainer.getModId();
		String displayName = modContainer.getModInfo().getDisplayName();
		return Component.translatableWithFallback(modId + ".configuration.title", "%s Configuration", displayName);
	}

	public static String getCategoryLocalizationKey(String modId, ModConfig modConfig) {
		String configFileName = modConfig.getFileName()
			.replaceAll("[^a-zA-Z0-9]+", ".")
			.replaceFirst("^\\.", "")
			.replaceFirst("\\.$", "")
			.toLowerCase(Locale.ENGLISH);
		return modId + ".configuration.section." + configFileName;
	}

	public static Component getCategoryName(String localizationKey, ModConfig.Type type) {
		String fallback = getDisplayNameFallback(type.extension());
		Component title = Component.translatableWithFallback(localizationKey + ".title", fallback);
		if (type == ModConfig.Type.COMMON) {
			title = Component.translatableWithFallback("mezz_config.config.native.local.title", "%s (local)", title);
		}
		return title;
	}

	@Nullable
	static ConfigScreenCategoryNavigationGroup getNavigationGroup(String modId, ModConfig.Type type, String localizationKey) {
		if (type != ModConfig.Type.COMMON || net.minecraft.locale.Language.getInstance().has(localizationKey + ".title")) {
			return null;
		}
		return new ConfigScreenCategoryNavigationGroup(
			"@neoforge:" + modId + ":common",
			getCategoryName(localizationKey, type),
			Component.translatableWithFallback("mezz_config.config.native.description.common.group",
				"These local configs may affect client or server behavior, depending on the mod. Changes here do not change a multiplayer server's config.")
		);
	}

	static List<Component> getDistinctCategoryNames(List<CategoryName> categories) {
		List<Component> names = new ArrayList<>();
		Map<String, List<Integer>> groups = new LinkedHashMap<>();
		for (int index = 0; index < categories.size(); index++) {
			Component title = categories.get(index).title();
			names.add(title);
			groups.computeIfAbsent(title.getString(), ignored -> new ArrayList<>()).add(index);
		}
		Set<String> usedNames = new HashSet<>(groups.keySet());
		for (List<Integer> group : groups.values()) {
			if (group.size() < 2) {
				continue;
			}
			Map<String, Integer> nestedCategoryCounts = new LinkedHashMap<>();
			for (int index : group) {
				categories.get(index).nestedCategories().stream()
					.map(Component::getString)
					.distinct()
					.forEach(name -> nestedCategoryCounts.merge(name, 1, Integer::sum));
			}
			for (int ordinal = 0; ordinal < group.size(); ordinal++) {
				int index = group.get(ordinal);
				CategoryName category = categories.get(index);
				Component numberedName = Component.translatableWithFallback(
					"mezz_config.config.native.numbered.title", "%s (%s)", category.title(), ordinal + 1
				);
				Component name = category.nestedCategories().stream()
					.filter(nestedCategory -> !nestedCategory.getString().isBlank() && nestedCategoryCounts.get(nestedCategory.getString()) == 1)
					.findFirst()
					.<Component>map(nestedCategory -> Component.translatableWithFallback(
						"mezz_config.config.native.section.title", "%s · %s", category.title(), nestedCategory
					))
					.orElse(numberedName);
				Component baseName = name;
				int suffix = ordinal + 1;
				while (!usedNames.add(name.getString())) {
					name = baseName.copy().append(" (" + suffix++ + ")");
				}
				names.set(index, name);
			}
		}
		return List.copyOf(names);
	}

	static List<Component> getNestedCategoryNames(List<List<ConfigValueCategoryPath.Category>> paths) {
		Map<String, Component> names = new LinkedHashMap<>();
		int maxDepth = paths.stream().mapToInt(List::size).max().orElse(0);
		// Prefer broad categories before falling back to a distinguishing nested category.
		for (int depth = 0; depth < maxDepth; depth++) {
			for (List<ConfigValueCategoryPath.Category> path : paths) {
				if (path.size() > depth) {
					Component title = path.get(depth).title();
					names.putIfAbsent(title.getString(), title);
				}
			}
		}
		return List.copyOf(names.values());
	}

	record CategoryName(Component title, List<Component> nestedCategories) {
		CategoryName {
			nestedCategories = List.copyOf(nestedCategories);
		}
	}

	public static Component getCategoryDescription(String localizationKey, ModConfig modConfig) {
		String tooltipKey = localizationKey + ".tooltip";
		String fallbackKey = switch (modConfig.getType()) {
			case COMMON -> "mezz_config.config.native.description.common";
			case CLIENT -> "mezz_config.config.native.description.client";
			case SERVER -> "mezz_config.config.native.description.server";
			case STARTUP -> "mezz_config.config.native.description.startup";
		};
		Component scope = Component.translatable(fallbackKey, modConfig.getFileName());
		if (net.minecraft.locale.Language.getInstance().has(tooltipKey)) {
			return Component.translatable(tooltipKey).append("\n\n").append(scope);
		}
		return scope;
	}

	public static String getValueLocalizationKey(String modId, List<String> path, @Nullable String configuredTranslationKey) {
		if (configuredTranslationKey != null && !configuredTranslationKey.isBlank()) {
			return configuredTranslationKey;
		}
		return modId + ".configuration." + String.join(".", path);
	}

	public static List<ConfigValueCategoryPath.Category> getCategoryPath(String modId, ModConfigSpec modConfigSpec, List<String> path) {
		List<ConfigValueCategoryPath.Category> result = new ArrayList<>();
		for (int level = 1; level < path.size(); level++) {
			List<String> categoryPath = path.subList(0, level);
			String categoryKey = getValueLocalizationKey(modId, categoryPath, modConfigSpec.getLevelTranslationKey(categoryPath));
			result.add(new ConfigValueCategoryPath.Category(
				categoryPath.getLast(),
				getValueName(categoryKey, categoryPath),
				getDescription(categoryKey, modConfigSpec.getLevelComment(categoryPath))
			));
		}
		return List.copyOf(result);
	}

	public static Component getValueName(String localizationKey, List<String> path) {
		String name = localizationKey;
		if (!path.isEmpty()) {
			name = path.getLast();
		}
		return Component.translatableWithFallback(localizationKey, getDisplayNameFallback(name));
	}

	public static Component getValueDescription(String localizationKey, ModConfigSpec.ValueSpec valueSpec) {
		String comment = valueSpec.getComment();
		ModConfigSpec.Range<?> range = valueSpec.getRange();
		if (comment != null && range != null && range.getMin() instanceof Number && range.getMax() instanceof Number) {
			// defineInRange appends this line. Numeric editors already display the localized range.
			String generatedRangeComment = " Range: " + range;
			if (comment.equals(generatedRangeComment)) {
				comment = "";
			} else if (comment.endsWith("\n" + generatedRangeComment)) {
				comment = comment.substring(0, comment.length() - generatedRangeComment.length() - 1);
			}
		}
		return getDescription(localizationKey, comment);
	}

	private static Component getDescription(String localizationKey, @Nullable String comment) {
		String tooltipKey = localizationKey + ".tooltip";
		if (net.minecraft.locale.Language.getInstance().has(tooltipKey) || !Strings.isBlank(comment)) {
			return Component.translatableWithFallback(tooltipKey, getCommentFallback(comment));
		}
		return Component.empty();
	}

	private static String getCommentFallback(@Nullable String comment) {
		if (comment == null) {
			return "";
		}
		return comment;
	}

	public static String getDisplayNameFallback(String name) {
		return ConfigNameUtil.getDisplayNameFallback(name);
	}
}
