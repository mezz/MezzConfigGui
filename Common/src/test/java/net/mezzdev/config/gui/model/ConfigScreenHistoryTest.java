package net.mezzdev.config.gui.model;

import net.mezzdev.config.gui.ConfigScreenCategory;
import net.mezzdev.config.gui.ConfigScreenCategoryGroup;
import net.mezzdev.config.gui.ConfigScreenCategoryNavigationGroup;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.config.ConfigGuiOptionsTestUtil;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigScreenHistoryTest {
	private static final String MOD_ID = "test_mod";
	private static final List<TestCategory> CATEGORIES = List.of(
		new TestCategory("general"),
		new TestCategory("advanced")
	);

	@BeforeEach
	void clearHistory() {
		try {
			Field field = ConfigScreenHistory.class.getDeclaredField("LAST_CATEGORY_BY_MOD_ID");
			field.setAccessible(true);
			Map<?, ?> history = (Map<?, ?>) field.get(null);
			history.clear();
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Failed to clear config screen history.", e);
		}
	}

	@Test
	void remembersLastCategoryByModIdWhenEnabled() {
		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("rememberLastCategory", true)) {
			ConfigScreenHistory.rememberCategory(MOD_ID, CATEGORIES.get(1));

			assertEquals(1, ConfigScreenHistory.getInitialCategoryIndex(MOD_ID, CATEGORIES));
			assertEquals(0, ConfigScreenHistory.getInitialCategoryIndex("other_mod", CATEGORIES));
		}
	}

	@Test
	void ignoresRememberedCategoryWhenOptionIsDisabled() {
		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("rememberLastCategory", true)) {
			ConfigScreenHistory.rememberCategory(MOD_ID, CATEGORIES.get(1));
		}

		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("rememberLastCategory", false)) {
			ConfigScreenHistory.rememberCategory(MOD_ID, CATEGORIES.get(0));

			assertEquals(0, ConfigScreenHistory.getInitialCategoryIndex(MOD_ID, CATEGORIES));
		}

		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("rememberLastCategory", true)) {
			assertEquals(1, ConfigScreenHistory.getInitialCategoryIndex(MOD_ID, CATEGORIES));
		}
	}

	private record TestCategory(String name) implements ConfigScreenCategory {
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
		public Collection<? extends IConfigScreenValue<?>> getConfigValues() {
			return List.of();
		}
	}
}
