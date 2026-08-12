package net.mezzdev.config.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mezzdev.config.gui.ConfigGuiColors.GuiColor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

public class ConfigGuiColorsTest {
	@Test
	public void loadLayeredColorOverrides() {
		Map<GuiColor, Integer> colors = createDefaultColors();
		ConfigGuiColors.loadColors(JsonParser.parseString("""
			{
			"configEntryText": "0x112233",
			"navItemActiveAccent": "0xFF445566"
			}
			"""), colors);
		ConfigGuiColors.loadColors(JsonParser.parseString("""
			{
			"configEntryText": "0x80224466",
			"navItemActiveAccent": "invalid"
			}
			"""), colors);

		Assertions.assertEquals(0x80224466, colors.get(GuiColor.CONFIG_ENTRY_TEXT));
		Assertions.assertEquals(0xFF445566, colors.get(GuiColor.NAV_ITEM_ACTIVE_ACCENT));
		Assertions.assertEquals(GuiColor.CONFIG_ENTRY_DISABLED_TEXT.getDefaultColor(), colors.get(GuiColor.CONFIG_ENTRY_DISABLED_TEXT));
	}

	@Test
	public void parseArgbColorString() {
		Assertions.assertEquals(0xFF808080, ConfigGuiColors.parseColorString("0xFF808080").orElseThrow());
		Assertions.assertEquals(0xDDFF0000, ConfigGuiColors.parseColorString("0xDDFF0000").orElseThrow());
		Assertions.assertEquals(0x30000000, ConfigGuiColors.parseColorString("0x30000000").orElseThrow());
	}

	@Test
	public void parseRgbColorStringAsOpaqueArgb() {
		Assertions.assertEquals(0xFF808080, ConfigGuiColors.parseColorString("0x808080").orElseThrow());
		Assertions.assertEquals(0xFFFFFFFF, ConfigGuiColors.parseColorString("0xFFFFFF").orElseThrow());
	}

	@Test
	public void rejectInvalidColor() {
		Assertions.assertTrue(ConfigGuiColors.parseColor(JsonParser.parseString("805306368")).isEmpty());
		Assertions.assertTrue(ConfigGuiColors.parseColorString("#123456").isEmpty());
		Assertions.assertTrue(ConfigGuiColors.parseColorString("123456").isEmpty());
		Assertions.assertTrue(ConfigGuiColors.parseColorString("0x12345").isEmpty());
		Assertions.assertTrue(ConfigGuiColors.parseColorString("0xGG000000").isEmpty());
		Assertions.assertTrue(ConfigGuiColors.parseColorString("0x123456789").isEmpty());
		Assertions.assertTrue(ConfigGuiColors.parseColor(JsonParser.parseString("true")).isEmpty());
	}

	@Test
	public void builtInResourceContainsEveryDefaultColor() throws IOException {
		try (InputStream inputStream = ConfigGuiColorsTest.class.getResourceAsStream("/assets/mezz_config/gui/colors.json")) {
			Assertions.assertNotNull(inputStream);
			JsonObject json = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
			Assertions.assertEquals(GuiColor.values().length + 1, json.size());
			for (GuiColor color : GuiColor.values()) {
				Assertions.assertEquals(color.getDefaultColorString(), json.get(color.getKey()).getAsString(), color.getKey());
			}
		}
	}

	private static Map<GuiColor, Integer> createDefaultColors() {
		Map<GuiColor, Integer> colors = new EnumMap<>(GuiColor.class);
		for (GuiColor color : GuiColor.values()) {
			colors.put(color, color.getDefaultColor());
		}
		return colors;
	}
}
