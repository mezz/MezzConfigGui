package net.mezzdev.config.gui.internal;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.ApiStatus;

/** Translation fallback for Minecraft versions predating translatableWithFallback. */
@ApiStatus.Internal
public final class LegacyTranslations {
	private LegacyTranslations() {}
	public static MutableComponent translatableWithFallback(String key, String fallback, Object... arguments) {
		if (Language.getInstance().has(key)) {
			return Component.translatable(key, arguments);
		}
		return Component.translatable(fallback, arguments);
	}
}
