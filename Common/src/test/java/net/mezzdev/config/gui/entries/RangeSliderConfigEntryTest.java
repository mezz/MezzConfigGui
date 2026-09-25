package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.ConfigValueRange;
import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.gui.api.IConfigRangeValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.config.ConfigGuiOptionsTestUtil;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RangeSliderConfigEntryTest {
	@Test
	@SuppressWarnings("DataFlowIssue")
	void registeredRangeEditorUsesSliderAndPreservesEditsWhenSwitchingToText() {
		ConfigEntryWidgetFactory factory = new ConfigEntryWidgetFactory(ignored -> {}, () -> {}, null, Map.of());
		var value = new TestRangeValue(new TestRangeSerializer(new ConfigValueRange<>(0, 10)));
		NumberDisplayConfigEntry<?> entry = assertInstanceOf(NumberDisplayConfigEntry.class, factory.create(value));
		assertEquals(RangeSliderConfigEntry.class, entry.getActiveEntryType());
		ConfigValueRange<Integer> edited = new ConfigValueRange<>(3, 8);
		entry.resetToDefault();
		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("numberDisplayMode", ConfigGuiOptions.NumberDisplayMode.TEXT_AND_BUTTONS)) {
			assertEquals(TextConfigEntry.class, entry.getActiveEntryType());
			assertEquals(edited, entry.getPendingChange().orElseThrow().value());
		}
		assertEquals(RangeSliderConfigEntry.class, entry.getActiveEntryType());
		assertEquals(edited, entry.getPendingChange().orElseThrow().value());
	}

	@Test
	@SuppressWarnings("DataFlowIssue")
	void wideRangesFallBackToTextInsteadOfCreatingAnImpreciseSlider() {
		ConfigEntryWidgetFactory factory = new ConfigEntryWidgetFactory(ignored -> {}, () -> {}, null, Map.of());
		var value = new TestRangeValue(new TestRangeSerializer(new ConfigValueRange<>(0, 1001)));
		assertInstanceOf(TextConfigEntry.class, factory.create(value));
	}

	private record TestRangeValue(TestRangeSerializer serializer) implements IConfigScreenValue<ConfigValueRange<Integer>> {
		@Override
		public String getName() { return "range"; }
		@Override
		public String getLocalizationKey() { return "test.range"; }
		@Override
		public ConfigValueRange<Integer> getValue() { return new ConfigValueRange<>(2, 5); }
		@Override
		public ConfigValueRange<Integer> getDefaultValue() { return new ConfigValueRange<>(3, 8); }
		@Override
		public boolean set(ConfigValueRange<Integer> value) { throw new AssertionError("Edits should be staged"); }
		@Override
		public Runnable addListener(Consumer<ConfigValueRange<Integer>> listener) { return () -> {}; }
		@Override
		public TestRangeSerializer getSerializer() { return serializer; }
	}

	private record TestRangeSerializer(ConfigValueRange<Integer> bounds) implements IConfigRangeValueEditorSerializer<Integer> {
		@Override
		public ConfigValueRange<Integer> getBounds() { return bounds; }
		@Override
		public String serialize(ConfigValueRange<Integer> value) { return value.min() + ".." + value.max(); }
		@Override
		public IDeserializeResult<ConfigValueRange<Integer>> deserialize(String string) {
			try {
				String[] parts = string.split("\\.\\.", -1);
				if (parts.length == 2) {
					ConfigValueRange<Integer> value = new ConfigValueRange<>(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
					if (isValid(value)) {
						return IDeserializeResult.success(value);
					}
				}
			} catch (NumberFormatException ignored) {
				// Report invalid text through the serializer result.
			}
			return IDeserializeResult.failure("Invalid range");
		}
		@Override
		public boolean isValid(ConfigValueRange<Integer> value) {
			return value.min() >= bounds.min() && value.max() <= bounds.max() && value.min() <= value.max();
		}
		@Override
		public String getValidValuesDescription() { return "min..max"; }
		@Override
		public Component getLocalizedValueName(String key, ConfigValueRange<Integer> value) { return Component.literal(serialize(value)); }
	}
}
