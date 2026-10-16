package net.mezzdev.config.gui.forge.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.mezzdev.config.gui.ConfigScreenCategoryGroup;
import net.mezzdev.config.gui.ConfigScreenCategoryNavigationGroup;
import net.mezzdev.config.gui.ConfigScreenConfig;
import net.mezzdev.config.gui.ConfigValueCategoryPath;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Discovers native Forge configs and builds their screen schemas from currently loaded files.
 */
public final class ForgeConfigScreenConfigs {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final List<ModConfig.Type> CONFIG_TYPE_ORDER = List.of(
		ModConfig.Type.CLIENT,
		ModConfig.Type.COMMON,
		ModConfig.Type.SERVER
	);

	private ForgeConfigScreenConfigs() {

	}

	public static Collection<? extends ConfigScreenConfig> getConfigScreens() {
		Map<String, ModContainer> modContainers = new LinkedHashMap<>();
		for (ModConfig.Type configType : CONFIG_TYPE_ORDER) {
			for (ModConfig modConfig : ConfigTracker.INSTANCE.configSets().get(configType)) {
				String modId = modConfig.getModId();
				if (modContainers.containsKey(modId) || !hasSupportedValues(modConfig)) {
					continue;
				}
				ModList.get()
					.getModContainerById(modId)
					.ifPresentOrElse(
						modContainer -> modContainers.put(modId, modContainer),
						() -> LOGGER.error("No mod container found for Forge config mod id: {}", modId)
					);
			}
		}
		modContainers.values().forEach(ForgeConfigValueReloads::register);
		return modContainers.values()
			.stream()
			.map(ForgeConfigScreenConfig::new)
			.toList();
	}

	static List<ForgeConfigCategory> getCategories(ModContainer modContainer) {
		String modId = modContainer.getModId();
		List<ForgeConfigCategory> categories = new ArrayList<>();
		for (ModConfig.Type configType : CONFIG_TYPE_ORDER) {
			List<ModConfig> configs = ConfigTracker.INSTANCE.configSets().get(configType).stream()
				.filter(config -> config.getModId().equals(modId) && config.getConfigData() != null)
				.sorted(Comparator.comparing(ModConfig::getFileName))
				.toList();
			List<ForgeConfigCategory> typeCategories = configs.stream()
				.map(config -> createCategory(modId, config))
				.flatMap(Optional::stream)
				.toList();
			if (configType == ModConfig.Type.COMMON) {
				// Common files are grouped after plugin customization, keeping filenames as API identities.
				categories.addAll(typeCategories);
				continue;
			}
			List<ForgeConfigLocalization.CategoryName> categoryNames = typeCategories.stream()
				.map(category -> new ForgeConfigLocalization.CategoryName(
					category.localizedName(),
					ForgeConfigLocalization.getNestedCategoryNames(category.configValues().stream()
						.map(ConfigValueCategoryPath::getCategories)
						.toList())
				))
				.toList();
			List<Component> names = ForgeConfigLocalization.getDistinctCategoryNames(categoryNames);
			for (int index = 0; index < typeCategories.size(); index++) {
				ForgeConfigCategory category = typeCategories.get(index);
				categories.add(new ForgeConfigCategory(
					category.name(), category.localizationKey(), names.get(index), category.localizedDescription(),
					category.group(), category.navigationGroup(), category.configValues()
				));
			}
		}
		return categories;
	}

	private static Optional<ForgeConfigCategory> createCategory(String modId, ModConfig modConfig) {
		if (modConfig.getConfigData() == null) {
			return Optional.empty();
		}
		if (!(modConfig.getSpec() instanceof ForgeConfigSpec modConfigSpec)) {
			return Optional.empty();
		}

		String localizationKey = ForgeConfigLocalization.getCategoryLocalizationKey(modId, modConfig);
		ConfigScreenCategoryNavigationGroup navigationGroup = ForgeConfigLocalization.getNavigationGroup(
			modId,
			modConfig.getType(),
			localizationKey
		);
		List<IConfigScreenValue<?>> values = createValues(modId, modConfig, modConfigSpec);
		if (values.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(new ForgeConfigCategory(
			modConfig.getFileName(),
			localizationKey,
			ForgeConfigLocalization.getCategoryName(localizationKey, modConfig.getType()),
			ForgeConfigLocalization.getCategoryDescription(localizationKey, modConfig),
			getScreenCategoryGroup(modConfig.getType()),
			navigationGroup,
			values
		));
	}

	private static ConfigScreenCategoryGroup getScreenCategoryGroup(ModConfig.Type type) {
		if (type == ModConfig.Type.SERVER) {
			return ConfigScreenCategoryGroup.LOADER_NATIVE_SERVER;
		}
		return ConfigScreenCategoryGroup.LOADER_NATIVE;
	}

	private static List<IConfigScreenValue<?>> createValues(
		String modId,
		ModConfig modConfig,
		ForgeConfigSpec modConfigSpec
	) {
		List<IConfigScreenValue<?>> values = new ArrayList<>();
		visitValues(modConfigSpec, (path, configValue, valueSpec) -> {
			Optional<ForgeConfigValue<?>> value = ForgeConfigValueFactory.create(
				modId,
				modConfig,
				modConfigSpec,
				configValue,
				valueSpec
			);
			if (value.isPresent()) {
				values.add(ConfigValueCategoryPath.withCategoryPath(
					value.get(),
					modConfig.getFileName(),
					ForgeConfigLocalization.getCategoryPath(modId, modConfigSpec, path)
				));
			} else {
				LOGGER.debug(
					"Skipping unsupported Forge config value: {} {} ({})",
					modConfig.getModId(),
					String.join(".", path),
					ForgeConfigValueFactory.getUnsupportedDescription(configValue)
				);
			}
			return true;
		});
		return values;
	}

	private static boolean hasSupportedValues(ModConfig modConfig) {
		if (!(modConfig.getSpec() instanceof ForgeConfigSpec modConfigSpec)) {
			return false;
		}
		return hasSupportedValues(modConfigSpec);
	}

	static boolean hasSupportedValues(ForgeConfigSpec modConfigSpec) {
		return !visitValues(modConfigSpec, (path, configValue, valueSpec) -> !ForgeConfigValueFactory.supports(configValue, valueSpec));
	}

	static boolean visitValues(ForgeConfigSpec modConfigSpec, ValueVisitor visitor) {
		return visitValues(
			new ArrayDeque<>(),
			modConfigSpec.getValues(),
			modConfigSpec.getSpec(),
			visitor
		);
	}

	private static boolean visitValues(
		ArrayDeque<String> path,
		UnmodifiableConfig valueConfig,
		UnmodifiableConfig specConfig,
		ValueVisitor visitor
	) {
		for (UnmodifiableConfig.Entry entry : valueConfig.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getRawValue();
			Object specValue = specConfig.getRaw(List.of(key));
			path.addLast(key);
			if (value instanceof ForgeConfigSpec.ConfigValue<?> configValue && specValue instanceof ForgeConfigSpec.ValueSpec valueSpec) {
				if (!visitor.visit(List.copyOf(path), configValue, valueSpec)) {
					return false;
				}
			} else if (value instanceof UnmodifiableConfig childValues && specValue instanceof UnmodifiableConfig childSpecs) {
				if (!visitValues(path, childValues, childSpecs, visitor)) {
					return false;
				}
			}
			path.removeLast();
		}
		return true;
	}

	@FunctionalInterface
	interface ValueVisitor {
		/** @return false to stop traversing the config, true to continue. */
		boolean visit(List<String> path, ForgeConfigSpec.ConfigValue<?> configValue, ForgeConfigSpec.ValueSpec valueSpec);
	}
}
