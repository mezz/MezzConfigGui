package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.config.ConfigGuiOptionsTestUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumberSliderModelTest {
	@Test
	void mapsIntegerRangeToSliderPositions() {
		NumberSliderModel<Integer> model = NumberSliderModel.create(new ConfigValueRange<>(-10, 30)).orElseThrow();

		assertEquals(0.0, model.getPosition(-10));
		assertEquals(0.5, model.getPosition(10));
		assertEquals(1.0, model.getPosition(30));
		assertEquals(-10, model.getValue(-1.0));
		assertEquals(10, model.getValue(0.5));
		assertEquals(30, model.getValue(2.0));
		assertEquals(9, model.getFineValue(10, -1));
		assertEquals(11, model.getFineValue(10, 1));
		assertEquals(30, model.getFineValue(30, 1));
	}

	@Test
	void mapsLargeLongRangeWithoutOverflow() {
		NumberSliderModel<Long> model = NumberSliderModel.create(
				new ConfigValueRange<>(-8_000_000_000_000_000_000L, 8_000_000_000_000_000_000L)
			)
			.orElseThrow();

		assertEquals(0.0, model.getPosition(-8_000_000_000_000_000_000L));
		assertEquals(0.5, model.getPosition(0L));
		assertEquals(1.0, model.getPosition(8_000_000_000_000_000_000L));
		assertEquals(0L, model.getValue(0.5));
		assertEquals(7_999_999_999_999_999_999L, model.getFineValue(8_000_000_000_000_000_000L, -1));
		assertEquals(8_000_000_000_000_000_000L, model.getFineValue(8_000_000_000_000_000_000L, 1));
	}

	@Test
	void mapsDoubleRangeContinuously() {
		NumberSliderModel<Double> model = NumberSliderModel.create(new ConfigValueRange<>(0.25, 4.25)).orElseThrow();

		assertEquals(0.25, model.getValue(0.0));
		assertEquals(2.25, model.getValue(0.5));
		assertEquals(4.25, model.getValue(1.0));
		assertEquals(0.75, model.getPosition(3.25));
		assertEquals(2.25, model.getFineValue(1.25, 1));
		assertEquals(0.25, model.getFineValue(0.25, -1));
	}

	@Test
	void rejectsRangesThatRepresentUnboundedValues() {
		assertTrue(NumberSliderModel.create(new ConfigValueRange<>(Integer.MIN_VALUE, Integer.MAX_VALUE)).isEmpty());
		assertTrue(NumberSliderModel.create(new ConfigValueRange<>(Long.MIN_VALUE, Long.MAX_VALUE)).isEmpty());
		assertTrue(NumberSliderModel.create(new ConfigValueRange<>(-Double.MAX_VALUE, Double.MAX_VALUE)).isEmpty());
		assertTrue(NumberSliderModel.create(new ConfigValueRange<>(1, 1)).isEmpty());
	}

	@Test
	void mouseWheelDeltasProduceWholeNumberSteps() {
		assertEquals(1, NumberSliderConfigEntry.getScrollSteps(1.0));
		assertEquals(-1, NumberSliderConfigEntry.getScrollSteps(-1.0));
		assertEquals(1, NumberSliderConfigEntry.getScrollSteps(0.25));
		assertEquals(-1, NumberSliderConfigEntry.getScrollSteps(-0.25));
		assertEquals(3, NumberSliderConfigEntry.getScrollSteps(3.0));
		assertEquals(0, NumberSliderConfigEntry.getScrollSteps(0.0));
		assertEquals(0, NumberSliderConfigEntry.getScrollSteps(Double.NaN));
	}

	@Test
	void numberDisplayOptionSelectsSliderOrExistingControl() {
		ConfigEntryWidgetFactory factory = new ConfigEntryWidgetFactory(ignored -> {}, () -> {}, null, Map.of());
		TestIntegerConfigValue value = new TestIntegerConfigValue();

		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue(
			"numberDisplayMode",
			ConfigGuiOptions.NumberDisplayMode.SLIDER
		)) {
			NumberDisplayConfigEntry<?> entry = assertInstanceOf(NumberDisplayConfigEntry.class, factory.create(value));
			assertEquals(NumberSliderConfigEntry.class, entry.getActiveEntryType());
			try (ConfigGuiOptionsTestUtil.OptionOverride textAndButtons = ConfigGuiOptionsTestUtil.setValue(
				"numberDisplayMode",
				ConfigGuiOptions.NumberDisplayMode.TEXT_AND_BUTTONS
			)) {
				assertEquals(IntegerConfigEntry.class, entry.getActiveEntryType());
			}
			assertEquals(NumberSliderConfigEntry.class, entry.getActiveEntryType());
		}
	}

	private static final class TestIntegerConfigValue implements IConfigScreenValue<Integer> {
		private static final TestIntegerSerializer SERIALIZER = new TestIntegerSerializer();
		private int value = 5;

		@Override
		public String getName() {
			return "number";
		}

		@Override
		public String getLocalizationKey() {
			return "test.number";
		}

		@Override
		public Integer getValue() {
			return value;
		}

		@Override
		public Integer getDefaultValue() {
			return 5;
		}

		@Override
		public boolean set(Integer value) {
			if (this.value == value) {
				return false;
			}
			this.value = value;
			return true;
		}

		@Override
		public Runnable addListener(Consumer<Integer> listener) {
			return () -> {};
		}

		@Override
		public IConfigValueSerializer<Integer> getSerializer() {
			return SERIALIZER;
		}
	}

	private static final class TestIntegerSerializer implements IConfigValueSerializer<Integer> {
		@Override
		public String serialize(Integer value) {
			return value.toString();
		}

		@Override
		public IDeserializeResult<Integer> deserialize(String string) {
			return IDeserializeResult.success(Integer.parseInt(string));
		}

		@Override
		public boolean isValid(Integer value) {
			return value >= 0 && value <= 10;
		}

		@Override
		public Optional<ConfigValueRange<Integer>> getRange() {
			return Optional.of(new ConfigValueRange<>(0, 10));
		}

		@Override
		public String getValidValuesDescription() {
			return "An integer from 0 to 10";
		}
	}
}
