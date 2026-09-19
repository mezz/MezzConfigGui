package net.mezzdev.config.gui;

import net.mezzdev.config.gui.api.ConfigInfo;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigInfoPanelTest {
	@Test
	void heightIncludesEveryWrappedTitleAndDescriptionLine() {
		ConfigInfoPanel panel = new ConfigInfoPanel();
		TestFont font = new TestFont();
		ConfigInfo info = new ConfigInfo(Component.literal("abcdefghijklmno"), List.of(Component.literal("abcdefghij"), Component.literal("klmno")));

		panel.update(info, font, 15, false);

		assertEquals(10 + 6 * font.lineHeight + 2, panel.getHeight());
	}

	@Test
	void equivalentInfoReusesWrappingButChangesAndResizingRecalculateIt() {
		ConfigInfoPanel panel = new ConfigInfoPanel();
		TestFont font = new TestFont();
		panel.update(createInfo(), font, 30, false);
		int originalHeight = panel.getHeight();
		int originalCalls = font.splitCalls;

		panel.update(createInfo(), font, 30, false);
		assertEquals(originalCalls, font.splitCalls);
		panel.update(createInfo(), font, 15, false);
		assertTrue(font.splitCalls > originalCalls);
		assertTrue(panel.getHeight() > originalHeight);

		panel.update(new ConfigInfo(Component.literal("Short"), List.of()), font, 15, false);
		assertEquals(10 + font.lineHeight + 2, panel.getHeight());
	}

	@Test
	void growingUnderThePointerKeepsTheDescriptionAvailableToRead() {
		ConfigInfoPanel panel = new ConfigInfoPanel();
		TestFont font = new TestFont();
		panel.update(createInfo(), font, 15, false);
		int height = panel.getHeight();
		int splitCalls = font.splitCalls;

		panel.update(new ConfigInfo(Component.literal("Category"), List.of()), font, 15, true);
		assertEquals(height, panel.getHeight());
		assertEquals(splitCalls, font.splitCalls);

		panel.update(null, font, 15, false);
		assertEquals(10, panel.getHeight());
	}

	private static ConfigInfo createInfo() {
		return new ConfigInfo(Component.literal("Long option title"), Component.literal("A description that wraps across several lines"));
	}

	private static class TestFont extends Font {
		private final StringSplitter splitter = new StringSplitter((codePoint, style) -> 1);
		private int splitCalls;

		private TestFont() {
			super(location -> null, false);
		}

		@Override
		public List<FormattedCharSequence> split(FormattedText text, int width) {
			splitCalls++;
			return splitter.splitLines(text, width, Style.EMPTY).stream()
				.map(Language.getInstance()::getVisualOrder)
				.toList();
		}
	}
}
