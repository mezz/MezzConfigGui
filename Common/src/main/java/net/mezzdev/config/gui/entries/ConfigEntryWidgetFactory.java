package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.ConfigValueRange;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.api.value.PackedColor;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditor;
import net.mezzdev.config.gui.api.IConfigValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigValueEditorFactory;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.keybindings.KeyMappingConfigEntry;
import net.mezzdev.config.gui.keybindings.KeyMappingValue;
import net.mezzdev.config.gui.popup.ConfigPopupSelector;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ErrorUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
		register(ConfigValueEditorTypes.BOOLEAN, value -> new CustomConfigEntry<>(value, new BooleanConfigValueEditor(), valueSelectorOpener, textures, false));
		register(ConfigValueEditorTypes.INTEGER, this::createIntegerEntry);
		register(ConfigValueEditorTypes.COLOR, this::createColorEntry);
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

	public ConfigEntryWidget<?> create(IConfigScreenValue<?> value) {
		if (ConfigGuiOptions.getNumberDisplayMode() == ConfigGuiOptions.NumberDisplayMode.SLIDER && allowsNumberSlider(value)) {
			Optional<? extends ConfigEntryWidget<?>> sliderEntry = NumberSliderConfigEntry.create(value, textures);
			if (sliderEntry.isPresent()) {
				return sliderEntry.get();
			}
		}
		ConfigValueEditorType<?> editorType = getEditorType(value);
		ConfigEntryWidgetCreator<?> creator = creators.get(editorType);
		if (creator == null) {
			throw new UnsupportedOperationException("Unsupported config value editor type: " + editorType);
		}
		return create(creator, value);
	}

	private static boolean allowsNumberSlider(IConfigScreenValue<?> value) {
		if (!(value.getValue() instanceof Number)) {
			return false;
		}
		IConfigValueSerializer<?> serializer = value.getSerializer();
		if (serializer instanceof IConfigValueEditorSerializer<?> editorSerializer) {
			ConfigValueEditorType<?> editorType = editorSerializer.getEditorType();
			return editorType == ConfigValueEditorTypes.INTEGER || editorType == ConfigValueEditorTypes.getText();
		}
		return true;
	}

	private static ConfigValueEditorType<?> getEditorType(IConfigScreenValue<?> value) {
		IConfigValueSerializer<?> serializer = value.getSerializer();
		if (serializer instanceof IConfigValueEditorSerializer<?> editorSerializer) {
			return editorSerializer.getEditorType();
		}
		if (value.getValue() instanceof List<?> && ConfigListValueEditorSerializers.canAdapt(serializer)) {
			return ConfigValueEditorTypes.getList();
		}
		if (value.getValue() instanceof PackedColor) {
			return ConfigValueEditorTypes.COLOR;
		}
		if (value.getValue() instanceof Integer && serializer.getRange().isPresent()) {
			return ConfigValueEditorTypes.INTEGER;
		}
		if (value.getValue() instanceof Boolean) {
			return ConfigValueEditorTypes.BOOLEAN;
		}
		if (serializer.getAllValidValues().isPresent()) {
			return ConfigValueEditorTypes.getSelection();
		}
		return ConfigValueEditorTypes.getText();
	}

	private ConfigEntryWidget<KeyMappingValue> createKeyMappingEntry(IConfigScreenValue<KeyMappingValue> value) {
		return new KeyMappingConfigEntry(value, textures);
	}

	@SuppressWarnings("unchecked")
	private static <T> ConfigEntryWidget<T> create(ConfigEntryWidgetCreator<?> creator, IConfigScreenValue<?> value) {
		return ((ConfigEntryWidgetCreator<T>) creator).create((IConfigScreenValue<T>) value);
	}

	private ConfigEntryWidget<Integer> createIntegerEntry(IConfigScreenValue<Integer> value) {
		IConfigValueSerializer<Integer> serializer = value.getSerializer();
		ConfigValueRange<Integer> range = serializer.getRange()
			.orElseThrow(() -> new UnsupportedOperationException("Integer editor requires a config value range."));
		return new IntegerConfigEntry(value, serializer, range, textures);
	}

	private ConfigEntryWidget<PackedColor> createColorEntry(IConfigScreenValue<PackedColor> value) {
		return new ColorConfigEntry(value, value.getSerializer(), valueSelectorOpener, textures);
	}

	private <T> ConfigEntryWidget<T> createTextEntry(IConfigScreenValue<T> value) {
		return new TextConfigEntry<>(value, value.getSerializer(), textures);
	}

	@SuppressWarnings("unchecked")
	private <T> ConfigEntryWidget<List<T>> createListEntry(IConfigScreenValue<List<T>> value) {
		IConfigListValueEditorSerializer<T> serializer = ConfigListValueEditorSerializers.adapt((IConfigValueSerializer<List<T>>) value.getSerializer());
		return new ListConfigEntry<>(value, serializer, valueSelectorOpener, layoutUpdater, textures);
	}

	private <T> void registerSelectionEditor() {
		register(ConfigValueEditorTypes.getSelection(), value -> new CustomConfigEntry<>(value, new SelectionConfigValueEditor<>(), valueSelectorOpener, textures));
	}

	@FunctionalInterface
	private interface ConfigEntryWidgetCreator<T> {
		ConfigEntryWidget<T> create(IConfigScreenValue<T> value);
	}
}
