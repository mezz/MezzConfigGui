package net.mezzdev.config.gui.config;

import net.mezzdev.config.gui.api.ConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.mezzdev.config.gui.api.IConfigGuiRegistration;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import net.minecraft.network.chat.Component;

/**
 * Registers MezzConfig GUI's own user-facing options.
 */
@ConfigGuiPlugin
public final class ConfigGuiOptionsPlugin implements IConfigGuiPlugin {
	@Override
	public String getModId() {
		return ConfigGuiOptions.MOD_ID;
	}

	@Override
	public void register(IConfigGuiRegistration registration) {
		registration.registerScreen(
			Component.translatable("mezz_config_gui.config.screen.title"),
			ConfigGuiOptions::getSchema
		);
		registration.configureScreen(screen -> {
			ConfigGuiOptions.getModNavigationConfig().configureCategory(screen.configureCategory("modList"));
			var value = ConfigGuiOptions.getNavigationDepthValue();
			var metadata = registration.getEditorMetadataFactory().createRange(
				value.getEditorInfo().getSerializer(), new ConfigValueRange<>(0, NavigationDepthRangeSerializer.MAX_DEPTH));
			screen.configureCategory("navigation").getValueBuilder(value)
				.hide()
				.insertBefore(IConfigScreenValue.configValue(value, metadata));
		});
	}
}
