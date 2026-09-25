package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.serializer.ConfigValueRange;

/**
 * Serializes an inclusive numeric interval edited with two slider handles.
 * <p>
 * The slider supports bounded {@link Integer}, {@link Long}, and {@link Double} endpoints, with the same limits as
 * single-value sliders. Wider or unsupported bounds use the serialized text editor. Implementations must validate
 * both endpoints and their ordering. The existing number display preference also allows editing as text.
 *
 * @param <T> the numeric endpoint type
 *
 * @since 0.5.8
 */
public interface IConfigRangeValueEditorSerializer<T> extends IConfigValueEditorSerializer<ConfigValueRange<T>> {
	/**
	 * The inclusive bounds within which both handles can move.
	 *
	 * @since 0.5.8
	 */
	ConfigValueRange<T> getBounds();

	@Override
	default ConfigValueEditorType<ConfigValueRange<T>> getEditorType() {
		return ConfigValueEditorTypes.getRange();
	}
}
