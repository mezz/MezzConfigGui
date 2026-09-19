package net.mezzdev.config.gui.test.fabric.custom;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.mezzdev.config.api.Configs;
import net.mezzdev.config.api.IConfigRegistration;
import net.mezzdev.config.api.schema.builder.IConfigCategoryBuilder;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.schema.builder.IConfigSchemaBuilder;
import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.gui.api.IConfigValueEditorSerializer;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.ConfigGuiPlugin;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigLocalizedValue;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditor;
import net.mezzdev.config.gui.api.IConfigValuePopup;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@ConfigGuiPlugin
public final class FabricMezzConfigCustomTestPlugin implements ClientModInitializer, IConfigGuiPlugin {
	private static final String MOD_ID = "mezz_config_gui_test_fabric_custom";
	private static final String LOCALIZATION_PATH = "mezz_config_gui_test.fabric.custom";
	private static final KeyMapping OPEN_SCREEN_KEY = new KeyMapping(
		"key.%s.openScreen".formatted(MOD_ID),
		InputConstants.Type.KEYSYM,
		com.mojang.blaze3d.platform.InputConstants.KEY_G,
		net.mezzdev.config.gui.ConfigInputUtil.keyCategory(MOD_ID)
	);
	private static final KeyMapping TOGGLE_OVERLAY_KEY = new KeyMapping(
		"key.%s.toggleOverlay".formatted(MOD_ID),
		InputConstants.Type.KEYSYM,
		com.mojang.blaze3d.platform.InputConstants.KEY_H,
		net.mezzdev.config.gui.ConfigInputUtil.keyCategory(MOD_ID)
	);

	@Nullable
	private static IConfigSchema schema;
	@Nullable
	private static IConfigValue<Boolean> primaryEnabled;
	@Nullable
	private static IConfigValue<Boolean> secondaryEnabled;
	@Nullable
	private static IConfigValue<TestColor> accentColor;
	@Nullable
	private static IConfigValue<TestMode> mode;

	@Override
	public String getModId() {
		return MOD_ID;
	}

	@Override
	public void onInitializeClient() {
		IConfigRegistration registration = Configs.forMod(MOD_ID);
		IConfigSchemaBuilder schemaBuilder = registration.createClientSchemaBuilder("config-gui-fabric-custom-test.ini", LOCALIZATION_PATH);
		IConfigCategoryBuilder controls = schemaBuilder.addCategory("controls");
		primaryEnabled = controls.addBoolean("primaryEnabled", true).build();
		secondaryEnabled = controls.addBoolean("secondaryEnabled", false).build();
		mode = controls.addEnum("mode", TestMode.BALANCED).build();
		accentColor = controls.addValue("accentColor", TestColor.GREEN, TestColorSerializer.INSTANCE).build();

		IConfigCategoryBuilder advanced = schemaBuilder.addCategory("advanced");
		advanced.addValue("displayName", "Fabric Custom", TextSerializer.INSTANCE).build();
		advanced.addValue(
				"favoriteModes",
				List.of(TestMode.BALANCED, TestMode.FAST),
				ModeListSerializer.INSTANCE
			)
			.build();
		advanced.addInteger("refreshTicks", 20, 1, 200).build();
		schema = schemaBuilder.build();
	}

	@Override
	public void register(IConfigGuiRegistration registration) {
		registration.registerValueEditor(TestColorSerializer.EDITOR_TYPE, ignored -> new TestColorEditor());
		registration.configureScreen(screenBuilder -> {
			screenBuilder.setTitle(Component.translatable("%s.config.screen.custom.title".formatted(MOD_ID)));
			screenBuilder.addCategory("overview")
				.setTitle(Component.translatable("%s.config.category.overview".formatted(MOD_ID)))
				.setDescription(Component.translatable("%s.config.category.overview.description".formatted(MOD_ID)))
				.setDefaultApplyMode(ConfigValueApplyMode.IMMEDIATE)
				.addScreenValue(new CombinedEnabledValue(getPrimaryEnabled(), getSecondaryEnabled()))
				.addValue(getAccentColor());
			screenBuilder.addCategory("controls")
				.setTitle(Component.translatable("%s.config.category.controls".formatted(MOD_ID)))
				.setDescription(Component.translatable("%s.config.category.controls.description".formatted(MOD_ID)))
				.addValue(getMode());
			screenBuilder.addCategory("keyMappings")
				.setTitle(Component.translatable("%s.config.category.keyMappings".formatted(MOD_ID)))
				.setDescription(Component.translatable("%s.config.category.keyMappings.description".formatted(MOD_ID)))
				.addKeyMappings(List.of(OPEN_SCREEN_KEY, TOGGLE_OVERLAY_KEY));
			screenBuilder.configureCategory("advanced")
				.setTitle(Component.translatable("%s.config.category.advanced".formatted(MOD_ID)))
				.setDescription(Component.translatable("%s.config.category.advanced.description".formatted(MOD_ID)));
		});
		registration.registerScreen(
			Component.translatable("%s.config.screen.title".formatted(MOD_ID)),
			FabricMezzConfigCustomTestPlugin::getSchema
		);
	}

	private static IConfigSchema getSchema() {
		return Objects.requireNonNull(schema, "schema");
	}

	private static IConfigValue<Boolean> getPrimaryEnabled() {
		return Objects.requireNonNull(primaryEnabled, "primaryEnabled");
	}

	private static IConfigValue<Boolean> getSecondaryEnabled() {
		return Objects.requireNonNull(secondaryEnabled, "secondaryEnabled");
	}

	private static IConfigValue<TestColor> getAccentColor() {
		return Objects.requireNonNull(accentColor, "accentColor");
	}

	private static IConfigValue<TestMode> getMode() {
		return Objects.requireNonNull(mode, "mode");
	}

	private enum TestMode {
		SLOW,
		BALANCED,
		FAST
	}

	private enum TestColor {
		RED(0xFFCC3333),
		GREEN(0xFF33AA55),
		BLUE(0xFF4477DD),
		GOLD(0xFFE0AA22);

		private final int argb;

		TestColor(int argb) {
			this.argb = argb;
		}

		public int getArgb() {
			return argb;
		}
	}

	private enum CombinedEnabled {
		OFF(false, false),
		PRIMARY_ONLY(true, false),
		BOTH(true, true);

		private final boolean primary;
		private final boolean secondary;

		CombinedEnabled(boolean primary, boolean secondary) {
			this.primary = primary;
			this.secondary = secondary;
		}

		public static CombinedEnabled from(boolean primary, boolean secondary) {
			if (!primary && !secondary) {
				return OFF;
			}
			if (primary && secondary) {
				return BOTH;
			}
			return PRIMARY_ONLY;
		}
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
				return IDeserializeResult.failure("Text must be between 1 and 40 characters.");
			}
			return IDeserializeResult.success(value);
		}

		@Override
		public boolean isValid(String value) {
			return value != null && !value.isBlank() && value.length() <= 40;
		}

		@Override
		public Optional<List<String>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public Component getLocalizedValueName(String configValueLocalizationKey, String value) {
			return Component.literal(value);
		}

		@Override
		public String getValidValuesDescription() {
			return "Text between 1 and 40 characters";
		}

		@Override
		public ConfigValueEditorType<String> getEditorType() {
			return ConfigValueEditorTypes.getText();
		}
	}

	private enum TestColorSerializer implements IConfigValueEditorSerializer<TestColor> {
		INSTANCE;

		private static final ConfigValueEditorType<TestColor> EDITOR_TYPE = ConfigValueEditorType.create(MOD_ID, "test_color");

		@Override
		public String serialize(TestColor value) {
			return value.name();
		}

		@Override
		public IDeserializeResult<TestColor> deserialize(String string) {
			try {
				return IDeserializeResult.success(TestColor.valueOf(string.trim().toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException e) {
				return IDeserializeResult.failure("Expected one of: " + getValidValuesDescription());
			}
		}

		@Override
		public boolean isValid(TestColor value) {
			return value != null;
		}

		@Override
		public Optional<List<TestColor>> getAllValidValues() {
			return Optional.of(List.of(TestColor.values()));
		}

		@Override
		public Component getLocalizedValueName(String configValueLocalizationKey, TestColor value) {
			String key = "%s.%s".formatted(configValueLocalizationKey, value.name().toLowerCase(Locale.ROOT));
			return Component.translatableWithFallback(key, getDisplayName(value.name()));
		}

		@Override
		public String getValidValuesDescription() {
			return Arrays.toString(TestColor.values());
		}

		@Override
		public ConfigValueEditorType<TestColor> getEditorType() {
			return EDITOR_TYPE;
		}
	}

	private enum CombinedEnabledSerializer implements IConfigValueEditorSerializer<CombinedEnabled> {
		INSTANCE;

		@Override
		public String serialize(CombinedEnabled value) {
			return value.name();
		}

		@Override
		public IDeserializeResult<CombinedEnabled> deserialize(String string) {
			try {
				return IDeserializeResult.success(CombinedEnabled.valueOf(string.trim().toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException e) {
				return IDeserializeResult.failure("Expected one of: " + getValidValuesDescription());
			}
		}

		@Override
		public boolean isValid(CombinedEnabled value) {
			return value != null;
		}

		@Override
		public Optional<List<CombinedEnabled>> getAllValidValues() {
			return Optional.of(List.of(CombinedEnabled.values()));
		}

		@Override
		public Component getLocalizedValueName(String configValueLocalizationKey, CombinedEnabled value) {
			String key = "%s.%s".formatted(configValueLocalizationKey, value.name().toLowerCase(Locale.ROOT));
			return Component.translatableWithFallback(key, getDisplayName(value.name()));
		}

		@Override
		public String getValidValuesDescription() {
			return Arrays.toString(CombinedEnabled.values());
		}

		@Override
		public ConfigValueEditorType<CombinedEnabled> getEditorType() {
			return ConfigValueEditorTypes.getSelection();
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
		public Optional<List<TestMode>> getAllValidValues() {
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
			List<String> errors = new ArrayList<>();
			List<TestMode> values = Arrays.stream(string.split(","))
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.map(ModeSerializer.INSTANCE::deserialize)
				.<TestMode>mapMulti((result, consumer) -> {
					result.getResult().ifPresent(consumer);
					errors.addAll(result.getDiagnostics());
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
		public Optional<List<List<TestMode>>> getAllValidValues() {
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

	private record CombinedEnabledValue(
		IConfigValue<Boolean> primary,
		IConfigValue<Boolean> secondary
	) implements IConfigScreenValue<CombinedEnabled>, IConfigLocalizedValue {
		@Override
		public String getName() {
			return "combinedEnabled";
		}

		@Override
		public String getLocalizationKey() {
			return "%s.config.value.combinedEnabled".formatted(MOD_ID);
		}

		@Override
		public Component getLocalizedName() {
			return Component.translatable("%s.config.value.combinedEnabled".formatted(MOD_ID));
		}

		@Override
		public Component getLocalizedDescription() {
			return Component.translatable("%s.config.value.combinedEnabled.description".formatted(MOD_ID));
		}

		@Override
		public CombinedEnabled getValue() {
			return CombinedEnabled.from(primary.get(), secondary.get());
		}

		@Override
		public CombinedEnabled getDefaultValue() {
			return CombinedEnabled.from(
				primary.getEditorInfo().getDefaultValue(),
				secondary.getEditorInfo().getDefaultValue()
			);
		}

		@Override
		public boolean set(CombinedEnabled value) {
			if (!CombinedEnabledSerializer.INSTANCE.isValid(value)) {
				throw new IllegalArgumentException("Invalid combined enabled value: " + value);
			}
			boolean primaryChanged = primary.set(value.primary);
			boolean secondaryChanged = secondary.set(value.secondary);
			return primaryChanged || secondaryChanged;
		}

		@Override
		public Runnable addListener(Consumer<CombinedEnabled> listener) {
			Runnable unsubscribePrimary = primary.addListener(ignored -> listener.accept(getValue()));
			Runnable unsubscribeSecondary = secondary.addListener(ignored -> listener.accept(getValue()));
			return () -> {
				unsubscribePrimary.run();
				unsubscribeSecondary.run();
			};
		}

		@Override
		public ConfigValueRestartRequirement getRestartRequirement() {
			return ConfigValueRestartRequirement.GAME_RESTART;
		}

		@Override
		public IConfigValueEditorSerializer<CombinedEnabled> getSerializer() {
			return CombinedEnabledSerializer.INSTANCE;
		}
	}

	private static final class TestColorEditor implements IConfigValueEditor<TestColor> {
		private static final int WIDTH = 92;
		private static final int HEIGHT = 18;

		@Override
		public int getControlWidth(IConfigScreenValue<TestColor> configValue, TestColor value) {
			return WIDTH;
		}

		@Override
		public int getControlHeight(IConfigScreenValue<TestColor> configValue, TestColor value) {
			return HEIGHT;
		}

		@Override
		public void draw(
			GuiGraphics guiGraphics,
			Rect2i area,
			IConfigScreenValue<TestColor> configValue,
			TestColor value,
			boolean hovered,
			boolean hasPendingChange
		) {
			guiGraphics.fill(area.getX(), area.getY(), area.getX() + area.getWidth(), area.getY() + area.getHeight(), value.getArgb());
			Font font = Minecraft.getInstance().font;
			Component valueName = ConfigValueLocalization.getValueName(configValue, value);
			guiGraphics.drawString(font, valueName, area.getX() + 4, area.getY() + 5, 0xFFFFFFFF, false);
		}

		@Override
		public Optional<ConfigInfo> getTooltipInfo(
			Rect2i area,
			IConfigScreenValue<TestColor> configValue,
			TestColor value,
			boolean hasPendingChange,
			double mouseX,
			double mouseY
		) {
			return Optional.of(new ConfigInfo(
				Component.translatable("%s.config.value.accentColor.tooltip.title".formatted(MOD_ID)),
				Component.literal("ARGB #%08X".formatted(value.getArgb()))
			));
		}

		@Override
		public Optional<IConfigValuePopup<TestColor>> createPopup(
			Rect2i area,
			IConfigScreenValue<TestColor> configValue,
			TestColor value,
			double mouseX,
			double mouseY,
			int button
		) {
			if (button != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
				return Optional.empty();
			}
			return Optional.of(new TestColorPopup(configValue));
		}
	}

	private record TestColorPopup(
		IConfigScreenValue<TestColor> configValue
	) implements IConfigValuePopup<TestColor> {
		private static final int WIDTH = 104;
		private static final int ROW_HEIGHT = 18;

		@Override
		public int getWidth() {
			return WIDTH;
		}

		@Override
		public int getHeight() {
			return TestColor.values().length * ROW_HEIGHT;
		}

		@Override
		public Optional<TestColor> getHoveredValue(Rect2i area, double mouseX, double mouseY) {
			int relativeY = (int) mouseY - area.getY();
			int index = relativeY / ROW_HEIGHT;
			if (index < 0 || index >= TestColor.values().length) {
				return Optional.empty();
			}
			return Optional.of(TestColor.values()[index]);
		}

		@Override
		public void draw(GuiGraphics guiGraphics, Rect2i area, double mouseX, double mouseY) {
			Font font = Minecraft.getInstance().font;
			for (int i = 0; i < TestColor.values().length; i++) {
				TestColor color = TestColor.values()[i];
				int y = area.getY() + i * ROW_HEIGHT;
				guiGraphics.fill(area.getX(), y, area.getX() + area.getWidth(), y + ROW_HEIGHT, color.getArgb());
				Component valueName = ConfigValueLocalization.getValueName(configValue, color);
				guiGraphics.drawString(font, valueName, area.getX() + 4, y + 5, 0xFFFFFFFF, false);
			}
		}
	}

	private static String getDisplayName(String name) {
		String[] words = name.toLowerCase(Locale.ROOT).split("_+");
		StringBuilder result = new StringBuilder();
		for (String word : words) {
			if (!result.isEmpty()) {
				result.append(' ');
			}
			result.append(Character.toUpperCase(word.charAt(0)));
			result.append(word.substring(1));
		}
		return result.toString();
	}
}
