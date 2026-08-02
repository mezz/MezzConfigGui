package net.mezzdev.config.gui.neoforge.config;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

final class NeoForgeConfigValueFactory {
	private NeoForgeConfigValueFactory() {

	}

	public static boolean supports(ModConfigSpec.ConfigValue<?> configValue, ModConfigSpec.ValueSpec valueSpec) {
		Object defaultValue = configValue.getDefault();
		if (defaultValue instanceof Boolean ||
			defaultValue instanceof Integer ||
			defaultValue instanceof String ||
			defaultValue instanceof Long ||
			defaultValue instanceof Double
		) {
			return true;
		}
		if (defaultValue instanceof Enum<?> enumValue) {
			Class<?> enumClass = enumValue.getDeclaringClass();
			return Arrays.stream(enumClass.getEnumConstants())
				.anyMatch(valueSpec::test);
		}
		return false;
	}

	public static Optional<NeoForgeConfigValue<?>> create(
		String modId,
		ModConfig modConfig,
		ModConfigSpec modConfigSpec,
		ModConfigSpec.ConfigValue<?> configValue,
		ModConfigSpec.ValueSpec valueSpec
	) {
		Object defaultValue = configValue.getDefault();
		if (defaultValue instanceof Boolean) {
			return Optional.of(createBoolean(modId, modConfig, modConfigSpec, configValue, valueSpec));
		}
		if (defaultValue instanceof Integer) {
			return Optional.of(createInteger(modId, modConfig, modConfigSpec, configValue, valueSpec));
		}
		if (defaultValue instanceof String) {
			return Optional.of(createString(modId, modConfig, modConfigSpec, configValue, valueSpec));
		}
		if (defaultValue instanceof Long) {
			return Optional.of(createLong(modId, modConfig, modConfigSpec, configValue, valueSpec));
		}
		if (defaultValue instanceof Double) {
			return Optional.of(createDouble(modId, modConfig, modConfigSpec, configValue, valueSpec));
		}
		if (defaultValue instanceof Enum<?> enumValue) {
			return createEnum(modId, modConfig, modConfigSpec, configValue, valueSpec, enumValue);
		}
		return Optional.empty();
	}

	private static NeoForgeConfigValue<Boolean> createBoolean(
		String modId,
		ModConfig modConfig,
		ModConfigSpec modConfigSpec,
		ModConfigSpec.ConfigValue<?> configValue,
		ModConfigSpec.ValueSpec valueSpec
	) {
		return new NeoForgeConfigValue<>(
			modId,
			modConfig,
			modConfigSpec,
			cast(configValue),
			valueSpec,
			NeoForgeBooleanSerializer.INSTANCE
		);
	}

	private static NeoForgeConfigValue<Integer> createInteger(
		String modId,
		ModConfig modConfig,
		ModConfigSpec modConfigSpec,
		ModConfigSpec.ConfigValue<?> configValue,
		ModConfigSpec.ValueSpec valueSpec
	) {
		IntegerRange range = getIntegerRange(valueSpec);
		return new NeoForgeConfigValue<>(
			modId,
			modConfig,
			modConfigSpec,
			cast(configValue),
			valueSpec,
			new NeoForgeIntegerSerializer(range.min(), range.max())
		);
	}

	private static NeoForgeConfigValue<String> createString(
		String modId,
		ModConfig modConfig,
		ModConfigSpec modConfigSpec,
		ModConfigSpec.ConfigValue<?> configValue,
		ModConfigSpec.ValueSpec valueSpec
	) {
		return new NeoForgeConfigValue<>(
			modId,
			modConfig,
			modConfigSpec,
			cast(configValue),
			valueSpec,
			new NeoForgeStringSerializer(valueSpec)
		);
	}

	private static NeoForgeConfigValue<Long> createLong(
		String modId,
		ModConfig modConfig,
		ModConfigSpec modConfigSpec,
		ModConfigSpec.ConfigValue<?> configValue,
		ModConfigSpec.ValueSpec valueSpec
	) {
		LongRange range = getLongRange(valueSpec);
		return new NeoForgeConfigValue<>(
			modId,
			modConfig,
			modConfigSpec,
			cast(configValue),
			valueSpec,
			new NeoForgeLongSerializer(valueSpec, range.min(), range.max())
		);
	}

	private static NeoForgeConfigValue<Double> createDouble(
		String modId,
		ModConfig modConfig,
		ModConfigSpec modConfigSpec,
		ModConfigSpec.ConfigValue<?> configValue,
		ModConfigSpec.ValueSpec valueSpec
	) {
		DoubleRange range = getDoubleRange(valueSpec);
		return new NeoForgeConfigValue<>(
			modId,
			modConfig,
			modConfigSpec,
			cast(configValue),
			valueSpec,
			new NeoForgeDoubleSerializer(valueSpec, range.min(), range.max())
		);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Optional<NeoForgeConfigValue<?>> createEnum(
		String modId,
		ModConfig modConfig,
		ModConfigSpec modConfigSpec,
		ModConfigSpec.ConfigValue<?> configValue,
		ModConfigSpec.ValueSpec valueSpec,
		Enum<?> enumValue
	) {
		Class enumClass = enumValue.getDeclaringClass();
		List validValues = Arrays.stream(enumClass.getEnumConstants())
			.filter(valueSpec::test)
			.toList();
		if (validValues.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new NeoForgeConfigValue<>(
			modId,
			modConfig,
			modConfigSpec,
			cast(configValue),
			valueSpec,
			new NeoForgeEnumSerializer<>(enumClass, validValues)
		));
	}

	private static IntegerRange getIntegerRange(ModConfigSpec.ValueSpec valueSpec) {
		ModConfigSpec.Range<?> range = valueSpec.getRange();
		if (range != null && range.getMin() instanceof Integer min && range.getMax() instanceof Integer max) {
			return new IntegerRange(min, max);
		}
		return new IntegerRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	private static LongRange getLongRange(ModConfigSpec.ValueSpec valueSpec) {
		ModConfigSpec.Range<?> range = valueSpec.getRange();
		if (range != null && range.getMin() instanceof Number min && range.getMax() instanceof Number max) {
			return new LongRange(min.longValue(), max.longValue());
		}
		return new LongRange(Long.MIN_VALUE, Long.MAX_VALUE);
	}

	private static DoubleRange getDoubleRange(ModConfigSpec.ValueSpec valueSpec) {
		ModConfigSpec.Range<?> range = valueSpec.getRange();
		if (range != null && range.getMin() instanceof Number min && range.getMax() instanceof Number max) {
			return new DoubleRange(min.doubleValue(), max.doubleValue());
		}
		return new DoubleRange(-Double.MAX_VALUE, Double.MAX_VALUE);
	}

	public static String getUnsupportedDescription(ModConfigSpec.ConfigValue<?> configValue) {
		Object defaultValue = configValue.getDefault();
		if (defaultValue == null) {
			return "default value is null";
		}
		if (defaultValue instanceof List<?>) {
			return "lists need an element-value policy before they can be auto-adapted";
		}
		return "default value type is %s".formatted(defaultValue.getClass().getName());
	}

	@SuppressWarnings("unchecked")
	private static <T> ModConfigSpec.ConfigValue<T> cast(ModConfigSpec.ConfigValue<?> configValue) {
		return (ModConfigSpec.ConfigValue<T>) configValue;
	}

	private record IntegerRange(
		int min,
		int max
	) {

	}

	private record LongRange(
		long min,
		long max
	) {

	}

	private record DoubleRange(
		double min,
		double max
	) {

	}
}
