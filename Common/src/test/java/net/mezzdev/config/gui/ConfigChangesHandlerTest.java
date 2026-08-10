package net.mezzdev.config.gui;

import net.mezzdev.config.api.value.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.api.value.IConfigValueSerializer;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.model.AppliedConfigValueChange;
import net.mezzdev.config.gui.model.ConfigValueChange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConfigChangesHandlerTest {
	@Test
	void stopsAtFailureAndReportsOnlySuccessfulChanges() {
		TestConfigValue first = new TestConfigValue("first", ConfigValueRestartRequirement.WORLD_RESTART, false);
		TestConfigValue failing = new TestConfigValue("failing", ConfigValueRestartRequirement.GAME_RESTART, true);
		TestConfigValue unattempted = new TestConfigValue("unattempted", ConfigValueRestartRequirement.GAME_RESTART, false);

		ConfigChangesResult result = ConfigChangesHandler.applySequentially(List.of(
			new ConfigValueChange<>(first, "first changed"),
			new ConfigValueChange<>(failing, "failing changed"),
			new ConfigValueChange<>(unattempted, "unattempted changed")
		));

		assertFalse(result.succeeded());
		assertEquals(ConfigValueRestartRequirement.WORLD_RESTART, result.restartRequirement());
		assertEquals("first changed", first.getValue());
		assertEquals("failing", failing.getValue());
		assertEquals("unattempted", unattempted.getValue());
		AppliedConfigValueChange<?> appliedChange = result.appliedChanges().getFirst();
		assertEquals("first", appliedChange.oldValue());
		assertEquals("first changed", appliedChange.newValue());
		assertSame(failing, result.failure().orElseThrow().change().configValue());
	}

	private static final class TestConfigValue implements IConfigScreenValue<String> {
		private final String name;
		private final ConfigValueRestartRequirement restartRequirement;
		private final boolean fails;
		private String value;

		private TestConfigValue(String value, ConfigValueRestartRequirement restartRequirement, boolean fails) {
			this.name = value;
			this.value = value;
			this.restartRequirement = restartRequirement;
			this.fails = fails;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getLocalizationKey() {
			return name;
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public String getDefaultValue() {
			return name;
		}

		@Override
		public boolean set(String value) {
			if (fails) {
				throw new IllegalStateException("schema is no longer active");
			}
			if (Objects.equals(this.value, value)) {
				return false;
			}
			this.value = value;
			return true;
		}

		@Override
		public Runnable addListener(Consumer<String> listener) {
			return () -> {};
		}

		@Override
		public ConfigValueRestartRequirement getRestartRequirement() {
			return restartRequirement;
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
			return "any non-null string";
		}
	}
}
