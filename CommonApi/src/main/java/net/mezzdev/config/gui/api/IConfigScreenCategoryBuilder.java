package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.IConfigValue;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.util.Collection;
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
	 * Set the default apply mode for config screen values in this category.
	 * <p>
	 * Individual values can override this with {@link #setValueApplyMode(IConfigScreenValueReference, ConfigValueApplyMode)}.
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
	default IConfigScreenCategoryBuilder setValueApplyMode(IConfigValue<?> value, ConfigValueApplyMode applyMode) {
		return setValueApplyMode(IConfigScreenValueReference.configValue(value), applyMode);
	}

	/**
	 * Set the apply mode for one config screen value in this category.
	 *
	 * @param value config screen value to configure
	 * @param applyMode when edits for this value are saved
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	default IConfigScreenCategoryBuilder setScreenValueApplyMode(IConfigScreenValue<?> value, ConfigValueApplyMode applyMode) {
		return setValueApplyMode(IConfigScreenValueReference.screenValue(value), applyMode);
	}

	/**
	 * Set the apply mode for one config screen value in this category by reference.
	 *
	 * @param valueReference config screen value reference
	 * @param applyMode when edits for this value are saved
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder setValueApplyMode(IConfigScreenValueReference valueReference, ConfigValueApplyMode applyMode);

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
	default IConfigScreenCategoryBuilder setValueRequiresRestart(IConfigValue<?> value, boolean requiresRestart) {
		return setValueRequiresRestart(IConfigScreenValueReference.configValue(value), requiresRestart);
	}

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
	default IConfigScreenCategoryBuilder setScreenValueRequiresRestart(IConfigScreenValue<?> value, boolean requiresRestart) {
		return setValueRequiresRestart(IConfigScreenValueReference.screenValue(value), requiresRestart);
	}

	/**
	 * Mark one config screen value in this category by reference as requiring a restart or larger reload after it is saved.
	 *
	 * @param valueReference config screen value reference
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	default IConfigScreenCategoryBuilder setValueRequiresRestart(IConfigScreenValueReference valueReference) {
		return setValueRequiresRestart(valueReference, true);
	}

	/**
	 * Set whether one config screen value in this category by reference requires a restart or larger reload after it is saved.
	 *
	 * @param valueReference config screen value reference
	 * @param requiresRestart true if saving this value requires a restart or larger reload
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder setValueRequiresRestart(IConfigScreenValueReference valueReference, boolean requiresRestart);

	/**
	 * Add one config value to this category.
	 * <p>
	 * Adding values to a category replaces that category's automatically detected values.
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
	 * Adding values to a category replaces that category's automatically detected values.
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
	 * Adding values to a category replaces that category's automatically detected values.
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
	 * Adding values to a category replaces that category's automatically detected values.
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
	 * Adding values to a category replaces that category's automatically detected values.
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
	 * Adding values to a category replaces that category's automatically detected values.
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
	 * Add one config value to this category by reference.
	 * <p>
	 * Adding values to a category replaces that category's automatically detected values.
	 *
	 * @param valueReference config value reference
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addValueReference(IConfigScreenValueReference valueReference);

	/**
	 * Hide one schema config value from this category by reference.
	 * <p>
	 * This is only needed when this category keeps automatically detected values and some of those values should still
	 * be omitted.
	 *
	 * @param valueReference config value reference to hide
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder hideValueReference(IConfigScreenValueReference valueReference);

	/**
	 * Add config values to this category by reference.
	 * <p>
	 * Adding values to a category replaces that category's automatically detected values.
	 *
	 * @param valueReferences config value references
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addValueReferences(Collection<? extends IConfigScreenValueReference> valueReferences);

	/**
	 * Hide schema config values from this category by reference.
	 * <p>
	 * This is only needed when this category keeps automatically detected values and some of those values should still
	 * be omitted.
	 *
	 * @param valueReferences config value references to hide
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder hideValueReferences(Collection<? extends IConfigScreenValueReference> valueReferences);

	/**
	 * Add one key mapping to this category.
	 * <p>
	 * Adding key mappings to any configured category disables the automatically detected key mappings category for this
	 * screen. Adding values to a category replaces that category's automatically detected values.
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
	 * screen. Adding values to a category replaces that category's automatically detected values.
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
	 * screen. Adding values to a category replaces that category's automatically detected values.
	 *
	 * @param keyMappingsSupplier supplies key mappings to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addKeyMappings(Supplier<? extends Collection<? extends KeyMapping>> keyMappingsSupplier);
}
