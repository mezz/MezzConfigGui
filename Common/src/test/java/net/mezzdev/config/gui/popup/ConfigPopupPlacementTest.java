package net.mezzdev.config.gui.popup;

import net.mezzdev.config.api.value.serializer.IDeserializeResult;
import net.mezzdev.config.api.value.serializer.IConfigValueSerializer;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValuePopup;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.api.LegacyGuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigPopupPlacementTest {
	@Test
	void popupDimensionsAreClampedToTheVisibleArea() {
		ImmutableRect2i anchor = new ImmutableRect2i(20, 20, 80, 18);
		ImmutableRect2i clip = new ImmutableRect2i(10, 10, 180, 160);

		ImmutableRect2i popup = ConfigPopupPlacement.placeNearAnchor(anchor, clip, 256, 225);

		assertEquals(new ImmutableRect2i(10, 10, 180, 160), popup);
	}

	@Test
	void publicPopupSizeNegotiationControlsPlacement() {
		ImmutableRect2i anchor = new ImmutableRect2i(20, 20, 80, 18);
		ImmutableRect2i clip = new ImmutableRect2i(10, 10, 180, 160);
		IConfigValuePopup<String> popup = new TestPopup();
		ConfigValuePopupSelector<String> selector = new ConfigValuePopupSelector<>(
			new TestConfigValue(),
			popup,
			() -> anchor,
			() -> false,
			value -> {}
		);

		selector.updateBounds(clip);

		assertEquals(new ImmutableRect2i(10, 37, 120, 90), selector.getArea());
	}

	private static final class TestPopup implements IConfigValuePopup<String> {
		@Override
		public int getWidth() {
			return 256;
		}

		@Override
		public int getHeight() {
			return 225;
		}

		@Override
		public Size getPreferredSize(int availableWidth, int availableHeight) {
			assertEquals(180, availableWidth);
			assertEquals(160, availableHeight);
			return new Size(120, 90);
		}

		@Override
		public Optional<String> getHoveredValue(Rect2i area, double mouseX, double mouseY) {
			return Optional.empty();
		}

		@Override
		public void draw(LegacyGuiGraphics guiGraphics, Rect2i area, double mouseX, double mouseY) {

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
			return "test";
		}

		@Override
		public String getDefaultValue() {
			return "test";
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
