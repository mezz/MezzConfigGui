package net.mezzdev.config.gui.info;

import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import net.mezzdev.config.gui.internal.NumberFormatting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigNumberInfoTest {
	@Test
	void typeLimitsHaveReadableNamesWithoutClaimingInfinity() {
		assertEquals("Range: 0 ~ maximum supported", ConfigNumberInfo.getRange(new ConfigValueRange<>(0, Integer.MAX_VALUE)).getString());
		assertEquals("Range: minimum supported ~ maximum supported", ConfigNumberInfo.getRange(new ConfigValueRange<>(Long.MIN_VALUE, Long.MAX_VALUE)).getString());
		assertEquals("Range: 0 ~ maximum supported", ConfigNumberInfo.getRange(new ConfigValueRange<>(0.0, Double.MAX_VALUE)).getString());
	}

	@Test
	void ordinaryAndNearlyMaximumBoundsKeepTheirExactValues() {
		int bound = Integer.MAX_VALUE - 1;
		assertEquals("Range: 0 ~ " + NumberFormatting.format(bound), ConfigNumberInfo.getRange(new ConfigValueRange<>(0, bound)).getString());
		assertEquals("Range: -10 ~ 50", ConfigNumberInfo.getRange(new ConfigValueRange<>(-10, 50)).getString());
	}
}
