package net.mezzdev.config.gui.popup;

import net.mezzdev.config.api.value.color.ConfigColorFormat;
import net.mezzdev.config.api.value.color.PackedColor;

/**
 * Mutable HSV representation used by the color picker.
 */
final class ColorPickerModel {
	private final ConfigColorFormat format;
	private float hue;
	private float saturation;
	private float value;
	private float alpha;

	ColorPickerModel(PackedColor color) {
		this.format = color.format();
		int argb = toArgb(color.packedValue(), format);
		this.alpha = ((argb >>> 24) & 0xFF) / 255.0f;
		setRgb((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF);
	}

	void setRgb(int red, int green, int blue) {
		red = Math.clamp(red, 0, 255);
		green = Math.clamp(green, 0, 255);
		blue = Math.clamp(blue, 0, 255);
		float r = red / 255.0f;
		float g = green / 255.0f;
		float b = blue / 255.0f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;

		value = max;
		saturation = 0.0f;
		if (max != 0.0f) {
			saturation = delta / max;
		}
		if (delta == 0.0f) {
			hue = 0.0f;
		} else if (max == r) {
			hue = ((g - b) / delta) / 6.0f;
		} else if (max == g) {
			hue = (((b - r) / delta) + 2.0f) / 6.0f;
		} else {
			hue = (((r - g) / delta) + 4.0f) / 6.0f;
		}
		if (hue < 0.0f) {
			hue += 1.0f;
		}
	}

	float getHue() {
		return hue;
	}

	float getSaturation() {
		return saturation;
	}

	float getValue() {
		return value;
	}

	float getAlpha() {
		return alpha;
	}

	int getAlphaChannel() {
		return Math.round(alpha * 255.0f);
	}

	Rgb getRgb() {
		int rgb = hsvToRgb(hue, saturation, value);
		return new Rgb((rgb >>> 16) & 0xFF, (rgb >>> 8) & 0xFF, rgb & 0xFF);
	}

	void setHue(float hue) {
		this.hue = clamp(hue);
	}

	void setSaturationAndValue(float saturation, float value) {
		this.saturation = clamp(saturation);
		this.value = clamp(value);
	}

	void setAlpha(float alpha) {
		this.alpha = clamp(alpha);
	}

	void setAlphaChannel(int alpha) {
		setAlpha(Math.clamp(alpha, 0, 255) / 255.0f);
	}

	PackedColor getPackedColor() {
		int rgb = hsvToRgb(hue, saturation, value);
		if (format == ConfigColorFormat.RGB) {
			return PackedColor.rgb(rgb);
		}
		return PackedColor.argb(Math.round(alpha * 255.0f) << 24 | rgb);
	}

	int getArgbColor() {
		return toArgb(getPackedColor().packedValue(), format);
	}

	int getOpaqueRgbColor() {
		return 0xFF000000 | hsvToRgb(hue, saturation, value);
	}

	static int hsvToArgb(float hue, float saturation, float value) {
		return 0xFF000000 | hsvToRgb(hue, saturation, value);
	}

	private static int hsvToRgb(float hue, float saturation, float value) {
		float wrappedHue = hue - (float) Math.floor(hue);
		float scaledHue = wrappedHue * 6.0f;
		int sector = (int) Math.floor(scaledHue);
		float fraction = scaledHue - sector;
		float p = value * (1.0f - saturation);
		float q = value * (1.0f - fraction * saturation);
		float t = value * (1.0f - (1.0f - fraction) * saturation);
		float red;
		float green;
		float blue;
		switch (sector % 6) {
			case 0 -> {
				red = value;
				green = t;
				blue = p;
			}
			case 1 -> {
				red = q;
				green = value;
				blue = p;
			}
			case 2 -> {
				red = p;
				green = value;
				blue = t;
			}
			case 3 -> {
				red = p;
				green = q;
				blue = value;
			}
			case 4 -> {
				red = t;
				green = p;
				blue = value;
			}
			default -> {
				red = value;
				green = p;
				blue = q;
			}
		}
		return toChannel(red) << 16 | toChannel(green) << 8 | toChannel(blue);
	}

	private static int toChannel(float value) {
		return Math.round(clamp(value) * 255.0f);
	}

	private static int toArgb(int color, ConfigColorFormat format) {
		if (format == ConfigColorFormat.RGB) {
			return 0xFF000000 | color;
		}
		return color;
	}

	private static float clamp(float value) {
		return Math.clamp(value, 0.0f, 1.0f);
	}

	record Rgb(int red, int green, int blue) {
	}
}
