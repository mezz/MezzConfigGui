package net.mezzdev.config.gui.entries;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.IConfigListValueEditorSerializer;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.input.InputType;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.popup.ConfigPopupSelector;
import net.mezzdev.config.gui.popup.ColorPickerPopup;
import net.mezzdev.config.gui.popup.MappedConfigValuePopup;
import net.mezzdev.config.gui.util.HexColorString;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HexColorConfigEntryTest {
	private static final ImmutableRect2i CLIP = new ImmutableRect2i(0, 0, 600, 500);

	@Test
	void pickerKeyboardInputAlsoConvertsBackToValidatedStrings() {
		String original = "0x80abcdef";
		HexColorString color = HexColorString.parse(original).orElseThrow();
		StringSerializer serializer = new StringSerializer(value -> !value.endsWith("ffffff"));
		MappedConfigValuePopup<?, String> popup = new MappedConfigValuePopup<>(
			new ColorPickerPopup(color.color(), true),
			updated -> color.update(original, updated, serializer)
		);
		List<String> values = new ArrayList<>();
		for (char c : "#80a1b2c3".toCharArray()) {
			assertTrue(popup.charTyped(c, 0, values::add));
		}
		assertEquals("0x80a1b2c3", values.getLast());
		assertTrue(popup.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0, values::add));
		assertEquals("0x80a1b2c3", values.getLast());
		for (char c : "#80ffffff".toCharArray()) {
			popup.charTyped(c, 0, values::add);
		}
		assertFalse(values.contains("0x80ffffff"));
	}

	@Test
	@SuppressWarnings("DataFlowIssue")
	void stringSwatchOpensPickerAndTextRemainsEditableAfterChangingToANonColor() throws Exception {
		List<ConfigPopupSelector> opened = new ArrayList<>();
		StringSerializer serializer = new StringSerializer(value -> true);
		TextConfigEntry<String> entry = new TextConfigEntry<>(new TestValue<>("0xabcdef", serializer), serializer, opened::add, null);
		setField(entry, "valueArea", new ImmutableRect2i(100, 0, 120, 18));
		assertTrue(entry.onMouseClicked(click(102, 5, InputType.SIMULATE)));
		assertTrue(opened.isEmpty());
		assertTrue(entry.onMouseClicked(click(102, 5, InputType.EXECUTE)));
		ConfigPopupSelector popup = opened.getFirst();
		selectColor(popup, 55);
		assertTrue(entry.getValue().matches("0x[0-9a-f]{6}"));
		assertNotEquals("0xabcdef", entry.getValue());
		assertTrue(entry.hasPendingChange());
		assertFalse(popup.closesAfterClick());

		assertTrue(entry.onMouseClicked(click(135, 5, InputType.EXECUTE)));
		entry.keyPressed(GLFW.GLFW_KEY_DELETE, 0, 0);
		for (char c : "automatic".toCharArray()) {
			assertTrue(entry.charTyped(c, 0));
		}
		entry.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
		assertEquals("automatic", entry.getValue());
		entry.onMouseClicked(click(102, 5, InputType.EXECUTE));
		assertEquals(1, opened.size());
		assertTrue(entry.charTyped('!', 0));
	}

	@Test
	@SuppressWarnings("DataFlowIssue")
	void stringPickerHonorsItsSerializerValidator() throws Exception {
		List<ConfigPopupSelector> opened = new ArrayList<>();
		StringSerializer serializer = new StringSerializer("#ABCDEF"::equals);
		TextConfigEntry<String> entry = new TextConfigEntry<>(new TestValue<>("#ABCDEF", serializer), serializer, opened::add, null);
		setField(entry, "valueArea", new ImmutableRect2i(100, 0, 120, 18));
		entry.onMouseClicked(click(102, 5, InputType.EXECUTE));
		ConfigPopupSelector popup = opened.getFirst();
		popup.updateBounds(CLIP);
		ImmutableRect2i area = popup.getArea();
		assertFalse(popup.onMouseDragged(area.getX() + 55, area.getY() + 55, 0));
		assertEquals("#ABCDEF", entry.getValue());
		assertFalse(entry.hasPendingChange());
	}

	@Test
	@SuppressWarnings("DataFlowIssue")
	void listSwatchEditsOnlyItsOwnElementAndSupportsRepeatedPickerUpdates() throws Exception {
		List<ConfigPopupSelector> opened = new ArrayList<>();
		List<String> original = List.of("automatic", "0X80ABCDEF", "#123456");
		ListSerializer serializer = new ListSerializer(values -> true);
		ListConfigEntry<String> entry = new ListConfigEntry<>(new TestValue<>(original, serializer), serializer, opened::add, () -> {}, null);
		clickListSwatch(entry, 1);
		ConfigPopupSelector popup = opened.getFirst();
		selectColor(popup, 55);
		String first = entry.getValue().get(1);
		assertTrue(first.matches("0X80[0-9A-F]{6}"));
		assertNotEquals(original.get(1), first);
		selectColor(popup, 95);
		assertNotEquals(first, entry.getValue().get(1));
		assertEquals("automatic", entry.getValue().getFirst());
		assertEquals("#123456", entry.getValue().getLast());
		assertEquals(original, entry.configValue.getValue());
		assertTrue(entry.hasPendingChange());
	}

	@Test
	@SuppressWarnings("DataFlowIssue")
	void listPickerHonorsWholeListValidation() throws Exception {
		List<ConfigPopupSelector> opened = new ArrayList<>();
		List<String> original = List.of("#ABCDEF", "#123456");
		ListSerializer serializer = new ListSerializer(original::equals);
		ListConfigEntry<String> entry = new ListConfigEntry<>(new TestValue<>(original, serializer), serializer, opened::add, () -> {}, null);
		clickListSwatch(entry, 0);
		ConfigPopupSelector popup = opened.getFirst();
		popup.updateBounds(CLIP);
		ImmutableRect2i area = popup.getArea();
		assertFalse(popup.onMouseDragged(area.getX() + 55, area.getY() + 55, 0));
		assertEquals(original, entry.getValue());
		assertFalse(entry.hasPendingChange());
	}

	private static void selectColor(ConfigPopupSelector popup, int x) {
		popup.updateBounds(CLIP);
		ImmutableRect2i area = popup.getArea();
		assertTrue(popup.onMouseDragged(area.getX() + x, area.getY() + 55, 0));
	}

	private static void clickListSwatch(ListConfigEntry<String> entry, int index) throws Exception {
		entry.area = CLIP;
		Field rowsField = ListConfigEntry.class.getDeclaredField("valueRows");
		rowsField.setAccessible(true);
		List<?> rows = (List<?>) rowsField.get(entry);
		Object row = rows.get(index);
		Method updateBounds = row.getClass().getDeclaredMethod("updateBounds", ImmutableRect2i.class);
		updateBounds.setAccessible(true);
		updateBounds.invoke(row, new ImmutableRect2i(0, 40, 400, 24));
		assertTrue(entry.createInputHandler().handleUserInput(null, click(6, 45, InputType.EXECUTE)).isPresent());
	}

	private static UserInput click(double x, double y, InputType type) {
		return UserInput.fromVanilla(x, y, 0, type).orElseThrow();
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private record TestValue<T>(T value, IConfigValueSerializer<T> serializer) implements IConfigScreenValue<T> {
		@Override
		public String getName() {
			return "color";
		}

		@Override
		public String getLocalizationKey() {
			return "test.color";
		}

		@Override
		public T getValue() {
			return value;
		}

		@Override
		public T getDefaultValue() {
			return value;
		}

		@Override
		public boolean set(T value) {
			return true;
		}

		@Override
		public Runnable addListener(Consumer<T> listener) {
			return () -> {};
		}

		@Override
		public IConfigValueSerializer<T> getSerializer() {
			return serializer;
		}
	}

	private record StringSerializer(Predicate<String> validator) implements IConfigValueSerializer<String> {
		@Override
		public String serialize(String value) {
			return value;
		}

		@Override
		public IDeserializeResult<String> deserialize(String value) {
			return IDeserializeResult.success(value);
		}

		@Override
		public boolean isValid(String value) {
			return validator.test(value);
		}

		@Override
		public String getValidValuesDescription() {
			return "test strings";
		}
	}

	private record ListSerializer(Predicate<List<String>> validator) implements IConfigListValueEditorSerializer<String> {
		@Override
		public IConfigValueSerializer<String> getElementSerializer() {
			return new StringSerializer(value -> true);
		}

		@Override
		public String serialize(List<String> value) {
			return value.toString();
		}

		@Override
		public IDeserializeResult<List<String>> deserialize(String value) {
			return IDeserializeResult.failure("Unused");
		}

		@Override
		public boolean isValid(List<String> value) {
			return validator.test(value);
		}

		@Override
		public String getValidValuesDescription() {
			return "test list";
		}

		@Override
		public Component getLocalizedValueName(String key, List<String> value) {
			return Component.literal(value.toString());
		}
	}
}
