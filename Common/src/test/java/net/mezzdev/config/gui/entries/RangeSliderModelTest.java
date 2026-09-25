package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import org.junit.jupiter.api.Test;

import static net.mezzdev.config.gui.entries.RangeSliderModel.Handle.MAXIMUM;
import static net.mezzdev.config.gui.entries.RangeSliderModel.Handle.MINIMUM;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RangeSliderModelTest {
	@Test
	void movingEitherEndpointStopsAtTheOther() {
		ConfigValueRange<Integer> bounds = new ConfigValueRange<>(0, 10);
		RangeSliderModel<Integer> model = new RangeSliderModel<>(NumberSliderModel.create(bounds).orElseThrow());
		ConfigValueRange<Integer> value = new ConfigValueRange<>(2, 8);
		assertEquals(new ConfigValueRange<>(8, 8), model.move(value, MINIMUM, 1.5));
		assertEquals(new ConfigValueRange<>(2, 2), model.move(value, MAXIMUM, -0.5));

		ConfigValueRange<Integer> overlap = new ConfigValueRange<>(5, 5);
		assertEquals(new ConfigValueRange<>(0, 5), model.move(overlap, MINIMUM, -1));
		assertEquals(new ConfigValueRange<>(5, 10), model.move(overlap, MAXIMUM, 2));
	}

	@Test
	void fineAdjustmentStopsAtSharedBounds() {
		ConfigValueRange<Integer> bounds = new ConfigValueRange<>(0, 10);
		RangeSliderModel<Integer> model = new RangeSliderModel<>(NumberSliderModel.create(bounds).orElseThrow());
		ConfigValueRange<Integer> value = new ConfigValueRange<>(2, 8);
		assertEquals(new ConfigValueRange<>(0, 8), model.step(value, MINIMUM, -100));
		assertEquals(new ConfigValueRange<>(2, 10), model.step(value, MAXIMUM, 100));
		assertEquals(new ConfigValueRange<>(8, 8), model.step(value, MINIMUM, 100));
		assertEquals(new ConfigValueRange<>(2, 2), model.step(value, MAXIMUM, -100));
	}

	@Test
	void largeLongEndpointsRetainPrecisionDuringFineAdjustment() {
		long base = 8_000_000_000_000_000_000L;
		ConfigValueRange<Long> bounds = new ConfigValueRange<>(base, base + 100);
		RangeSliderModel<Long> model = new RangeSliderModel<>(NumberSliderModel.create(bounds).orElseThrow());
		ConfigValueRange<Long> value = new ConfigValueRange<>(base + 10, base + 90);
		assertEquals(new ConfigValueRange<>(base + 11, base + 90), model.step(value, MINIMUM, 1));
		assertEquals(new ConfigValueRange<>(base + 10, base + 89), model.step(value, MAXIMUM, -1));
		assertEquals(new ConfigValueRange<>(base + 90, base + 90), model.step(value, MINIMUM, 100));
	}

	@Test
	void doubleRangesKeepIndependentContinuousEndpoints() {
		ConfigValueRange<Double> bounds = new ConfigValueRange<>(0.0, 1.0);
		RangeSliderModel<Double> model = new RangeSliderModel<>(NumberSliderModel.create(bounds).orElseThrow());
		ConfigValueRange<Double> value = new ConfigValueRange<>(0.25, 0.75);
		assertEquals(new ConfigValueRange<>(0.375, 0.75), model.move(value, MINIMUM, 0.375));
		assertEquals(new ConfigValueRange<>(0.25, 0.625), model.move(value, MAXIMUM, 0.625));
	}
}
