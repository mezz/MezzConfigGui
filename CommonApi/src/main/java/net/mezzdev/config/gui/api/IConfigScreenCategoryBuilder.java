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
	 * Add one config value to this category.
	 *
	 * @param value config value to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addValue(IConfigValue<?> value);

	/**
	 * Add config values to this category.
	 *
	 * @param values config values to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addValues(Collection<? extends IConfigValue<?>> values);

	/**
	 * Add config values to this category when the screen is opened.
	 *
	 * @param valuesSupplier supplies config values to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addValues(Supplier<? extends Collection<? extends IConfigValue<?>>> valuesSupplier);

	/**
	 * Hide one schema config value from this category.
	 * <p>
	 * This is useful when a custom screen value represents one or more lower-level schema values.
	 *
	 * @param value config value to hide
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder hideValue(IConfigValue<?> value);

	/**
	 * Hide schema config values from this category.
	 * <p>
	 * This is useful when a custom screen value represents one or more lower-level schema values.
	 *
	 * @param values config values to hide
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder hideValues(Collection<? extends IConfigValue<?>> values);

	/**
	 * Hide schema config values from this category when the screen is opened.
	 * <p>
	 * This is useful when a custom screen value represents one or more lower-level schema values.
	 *
	 * @param valuesSupplier supplies config values to hide
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder hideValues(Supplier<? extends Collection<? extends IConfigValue<?>>> valuesSupplier);

	/**
	 * Add one config value to this category by reference.
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
	 * This is useful when a custom screen value represents one or more lower-level schema values that are only known
	 * through platform-native references.
	 *
	 * @param valueReference config value reference to hide
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder hideValueReference(IConfigScreenValueReference valueReference);

	/**
	 * Add config values to this category by reference.
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
	 * This is useful when custom screen values represent lower-level schema values that are only known through
	 * platform-native references.
	 *
	 * @param valueReferences config value references to hide
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder hideValueReferences(Collection<? extends IConfigScreenValueReference> valueReferences);

	/**
	 * Add one key mapping to this category.
	 *
	 * @param keyMapping key mapping to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addKeyMapping(KeyMapping keyMapping);

	/**
	 * Add key mappings to this category.
	 *
	 * @param keyMappings key mappings to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addKeyMappings(Collection<? extends KeyMapping> keyMappings);

	/**
	 * Add key mappings to this category when the screen is opened.
	 *
	 * @param keyMappingsSupplier supplies key mappings to display
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	IConfigScreenCategoryBuilder addKeyMappings(Supplier<? extends Collection<? extends KeyMapping>> keyMappingsSupplier);
}
