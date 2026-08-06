package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.api.value.IDeserializeResult;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditorSerializer;
import net.mezzdev.config.gui.config.ConfigGuiOptionsTestUtil;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyMappingConfigEntryTest {
	@Test
	void conflictDetailsOptionControlsConflictLookup() {
		TestConfigKeyMapping keyMapping = new TestConfigKeyMapping();
		KeyMappingConfigEntry entry = new KeyMappingConfigEntry(new TestScreenValue(keyMapping), null);

		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("showKeyConflictDetails", false)) {
			assertEquals(List.of(), getConflicts(entry));
			assertEquals(0, keyMapping.conflictLookupCount);
		}

		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("showKeyConflictDetails", true)) {
			assertEquals(1, getConflicts(entry).size());
			assertEquals(1, keyMapping.conflictLookupCount);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<ConfigKeyMappingConflict> getConflicts(KeyMappingConfigEntry entry) {
		try {
			Method method = KeyMappingConfigEntry.class.getDeclaredMethod("getConflicts");
			method.setAccessible(true);
			return (List<ConfigKeyMappingConflict>) method.invoke(entry);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Failed to invoke getConflicts.", e);
		}
	}

	private static final class TestScreenValue implements IConfigScreenValue<KeyMappingValue> {
		private final TestConfigKeyMapping keyMapping;
		private final KeyMappingValue defaultValue;
		private KeyMappingValue value;

		private TestScreenValue(TestConfigKeyMapping keyMapping) {
			this.keyMapping = keyMapping;
			this.defaultValue = new KeyMappingValue(keyMapping.getDefaultValue(), keyMapping);
			this.value = new KeyMappingValue(keyMapping.getValue(), keyMapping);
		}

		@Override
		public String getName() {
			return keyMapping.getName();
		}

		@Override
		public String getLocalizationKey() {
			return keyMapping.getName();
		}

		@Override
		public KeyMappingValue getValue() {
			return value;
		}

		@Override
		public KeyMappingValue getDefaultValue() {
			return defaultValue;
		}

		@Override
		public boolean set(KeyMappingValue value) {
			this.value = value;
			return true;
		}

		@Override
		public Runnable addListener(Consumer<KeyMappingValue> listener) {
			return () -> {};
		}

		@Override
		public IConfigValueEditorSerializer<KeyMappingValue> getSerializer() {
			return TestKeyMappingSerializer.INSTANCE;
		}
	}

	private static final class TestConfigKeyMapping implements IConfigKeyMapping {
		private static final ConfigKeyBinding DEFAULT_BINDING = new ConfigKeyBinding("key.keyboard.k", ConfigKeyModifier.NONE);

		private int conflictLookupCount;

		@Override
		public String getName() {
			return "key.test_mod.open";
		}

		@Override
		public Component getLocalizedName() {
			return Component.literal("Open");
		}

		@Override
		public Component getLocalizedContext() {
			return Component.literal("In Game");
		}

		@Override
		public Component getLocalizedDescription() {
			return Component.empty();
		}

		@Override
		public ConfigKeyBinding getValue() {
			return DEFAULT_BINDING;
		}

		@Override
		public ConfigKeyBinding getDefaultValue() {
			return DEFAULT_BINDING;
		}

		@Override
		public ConfigKeyBinding normalize(ConfigKeyBinding value) {
			return value;
		}

		@Override
		public void set(ConfigKeyBinding value) {

		}

		@Override
		public Component getValueName(ConfigKeyBinding value) {
			return Component.literal(value.keyName());
		}

		@Override
		public ConfigKeyModifier getKeyModifier(String keyName) {
			return ConfigKeyModifier.NONE;
		}

		@Override
		public List<ConfigKeyMappingConflict> getConflicts(ConfigKeyBinding value) {
			conflictLookupCount++;
			return List.of(new ConfigKeyMappingConflict(
				Component.literal("Conflict"),
				Component.literal(value.keyName()),
				Component.literal("Test Mod"),
				Component.literal("Test Category")
			));
		}
	}

	private enum TestKeyMappingSerializer implements IConfigValueEditorSerializer<KeyMappingValue> {
		INSTANCE;

		@Override
		public String serialize(KeyMappingValue value) {
			return value.binding().keyName();
		}

		@Override
		public IDeserializeResult<KeyMappingValue> deserialize(String string) {
			return IDeserializeResult.failure("Not supported by test serializer.");
		}

		@Override
		public boolean isValid(KeyMappingValue value) {
			return true;
		}

		@Override
		public Optional<Collection<KeyMappingValue>> getAllValidValues() {
			return Optional.empty();
		}

		@Override
		public ConfigValueEditorType<KeyMappingValue> getEditorType() {
			return ConfigValueEditorTypes.getKeyMapping();
		}

		@Override
		public Component getLocalizedValueName(String configValueLocalizationKey, KeyMappingValue value) {
			return value.configKeyMapping().getValueName(value.binding());
		}

		@Override
		public String getValidValuesDescription() {
			return "";
		}
	}
}
