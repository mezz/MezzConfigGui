package net.mezzdev.config.gui;

import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.model.AppliedConfigValueChange;
import net.mezzdev.config.gui.model.ConfigValueChange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppliedConfigChangeTrackerTest {
	@Test
	void coalescesMultipleChangesToOriginalValue() {
		AppliedConfigChangeTracker tracker = new AppliedConfigChangeTracker();
		TestConfigValue value = new TestConfigValue("first");

		tracker.add(new AppliedConfigValueChange<>(value, "first", "second"));
		tracker.add(new AppliedConfigValueChange<>(value, "second", "third"));

		List<ConfigValueChange<?>> undoChanges = tracker.getUndoChanges();
		assertEquals(1, undoChanges.size());
		assertEquals("first", undoChanges.getFirst().value());
	}

	@Test
	void removesChangeWhenValueReturnsToOriginal() {
		AppliedConfigChangeTracker tracker = new AppliedConfigChangeTracker();
		TestConfigValue value = new TestConfigValue("first");

		tracker.add(new AppliedConfigValueChange<>(value, "first", "second"));
		tracker.add(new AppliedConfigValueChange<>(value, "second", "first"));

		assertFalse(tracker.hasChanges());
		assertTrue(tracker.getUndoChanges().isEmpty());
	}

	@Test
	void tracksValuesByIdentity() {
		AppliedConfigChangeTracker tracker = new AppliedConfigChangeTracker();
		TestConfigValue firstValue = new TestConfigValue("first");
		TestConfigValue secondValue = new TestConfigValue("first");

		tracker.add(new AppliedConfigValueChange<>(firstValue, "first", "second"));
		tracker.add(new AppliedConfigValueChange<>(secondValue, "first", "third"));

		assertEquals(2, tracker.getUndoChanges().size());
	}

	@Test
	void coalescesDecoratorsByStableIdentityKey() {
		AppliedConfigChangeTracker tracker = new AppliedConfigChangeTracker();
		TestConfigValue value = new TestConfigValue("first");
		IConfigScreenValue<String> decoratedValue = IConfigScreenValue.withApplyMode(
			value,
			ConfigValueApplyMode.IMMEDIATE
		);

		tracker.add(new AppliedConfigValueChange<>(value, "first", "second"));
		tracker.add(new AppliedConfigValueChange<>(decoratedValue, "second", "third"));

		List<ConfigValueChange<?>> undoChanges = tracker.getUndoChanges();
		assertEquals(1, undoChanges.size());
		assertEquals("first", undoChanges.getFirst().value());
	}

	@Test
	void undoesChangesInReverseApplicationOrder() {
		AppliedConfigChangeTracker tracker = new AppliedConfigChangeTracker();
		TestConfigValue firstValue = new TestConfigValue("first");
		TestConfigValue secondValue = new TestConfigValue("second");

		tracker.add(new AppliedConfigValueChange<>(firstValue, "first", "first changed"));
		tracker.add(new AppliedConfigValueChange<>(secondValue, "second", "second changed"));

		List<ConfigValueChange<?>> undoChanges = tracker.getUndoChanges();
		assertEquals(List.of(secondValue, firstValue), undoChanges.stream().map(ConfigValueChange::configValue).toList());
	}

	@Test
	void ordersCoalescedChangeByItsMostRecentApplication() {
		AppliedConfigChangeTracker tracker = new AppliedConfigChangeTracker();
		TestConfigValue firstValue = new TestConfigValue("first");
		TestConfigValue secondValue = new TestConfigValue("second");

		tracker.add(new AppliedConfigValueChange<>(firstValue, "first", "first changed"));
		tracker.add(new AppliedConfigValueChange<>(secondValue, "second", "second changed"));
		tracker.add(new AppliedConfigValueChange<>(firstValue, "first changed", "first changed again"));

		List<ConfigValueChange<?>> undoChanges = tracker.getUndoChanges();
		assertEquals(List.of(firstValue, secondValue), undoChanges.stream().map(ConfigValueChange::configValue).toList());
		assertEquals("first", undoChanges.getFirst().value());
	}

	private static final class TestConfigValue implements IConfigScreenValue<String> {
		private final String defaultValue;

		private TestConfigValue(String defaultValue) {
			this.defaultValue = defaultValue;
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public String getLocalizationKey() {
			return "test";
		}

		@Override
		public String getValue() {
			return defaultValue;
		}

		@Override
		public String getDefaultValue() {
			return defaultValue;
		}

		@Override
		public boolean set(String value) {
			return false;
		}

		@Override
		public Runnable addListener(Consumer<String> listener) {
			return () -> {};
		}

		@Override
		public IConfigValueSerializer<String> getSerializer() {
			return TestSerializer.INSTANCE;
		}
	}

	private enum TestSerializer implements IConfigValueSerializer<String> {
		INSTANCE;

		@Override
		public String serialize(String value) {
			return value;
		}

		@Override
		public IDeserializeResult<String> deserialize(String string) {
			return IDeserializeResult.success(string);
		}

		@Override
		public boolean isValid(String value) {
			return true;
		}

		@Override
		public Optional<List<String>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public String getValidValuesDescription() {
			return "";
		}
	}
}
