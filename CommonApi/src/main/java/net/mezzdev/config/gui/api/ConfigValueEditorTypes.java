package net.mezzdev.config.gui.api;

import net.mezzdev.config.api.value.PackedColor;

import java.util.List;

/**
 * Standard editor types used by config screens.
 *
 * @since 0.1.0
 */
public final class ConfigValueEditorTypes {
	private static final String CONFIG_ID = "mezz_config";

	public static final ConfigValueEditorType<Boolean> BOOLEAN = ConfigValueEditorType.create(CONFIG_ID, "boolean");
	public static final ConfigValueEditorType<Integer> INTEGER = ConfigValueEditorType.create(CONFIG_ID, "integer");
	public static final ConfigValueEditorType<PackedColor> COLOR = ConfigValueEditorType.create(CONFIG_ID, "color");

	private static final ConfigValueEditorType<Object> TEXT = ConfigValueEditorType.create(CONFIG_ID, "text");
	private static final ConfigValueEditorType<Object> SELECTION = ConfigValueEditorType.create(CONFIG_ID, "selection");
	private static final ConfigValueEditorType<List<Object>> LIST = ConfigValueEditorType.create(CONFIG_ID, "list");
	private static final ConfigValueEditorType<Object> KEY_MAPPING = ConfigValueEditorType.create(CONFIG_ID, "key_mapping");

	private ConfigValueEditorTypes() {
	}

	/**
	 * An editor for values that are best edited as serialized text.
	 *
	 * @since 0.1.0
	 */
	@SuppressWarnings("unchecked")
	public static <T> ConfigValueEditorType<T> getText() {
		return (ConfigValueEditorType<T>) TEXT;
	}

	/**
	 * An editor for values with a finite set of valid options.
	 *
	 * @since 0.1.0
	 */
	@SuppressWarnings("unchecked")
	public static <T> ConfigValueEditorType<T> getSelection() {
		return (ConfigValueEditorType<T>) SELECTION;
	}

	/**
	 * An editor for ordered list values.
	 *
	 * @since 0.1.0
	 */
	@SuppressWarnings("unchecked")
	public static <T> ConfigValueEditorType<List<T>> getList() {
		return (ConfigValueEditorType<List<T>>) (Object) LIST;
	}

	/**
	 * An editor for key mapping values.
	 *
	 * @since 0.1.0
	 */
	@SuppressWarnings("unchecked")
	public static <T> ConfigValueEditorType<T> getKeyMapping() {
		return (ConfigValueEditorType<T>) KEY_MAPPING;
	}
}
