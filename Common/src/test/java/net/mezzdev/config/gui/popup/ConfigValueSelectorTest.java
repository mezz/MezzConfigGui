package net.mezzdev.config.gui.popup;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValuePopup;
import net.mezzdev.config.gui.input.InputType;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.api.LegacyGuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigValueSelectorTest {
	@Test
	void scrollsValuesThatDoNotFitInThePopup() {
		ConfigValueSelector<String> selector = new ConfigValueSelector<>(
			new TestConfigValue(),
			List.of("first", "second", "third"),
			null
		);
		Rect2i area = new Rect2i(0, 0, 80, 22);

		assertEquals("first", selector.getHoveredValue(area, 10, 10).orElseThrow());
		assertTrue(selector.mouseScrolled(area, 10, 10, 0, -1));
		assertEquals("second", selector.getHoveredValue(area, 10, 10).orElseThrow());
	}

	@Test
	void routesNonLeftClicksToThePopupContract() {
		AtomicInteger clickedButton = new AtomicInteger(-1);
		AtomicReference<@Nullable String> selectedValue = new AtomicReference<>();
		IConfigValuePopup<String> popup = new TestPopup(clickedButton);
		ConfigValuePopupSelector<String> selector = new ConfigValuePopupSelector<>(
			new TestConfigValue(),
			popup,
			() -> new ImmutableRect2i(10, 10, 20, 20),
			() -> false,
			selectedValue::set
		);
		ImmutableRect2i clipArea = new ImmutableRect2i(0, 0, 100, 100);
		ConfigValueSelectorInputHandler inputHandler = new ConfigValueSelectorInputHandler(
			() -> selector,
			() -> clipArea,
			() -> {},
			() -> {}
		);

		UserInput simulate = UserInput.fromVanilla(15.0, 35.0, com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT, InputType.SIMULATE).orElseThrow();
		UserInput execute = UserInput.fromVanilla(15.0, 35.0, com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT, InputType.EXECUTE).orElseThrow();
		assertTrue(inputHandler.handleUserInput(null, simulate).isPresent());
		assertTrue(inputHandler.handleUserInput(null, execute).isPresent());
		assertEquals(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT, clickedButton.get());
		assertEquals("selected", selectedValue.get());
	}

	@Test
	void dragReleaseDoesNotPerformASecondClickSelection() {
		DragPopup popup = new DragPopup();
		List<String> selectedValues = new ArrayList<>();
		ConfigValuePopupSelector<String> selector = new ConfigValuePopupSelector<>(
			new TestConfigValue(),
			popup,
			() -> new ImmutableRect2i(10, 10, 20, 20),
			() -> false,
			selectedValues::add
		);
		ImmutableRect2i clipArea = new ImmutableRect2i(0, 0, 100, 100);
		ConfigValueSelectorInputHandler inputHandler = new ConfigValueSelectorInputHandler(
			() -> selector,
			() -> clipArea,
			() -> {},
			() -> {}
		);

		UserInput simulate = UserInput.fromVanilla(15.0, 35.0, com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, InputType.SIMULATE).orElseThrow();
		UserInput execute = UserInput.fromVanilla(18.0, 38.0, com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, InputType.EXECUTE).orElseThrow();
		assertTrue(inputHandler.handleUserInput(null, simulate).isPresent());
		assertTrue(inputHandler.handleMouseDragged(null, 18.0, 38.0, com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 3.0, 3.0).isPresent());
		assertTrue(inputHandler.handleUserInput(null, execute).isPresent());

		assertEquals(List.of("dragged"), selectedValues);
		assertEquals(1, popup.clickedCount);
		assertEquals(1, popup.releasedCount);
	}

	private static final class TestPopup implements IConfigValuePopup<String> {
		private final AtomicInteger clickedButton;

		private TestPopup(AtomicInteger clickedButton) {
			this.clickedButton = clickedButton;
		}

		@Override
		public int getWidth() {
			return 20;
		}

		@Override
		public int getHeight() {
			return 20;
		}

		@Override
		public Optional<String> getHoveredValue(Rect2i area, double mouseX, double mouseY) {
			return Optional.empty();
		}

		@Override
		public void draw(LegacyGuiGraphics guiGraphics, Rect2i area, double mouseX, double mouseY) {

		}

		@Override
		public Optional<String> getClickedValue(Rect2i area, double mouseX, double mouseY, int button) {
			clickedButton.set(button);
			return Optional.of("selected");
		}
	}

	private static final class DragPopup implements IConfigValuePopup<String> {
		private int clickedCount;
		private int releasedCount;

		@Override
		public int getWidth() {
			return 20;
		}

		@Override
		public int getHeight() {
			return 20;
		}

		@Override
		public Optional<String> getHoveredValue(Rect2i area, double mouseX, double mouseY) {
			return Optional.empty();
		}

		@Override
		public void draw(LegacyGuiGraphics guiGraphics, Rect2i area, double mouseX, double mouseY) {

		}

		@Override
		public Optional<String> getClickedValue(Rect2i area, double mouseX, double mouseY, int button) {
			clickedCount++;
			return Optional.of("clicked");
		}

		@Override
		public Optional<String> getDraggedValue(Rect2i area, double mouseX, double mouseY, int button) {
			return Optional.of("dragged");
		}

		@Override
		public void mouseReleased(Rect2i area, double mouseX, double mouseY, int button) {
			releasedCount++;
		}
	}

	private static final class TestConfigValue implements IConfigScreenValue<String> {
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
			return "first";
		}

		@Override
		public String getDefaultValue() {
			return "first";
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
		public boolean isValid(@Nullable String value) {
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
