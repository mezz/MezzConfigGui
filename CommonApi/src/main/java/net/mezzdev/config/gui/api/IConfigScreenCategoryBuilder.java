package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.sorting.ISortingConfig;
import net.mezzdev.config.api.value.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;

/**
 * Customizes one category in a config screen.
 *
 * @since 0.1.0
 */
@ApiStatus.NonExtendable
public interface IConfigScreenCategoryBuilder {
	/**
	 * Override the title shown for this category.
	 * <p>
	 * If this is not called, the config GUI uses the existing schema category title or the default category
	 * localization key.
	 *
	 * @param title category title
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder setTitle(Component title);

	/**
	 * Override the description shown for this category.
	 * <p>
	 * If this is not called, the config GUI uses the existing schema category description or the default category
	 * description localization key.
	 *
	 * @param description category description
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder setDescription(Component description);

	/**
	 * Remove this category's automatically detected values from the screen.
	 * <p>
	 * Values added with this builder are still shown. Use this when a category should be manually populated instead of
	 * appending to its automatically detected values.
	 *
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder clearDefaultValues();

	/**
	 * Set the default apply mode for config screen values in this category.
	 * <p>
	 * Individual values can override this with {@link #getValueBuilder(IConfigValue)},
	 * {@link #getScreenValueBuilder(IConfigScreenValue)}, or {@link #getValueBuilderByName(String)}.
	 *
	 * @param applyMode when edits for this category's values are saved
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder setDefaultApplyMode(ConfigValueApplyMode applyMode);

	/**
	 * Get a builder for configuring one config value in this category.
	 *
	 * @param value config value to configure
	 * @return value builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenValueBuilder getValueBuilder(IConfigValue<?> value);

	/**
	 * Get a builder for configuring one config screen value in this category.
	 *
	 * @param value config screen value to configure
	 * @return value builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenValueBuilder getScreenValueBuilder(IConfigScreenValue<?> value);

	/**
	 * Get a builder for configuring one config screen value in this category by stable name.
	 * <p>
	 * The value name is resolved against this category first, so category-local value names can be used safely when
	 * multiple categories contain the same value name.
	 *
	 * @param valueName stable config screen value name
	 * @return value builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenValueBuilder getValueBuilderByName(String valueName);

	/**
	 * Set the apply mode for one config value in this category.
	 * <p>
	 * Equivalent to {@code getValueBuilder(value).setApplyMode(applyMode)}.
	 *
	 * @param value config value to configure
	 * @param applyMode when edits for this value are saved
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder setValueApplyMode(IConfigValue<?> value, ConfigValueApplyMode applyMode);

	/**
	 * Set the apply mode for one config screen value in this category.
	 * <p>
	 * Equivalent to {@code getScreenValueBuilder(value).setApplyMode(applyMode)}.
	 *
	 * @param value config screen value to configure
	 * @param applyMode when edits for this value are saved
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder setScreenValueApplyMode(IConfigScreenValue<?> value, ConfigValueApplyMode applyMode);

	/**
	 * Set the apply mode for one config screen value in this category by stable name.
	 * <p>
	 * Equivalent to {@code getValueBuilderByName(valueName).setApplyMode(applyMode)}.
	 * <p>
	 * This is useful for screen values that do not have a MezzConfig {@link IConfigValue} backing object, such as
	 * platform-native config values adapted for the config GUI.
	 *
	 * @param valueName stable config screen value name
	 * @param applyMode when edits for this value are saved
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder setValueApplyModeByName(String valueName, ConfigValueApplyMode applyMode);

	/**
	 * Set the restart or reload required after one config value in this category is saved.
	 *
	 * @param value config value to configure
	 * @param restartRequirement the required restart or reload
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder setValueRestartRequirement(
		IConfigValue<?> value,
		ConfigValueRestartRequirement restartRequirement
	);

	/**
	 * Set the restart or reload required after one config screen value in this category is saved.
	 *
	 * @param value config screen value to configure
	 * @param restartRequirement the required restart or reload
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder setScreenValueRestartRequirement(
		IConfigScreenValue<?> value,
		ConfigValueRestartRequirement restartRequirement
	);

	/**
	 * Set the restart or reload required after one config screen value in this category is saved, selected by stable
	 * name.
	 *
	 * @param valueName stable config screen value name
	 * @param restartRequirement the required restart or reload
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder setValueRestartRequirementByName(
		String valueName,
		ConfigValueRestartRequirement restartRequirement
	);

	/**
	 * Add one config value to this category.
	 * <p>
	 * Added values are appended to this category's automatically detected values. Call {@link #clearDefaultValues()} to
	 * replace the automatically detected values.
	 *
	 * @param value config value to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addValue(IConfigValue<?> value);

	/**
	 * Add one config screen value to this category.
	 * <p>
	 * Added values are appended to this category's automatically detected values. Call {@link #clearDefaultValues()} to
	 * replace the automatically detected values.
	 *
	 * @param value config screen value to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addScreenValue(IConfigScreenValue<?> value);

	/**
	 * Add config values to this category.
	 * <p>
	 * Added values are appended to this category's automatically detected values. Call {@link #clearDefaultValues()} to
	 * replace the automatically detected values.
	 *
	 * @param values config values to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addValues(Collection<? extends IConfigValue<?>> values);

	/**
	 * Add config screen values to this category.
	 * <p>
	 * Added values are appended to this category's automatically detected values. Call {@link #clearDefaultValues()} to
	 * replace the automatically detected values.
	 *
	 * @param values config screen values to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addScreenValues(Collection<? extends IConfigScreenValue<?>> values);

	/**
	 * Add a GUI list editor for a MezzConfig sorting config.
	 * <p>
	 * The sorting config keeps owning storage and sorting behavior. The config GUI adapts it into a list editor for the
	 * given runtime value set.
	 *
	 * @param name stable config screen value name
	 * @param localizationKey translation key for this sort-order value's name
	 * @param sortingConfig sorting config that stores and applies the edited order
	 * @param values complete runtime value set to expose in the list editor
	 * @param valueSerializer serializer and display metadata for one sortable value
	 * @param <T> the sortable value type
	 * @return builder for configuring how this sorting config is shown
	 *
	 * @since 0.1.0
	 */
	<T> ISortingConfigGuiBuilder<T> addSortingConfig(
		String name,
		String localizationKey,
		ISortingConfig<T> sortingConfig,
		Collection<T> values,
		IConfigValueSerializer<T> valueSerializer
	);

	/**
	 * Add a GUI list editor for a string-backed MezzConfig sorting config.
	 *
	 * @param name stable config screen value name
	 * @param localizationKey translation key for this sort-order value's name
	 * @param sortingConfig sorting config that stores and applies the edited order
	 * @param values complete runtime value set to expose in the list editor
	 * @return builder for configuring how this sorting config is shown
	 *
	 * @since 0.1.0
	 */
	ISortingConfigGuiBuilder<String> addStringSortingConfig(
		String name,
		String localizationKey,
		ISortingConfig<String> sortingConfig,
		Collection<String> values
	);

	/**
	 * Hide one schema config value from this category.
	 * <p>
	 * This is only needed when this category keeps automatically detected values and some of those values should still
	 * be omitted.
	 *
	 * @param value config value to hide
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder hideValue(IConfigValue<?> value);

	/**
	 * Hide one config screen value from this category.
	 * <p>
	 * This is only needed when this category keeps automatically detected values and some of those values should still
	 * be omitted.
	 *
	 * @param value config screen value to hide
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder hideScreenValue(IConfigScreenValue<?> value);

	/**
	 * Hide schema config values from this category.
	 * <p>
	 * This is only needed when this category keeps automatically detected values and some of those values should still
	 * be omitted.
	 *
	 * @param values config values to hide
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder hideValues(Collection<? extends IConfigValue<?>> values);

	/**
	 * Hide config screen values from this category.
	 * <p>
	 * This is only needed when this category keeps automatically detected values and some of those values should still
	 * be omitted.
	 *
	 * @param values config screen values to hide
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder hideScreenValues(Collection<? extends IConfigScreenValue<?>> values);

	/**
	 * Add one config screen value to this category by stable name.
	 * <p>
	 * Added values are appended to this category's automatically detected values. Call {@link #clearDefaultValues()} to
	 * replace the automatically detected values. The value name is resolved against this category first, so category-local
	 * value names can be used safely when multiple categories contain the same value name.
	 * This is useful for screen values that do not have a MezzConfig {@link IConfigValue} backing object, such as
	 * platform-native config values adapted for the config GUI.
	 *
	 * @param valueName stable config screen value name
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addValueByName(String valueName);

	/**
	 * Hide one config screen value from this category by stable name.
	 * <p>
	 * This is only needed when this category keeps automatically detected values and some of those values should still
	 * be omitted. The value name is resolved against this category first, so category-local value names can be used
	 * safely when multiple categories contain the same value name.
	 * This is useful for screen values that do not have a MezzConfig {@link IConfigValue} backing object, such as
	 * platform-native config values adapted for the config GUI.
	 *
	 * @param valueName stable config screen value name to hide
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder hideValueByName(String valueName);

	/**
	 * Add config screen values to this category by stable name.
	 * <p>
	 * Added values are appended to this category's automatically detected values. Call {@link #clearDefaultValues()} to
	 * replace the automatically detected values. Value names are resolved against this category first, so category-local
	 * value names can be used safely when multiple categories contain the same value name.
	 * This is useful for screen values that do not have a MezzConfig {@link IConfigValue} backing object, such as
	 * platform-native config values adapted for the config GUI.
	 *
	 * @param valueNames stable config screen value names
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addValuesByName(Collection<String> valueNames);

	/**
	 * Hide config screen values from this category by stable name.
	 * <p>
	 * This is only needed when this category keeps automatically detected values and some of those values should still
	 * be omitted. Value names are resolved against this category first, so category-local value names can be used safely
	 * when multiple categories contain the same value name.
	 * This is useful for screen values that do not have a MezzConfig {@link IConfigValue} backing object, such as
	 * platform-native config values adapted for the config GUI.
	 *
	 * @param valueNames stable config screen value names to hide
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder hideValuesByName(Collection<String> valueNames);

	/**
	 * Add one key mapping to this category.
	 * <p>
	 * Adding key mappings to any configured category disables the automatically detected key mappings category for this
	 * screen. Added key mappings are appended to this category's automatically detected values.
	 *
	 * @param keyMapping key mapping to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addKeyMapping(KeyMapping keyMapping);

	/**
	 * Add key mappings to this category.
	 * <p>
	 * Adding key mappings to any configured category disables the automatically detected key mappings category for this
	 * screen. Added key mappings are appended to this category's automatically detected values.
	 *
	 * @param keyMappings key mappings to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addKeyMappings(Collection<? extends KeyMapping> keyMappings);
}
