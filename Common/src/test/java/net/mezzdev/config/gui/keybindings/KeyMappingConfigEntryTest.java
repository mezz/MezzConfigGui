package net.mezzdev.config.gui.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import net.mezzdev.config.gui.TestMinecraft;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.ConfigValueLocalization;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.keybindings.IKeyMappingBinding;
import net.mezzdev.config.gui.config.ConfigGuiOptionsTestUtil;
import net.mezzdev.config.gui.entries.ConfigEntryWidgetFactory;
import net.mezzdev.config.gui.info.ConfigValueIcon;
import net.mezzdev.config.gui.info.ConfigValueInfoFactory;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyMappingConfigEntryTest {
	@Test
	@SuppressWarnings("DataFlowIssue")
	void decoratedKeyMappingsProvideEditorAndDisplayInformationWithoutSerialization() {
		var mapping = new ConfigKeyMapping(TestMinecraft.keyMapping("key.test_mod.open", InputConstants.KEY_K, "key.categories.misc"));
		var value = new KeyMappingConfigValue(mapping, () -> {});
		IConfigScreenValue<KeyMappingValue> wrapped = IConfigScreenValue.withApplyMode(
			IConfigScreenValue.withRestartRequirement(value, ConfigValueRestartRequirement.GAME_RESTART),
			ConfigValueApplyMode.ON_APPLY
		);
		var factory = new ConfigEntryWidgetFactory(popup -> {}, () -> {}, null, Map.of());
		assertInstanceOf(KeyMappingConfigEntry.class, factory.create(wrapped));

		assertEquals(mapping.getValueName(mapping.getValue()), ConfigValueLocalization.getValueName(wrapped, wrapped.getValue()));
		assertTrue(ConfigValueLocalization.getValueDescription(wrapped, wrapped.getValue()).isEmpty());
		assertEquals(0, ConfigValueIcon.getTextOffset(wrapped, wrapped.getValue()));
		try (ConfigGuiOptionsTestUtil.OptionOverride ignored = ConfigGuiOptionsTestUtil.setValue("showAdvancedValueDetails", true)) {
			var lines = ConfigValueInfoFactory.createSharedInfo(wrapped, false).lines().stream().map(Component::getString).toList();
			assertTrue(lines.stream().anyMatch(line -> line.startsWith("Editor: ")));
			assertFalse(lines.stream().anyMatch(line -> line.startsWith("Serializer: ")));
		}
	}

	@Test
	void conflictTooltipsSeparateEachBindingAndKeepItsDetailsTogether() {
		ConfigKeyMappingConflict first = new ConfigKeyMappingConflict(
			Component.literal("First action"), Component.literal("K"), Component.literal("First mod"), Component.literal("First category")
		);
		ConfigKeyMappingConflict second = new ConfigKeyMappingConflict(
			Component.literal("Second action"), Component.literal("K"), Component.literal("Second mod"), Component.literal("Second category")
		);
		List<Component> lines = KeyMappingConfigEntry.getConflictTooltipInfo(List.of(first, second)).lines();

		assertEquals(7, lines.size());
		assertTrue(lines.get(0).getString().startsWith("• "));
		assertTrue(lines.get(0).getStyle().isBold());
		assertTrue(lines.get(1).getString().startsWith("  "));
		assertTrue(lines.get(2).getString().startsWith("  "));
		assertTrue(lines.get(3).getString().isEmpty());
		assertTrue(lines.get(4).getString().startsWith("• "));
		assertTrue(lines.get(4).getStyle().isBold());
	}

	@Test
	@SuppressWarnings("DataFlowIssue")
	void missingOptionalTranslationsDoNotLeakIntoInfo() {
		ConfigKeyMapping keyMapping = new ConfigKeyMapping(TestMinecraft.keyMapping("key.test_mod.without_details", -1, "key.categories.misc"));
		KeyMappingConfigEntry entry = new KeyMappingConfigEntry(new KeyMappingConfigValue(keyMapping), null);

		assertTrue(keyMapping.getLocalizedDescription().getString().isEmpty());
		assertTrue(keyMapping.getLocalizedContext().getString().isEmpty());
		assertTrue(entry.getInfo().lines().isEmpty());
	}

	@Test
	void screenValueRejectsInvalidInputWithAnException() {
		KeyMappingConfigValue configValue = new KeyMappingConfigValue(new TestConfigKeyMapping());

		assertThrows(IllegalArgumentException.class, () -> configValue.set(null));
	}

	@Test
	void screenValueNotifiesListenersAfterSavingAChange() {
		TestConfigKeyMapping keyMapping = new TestConfigKeyMapping();
		KeyMappingConfigValue configValue = new KeyMappingConfigValue(keyMapping, () -> {});
		List<KeyMappingValue> listenerValues = new ArrayList<>();
		Runnable removeListener = configValue.addListener(listenerValues::add);
		KeyMappingValue changedValue = new KeyMappingValue(
			ConfigKeyMappingHelper.INSTANCE.createBinding(InputConstants.getKey("key.keyboard.l"), Set.of()),
			keyMapping
		);

		assertTrue(configValue.set(changedValue));
		assertFalse(configValue.set(changedValue));
		assertEquals(List.of(changedValue), listenerValues);

		removeListener.run();
		assertTrue(configValue.set(new KeyMappingValue(ConfigKeyBinding.UNKNOWN, keyMapping)));
		assertEquals(List.of(changedValue), listenerValues);
	}

	@Test
	@SuppressWarnings("DataFlowIssue")
	void conflictDetailsOptionControlsConflictLookup() {
		TestConfigKeyMapping keyMapping = new TestConfigKeyMapping();
		KeyMappingConfigEntry entry = new KeyMappingConfigEntry(new KeyMappingConfigValue(keyMapping, () -> {}), null);

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

	private static final class TestConfigKeyMapping implements IConfigKeyMapping {
		private static final IKeyMappingBinding DEFAULT_BINDING = ConfigKeyMappingHelper.INSTANCE.createBinding(InputConstants.getKey("key.keyboard.k"), Set.of());

		private int conflictLookupCount;
		private IKeyMappingBinding value = DEFAULT_BINDING;

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
		public IKeyMappingBinding getValue() {
			return value;
		}

		@Override
		public IKeyMappingBinding getDefaultValue() {
			return DEFAULT_BINDING;
		}

		@Override
		public void set(IKeyMappingBinding value) {
			this.value = value;
		}

		@Override
		public Component getValueName(IKeyMappingBinding value) {
			return Component.literal(value.getKey().getName());
		}

		@Override
		public boolean isModifier(InputConstants.Key key) {
			return ConfigKeyMappingHelper.INSTANCE.getModifier(key) != null;
		}

		@Override
		public IKeyMappingBinding createBinding(InputConstants.Key key, int eventModifiers) {
			var helper = ConfigKeyMappingHelper.INSTANCE;
			return helper.createInputFromEvent(key, eventModifiers).toBinding();
		}

		@Override
		public List<ConfigKeyMappingConflict> getConflicts(IKeyMappingBinding value) {
			conflictLookupCount++;
			return List.of(new ConfigKeyMappingConflict(
				Component.literal("Conflict"),
				Component.literal(value.getKey().getName()),
				Component.literal("Test Mod"),
				Component.literal("Test Category")
			));
		}
	}

}
