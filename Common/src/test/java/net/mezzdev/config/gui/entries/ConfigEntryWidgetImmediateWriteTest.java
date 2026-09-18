package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.gui.ConfigValueAccess;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.minecraft.client.gui.GuiGraphics;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigEntryWidgetImmediateWriteTest {
	@Test
	void nativeReadOnlyPolicySurvivesApplyModeAndRestartWrappers() {
		TestConfigValue configValue = new TestConfigValue("initial", false);
		configValue.editable = false;
		IConfigScreenValue<String> wrapped = IConfigScreenValue.withRestartRequirement(
			IConfigScreenValue.withApplyMode(configValue, ConfigValueApplyMode.ON_APPLY),
			ConfigValueRestartRequirement.GAME_RESTART
		);
		TestConfigEntryWidget widget = new TestConfigEntryWidget(wrapped);

		assertFalse(widget.isEditable());
		assertFalse(widget.setDisplayedValue("changed"));
		assertEquals("initial", configValue.getStoredValue());
		assertTrue(widget.getPendingChange().isEmpty());
		configValue.editable = true;
		assertTrue(widget.isEditable());
	}

	@Test
	void commitsDisplayedValueOnlyAfterImmediateWriteSucceeds() {
		TestConfigValue configValue = new TestConfigValue("initial", false);
		TestConfigEntryWidget widget = new TestConfigEntryWidget(configValue);
		configValue.setBeforeWrite(() -> assertEquals("initial", widget.getDisplayedValue()));
		widget.setImmediateChangeHandler(change -> change.apply());
		widget.subscribeToConfigValue();

		assertTrue(widget.setDisplayedValue("changed"));

		assertEquals("changed", widget.getDisplayedValue());
		assertEquals("changed", configValue.getStoredValue());
		assertEquals(1, widget.getValueChangedCount());
	}

	@Test
	void restoresStoredValueAndReportsImmediateWriteFailure() {
		TestConfigValue configValue = new TestConfigValue("initial", true);
		TestConfigEntryWidget widget = new TestConfigEntryWidget(configValue);
		RuntimeException[] failure = new RuntimeException[1];
		widget.setImmediateChangeHandler(change -> {
			try {
				return change.apply();
			} catch (RuntimeException exception) {
				failure[0] = exception;
				return false;
			}
		});
		int readsBeforeWrite = configValue.getValueReadCount();

		assertFalse(widget.setDisplayedValue("rejected"));

		assertEquals("save failed", failure[0].getMessage());
		assertEquals("initial", widget.getDisplayedValue());
		assertEquals("initial", configValue.getStoredValue());
		assertEquals(readsBeforeWrite, configValue.getValueReadCount());
		assertEquals(0, widget.getValueChangedCount());
	}

	@Test
	void readOnlyWidgetRejectsEditsWithoutWriting() {
		TestConfigValue configValue = new TestConfigValue("initial", false);
		TestConfigEntryWidget widget = new TestConfigEntryWidget(configValue);
		widget.setEditableSupplier(() -> false);

		assertFalse(widget.setDisplayedValue("changed"));

		assertEquals("initial", widget.getDisplayedValue());
		assertEquals("initial", configValue.getStoredValue());
	}

	private static final class TestConfigEntryWidget extends ConfigEntryWidget<String> {
		private int valueChangedCount;

		private TestConfigEntryWidget(IConfigScreenValue<String> configValue) {
			super(configValue, null);
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

	private static final class TestConfigValue implements IConfigScreenValue<String>, ConfigValueAccess {
		private boolean editable = true;
		private final boolean fails;
		private final List<Consumer<String>> listeners = new ArrayList<>();
		private String value;
		private int valueReadCount;
		private Runnable beforeWrite = () -> {};

		private TestConfigValue(String value, boolean fails) {
			this.value = value;
			this.fails = fails;
		}

		@Override
		public boolean isEditable() {
			return editable;
		}

		private void setBeforeWrite(Runnable beforeWrite) {
			this.beforeWrite = beforeWrite;
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
			valueReadCount++;
			return value;
		}

		private String getStoredValue() {
			return value;
		}

		private int getValueReadCount() {
			return valueReadCount;
		}

		@Override
		public String getDefaultValue() {
			return "initial";
		}

		@Override
		public boolean set(String value) {
			beforeWrite.run();
			if (fails) {
				throw new IllegalStateException("save failed");
			}
			if (this.value.equals(value)) {
				return false;
			}
			this.value = value;
			List.copyOf(listeners).forEach(listener -> listener.accept(value));
			return true;
		}

		@Override
		public Runnable addListener(Consumer<String> listener) {
			listeners.add(listener);
			return () -> listeners.remove(listener);
		}

		@Override
		public ConfigValueApplyMode getApplyMode() {
			return ConfigValueApplyMode.IMMEDIATE;
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
