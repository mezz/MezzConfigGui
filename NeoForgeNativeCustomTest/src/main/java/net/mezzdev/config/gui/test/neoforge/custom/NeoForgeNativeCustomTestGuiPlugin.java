package net.mezzdev.config.gui.test.neoforge.custom;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.ConfigGuiPlugin;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.ConfigRestartResult;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigScreenValueReference;
import net.mezzdev.config.gui.api.IConfigValueEditor;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
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

	@Override
	public String getModId() {
		return NeoForgeNativeCustomTestMod.MOD_ID;
	}

	@Override
	public void register(IConfigGuiRegistration registration) {
		registration.registerValueEditor(ConfigValueEditorTypes.getSelection(), ignored -> new CyclingSelectionEditor<>());
		registration.configureScreen(screenBuilder -> {
			screenBuilder.setTitle(Component.translatable("%s.configuration.custom.title".formatted(NeoForgeNativeCustomTestMod.MOD_ID)));
			screenBuilder.setRestartHandler(() -> ConfigRestartResult.HANDLED);
			screenBuilder.addCategory("quick")
				.setTitle(Component.translatable("%s.configuration.category.quick".formatted(NeoForgeNativeCustomTestMod.MOD_ID)))
				.setDescription(Component.translatable("%s.configuration.category.quick.tooltip".formatted(NeoForgeNativeCustomTestMod.MOD_ID)))
				.addValueReferences(List.of(
					configValue(NeoForgeNativeCustomTestMod.ENABLED),
					configValue(NeoForgeNativeCustomTestMod.MODE)
				));
			screenBuilder.addCategory("keyMappings")
				.setTitle(Component.translatable("%s.configuration.category.keyMappings".formatted(NeoForgeNativeCustomTestMod.MOD_ID)))
				.setDescription(Component.translatable("%s.configuration.category.keyMappings.tooltip".formatted(NeoForgeNativeCustomTestMod.MOD_ID)))
				.addKeyMapping(OPEN_NATIVE_SCREEN_KEY);
		});
	}

	private static IConfigScreenValueReference configValue(ModConfigSpec.ConfigValue<?> configValue) {
		String name = String.join(".", configValue.getPath());
		return IConfigScreenValueReference.named(name);
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
