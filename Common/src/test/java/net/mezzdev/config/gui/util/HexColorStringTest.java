package net.mezzdev.config.gui.util;

import net.mezzdev.config.api.value.color.PackedColor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HexColorStringTest {
	@Test
	void detectsExplicitRgbAndArgbWithoutGuessingBareIdentifiersOrOtherFormats() {
		assertEquals(PackedColor.rgb(0x12abcd), parse("#12abcd").color());
		assertEquals(PackedColor.argb(0x8012abcd), parse("#8012ABCD").color());
		assertEquals(PackedColor.rgb(0xabcdef), parse("0xabcdef").color());
		assertEquals(PackedColor.argb(0xFFFFFFFF), parse("0XFFFFFFFF").color());
		for (Object value : List.of("abcdef", "FFFFFFFF", "red", "#abc", "#abcd", "#12345", "#123456789", "#12gg00", "#１２３４５６", " #123456", "#123456 ", "#-12345", "0x+12345", 0x123456)) {
			assertTrue(HexColorString.parse(value).isEmpty(), value.toString());
		}
	}

	@Test
	void preservesPrefixesAlphaWidthCaseAndUnchangedMixedCase() {
		assertEquals("#00000A", parse("#ABCDEF").format(PackedColor.rgb(10)));
		assertEquals("0x00000a", parse("0xabcdef").format(PackedColor.rgb(10)));
		assertEquals("0X00ABCDEF", parse("0X80ABCDEF").format(PackedColor.argb(0xabcdef)));
		assertEquals("#ffffffff", parse("#00abcdef").format(PackedColor.argb(-1)));
		assertEquals("#aBcDeF", parse("#aBcDeF").format(PackedColor.rgb(0xabcdef)));
	}

	private static HexColorString parse(String text) {
		return HexColorString.parse(text).orElseThrow();
	}
}
