package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.ConfigValueEditorType;
import net.mezzdev.config.api.value.ConfigValueEditorTypes;
import net.mezzdev.config.api.value.IConfigIntegerValueSerializer;
import net.mezzdev.config.api.value.IConfigListValueSerializer;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.gui.api.IConfigValueEditor;
import net.mezzdev.config.gui.api.IConfigValueEditorFactory;
import net.mezzdev.config.gui.keybindings.KeyMappingConfigEntry;
import net.mezzdev.config.gui.keybindings.KeyMappingValue;
import net.mezzdev.config.gui.popup.ConfigPopupSelector;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ErrorUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Creates the appropriate row widget for each supported config value editor type.
 */
public final class ConfigEntryWidgetFactory {
	private final Map<ConfigValueEditorType<?>, ConfigEntryWidgetCreator<?>> creators = new HashMap<>();
	private final Consumer<ConfigPopupSelector> valueSelectorOpener;
	private final Runnable layoutUpdater;
	private final ConfigTextures textures;

	public ConfigEntryWidgetFactory(
		Consumer<ConfigPopupSelector> valueSelectorOpener,
		Runnable layoutUpdater,
		ConfigTextures textures,
		Map<ConfigValueEditorType<?>, IConfigValueEditorFactory<?>> valueEditorFactories
	) {
		this.valueSelectorOpener = valueSelectorOpener;
		this.layoutUpdater = layoutUpdater;
		this.textures = textures;

		register(ConfigValueEditorTypes.<KeyMappingValue>getKeyMapping(), this::createKeyMappingEntry);
		register(ConfigValueEditorTypes.BOOLEAN, value -> new CustomConfigEntry<>(value, new BooleanConfigValueEditor(), valueSelectorOpener, textures));
		register(ConfigValueEditorTypes.INTEGER, this::createIntegerEntry);
		register(ConfigValueEditorTypes.getText(), this::createTextEntry);
		register(ConfigValueEditorTypes.getList(), this::createListEntry);
		registerSelectionEditor();
		valueEditorFactories.forEach(this::registerCustomEditor);
	}

	private <T> void register(ConfigValueEditorType<T> editorType, ConfigEntryWidgetCreator<T> creator) {
		creators.put(editorType, creator);
	}

	@SuppressWarnings("unchecked")
	private <T> void registerCustomEditor(ConfigValueEditorType<?> editorType, IConfigValueEditorFactory<?> editorFactory) {
		ConfigValueEditorType<T> typedEditorType = (ConfigValueEditorType<T>) editorType;
		IConfigValueEditorFactory<T> typedEditorFactory = (IConfigValueEditorFactory<T>) editorFactory;
		register(typedEditorType, value -> {
			IConfigValueEditor<T> editor = typedEditorFactory.createEditor(value);
			editor = ErrorUtil.checkNotNull(editor, "editorFactory result");
			return new CustomConfigEntry<>(value, editor, valueSelectorOpener, textures);
		});
	}

	public ConfigEntryWidget<?> create(IConfigValue<?> value) {
		ConfigValueEditorType<?> editorType = value.getSerializer().getEditorType();
		ConfigEntryWidgetCreator<?> creator = creators.get(editorType);
		if (creator == null) {
			throw new UnsupportedOperationException("Unsupported config value editor type: " + editorType);
		}
		return create(creator, value);
	}

	private ConfigEntryWidget<KeyMappingValue> createKeyMappingEntry(IConfigValue<KeyMappingValue> value) {
		return new KeyMappingConfigEntry(value, textures);
	}

	@SuppressWarnings("unchecked")
	private static <T> ConfigEntryWidget<T> create(ConfigEntryWidgetCreator<?> creator, IConfigValue<?> value) {
		return ((ConfigEntryWidgetCreator<T>) creator).create((IConfigValue<T>) value);
	}

	private ConfigEntryWidget<Integer> createIntegerEntry(IConfigValue<Integer> value) {
		IConfigIntegerValueSerializer serializer = (IConfigIntegerValueSerializer) value.getSerializer();
		return new IntegerConfigEntry(value, serializer, textures);
	}

	private <T> ConfigEntryWidget<T> createTextEntry(IConfigValue<T> value) {
		return new TextConfigEntry<>(value, value.getSerializer(), textures);
	}

	@SuppressWarnings("unchecked")
	private <T> ConfigEntryWidget<List<T>> createListEntry(IConfigValue<List<T>> value) {
		IConfigListValueSerializer<T> serializer = (IConfigListValueSerializer<T>) value.getSerializer();
		return new ListConfigEntry<>(value, serializer, layoutUpdater, textures);
	}

	private <T> void registerSelectionEditor() {
		register(ConfigValueEditorTypes.getSelection(), value -> new CustomConfigEntry<>(value, new SelectionConfigValueEditor<>(textures), valueSelectorOpener, textures));
	}

	@FunctionalInterface
	private interface ConfigEntryWidgetCreator<T> {
		ConfigEntryWidget<T> create(IConfigValue<T> value);
	}
}
