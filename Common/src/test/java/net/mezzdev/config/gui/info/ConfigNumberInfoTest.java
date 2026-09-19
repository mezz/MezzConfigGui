package net.mezzdev.config.gui.info;

import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import net.mezzdev.config.gui.internal.NumberFormatting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigNumberInfoTest {
	@Test
	void upperTypeLimitsUseAnInclusiveLowerBound() {
		assertEquals("Range: >= 0", ConfigNumberInfo.getRange(new ConfigValueRange<>(0, Integer.MAX_VALUE)).getString());
		assertEquals("Range: >= 0", ConfigNumberInfo.getRange(new ConfigValueRange<>(0L, Long.MAX_VALUE)).getString());
		assertEquals("Range: >= 0", ConfigNumberInfo.getRange(new ConfigValueRange<>(0.0, Double.MAX_VALUE)).getString());
		assertEquals("Range: >= 0", ConfigNumberInfo.getRange(new ConfigValueRange<>(0.0f, Float.MAX_VALUE)).getString());
	}

	@Test
	void lowerTypeLimitsUseAnInclusiveUpperBound() {
		assertEquals("Range: <= 50", ConfigNumberInfo.getRange(new ConfigValueRange<>(Integer.MIN_VALUE, 50)).getString());
		assertEquals("Range: <= 50", ConfigNumberInfo.getRange(new ConfigValueRange<>(Long.MIN_VALUE, 50L)).getString());
		assertEquals("Range: <= 50", ConfigNumberInfo.getRange(new ConfigValueRange<>(-Double.MAX_VALUE, 50.0)).getString());
		assertEquals("Range: <= 50", ConfigNumberInfo.getRange(new ConfigValueRange<>(-Float.MAX_VALUE, 50.0f)).getString());
	}

	@Test
	void fullTypeRangesAllowAnyValue() {
		assertEquals("Range: any", ConfigNumberInfo.getRange(new ConfigValueRange<>(Byte.MIN_VALUE, Byte.MAX_VALUE)).getString());
		assertEquals("Range: any", ConfigNumberInfo.getRange(new ConfigValueRange<>(Short.MIN_VALUE, Short.MAX_VALUE)).getString());
		assertEquals("Range: any", ConfigNumberInfo.getRange(new ConfigValueRange<>(Integer.MIN_VALUE, Integer.MAX_VALUE)).getString());
		assertEquals("Range: any", ConfigNumberInfo.getRange(new ConfigValueRange<>(Long.MIN_VALUE, Long.MAX_VALUE)).getString());
		assertEquals("Range: any", ConfigNumberInfo.getRange(new ConfigValueRange<>(-Double.MAX_VALUE, Double.MAX_VALUE)).getString());
		assertEquals("Range: any", ConfigNumberInfo.getRange(new ConfigValueRange<>(-Float.MAX_VALUE, Float.MAX_VALUE)).getString());
	}

	@Test
	void oneSidedBoundsKeepLocalizedNumberFormatting() {
		assertEquals("Range: >= " + NumberFormatting.format(10000), ConfigNumberInfo.getRange(new ConfigValueRange<>(10000, Integer.MAX_VALUE)).getString());
		assertEquals("Range: <= " + NumberFormatting.format(10000), ConfigNumberInfo.getRange(new ConfigValueRange<>(Integer.MIN_VALUE, 10000)).getString());
	}

	@Test
	void ordinaryAndNearlyMaximumBoundsKeepTheirExactValues() {
		int bound = Integer.MAX_VALUE - 1;
		assertEquals("Range: 0 ~ " + NumberFormatting.format(bound), ConfigNumberInfo.getRange(new ConfigValueRange<>(0, bound)).getString());
		assertEquals("Range: -10 ~ 50", ConfigNumberInfo.getRange(new ConfigValueRange<>(-10, 50)).getString());
	}

	@Test
	void infiniteBoundsUseTheSameSimpleRangeLabels() {
		assertEquals("Range: any", ConfigNumberInfo.getRange(new ConfigValueRange<>(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)).getString());
		assertEquals("Range: any", ConfigNumberInfo.getRange(new ConfigValueRange<>(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)).getString());
		assertEquals("Range: >= 0", ConfigNumberInfo.getRange(new ConfigValueRange<>(0.0, Double.POSITIVE_INFINITY)).getString());
		assertEquals("Range: <= 50", ConfigNumberInfo.getRange(new ConfigValueRange<>(Float.NEGATIVE_INFINITY, 50.0f)).getString());
	}

	@Test
	void singleValueRangesKeepTheirExactValueEvenAtTypeLimits() {
		for (int value : new int[]{Integer.MIN_VALUE, 0, 50, Integer.MAX_VALUE}) {
			assertEquals("Range: " + NumberFormatting.format(value), ConfigNumberInfo.getRange(new ConfigValueRange<>(value, value)).getString());
		}
		assertEquals("Range: " + NumberFormatting.format(Double.MAX_VALUE), ConfigNumberInfo.getRange(new ConfigValueRange<>(Double.MAX_VALUE, Double.MAX_VALUE)).getString());
	}

	@Test
	void smallPositiveFloatingPointBoundsAreNotConfusedWithLowerTypeLimits() {
		assertEquals("Range: >= " + NumberFormatting.format(Double.MIN_VALUE), ConfigNumberInfo.getRange(new ConfigValueRange<>(Double.MIN_VALUE, Double.MAX_VALUE)).getString());
		assertEquals("Range: >= " + NumberFormatting.format(Float.MIN_VALUE), ConfigNumberInfo.getRange(new ConfigValueRange<>(Float.MIN_VALUE, Float.MAX_VALUE)).getString());
	}
}
