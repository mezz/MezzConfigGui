package net.mezzdev.config.gui.api;

/**
 * Creates custom value editors for config value rows.
 *
 * @param <T> the config value type edited by this factory
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface IConfigValueEditorFactory<T> {
	/**
	 * Create an editor for one config value row.
	 *
	 * @param configValue the config screen value being edited
	 * @return the editor for this row
	 *
	 * @since 0.1.0
	 */
	IConfigValueEditor<T> createEditor(IConfigScreenValue<T> configValue);
}
