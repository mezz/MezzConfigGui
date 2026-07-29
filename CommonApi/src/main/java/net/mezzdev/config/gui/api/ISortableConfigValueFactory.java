package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.sorting.ISortingConfig;
import net.mezzdev.config.api.value.ConfigValueUpdateType;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Creates config values for sortable value lists that are discovered at runtime.
 *
 * @since 0.1.0
 */
public interface ISortableConfigValueFactory {
	/**
	 * Create a config value for editing a runtime value list backed by a sorting config.
	 * <p>
	 * The given values are copied when this method is called. The sorting config keeps owning storage and sorting
	 * behavior; this config value only adapts it for the config GUI list editor.
	 *
	 * @param name stable config value name
	 * @param localizationKey translation key for this config value's name
	 * @param sortingConfig sorting config that stores and applies the edited order
	 * @param values complete runtime value set to expose in the list editor
	 * @param valueSerializer serializer and display metadata for one list value
	 * @return a config value for the sortable runtime list
	 *
	 * @since 0.1.0
	 */
	<T> IConfigValue<List<T>> create(
		String name,
		String localizationKey,
		ISortingConfig<T> sortingConfig,
		Collection<T> values,
		IConfigValueSerializer<T> valueSerializer
	);

	/**
	 * Create a config value for editing a runtime value list backed by a sorting config.
	 * <p>
	 * The given values are copied when this method is called. The sorting config keeps owning storage and sorting
	 * behavior; this config value only adapts it for the config GUI list editor.
	 *
	 * @param name stable config value name
	 * @param localizationKey translation key for this config value's name
	 * @param sortingConfig sorting config that stores and applies the edited order
	 * @param values complete runtime value set to expose in the list editor
	 * @param valueSerializer serializer and display metadata for one list value
	 * @param updateType update type for saved order changes
	 * @return a config value for the sortable runtime list
	 *
	 * @since 0.1.0
	 */
	<T> IConfigValue<List<T>> create(
		String name,
		String localizationKey,
		ISortingConfig<T> sortingConfig,
		Collection<T> values,
		IConfigValueSerializer<T> valueSerializer,
		ConfigValueUpdateType updateType
	);

	/**
	 * Create a string-list config value for editing a runtime value list backed by a sorting config.
	 *
	 * @param name stable config value name
	 * @param localizationKey translation key for this config value's name
	 * @param sortingConfig sorting config that stores and applies the edited order
	 * @param values complete runtime value set to expose in the list editor
	 * @param valueNames display names for list values
	 * @param valueDescriptions optional descriptions for list values
	 * @param valueIcons optional icons for list values
	 * @return a config value for the sortable runtime list
	 *
	 * @since 0.1.0
	 */
	IConfigValue<List<String>> createStringList(
		String name,
		String localizationKey,
		ISortingConfig<String> sortingConfig,
		Collection<String> values,
		Map<String, Component> valueNames,
		Map<String, Component> valueDescriptions,
		Map<String, IConfigValueIcon> valueIcons
	);

	/**
	 * Create a string-list config value for editing a runtime value list backed by a sorting config.
	 *
	 * @param name stable config value name
	 * @param localizationKey translation key for this config value's name
	 * @param sortingConfig sorting config that stores and applies the edited order
	 * @param values complete runtime value set to expose in the list editor
	 * @param valueNames display names for list values
	 * @param valueDescriptions optional descriptions for list values
	 * @param valueIcons optional icons for list values
	 * @param updateType update type for saved order changes
	 * @return a config value for the sortable runtime list
	 *
	 * @since 0.1.0
	 */
	IConfigValue<List<String>> createStringList(
		String name,
		String localizationKey,
		ISortingConfig<String> sortingConfig,
		Collection<String> values,
		Map<String, Component> valueNames,
		Map<String, Component> valueDescriptions,
		Map<String, IConfigValueIcon> valueIcons,
		ConfigValueUpdateType updateType
	);
}
