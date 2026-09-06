package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.client.gui.GuiGraphics;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigEntryWidgetListenerTest {

	@Test
	void untouchedWidgetFollowsLiveValueChanges() {
		TestConfigValue configValue = new TestConfigValue("initial");
		TestConfigEntryWidget widget = new TestConfigEntryWidget(configValue);

		widget.subscribeToConfigValue();
		configValue.setExternalValue("reloaded");

		assertEquals("reloaded", widget.getDisplayedValue());
		assertFalse(widget.hasPendingChange());
		assertEquals(1, widget.getValueChangedCount());
	}

	@Test
	void pendingWidgetPreservesItsStagedValueAcrossLiveChanges() {
		TestConfigValue configValue = new TestConfigValue("initial");
		TestConfigEntryWidget widget = new TestConfigEntryWidget(configValue);
		widget.subscribeToConfigValue();
		widget.setDisplayedValue("staged");

		configValue.setExternalValue("reloaded");

		assertEquals("staged", widget.getDisplayedValue());
		assertTrue(widget.hasPendingChange());
	}

	@Test
	void listenerRegistrationFollowsWidgetLifecycle() {
		TestConfigValue configValue = new TestConfigValue("initial");
		TestConfigEntryWidget widget = new TestConfigEntryWidget(configValue);

		widget.subscribeToConfigValue();
		widget.subscribeToConfigValue();
		assertEquals(1, configValue.getListenerCount());

		widget.unsubscribeFromConfigValue();
		widget.unsubscribeFromConfigValue();
		assertEquals(0, configValue.getListenerCount());

		configValue.setExternalValue("while closed");
		assertEquals("initial", widget.getDisplayedValue());

		widget.subscribeToConfigValue();
		assertEquals("while closed", widget.getDisplayedValue());
		assertFalse(widget.hasPendingChange());
	}

	@Test
	void listenerChangesAreDispatchedBeforeUpdatingTheWidget() {
		TestConfigValue configValue = new TestConfigValue("initial");
		Deque<Runnable> clientTasks = new ArrayDeque<>();
		TestConfigEntryWidget widget = new TestConfigEntryWidget(configValue, clientTasks::addLast);
		widget.subscribeToConfigValue();

		configValue.setExternalValue("reloaded");

		assertEquals("initial", widget.getDisplayedValue());
		assertEquals(1, clientTasks.size());
		clientTasks.removeFirst().run();
		assertEquals("reloaded", widget.getDisplayedValue());
	}

	@Test
	void queuedListenerChangeIsIgnoredAfterUnsubscribe() {
		TestConfigValue configValue = new TestConfigValue("initial");
		Deque<Runnable> clientTasks = new ArrayDeque<>();
		TestConfigEntryWidget widget = new TestConfigEntryWidget(configValue, clientTasks::addLast);
		widget.subscribeToConfigValue();
		configValue.setExternalValue("reloaded");

		widget.unsubscribeFromConfigValue();
		clientTasks.removeFirst().run();

		assertEquals("initial", widget.getDisplayedValue());
	}

	private static final class TestConfigEntryWidget extends ConfigEntryWidget<String> {
		private int valueChangedCount;

		private TestConfigEntryWidget(IConfigScreenValue<String> configValue) {
			super(configValue, null);
		}

		private TestConfigEntryWidget(IConfigScreenValue<String> configValue, Consumer<Runnable> clientThreadDispatcher) {
			super(configValue, null, clientThreadDispatcher);
		}

		private String getDisplayedValue() {
			return getValue();
		}

		private boolean setDisplayedValue(String value) {
			return setValue(value);
		}

		private int getValueChangedCount() {
			return valueChangedCount;
		}

		@Override
		protected void onValueChanged() {
			valueChangedCount++;
		}

		@Override
		protected void drawContent(GuiGraphics guiGraphics, double mouseX, double mouseY) {

		}
	}

	private static final class TestConfigValue implements IConfigScreenValue<String> {
		private final List<Consumer<String>> listeners = new ArrayList<>();
		private String value;

		private TestConfigValue(String value) {
			this.value = value;
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public String getLocalizationKey() {
			return "test.value";
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public String getDefaultValue() {
			return "initial";
		}

		@Override
		public boolean set(String value) {
			if (this.value.equals(value)) {
				return false;
			}
			setExternalValue(value);
			return true;
		}

		@Override
		public Runnable addListener(Consumer<String> listener) {
			listeners.add(listener);
			return () -> listeners.remove(listener);
		}

		@Override
		public IConfigValueSerializer<String> getSerializer() {
			return TestSerializer.INSTANCE;
		}

		private void setExternalValue(String value) {
			this.value = value;
			List.copyOf(listeners).forEach(listener -> listener.accept(value));
		}

		private int getListenerCount() {
			return listeners.size();
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
			return value != null;
		}

		@Override
		public Optional<List<String>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public String getValidValuesDescription() {
			return "any string";
		}
	}
}
