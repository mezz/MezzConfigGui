package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.ConfigValueRestartRequirement;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Customizes how a MezzConfig sorting config is shown in a config screen.
 *
 * @param <T> the sortable value type
 *
 * @since 0.1.0
 */
@ApiStatus.NonExtendable
public interface ISortingConfigGuiBuilder<T> {
	/**
	 * Set when edited sort-order values are saved.
	 *
	 * @param applyMode when edits for this sort-order value are saved
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	ISortingConfigGuiBuilder<T> setApplyMode(ConfigValueApplyMode applyMode);

	/**
	 * Set the restart or reload required after this sort-order value is saved.
	 *
	 * @param restartRequirement the required restart or reload
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	ISortingConfigGuiBuilder<T> setRestartRequirement(ConfigValueRestartRequirement restartRequirement);

	/**
	 * Set display names for sortable values.
	 * <p>
	 * Values missing from the map use the default value name from their serializer.
	 *
	 * @param valueNames display names by sortable value
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	ISortingConfigGuiBuilder<T> setValueNames(Map<T, Component> valueNames);

	/**
	 * Set display names for sortable values.
	 *
	 * @param valueNameFactory creates a display name for one sortable value
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	ISortingConfigGuiBuilder<T> setValueName(Function<T, Component> valueNameFactory);

	/**
	 * Set descriptions for sortable values.
	 * <p>
	 * Values missing from the map use the default value description from their serializer.
	 *
	 * @param valueDescriptions descriptions by sortable value
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	ISortingConfigGuiBuilder<T> setValueDescriptions(Map<T, Component> valueDescriptions);

	/**
	 * Set descriptions for sortable values.
	 *
	 * @param valueDescriptionFactory creates an optional description for one sortable value
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	ISortingConfigGuiBuilder<T> setValueDescription(Function<T, Optional<Component>> valueDescriptionFactory);

	/**
	 * Set icons for sortable values.
	 * <p>
	 * Values missing from the map have no custom icon.
	 *
	 * @param valueIcons icons by sortable value
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	ISortingConfigGuiBuilder<T> setValueIcons(Map<T, IConfigValueIcon> valueIcons);

	/**
	 * Set icons for sortable values.
	 *
	 * @param valueIconFactory creates an optional icon for one sortable value
	 * @return this builder
	 *
	 * @since 0.1.0
	 */
	ISortingConfigGuiBuilder<T> setValueIcon(Function<T, Optional<IConfigValueIcon>> valueIconFactory);
}
