package net.mezzdev.config.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageManager;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class ConfigLocale {
	@Nullable
	private static String cachedLocaleCode;
	@Nullable
	private static Locale cachedLocale;

	private ConfigLocale() {

	}

	public static String toLowercase(String string) {
		return string.toLowerCase(getLocale());
	}

	private static Locale getLocale() {
		Minecraft minecraft = Minecraft.getInstance();
		//noinspection ConstantValue
		if (minecraft == null) {
			return Locale.getDefault();
		}
		LanguageManager languageManager = minecraft.getLanguageManager();
		String code = languageManager.getSelected().getCode();
		if (cachedLocale == null || !code.equals(cachedLocaleCode)) {
			cachedLocaleCode = code;
			String[] splitLangCode = code.split("_", 2);
			if (splitLangCode.length == 1) {
				cachedLocale = new Locale(code);
			} else {
				cachedLocale = new Locale(splitLangCode[0], splitLangCode[1]);
			}
		}
		return cachedLocale;
	}
}
