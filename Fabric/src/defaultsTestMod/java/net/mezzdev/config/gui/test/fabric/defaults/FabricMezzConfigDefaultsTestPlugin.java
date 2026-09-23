package net.mezzdev.config.gui.test.fabric.defaults;

import net.fabricmc.api.ClientModInitializer;
import net.mezzdev.config.api.Configs;
import net.mezzdev.config.api.IConfigRegistration;
import net.mezzdev.config.api.schema.builder.IConfigCategoryBuilder;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.schema.builder.IConfigSchemaBuilder;
import net.mezzdev.config.api.value.color.ConfigColorFormat;
import net.mezzdev.config.api.value.editor.ConfigValueEditMode;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.IConfigKeyValueSerializer;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.api.value.color.PackedColor;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public final class FabricMezzConfigDefaultsTestPlugin implements ClientModInitializer, IConfigGuiPlugin {
	private static final String MOD_ID = "mezz_config_gui_test_fabric_defaults";
	private static final String LOCALIZATION_PATH = "mezz_config_gui_test.fabric.defaults";
	@Nullable
	private static IConfigSchema schema;

	@Override
	public String getModId() {
		return MOD_ID;
	}

	@Override
	public void onInitializeClient() {
		IConfigRegistration registration = Configs.forMod(MOD_ID);
		IConfigSchemaBuilder schemaBuilder = registration.createClientSchemaBuilder("config-gui-fabric-defaults-test.ini", LOCALIZATION_PATH);
		IConfigCategoryBuilder general = schemaBuilder.addCategory("general");
		general.addBoolean("enabled", true)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		general.addBoolean("requiresRestart", false)
			.setEditMode(ConfigValueEditMode.BATCH)
			.setRestartRequirement(ConfigValueRestartRequirement.GAME_RESTART)
			.build();
		general.addBooleanList("enabledHistory", List.of(true, false, true)).build();

		IConfigCategoryBuilder numbers = schemaBuilder.addCategory("numbers");
		numbers.addInteger("maxVisibleRows", 8, 1, 16).build();
		numbers.addInteger("unboundedInteger", 1024).build();
		numbers.addInteger("tinySelectionRange", 2, 0, 4)
			.setEditMode(ConfigValueEditMode.IMMEDIATE)
			.build();
		numbers.addIntegerList("favoriteNumbers", List.of(1, 2, 3), 0, 16).build();
		numbers.addColor("accentColor", PackedColor.argb(0xFF33AA55)).build();
		numbers.addColorList(
				"palette",
				List.of(
					PackedColor.argb(0xFF33AA55),
					PackedColor.rgb(0x4477DD),
					PackedColor.argb(0x80E0AA22)
				)
			)
			.build();
		numbers.addList(
				"namedColors",
				List.of(
					new NamedColor("Highlight", PackedColor.rgb(0xF2C94C)),
					new NamedColor("Success", PackedColor.rgb(0x33AA55)),
					new NamedColor("Warning", PackedColor.rgb(0xE07030))
				),
				NamedColorSerializer.INSTANCE
			)
			.build();
		numbers.addLong("maxEnergy", 10_000_000_000L).build();
		numbers.addLong("boundedLong", 64L, 0L, 1024L).build();
		numbers.addLongList("longBreakpoints", List.of(128L, 256L, 512L), 0L, 1024L).build();
		numbers.addDouble("scale", 1.25, 0.25, 4.0).build();
		numbers.addDoubleList("thresholds", List.of(0.25, 0.5, 0.75), 0.0, 1.0).build();

		IConfigCategoryBuilder text = schemaBuilder.addCategory("text");
		text.addString("screenLabel", "Fabric Defaults").build();
		text.addStringList("aliases", List.of("default", "sample")).build();

		IConfigCategoryBuilder selections = schemaBuilder.addCategory("selections");
		selections.addEnum("mode", TestMode.BALANCED).build();
		selections.addEnum("restrictedMode", TestMode.FAST, List.of(TestMode.BALANCED, TestMode.FAST)).build();
		selections.addEnumList("favoriteModes", List.of(TestMode.BALANCED, TestMode.FAST), TestMode.class).build();
		selections.addEnumList("fastModes", List.of(TestMode.FAST), List.of(TestMode.BALANCED, TestMode.FAST)).build();
		schema = schemaBuilder.build();
	}

	@Override
	public void register(IConfigGuiRegistration registration) {
		registration.registerScreen(
			Component.translatable(MOD_ID + ".config.screen.title"),
			FabricMezzConfigDefaultsTestPlugin::getSchema
		);
	}

	private static IConfigSchema getSchema() {
		return Objects.requireNonNull(schema, "schema");
	}

	private enum TestMode {
		SLOW,
		BALANCED,
		FAST
	}

	private record NamedColor(String name, PackedColor color) {}

	private static final class NamedColorSerializer implements IConfigKeyValueSerializer<NamedColor, String, PackedColor> {
		private static final NamedColorSerializer INSTANCE = new NamedColorSerializer();

		@Override
		public IConfigValueSerializer<String> getKeySerializer() {
			return NameSerializer.INSTANCE;
		}

		@Override
		public IConfigValueSerializer<PackedColor> getValueSerializer() {
			return RgbColorSerializer.INSTANCE;
		}

		@Override
		public String getKey(NamedColor entry) {
			return entry.name();
		}

		@Override
		public PackedColor getValue(NamedColor entry) {
			return entry.color();
		}

		@Override
		public NamedColor createEntry(String key, PackedColor value) {
			return new NamedColor(key, value);
		}

		@Override
		public String serialize(NamedColor value) {
			return value.name() + ":" + RgbColorSerializer.INSTANCE.serialize(value.color());
		}

		@Override
		public IDeserializeResult<NamedColor> deserialize(String string) {
			String[] parts = string.split(":", 2);
			if (parts.length != 2 || !NameSerializer.INSTANCE.isValid(parts[0])) {
				return IDeserializeResult.failure("Named colors must contain a name and RGB color separated by ':'");
			}
			IDeserializeResult<PackedColor> colorResult = RgbColorSerializer.INSTANCE.deserialize(parts[1]);
			if (!colorResult.getDiagnostics().isEmpty()) {
				return IDeserializeResult.failure("Named colors must contain an RGB color");
			}
			return colorResult.getResult()
				.map(color -> IDeserializeResult.success(new NamedColor(parts[0], color)))
				.orElseGet(() -> IDeserializeResult.failure("Named colors must contain an RGB color"));
		}

		@Override
		public boolean isValid(NamedColor value) {
			return NameSerializer.INSTANCE.isValid(value.name()) && RgbColorSerializer.INSTANCE.isValid(value.color());
		}

		@Override
		public String getValidValuesDescription() {
			return "A name and RGB color separated by ':'";
		}
	}

	private static final class NameSerializer implements IConfigValueSerializer<String> {
		private static final NameSerializer INSTANCE = new NameSerializer();

		@Override
		public String serialize(String value) {
			return value;
		}

		@Override
		public IDeserializeResult<String> deserialize(String string) {
			if (!isValid(string)) {
				return IDeserializeResult.failure("Names must not be blank or contain ':'");
			}
			return IDeserializeResult.success(string);
		}

		@Override
		public boolean isValid(String value) {
			return !value.isBlank() && !value.contains(":");
		}

		@Override
		public String getValidValuesDescription() {
			return "A non-blank name without ':'";
		}
	}

	private static final class RgbColorSerializer implements IConfigValueSerializer<PackedColor> {
		private static final RgbColorSerializer INSTANCE = new RgbColorSerializer();

		@Override
		public String serialize(PackedColor value) {
			return "0x%06X".formatted(value.packedValue());
		}

		@Override
		public IDeserializeResult<PackedColor> deserialize(String string) {
			String hex = string;
			if (hex.startsWith("#")) {
				hex = hex.substring(1);
			}
			if (hex.startsWith("0x") || hex.startsWith("0X")) {
				hex = hex.substring(2);
			}
			if (hex.length() != 6) {
				return IDeserializeResult.failure("RGB colors must contain exactly 6 hex digits");
			}
			try {
				return IDeserializeResult.success(PackedColor.rgb(Integer.parseInt(hex, 16)));
			} catch (NumberFormatException e) {
				return IDeserializeResult.failure("RGB colors must contain exactly 6 hex digits");
			}
		}

		@Override
		public boolean isValid(PackedColor value) {
			return value.format() == ConfigColorFormat.RGB;
		}

		@Override
		public String getValidValuesDescription() {
			return "An RGB color written as 0xRRGGBB";
		}
	}
}
