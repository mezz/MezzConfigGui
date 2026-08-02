package net.mezzdev.config.gui.test.fabric.defaults;

import net.mezzdev.config.api.plugin.IConfigPlugin;
import net.mezzdev.config.api.plugin.IConfigRegistration;
import net.mezzdev.config.api.schema.IConfigCategoryBuilder;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.schema.IConfigSchemaBuilder;
import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.gui.api.ConfigRestartResult;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigValueEditorSerializer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class FabricMezzConfigDefaultsTestPlugin implements IConfigPlugin, IConfigGuiPlugin {
	private static final String MOD_ID = "mezz_config_gui_test_fabric_defaults";
	private static final String LOCALIZATION_PATH = "mezz_config_gui_test.fabric.defaults";
	@Nullable
	private static IConfigSchema schema;
	@Nullable
	private static IConfigValue<Boolean> requiresRestart;
	@Nullable
	private static IConfigValue<Integer> tinySelectionRange;

	@Override
	public String getModId() {
		return MOD_ID;
	}

	@Override
	public void registerConfigFiles(IConfigRegistration registration) {
		IConfigSchemaBuilder schemaBuilder = registration.createSchemaBuilder("config-gui-fabric-defaults-test.ini", LOCALIZATION_PATH);
		IConfigCategoryBuilder general = schemaBuilder.addCategory("general");
		general.addBoolean("enabled", true).build();
		requiresRestart = general.addBoolean("requiresRestart", false).build();

		IConfigCategoryBuilder numbers = schemaBuilder.addCategory("numbers");
		numbers.addInteger("maxVisibleRows", 8, 1, 16).build();
		tinySelectionRange = numbers.addInteger("tinySelectionRange", 2, 0, 4).build();

		IConfigCategoryBuilder text = schemaBuilder.addCategory("text");
		text.addValue("screenLabel", "Fabric Defaults", TextSerializer.INSTANCE).build();

		IConfigCategoryBuilder selections = schemaBuilder.addCategory("selections");
		selections.addEnum("mode", TestMode.BALANCED).build();
		selections.addValue(
			"favoriteModes",
			List.of(TestMode.BALANCED, TestMode.FAST),
			ModeListSerializer.INSTANCE
		).build();
		schema = schemaBuilder.build();
	}

	@Override
	public void register(IConfigGuiRegistration registration) {
		registration.configureScreen(screenBuilder -> {
			screenBuilder.configureCategory("general")
				.setDefaultApplyMode(ConfigValueApplyMode.IMMEDIATE)
				.setValueRequiresRestart(getRequiresRestart());
			screenBuilder.configureCategory("numbers")
				.setValueApplyMode(getTinySelectionRange(), ConfigValueApplyMode.IMMEDIATE);
		});
		registration.registerScreen(
			Component.translatable(MOD_ID + ".config.screen.title"),
			FabricMezzConfigDefaultsTestPlugin::getSchema,
			() -> ConfigRestartResult.NEXT_GAME_START
		);
	}

	private static IConfigSchema getSchema() {
		return Objects.requireNonNull(schema, "schema");
	}

	private static IConfigValue<Integer> getTinySelectionRange() {
		return Objects.requireNonNull(tinySelectionRange, "tinySelectionRange");
	}

	private static IConfigValue<Boolean> getRequiresRestart() {
		return Objects.requireNonNull(requiresRestart, "requiresRestart");
	}

	private enum TestMode {
		SLOW,
		BALANCED,
		FAST
	}

	private enum TextSerializer implements IConfigValueEditorSerializer<String> {
		INSTANCE;

		@Override
		public String serialize(String value) {
			return value;
		}

		@Override
		public IDeserializeResult<String> deserialize(String string) {
			String value = string.trim();
			if (!isValid(value)) {
				return IDeserializeResult.failure("Text must be between 1 and 32 characters.");
			}
			return IDeserializeResult.success(value);
		}

		@Override
		public boolean isValid(String value) {
			return value != null && !value.isBlank() && value.length() <= 32;
		}

		@Override
		public Optional<Collection<String>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public Component getLocalizedValueName(String configValueLocalizationKey, String value) {
			return Component.literal(value);
		}

		@Override
		public String getValidValuesDescription() {
			return "Text between 1 and 32 characters";
		}

		@Override
		public ConfigValueEditorType<String> getEditorType() {
			return ConfigValueEditorTypes.getText();
		}
	}

	private enum ModeSerializer implements IConfigValueEditorSerializer<TestMode> {
		INSTANCE;

		@Override
		public String serialize(TestMode value) {
			return value.name();
		}

		@Override
		public IDeserializeResult<TestMode> deserialize(String string) {
			try {
				return IDeserializeResult.success(TestMode.valueOf(string.trim().toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException e) {
				return IDeserializeResult.failure("Expected one of: " + getValidValuesDescription());
			}
		}

		@Override
		public boolean isValid(TestMode value) {
			return value != null;
		}

		@Override
		public Optional<Collection<TestMode>> getAllValidValues() {
			return Optional.of(List.of(TestMode.values()));
		}

		@Override
		public Component getLocalizedValueName(String configValueLocalizationKey, TestMode value) {
			String key = "%s.%s".formatted(configValueLocalizationKey, value.name().toLowerCase(Locale.ROOT));
			return Component.translatableWithFallback(key, getDisplayName(value.name()));
		}

		@Override
		public String getValidValuesDescription() {
			return Arrays.toString(TestMode.values());
		}

		@Override
		public ConfigValueEditorType<TestMode> getEditorType() {
			return ConfigValueEditorTypes.getSelection();
		}
	}

	private enum ModeListSerializer implements IConfigListValueEditorSerializer<TestMode> {
		INSTANCE;

		@Override
		public IConfigValueSerializer<TestMode> getElementSerializer() {
			return ModeSerializer.INSTANCE;
		}

		@Override
		public String serialize(List<TestMode> values) {
			return String.join(", ", values.stream().map(TestMode::name).toList());
		}

		@Override
		public IDeserializeResult<List<TestMode>> deserialize(String string) {
			List<String> errors = new java.util.ArrayList<>();
			List<TestMode> values = Arrays.stream(string.split(","))
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.map(ModeSerializer.INSTANCE::deserialize)
				.<TestMode>mapMulti((result, consumer) -> {
					result.getResult().ifPresent(consumer);
					errors.addAll(result.getErrors());
				})
				.toList();
			if (!errors.isEmpty()) {
				return IDeserializeResult.failure(errors);
			}
			return IDeserializeResult.success(values);
		}

		@Override
		public boolean isValid(List<TestMode> value) {
			return value != null && value.stream().allMatch(ModeSerializer.INSTANCE::isValid);
		}

		@Override
		public Optional<Collection<List<TestMode>>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public Component getLocalizedValueName(String configValueLocalizationKey, List<TestMode> value) {
			return Component.literal(serialize(value));
		}

		@Override
		public String getValidValuesDescription() {
			return "A comma-separated list containing values of: " + ModeSerializer.INSTANCE.getValidValuesDescription();
		}

	}

	private static String getDisplayName(String name) {
		String lower = name.toLowerCase(Locale.ROOT);
		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}
}
