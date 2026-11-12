package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.internal.ConfigValueEditorMetadataFactory;
import net.mezzdev.config.gui.api.ConfigValueEditorTypes;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditorMetadata;
import net.mezzdev.config.gui.input.InputType;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnboundedIntegerConfigEntryTest {

	@Test
	@SuppressWarnings("DataFlowIssue")
	void integerWithoutARangeCanBeEditedAndStillHonorsItsValidator() throws Exception {
		ConfigEntryWidgetFactory factory = new ConfigEntryWidgetFactory(ignored -> {}, () -> {}, null, Map.of());
		EvenIntegerValue value = new EvenIntegerValue();
		ConfigEntryWidget<?> entry = factory.create(value);
		Field valueArea = entry.getClass().getDeclaredField("valueArea");
		valueArea.setAccessible(true);
		valueArea.set(entry, new ImmutableRect2i(100, 0, 120, 18));

		edit(entry, "-2147483648");
		assertEquals(Integer.MIN_VALUE, entry.getPendingChange().orElseThrow().value());
		assertEquals(2, value.getValue());
		assertTrue(entry.getPendingChange().orElseThrow().apply());
		assertEquals(Integer.MIN_VALUE, value.getValue());
		assertFalse(entry.hasPendingChange());

		edit(entry, "3");
		assertFalse(entry.hasPendingChange(), "The native config's custom validator must reject odd integers");
		edit(entry, "2147483648");
		assertFalse(entry.hasPendingChange(), "Values outside the integer domain must not be committed");
		edit(entry, "not an integer");
		assertFalse(entry.hasPendingChange());
		edit(entry, "8");
		assertEquals(8, entry.getPendingChange().orElseThrow().value());
	}

	private static void edit(ConfigEntryWidget<?> entry, String text) {
		UserInput click = UserInput.fromVanilla(110.0, 5.0, GLFW.GLFW_MOUSE_BUTTON_LEFT, InputType.EXECUTE).orElseThrow();
		assertTrue(entry.onMouseClicked(click));
		assertTrue(entry.keyPressed(GLFW.GLFW_KEY_DELETE, 0, 0));
		for (char character : text.toCharArray()) {
			assertTrue(entry.charTyped(character, 0));
		}
		assertTrue(entry.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0));
	}

	private static final class EvenIntegerValue implements IConfigScreenValue<Integer> {
		private static final IConfigValueSerializer<Integer> SERIALIZER = new IConfigValueSerializer<>() {
			@Override
			public Class<Integer> getValueClass() {
				return Integer.class;
			}

			@Override
			public String serialize(Integer value) {
				return value.toString();
			}

			@Override
			public IDeserializeResult<Integer> deserialize(String string) {
				try {
					return IDeserializeResult.success(Integer.parseInt(string));
				} catch (NumberFormatException e) {
					return IDeserializeResult.failure("Invalid integer");
				}
			}

			@Override
			public boolean isValid(Integer value) {
				return value % 2 == 0;
			}

			@Override
			public String getValidValuesDescription() {
				return "An even integer";
			}
		};
		private int value = 2;

		@Override
		public String getName() { return "number"; }
		@Override
		public String getLocalizationKey() { return "test.number"; }
		@Override
		public Integer getValue() { return value; }
		@Override
		public Integer getDefaultValue() { return 2; }
		@Override
		public boolean set(Integer value) {
			this.value = value;
			return true;
		}
		@Override
		public Runnable addListener(Consumer<Integer> listener) { return () -> {}; }
		@Override
		public IConfigValueSerializer<Integer> getSerializer() { return SERIALIZER; }
		@Override
		public IConfigValueEditorMetadata<Integer> getEditorMetadata() {
			return ConfigValueEditorMetadataFactory.INSTANCE.create(SERIALIZER, ConfigValueEditorTypes.INTEGER);
		}
	}
}
