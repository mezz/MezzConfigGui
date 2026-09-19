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
	void fullTypeRangesDoNotClaimInfinity() {
		assertEquals("Range: any supported value", ConfigNumberInfo.getRange(new ConfigValueRange<>(Integer.MIN_VALUE, Integer.MAX_VALUE)).getString());
		assertEquals("Range: any supported value", ConfigNumberInfo.getRange(new ConfigValueRange<>(Long.MIN_VALUE, Long.MAX_VALUE)).getString());
		assertEquals("Range: any supported value", ConfigNumberInfo.getRange(new ConfigValueRange<>(-Double.MAX_VALUE, Double.MAX_VALUE)).getString());
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
}
