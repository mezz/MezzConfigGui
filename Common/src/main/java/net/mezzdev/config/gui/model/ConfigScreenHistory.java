package net.mezzdev.config.gui.model;

import net.mezzdev.config.gui.ConfigScreenCategory;
import net.mezzdev.config.gui.config.ConfigGuiOptions;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers lightweight per-mod config screen state for the current game session.
 */
public final class ConfigScreenHistory {
	private static final Map<String, String> LAST_CATEGORY_BY_MOD_ID = new ConcurrentHashMap<>();

	private ConfigScreenHistory() {

	}

	public static int getInitialCategoryIndex(String modId, List<? extends ConfigScreenCategory> categories) {
		if (!ConfigGuiOptions.rememberLastCategory()) {
			return 0;
		}
		return getLastCategoryName(modId)
			.flatMap(categoryName -> findCategoryIndex(categories, categoryName))
			.orElse(0);
	}

	public static void rememberCategory(String modId, ConfigScreenCategory category) {
		if (!ConfigGuiOptions.rememberLastCategory()) {
			return;
		}
		LAST_CATEGORY_BY_MOD_ID.put(modId, category.getName());
	}

	private static Optional<String> getLastCategoryName(String modId) {
		return Optional.ofNullable(LAST_CATEGORY_BY_MOD_ID.get(modId));
	}

	private static Optional<Integer> findCategoryIndex(List<? extends ConfigScreenCategory> categories, String categoryName) {
		for (int i = 0; i < categories.size(); i++) {
			if (categories.get(i).getName().equals(categoryName)) {
				return Optional.of(i);
			}
		}
		return Optional.empty();
	}
}
