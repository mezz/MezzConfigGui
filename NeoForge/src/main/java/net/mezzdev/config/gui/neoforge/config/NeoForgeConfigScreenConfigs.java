package net.mezzdev.config.gui.neoforge.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.mezzdev.config.gui.ConfigScreenConfig;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.ModConfigSpec;
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

public final class NeoForgeConfigScreenConfigs {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final List<ModConfig.Type> CONFIG_TYPE_ORDER = List.of(
		ModConfig.Type.CLIENT,
		ModConfig.Type.COMMON,
		ModConfig.Type.STARTUP,
		ModConfig.Type.SERVER
	);

	private NeoForgeConfigScreenConfigs() {

	}

	public static Collection<? extends ConfigScreenConfig> getConfigScreens() {
		Map<String, ModContainer> modContainers = new LinkedHashMap<>();
		for (ModConfig.Type configType : CONFIG_TYPE_ORDER) {
			for (ModConfig modConfig : ModConfigs.getConfigSet(configType)) {
				String modId = modConfig.getModId();
				if (modContainers.containsKey(modId) || !hasSupportedValues(modConfig)) {
					continue;
				}
				ModList.get()
					.getModContainerById(modId)
					.ifPresentOrElse(
						modContainer -> modContainers.put(modId, modContainer),
						() -> LOGGER.error("No mod container found for NeoForge config mod id: {}", modId)
					);
			}
		}
		modContainers.values().forEach(NeoForgeConfigValueReloads::register);
		return modContainers.values()
			.stream()
			.map(NeoForgeConfigScreenConfig::new)
			.toList();
	}

	static List<NeoForgeConfigCategory> getCategories(ModContainer modContainer) {
		String modId = modContainer.getModId();
		List<NeoForgeConfigCategory> categories = new ArrayList<>();
		for (ModConfig.Type configType : CONFIG_TYPE_ORDER) {
			List<ModConfig> configs = ModConfigs.getModConfigs(modId).stream()
				.filter(config -> config.getType() == configType && config.getLoadedConfig() != null)
				.sorted(Comparator.comparing(ModConfig::getFileName))
				.toList();
			List<NeoForgeConfigCategory> typeCategories = configs.stream()
				.map(config -> createCategory(modId, config))
				.flatMap(Optional::stream)
				.toList();
			if (configType == ModConfig.Type.COMMON) {
				// Common files are grouped after plugin customization, keeping filenames as API identities.
				categories.addAll(typeCategories);
				continue;
			}
			List<NeoForgeConfigLocalization.CategoryName> categoryNames = typeCategories.stream()
				.map(category -> new NeoForgeConfigLocalization.CategoryName(
					category.localizedName(),
					NeoForgeConfigLocalization.getSectionNames(category.configValues().stream()
						.map(NeoForgeConfigValue::getSections)
						.toList())
				))
				.toList();
			List<Component> names = NeoForgeConfigLocalization.getDistinctCategoryNames(categoryNames);
			for (int index = 0; index < typeCategories.size(); index++) {
				NeoForgeConfigCategory category = typeCategories.get(index);
				categories.add(new NeoForgeConfigCategory(
					category.name(), category.localizationKey(), names.get(index), category.localizedDescription(),
					category.modConfig(), category.modConfigSpec(), category.configValues()
				));
			}
		}
		return categories;
	}

	private static Optional<NeoForgeConfigCategory> createCategory(String modId, ModConfig modConfig) {
		if (modConfig.getLoadedConfig() == null) {
			return Optional.empty();
		}
		if (!(modConfig.getSpec() instanceof ModConfigSpec modConfigSpec)) {
			return Optional.empty();
		}

		List<NeoForgeConfigValue<?>> values = createValues(modId, modConfig, modConfigSpec);
		if (values.isEmpty()) {
			return Optional.empty();
		}

		String localizationKey = NeoForgeConfigLocalization.getCategoryLocalizationKey(modId, modConfig);
		return Optional.of(new NeoForgeConfigCategory(
			modConfig.getFileName(),
			localizationKey,
			NeoForgeConfigLocalization.getCategoryName(localizationKey, modConfig.getType()),
			NeoForgeConfigLocalization.getCategoryDescription(localizationKey, modConfig),
			modConfig,
			modConfigSpec,
			values
		));
	}

	private static List<NeoForgeConfigValue<?>> createValues(String modId, ModConfig modConfig, ModConfigSpec modConfigSpec) {
		List<NeoForgeConfigValue<?>> values = new ArrayList<>();
		visitValues(modConfigSpec, (path, configValue, valueSpec) -> {
			Optional<NeoForgeConfigValue<?>> value = NeoForgeConfigValueFactory.create(
				modId,
				modConfig,
				modConfigSpec,
				configValue,
				valueSpec
			);
			if (value.isPresent()) {
				values.add(value.get());
			} else {
				LOGGER.debug(
					"Skipping unsupported NeoForge config value: {} {} ({})",
					modConfig.getModId(),
					String.join(".", path),
					NeoForgeConfigValueFactory.getUnsupportedDescription(configValue)
				);
			}
			return true;
		});
		return values;
	}

	private static boolean hasSupportedValues(ModConfig modConfig) {
		if (!(modConfig.getSpec() instanceof ModConfigSpec modConfigSpec)) {
			return false;
		}
		return hasSupportedValues(modConfigSpec);
	}

	static boolean hasSupportedValues(ModConfigSpec modConfigSpec) {
		return !visitValues(modConfigSpec, (path, configValue, valueSpec) -> !NeoForgeConfigValueFactory.supports(configValue, valueSpec));
	}

	static boolean visitValues(ModConfigSpec modConfigSpec, ValueVisitor visitor) {
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
			if (value instanceof ModConfigSpec.ConfigValue<?> configValue && specValue instanceof ModConfigSpec.ValueSpec valueSpec) {
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
		boolean visit(List<String> path, ModConfigSpec.ConfigValue<?> configValue, ModConfigSpec.ValueSpec valueSpec);
	}
}
