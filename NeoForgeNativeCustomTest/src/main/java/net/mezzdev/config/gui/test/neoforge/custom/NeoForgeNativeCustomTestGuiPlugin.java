package net.mezzdev.config.gui.test.neoforge.custom;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.api.value.ConfigValueRestartRequirement;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.ConfigGuiPlugin;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import net.mezzdev.config.gui.api.IConfigScreenCategoryBuilder;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

@ConfigGuiPlugin
public final class NeoForgeNativeCustomTestGuiPlugin implements IConfigGuiPlugin {
	private static final KeyMapping OPEN_NATIVE_SCREEN_KEY = new KeyMapping(
		"key.%s.openNativeScreen".formatted(NeoForgeNativeCustomTestMod.MOD_ID),
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_J,
		"key.categories.%s".formatted(NeoForgeNativeCustomTestMod.MOD_ID)
	);
	private static final KeyMapping TOGGLE_NATIVE_OVERLAY_KEY = new KeyMapping(
		"key.%s.toggleNativeOverlay".formatted(NeoForgeNativeCustomTestMod.MOD_ID),
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_O,
		"key.categories.%s".formatted(NeoForgeNativeCustomTestMod.MOD_ID)
	);

	@Override
	public String getModId() {
		return NeoForgeNativeCustomTestMod.MOD_ID;
	}

	@Override
	public void register(IConfigGuiRegistration registration) {
		registration.registerValueEditor(ConfigValueEditorTypes.getSelection(), ignored -> new CyclingSelectionEditor<>());
		registration.configureScreen(screenBuilder -> {
			screenBuilder.setTitle(Component.translatable("%s.configuration.custom.title".formatted(NeoForgeNativeCustomTestMod.MOD_ID)));
			IConfigScreenCategoryBuilder quickCategory = screenBuilder.addCategory("quick")
				.setTitle(Component.translatable("%s.configuration.category.quick".formatted(NeoForgeNativeCustomTestMod.MOD_ID)))
				.setDescription(Component.translatable("%s.configuration.category.quick.tooltip".formatted(NeoForgeNativeCustomTestMod.MOD_ID)))
				.setDefaultApplyMode(ConfigValueApplyMode.IMMEDIATE);
			quickCategory.getValueBuilderByName(configValueName(NeoForgeNativeCustomTestMod.MODE))
				.setApplyMode(ConfigValueApplyMode.ON_APPLY)
				.setRestartRequirement(ConfigValueRestartRequirement.GAME_RESTART);
			quickCategory.addValuesByName(List.of(
				configValueName(NeoForgeNativeCustomTestMod.ENABLED),
				configValueName(NeoForgeNativeCustomTestMod.MODE),
				configValueName(NeoForgeNativeCustomTestMod.ROW_COUNT)
			));

			IConfigScreenCategoryBuilder listsCategory = screenBuilder.addCategory("lists")
				.setTitle(Component.translatable("%s.configuration.category.lists".formatted(NeoForgeNativeCustomTestMod.MOD_ID)))
				.setDescription(Component.translatable("%s.configuration.category.lists.tooltip".formatted(NeoForgeNativeCustomTestMod.MOD_ID)))
				.setDefaultApplyMode(ConfigValueApplyMode.ON_APPLY);
			listsCategory.getValueBuilderByName(configValueName(NeoForgeNativeCustomTestMod.ALIASES))
				.setApplyMode(ConfigValueApplyMode.IMMEDIATE);
			listsCategory.getValueBuilderByName(configValueName(NeoForgeNativeCustomTestMod.OPACITY_STEPS))
				.setRestartRequirement(ConfigValueRestartRequirement.GAME_RESTART);
			listsCategory.addValuesByName(List.of(
				configValueName(NeoForgeNativeCustomTestMod.ENABLED_HISTORY),
				configValueName(NeoForgeNativeCustomTestMod.FAVORITE_ROWS),
				configValueName(NeoForgeNativeCustomTestMod.FAVORITE_MODES),
				configValueName(NeoForgeNativeCustomTestMod.ALIASES),
				configValueName(NeoForgeNativeCustomTestMod.CACHE_BREAKPOINTS),
				configValueName(NeoForgeNativeCustomTestMod.OPACITY_STEPS)
			));

			screenBuilder.addCategory("keyMappings")
				.setTitle(Component.translatable("%s.configuration.category.keyMappings".formatted(NeoForgeNativeCustomTestMod.MOD_ID)))
				.setDescription(Component.translatable("%s.configuration.category.keyMappings.tooltip".formatted(NeoForgeNativeCustomTestMod.MOD_ID)))
				.addKeyMapping(OPEN_NATIVE_SCREEN_KEY)
				.addKeyMappings(List.of(TOGGLE_NATIVE_OVERLAY_KEY));

			IConfigScreenCategoryBuilder clientCategory = screenBuilder.configureCategory(NeoForgeNativeCustomTestMod.CLIENT_FILE_NAME)
				.setTitle(Component.translatable("%s.configuration.category.native".formatted(NeoForgeNativeCustomTestMod.MOD_ID)))
				.setDescription(Component.translatable("%s.configuration.category.native.tooltip".formatted(NeoForgeNativeCustomTestMod.MOD_ID)))
				.setDefaultApplyMode(ConfigValueApplyMode.ON_APPLY);
			clientCategory.getValueBuilderByName(configValueName(NeoForgeNativeCustomTestMod.EXTRA_EFFECTS))
				.setApplyMode(ConfigValueApplyMode.IMMEDIATE);
			clientCategory.getValueBuilderByName(configValueName(NeoForgeNativeCustomTestMod.LABEL))
				.setRestartRequirement(ConfigValueRestartRequirement.GAME_RESTART);
			clientCategory.hideValuesByName(List.of(
				configValueName(NeoForgeNativeCustomTestMod.ENABLED),
				configValueName(NeoForgeNativeCustomTestMod.SECRET_DIAGNOSTICS),
				configValueName(NeoForgeNativeCustomTestMod.MODE),
				configValueName(NeoForgeNativeCustomTestMod.ROW_COUNT),
				configValueName(NeoForgeNativeCustomTestMod.ENABLED_HISTORY),
				configValueName(NeoForgeNativeCustomTestMod.FAVORITE_ROWS),
				configValueName(NeoForgeNativeCustomTestMod.FAVORITE_MODES),
				configValueName(NeoForgeNativeCustomTestMod.ALIASES),
				configValueName(NeoForgeNativeCustomTestMod.CACHE_BREAKPOINTS),
				configValueName(NeoForgeNativeCustomTestMod.OPACITY_STEPS)
			));

			IConfigScreenCategoryBuilder commonCategory = screenBuilder.configureCategory(NeoForgeNativeCustomTestMod.COMMON_FILE_NAME)
				.setTitle(Component.translatable("%s.configuration.category.common".formatted(NeoForgeNativeCustomTestMod.MOD_ID)))
				.setDescription(Component.translatable("%s.configuration.category.common.tooltip".formatted(NeoForgeNativeCustomTestMod.MOD_ID)))
				.setDefaultApplyMode(ConfigValueApplyMode.ON_APPLY);
			commonCategory.getValueBuilderByName(configValueName(NeoForgeNativeCustomTestMod.COMMON_ENABLED))
				.setApplyMode(ConfigValueApplyMode.IMMEDIATE);
			commonCategory.getValueBuilderByName(configValueName(NeoForgeNativeCustomTestMod.COMMON_CACHE_BUDGET))
				.setRestartRequirement(ConfigValueRestartRequirement.GAME_RESTART);
		});
	}

	private static String configValueName(ModConfigSpec.ConfigValue<?> configValue) {
		return String.join(".", configValue.getPath());
	}

	private static final class CyclingSelectionEditor<T> implements IConfigValueEditor<T> {
		private static final int WIDTH = 96;
		private static final int HEIGHT = 18;

		@Override
		public int getControlWidth(IConfigScreenValue<T> configValue, T value) {
			return WIDTH;
		}

		@Override
		public int getControlHeight(IConfigScreenValue<T> configValue, T value) {
			return HEIGHT;
		}

		@Override
		public void draw(
			GuiGraphics guiGraphics,
			Rect2i area,
			IConfigScreenValue<T> configValue,
			T value,
			boolean hovered,
			boolean hasPendingChange
		) {
			int backgroundColor = getBackgroundColor(hovered);
			guiGraphics.fill(area.getX(), area.getY(), area.getX() + area.getWidth(), area.getY() + area.getHeight(), backgroundColor);
			Font font = Minecraft.getInstance().font;
			Component valueName = ConfigValueLocalization.getValueName(configValue, value);
			guiGraphics.drawString(font, valueName, area.getX() + 4, area.getY() + 5, 0xFFFFFFFF, false);
		}

		private static int getBackgroundColor(boolean hovered) {
			if (hovered) {
				return 0xFF345C7C;
			}
			return 0xFF24384A;
		}

		@Override
		public Optional<ConfigInfo> getTooltipInfo(
			Rect2i area,
			IConfigScreenValue<T> configValue,
			T value,
			boolean hasPendingChange,
			double mouseX,
			double mouseY
		) {
			return Optional.of(new ConfigInfo(
				Component.translatable("%s.configuration.customSelection.tooltip.title".formatted(NeoForgeNativeCustomTestMod.MOD_ID)),
				Component.translatable("%s.configuration.customSelection.tooltip.line".formatted(NeoForgeNativeCustomTestMod.MOD_ID))
			));
		}

		@Override
		public Optional<T> getClickedValue(
			Rect2i area,
			IConfigScreenValue<T> configValue,
			T value,
			double mouseX,
			double mouseY,
			int button
		) {
			if (button != 0) {
				return Optional.empty();
			}
			List<T> values = configValue.getSerializer()
				.getAllValidValues()
				.map(List::copyOf)
				.orElseGet(List::of);
			int index = values.indexOf(value);
			if (index < 0 || values.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(values.get((index + 1) % values.size()));
		}
	}
}
