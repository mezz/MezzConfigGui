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
		boolean minimumLimit = isTypeLimit(range.min(), false);
		boolean maximumLimit = isTypeLimit(range.max(), true);
		if (minimumLimit && maximumLimit) {
			return Component.translatableWithFallback("mezz_config.config.screen.range.full", "Range: any supported value");
		}
		if (maximumLimit) {
			return Component.translatableWithFallback("mezz_config.config.screen.range.atLeast", "Range: >= %s", getBound(range.min()));
		}
		if (minimumLimit) {
			return Component.translatableWithFallback("mezz_config.config.screen.range.atMost", "Range: <= %s", getBound(range.max()));
		}
		return Component.translatableWithFallback("mezz_config.config.screen.range", "Range: %s ~ %s",
			getBound(range.min()), getBound(range.max()));
	}

	private static boolean isTypeLimit(Object value, boolean upper) {
		return switch (value) {
			case Integer number -> upper && number == Integer.MAX_VALUE || !upper && number == Integer.MIN_VALUE;
			case Long number -> upper && number == Long.MAX_VALUE || !upper && number == Long.MIN_VALUE;
			case Double number -> upper && number == Double.MAX_VALUE || !upper && number == -Double.MAX_VALUE;
			case Float number -> upper && number == Float.MAX_VALUE || !upper && number == -Float.MAX_VALUE;
			default -> false;
		};
	}

	private static Component getBound(Object value) {
		if (value instanceof Number number) {
			return Component.literal(NumberFormatting.format(number));
		}
		return Component.literal(String.valueOf(value));
	}
}
