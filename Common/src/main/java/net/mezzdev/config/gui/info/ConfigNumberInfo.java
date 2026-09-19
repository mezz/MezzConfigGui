package net.mezzdev.config.gui.info;

import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.internal.NumberFormatting;
import net.minecraft.network.chat.Component;

/**
 * Readable numeric bounds, including the underlying number type's limits.
 */
public final class ConfigNumberInfo {
	private ConfigNumberInfo() {

	}

	public static Component getValidValuesDescription(IConfigValueSerializer<?> serializer) {
		return serializer.getRange()
			.filter(range -> range.min() instanceof Number && range.max() instanceof Number)
			.map(ConfigNumberInfo::getRange)
			.orElseGet(() -> Component.translatable("mezz_config.config.screen.validValues", serializer.getValidValuesDescription()));
	}

	public static Component getRange(ConfigValueRange<?> range) {
		if (range.min().equals(range.max())) {
			return net.mezzdev.config.gui.internal.LegacyTranslations.translatableWithFallback("mezz_config.config.screen.range.exact", "Range: %s", getBound(range.min()));
		}
		boolean minimumLimit = isTypeLimit(range.min(), false);
		boolean maximumLimit = isTypeLimit(range.max(), true);
		if (minimumLimit && maximumLimit) {
			return net.mezzdev.config.gui.internal.LegacyTranslations.translatableWithFallback("mezz_config.config.screen.range.full", "Range: any");
		}
		if (maximumLimit) {
			return net.mezzdev.config.gui.internal.LegacyTranslations.translatableWithFallback("mezz_config.config.screen.range.atLeast", "Range: >= %s", getBound(range.min()));
		}
		if (minimumLimit) {
			return net.mezzdev.config.gui.internal.LegacyTranslations.translatableWithFallback("mezz_config.config.screen.range.atMost", "Range: <= %s", getBound(range.max()));
		}
		return net.mezzdev.config.gui.internal.LegacyTranslations.translatableWithFallback("mezz_config.config.screen.range", "Range: %s ~ %s",
			getBound(range.min()), getBound(range.max()));
	}

	private static boolean isTypeLimit(Object value, boolean upper) {
		if (value instanceof Byte number) {
			return upper && number == Byte.MAX_VALUE || !upper && number == Byte.MIN_VALUE;
		}
		if (value instanceof Short number) {
			return upper && number == Short.MAX_VALUE || !upper && number == Short.MIN_VALUE;
		}
		if (value instanceof Integer number) {
			return upper && number == Integer.MAX_VALUE || !upper && number == Integer.MIN_VALUE;
		}
		if (value instanceof Long number) {
			return upper && number == Long.MAX_VALUE || !upper && number == Long.MIN_VALUE;
		}
		if (value instanceof Double number) {
			return upper && number >= Double.MAX_VALUE || !upper && number <= -Double.MAX_VALUE;
		}
		if (value instanceof Float number) {
			return upper && number >= Float.MAX_VALUE || !upper && number <= -Float.MAX_VALUE;
		}
		return false;
	}

	private static Component getBound(Object value) {
		if (value instanceof Number number) {
			return Component.literal(NumberFormatting.format(number));
		}
		return Component.literal(String.valueOf(value));
	}
}
