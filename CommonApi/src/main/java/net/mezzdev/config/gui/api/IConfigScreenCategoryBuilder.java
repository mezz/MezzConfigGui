package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.IConfigValue;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Customizes one category in a config screen.
 *
 * @since 0.1.0
 */
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
	default IConfigScreenCategoryBuilder clearDefaultValues() {
		return this;
	}

	/**
	 * Set the default apply mode for config screen values in this category.
	 * <p>
	 * Individual values can override this with {@link #setValueApplyMode(IConfigValue, ConfigValueApplyMode)},
	 * {@link #setScreenValueApplyMode(IConfigScreenValue, ConfigValueApplyMode)}, or
	 * {@link #setValueApplyModeByName(String, ConfigValueApplyMode)}.
	 *
	 * @param applyMode when edits for this category's values are saved
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder setDefaultApplyMode(ConfigValueApplyMode applyMode);

	/**
	 * Set the apply mode for one config value in this category.
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
	 * Mark one config value in this category as requiring a restart or larger reload after it is saved.
	 *
	 * @param value config value to configure
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	default IConfigScreenCategoryBuilder setValueRequiresRestart(IConfigValue<?> value) {
		return setValueRequiresRestart(value, true);
	}

	/**
	 * Set whether one config value in this category requires a restart or larger reload after it is saved.
	 *
	 * @param value config value to configure
	 * @param requiresRestart true if saving this value requires a restart or larger reload
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder setValueRequiresRestart(IConfigValue<?> value, boolean requiresRestart);

	/**
	 * Mark one config screen value in this category as requiring a restart or larger reload after it is saved.
	 *
	 * @param value config screen value to configure
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	default IConfigScreenCategoryBuilder setScreenValueRequiresRestart(IConfigScreenValue<?> value) {
		return setScreenValueRequiresRestart(value, true);
	}

	/**
	 * Set whether one config screen value in this category requires a restart or larger reload after it is saved.
	 *
	 * @param value config screen value to configure
	 * @param requiresRestart true if saving this value requires a restart or larger reload
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder setScreenValueRequiresRestart(IConfigScreenValue<?> value, boolean requiresRestart);

	/**
	 * Mark one config screen value in this category by stable name as requiring a restart or larger reload after it is saved.
	 *
	 * @param valueName stable config screen value name
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	default IConfigScreenCategoryBuilder setValueRequiresRestartByName(String valueName) {
		return setValueRequiresRestartByName(valueName, true);
	}

	/**
	 * Set whether one config screen value in this category by stable name requires a restart or larger reload after it is saved.
	 * <p>
	 * This is useful for screen values that do not have a MezzConfig {@link IConfigValue} backing object, such as
	 * platform-native config values adapted for the config GUI.
	 *
	 * @param valueName stable config screen value name
	 * @param requiresRestart true if saving this value requires a restart or larger reload
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder setValueRequiresRestartByName(String valueName, boolean requiresRestart);

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
	default IConfigScreenCategoryBuilder addValue(IConfigValue<?> value) {
		return addScreenValue(IConfigScreenValue.configValue(value));
	}

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
	default IConfigScreenCategoryBuilder addValues(Collection<? extends IConfigValue<?>> values) {
		return addScreenValues(values.stream()
			.map(IConfigScreenValue::configValue)
			.toList());
	}

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
	 * Add config values to this category when the screen is opened.
	 * <p>
	 * Added values are appended to this category's automatically detected values. Call {@link #clearDefaultValues()} to
	 * replace the automatically detected values.
	 *
	 * @param valuesSupplier supplies config values to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	default IConfigScreenCategoryBuilder addValues(Supplier<? extends Collection<? extends IConfigValue<?>>> valuesSupplier) {
		return addScreenValues(() -> valuesSupplier.get()
			.stream()
			.map(IConfigScreenValue::configValue)
			.toList());
	}

	/**
	 * Add config screen values to this category when the screen is opened.
	 * <p>
	 * Added values are appended to this category's automatically detected values. Call {@link #clearDefaultValues()} to
	 * replace the automatically detected values.
	 *
	 * @param valuesSupplier supplies config screen values to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addScreenValues(Supplier<? extends Collection<? extends IConfigScreenValue<?>>> valuesSupplier);

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
	default IConfigScreenCategoryBuilder hideValue(IConfigValue<?> value) {
		return hideScreenValue(IConfigScreenValue.configValue(value));
	}

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
	default IConfigScreenCategoryBuilder hideValues(Collection<? extends IConfigValue<?>> values) {
		return hideScreenValues(values.stream()
			.map(IConfigScreenValue::configValue)
			.toList());
	}

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
	 * Hide schema config values from this category when the screen is opened.
	 * <p>
	 * This is only needed when this category keeps automatically detected values and some of those values should still
	 * be omitted.
	 *
	 * @param valuesSupplier supplies config values to hide
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	default IConfigScreenCategoryBuilder hideValues(Supplier<? extends Collection<? extends IConfigValue<?>>> valuesSupplier) {
		return hideScreenValues(() -> valuesSupplier.get()
			.stream()
			.map(IConfigScreenValue::configValue)
			.toList());
	}

	/**
	 * Hide config screen values from this category when the screen is opened.
	 * <p>
	 * This is only needed when this category keeps automatically detected values and some of those values should still
	 * be omitted.
	 *
	 * @param valuesSupplier supplies config screen values to hide
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder hideScreenValues(Supplier<? extends Collection<? extends IConfigScreenValue<?>>> valuesSupplier);

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
	default IConfigScreenCategoryBuilder addValuesByName(Collection<String> valueNames) {
		Collection<String> checkedValueNames = Objects.requireNonNull(valueNames, "valueNames");
		for (String valueName : checkedValueNames) {
			addValueByName(valueName);
		}
		return this;
	}

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
	default IConfigScreenCategoryBuilder hideValuesByName(Collection<String> valueNames) {
		Collection<String> checkedValueNames = Objects.requireNonNull(valueNames, "valueNames");
		for (String valueName : checkedValueNames) {
			hideValueByName(valueName);
		}
		return this;
	}

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

	/**
	 * Add key mappings to this category when the screen is opened.
	 * <p>
	 * Adding key mappings to any configured category disables the automatically detected key mappings category for this
	 * screen. Added key mappings are appended to this category's automatically detected values.
	 *
	 * @param keyMappingsSupplier supplies key mappings to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addKeyMappings(Supplier<? extends Collection<? extends KeyMapping>> keyMappingsSupplier);
}
