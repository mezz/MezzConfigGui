package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RangeSliderDragTest {
	@Test
	void eitherSideOfOverlappingHandlesCanDragInEitherDirection() {
		ConfigValueRange<Integer> value = new ConfigValueRange<>(5, 5);
		for (double grabX : new double[]{50.5, 54, 57.5}) {
			for (boolean fineControl : new boolean[]{false, true}) {
				RangeSliderDrag<Integer> left = drag(value, grabX, 100, fineControl);
				RangeSliderDrag<Integer> right = drag(value, grabX, 100, fineControl);
				assertEquals(value, left.update(value, grabX, fineControl));
				double distance = 10;
				if (fineControl) {
					distance = 1;
				}
				assertEquals(new ConfigValueRange<>(4, 5), left.update(value, grabX - distance, fineControl));
				assertEquals(new ConfigValueRange<>(5, 6), right.update(value, grabX + distance, fineControl));
			}
		}
	}

	@Test
	void tinyInitialMovementDoesNotLockOntoTheWrongHandle() {
		ConfigValueRange<Integer> value = new ConfigValueRange<>(5, 5);
		for (boolean fineControl : new boolean[]{false, true}) {
			RangeSliderDrag<Integer> drag = drag(value, 57, 100, fineControl);
			assertEquals(value, drag.update(value, 57.1, fineControl));
			assertEquals(value, drag.update(value, 56.9, fineControl));
			double distance = 10;
			if (fineControl) {
				distance = 1;
			}
			assertEquals(new ConfigValueRange<>(4, 5), drag.update(value, 57 - distance, fineControl));
		}
	}

	@Test
	void partiallyOverlappingHandlesAlsoChooseFromDragDirection() {
		ConfigValueRange<Integer> value = new ConfigValueRange<>(4, 5);
		// On a narrow track these distinct endpoints have overlapping eight-pixel handles.
		for (double grabX : new double[]{20.5, 22, 23.5}) {
			assertEquals(new ConfigValueRange<>(2, 5), drag(value, grabX, 40, false).update(value, grabX - 8, false));
			assertEquals(new ConfigValueRange<>(4, 7), drag(value, grabX, 40, false).update(value, grabX + 8, false));
		}
	}

	@Test
	void draggingPastABoundDoesNotPreventReversingIntoTheRange() {
		ConfigValueRange<Integer> low = new ConfigValueRange<>(0, 0);
		RangeSliderDrag<Integer> lowDrag = drag(low, 4, 100, false);
		assertEquals(low, lowDrag.update(low, -6, false));
		assertEquals(new ConfigValueRange<>(0, 1), lowDrag.update(low, 14, false));

		ConfigValueRange<Integer> high = new ConfigValueRange<>(10, 10);
		RangeSliderDrag<Integer> highDrag = drag(high, 104, 100, false);
		assertEquals(high, highDrag.update(high, 114, false));
		assertEquals(new ConfigValueRange<>(9, 10), highDrag.update(high, 94, false));
	}

	@Test
	void chosenEndpointStaysSelectedAndFineControlTransitionsDoNotJump() {
		ConfigValueRange<Integer> initial = new ConfigValueRange<>(5, 5);
		RangeSliderDrag<Integer> drag = drag(initial, 57, 100, false);
		ConfigValueRange<Integer> expanded = drag.update(initial, 67, false);
		assertEquals(new ConfigValueRange<>(5, 6), expanded);
		assertEquals(expanded, drag.update(expanded, 67, true));
		ConfigValueRange<Integer> fine = drag.update(expanded, 68, true);
		assertEquals(new ConfigValueRange<>(5, 7), fine);
		assertEquals(fine, drag.update(fine, 68, false));
		assertEquals(new ConfigValueRange<>(5, 8), drag.update(fine, 78, false));
		assertEquals(initial, drag.update(fine, 0, false));
	}

	@Test
	void separateHandlesAndTrackClicksKeepTheNearestEndpoint() {
		ConfigValueRange<Integer> value = new ConfigValueRange<>(2, 8);
		assertEquals(new ConfigValueRange<>(8, 8), drag(value, 24, 100, false).update(value, 104, false));
		assertEquals(new ConfigValueRange<>(2, 2), drag(value, 84, 100, false).update(value, 4, false));
		assertEquals(new ConfigValueRange<>(4, 8), drag(value, 44, 100, false).update(value, 44, false));
	}

	private static RangeSliderDrag<Integer> drag(ConfigValueRange<Integer> value, double mouseX, double width, boolean fineControl) {
		ConfigValueRange<Integer> bounds = new ConfigValueRange<>(0, 10);
		RangeSliderModel<Integer> model = new RangeSliderModel<>(NumberSliderModel.create(bounds).orElseThrow());
		return new RangeSliderDrag<>(model, value, mouseX, 4, width, 8, fineControl);
	}
}
