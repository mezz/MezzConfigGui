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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigInfoPanelTest {
	@Test
	void readingThePanelKeepsItsSourceButStillRefreshesChangingStatus() {
		ConfigInfoPanel panel = new ConfigInfoPanel();
		TestFont font = new TestFont();
		AtomicReference<ConfigInfo> liveInfo = new AtomicReference<>(new ConfigInfo(Component.literal("Server"), Component.literal("Checking")));
		Supplier<ConfigInfo> source = liveInfo::get;
		panel.updateSource(source, font, 50, false);
		int initialCalls = font.splitCalls;
		panel.updateSource(() -> null, font, 50, true);
		assertEquals(initialCalls, font.splitCalls);

		liveInfo.set(new ConfigInfo(Component.literal("Server"), Component.literal("You can edit these settings on the server now.")));
		panel.updateSource(() -> null, font, 50, true);
		assertTrue(font.splitCalls > initialCalls);
		assertEquals(10 + 3 * font.lineHeight + 2, panel.getHeight());
	}

	@Test
	void heightIncludesEveryWrappedTitleAndDescriptionLine() {
		ConfigInfoPanel panel = new ConfigInfoPanel();
		TestFont font = new TestFont();
		ConfigInfo info = new ConfigInfo(Component.literal("abcdefghijklmno"), List.of(Component.literal("abcdefghij"), Component.literal("klmno")));

		panel.updateSource(() -> info, font, 15, false);

		assertEquals(10 + 6 * font.lineHeight + 2, panel.getHeight());
	}

	@Test
	void equivalentInfoReusesWrappingButChangesAndResizingRecalculateIt() {
		ConfigInfoPanel panel = new ConfigInfoPanel();
		TestFont font = new TestFont();
		panel.updateSource(() -> createInfo(), font, 30, false);
		int originalHeight = panel.getHeight();
		int originalCalls = font.splitCalls;

		panel.updateSource(() -> createInfo(), font, 30, false);
		assertEquals(originalCalls, font.splitCalls);
		panel.updateSource(() -> createInfo(), font, 15, false);
		assertTrue(font.splitCalls > originalCalls);
		assertTrue(panel.getHeight() > originalHeight);

		panel.updateSource(() -> new ConfigInfo(Component.literal("Short"), List.of()), font, 15, false);
		assertEquals(10 + font.lineHeight + 2, panel.getHeight());
	}

	@Test
	void growingUnderThePointerKeepsTheDescriptionAvailableToRead() {
		ConfigInfoPanel panel = new ConfigInfoPanel();
		TestFont font = new TestFont();
		panel.updateSource(() -> createInfo(), font, 15, false);
		int height = panel.getHeight();
		int splitCalls = font.splitCalls;

		panel.updateSource(() -> new ConfigInfo(Component.literal("Category"), List.of()), font, 15, true);
		assertEquals(height, panel.getHeight());
		assertEquals(splitCalls, font.splitCalls);

		panel.updateSource(() -> null, font, 15, false);
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
