package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.ConfigValueRange;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;

/**
 * Maps supported bounded number values to and from slider positions.
 */
final class NumberSliderModel<T> {
	private static final MathContext LONG_MATH_CONTEXT = MathContext.DECIMAL128;

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <T> Optional<NumberSliderModel<T>> create(ConfigValueRange<T> range) {
		if (range.min() instanceof Integer && range.max() instanceof Integer) {
			return (Optional) createInteger((ConfigValueRange<Integer>) (Object) range);
		}
		if (range.min() instanceof Long && range.max() instanceof Long) {
			return (Optional) createLong((ConfigValueRange<Long>) (Object) range);
		}
		if (range.min() instanceof Double && range.max() instanceof Double) {
			return (Optional) createDouble((ConfigValueRange<Double>) (Object) range);
		}
		return Optional.empty();
	}

	private static Optional<NumberSliderModel<Integer>> createInteger(ConfigValueRange<Integer> range) {
		int min = range.min();
		int max = range.max();
		if (min >= max || min == Integer.MIN_VALUE || max == Integer.MAX_VALUE) {
			return Optional.empty();
		}
		long span = (long) max - min;
		return Optional.of(new NumberSliderModel<>(
			range,
			value -> ((long) value - min) / (double) span,
			position -> (int) (min + Math.round(position * span)),
			(value, steps) -> (int) Math.clamp((long) value + steps, min, max)
		));
	}

	private static Optional<NumberSliderModel<Long>> createLong(ConfigValueRange<Long> range) {
		long min = range.min();
		long max = range.max();
		if (min >= max || min == Long.MIN_VALUE || max == Long.MAX_VALUE) {
			return Optional.empty();
		}
		BigDecimal decimalMin = BigDecimal.valueOf(min);
		BigDecimal span = BigDecimal.valueOf(max).subtract(decimalMin);
		BigInteger integerMin = BigInteger.valueOf(min);
		BigInteger integerMax = BigInteger.valueOf(max);
		return Optional.of(new NumberSliderModel<>(
			range,
			value -> BigDecimal.valueOf(value)
				.subtract(decimalMin)
				.divide(span, LONG_MATH_CONTEXT)
				.doubleValue(),
			position -> decimalMin
				.add(span.multiply(BigDecimal.valueOf(position), LONG_MATH_CONTEXT))
				.setScale(0, RoundingMode.HALF_UP)
				.longValueExact(),
			(value, steps) -> BigInteger.valueOf(value)
				.add(BigInteger.valueOf(steps))
				.max(integerMin)
				.min(integerMax)
				.longValueExact()
		));
	}

	private static Optional<NumberSliderModel<Double>> createDouble(ConfigValueRange<Double> range) {
		double min = range.min();
		double max = range.max();
		double span = max - min;
		if (!Double.isFinite(min) || !Double.isFinite(max) || !Double.isFinite(span) ||
			min >= max || min == -Double.MAX_VALUE || max == Double.MAX_VALUE
		) {
			return Optional.empty();
		}
		return Optional.of(new NumberSliderModel<>(
			range,
			value -> (value - min) / span,
			position -> Math.fma(span, position, min),
			(value, steps) -> Math.clamp(value + steps, min, max)
		));
	}

	private final ConfigValueRange<T> range;
	private final ToDoubleFunction<T> positionGetter;
	private final DoubleFunction<T> valueGetter;
	private final BiFunction<T, Long, T> fineValueGetter;

	private NumberSliderModel(
		ConfigValueRange<T> range,
		ToDoubleFunction<T> positionGetter,
		DoubleFunction<T> valueGetter,
		BiFunction<T, Long, T> fineValueGetter
	) {
		this.range = range;
		this.positionGetter = positionGetter;
		this.valueGetter = valueGetter;
		this.fineValueGetter = fineValueGetter;
	}

	public ConfigValueRange<T> getRange() {
		return range;
	}

	public double getPosition(T value) {
		return Math.clamp(positionGetter.applyAsDouble(value), 0.0, 1.0);
	}

	public T getValue(double position) {
		double clampedPosition = Math.clamp(position, 0.0, 1.0);
		if (clampedPosition == 0.0) {
			return range.min();
		}
		if (clampedPosition == 1.0) {
			return range.max();
		}
		return valueGetter.apply(clampedPosition);
	}

	public T getFineValue(T value, long steps) {
		return fineValueGetter.apply(value, steps);
	}
}
