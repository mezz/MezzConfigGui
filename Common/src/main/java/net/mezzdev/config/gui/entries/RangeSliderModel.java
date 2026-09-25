package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.ConfigValueRange;

/** Maps range endpoints to the same scale while preventing either handle from crossing the other. */
final class RangeSliderModel<T> {
	enum Handle {
		MINIMUM,
		MAXIMUM
	}

	private final NumberSliderModel<T> numbers;

	RangeSliderModel(NumberSliderModel<T> numbers) {
		this.numbers = numbers;
	}

	double getPosition(ConfigValueRange<T> value, Handle handle) {
		return numbers.getPosition(getEndpoint(value, handle));
	}

	Handle getNearestHandle(ConfigValueRange<T> value, double position) {
		double min = getPosition(value, Handle.MINIMUM);
		double max = getPosition(value, Handle.MAXIMUM);
		if (position <= (min + max) / 2.0) {
			return Handle.MINIMUM;
		}
		return Handle.MAXIMUM;
	}

	ConfigValueRange<T> move(ConfigValueRange<T> value, Handle handle, double position) {
		return withEndpoint(value, handle, numbers.getValue(position));
	}

	ConfigValueRange<T> step(ConfigValueRange<T> value, Handle handle, long steps) {
		return withEndpoint(value, handle, numbers.getFineValue(getEndpoint(value, handle), steps));
	}

	private ConfigValueRange<T> withEndpoint(ConfigValueRange<T> value, Handle handle, T endpoint) {
		if (handle == Handle.MINIMUM) {
			if (numbers.getPosition(endpoint) > numbers.getPosition(value.max())) {
				endpoint = value.max();
			}
			return new ConfigValueRange<>(endpoint, value.max());
		}
		if (numbers.getPosition(endpoint) < numbers.getPosition(value.min())) {
			endpoint = value.min();
		}
		return new ConfigValueRange<>(value.min(), endpoint);
	}

	private T getEndpoint(ConfigValueRange<T> value, Handle handle) {
		if (handle == Handle.MINIMUM) {
			return value.min();
		}
		return value.max();
	}
}
