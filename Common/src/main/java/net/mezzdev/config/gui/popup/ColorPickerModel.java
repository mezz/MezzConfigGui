package net.mezzdev.config.gui.popup;

import net.mezzdev.config.api.value.ConfigColorFormat;
import net.mezzdev.config.api.value.PackedColor;

/**
 * Mutable HSV representation used by the color picker.
 */
final class ColorPickerModel {
	private static final double D65_WHITE_X = 0.95047;
	private static final double D65_WHITE_Y = 1.0;
	private static final double D65_WHITE_Z = 1.08883;
	private static final double LAB_DELTA = 6.0 / 29.0;

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

	Cmyk getCmyk() {
		Rgb rgb = getRgb();
		double red = rgb.red() / 255.0;
		double green = rgb.green() / 255.0;
		double blue = rgb.blue() / 255.0;
		double black = 1.0 - Math.max(red, Math.max(green, blue));
		if (black >= 1.0) {
			return new Cmyk(0.0, 0.0, 0.0, 1.0);
		}
		double scale = 1.0 - black;
		double cyan = (1.0 - red - black) / scale;
		double magenta = (1.0 - green - black) / scale;
		double yellow = (1.0 - blue - black) / scale;
		return new Cmyk(clamp(cyan), clamp(magenta), clamp(yellow), clamp(black));
	}

	void setCmyk(double cyan, double magenta, double yellow, double black) {
		cyan = clamp(cyan);
		magenta = clamp(magenta);
		yellow = clamp(yellow);
		black = clamp(black);
		int red = toChannel((1.0 - cyan) * (1.0 - black));
		int green = toChannel((1.0 - magenta) * (1.0 - black));
		int blue = toChannel((1.0 - yellow) * (1.0 - black));
		setRgb(red, green, blue);
	}

	Lab getLab() {
		Rgb rgb = getRgb();
		double red = srgbToLinear(rgb.red() / 255.0);
		double green = srgbToLinear(rgb.green() / 255.0);
		double blue = srgbToLinear(rgb.blue() / 255.0);

		double x = red * 0.4124564 + green * 0.3575761 + blue * 0.1804375;
		double y = red * 0.2126729 + green * 0.7151522 + blue * 0.0721750;
		double z = red * 0.0193339 + green * 0.1191920 + blue * 0.9503041;
		double fx = labFunction(x / D65_WHITE_X);
		double fy = labFunction(y / D65_WHITE_Y);
		double fz = labFunction(z / D65_WHITE_Z);
		return new Lab(116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz));
	}

	void setLab(double lightness, double a, double b) {
		lightness = Math.clamp(lightness, 0.0, 100.0);
		a = Math.clamp(a, -128.0, 127.0);
		b = Math.clamp(b, -128.0, 127.0);

		double fy = (lightness + 16.0) / 116.0;
		double fx = fy + a / 500.0;
		double fz = fy - b / 200.0;
		double x = D65_WHITE_X * inverseLabFunction(fx);
		double y = D65_WHITE_Y * inverseLabFunction(fy);
		double z = D65_WHITE_Z * inverseLabFunction(fz);

		double red = x * 3.2404542 + y * -1.5371385 + z * -0.4985314;
		double green = x * -0.9692660 + y * 1.8760108 + z * 0.0415560;
		double blue = x * 0.0556434 + y * -0.2040259 + z * 1.0572252;
		setRgb(toChannel(linearToSrgb(red)), toChannel(linearToSrgb(green)), toChannel(linearToSrgb(blue)));
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

	private static int toChannel(double value) {
		return (int) Math.round(clamp(value) * 255.0);
	}

	private static double srgbToLinear(double value) {
		if (value <= 0.04045) {
			return value / 12.92;
		}
		return Math.pow((value + 0.055) / 1.055, 2.4);
	}

	private static double linearToSrgb(double value) {
		if (value <= 0.0031308) {
			return value * 12.92;
		}
		return 1.055 * Math.pow(value, 1.0 / 2.4) - 0.055;
	}

	private static double labFunction(double value) {
		double deltaCubed = LAB_DELTA * LAB_DELTA * LAB_DELTA;
		if (value > deltaCubed) {
			return Math.cbrt(value);
		}
		return value / (3.0 * LAB_DELTA * LAB_DELTA) + 4.0 / 29.0;
	}

	private static double inverseLabFunction(double value) {
		if (value > LAB_DELTA) {
			return value * value * value;
		}
		return 3.0 * LAB_DELTA * LAB_DELTA * (value - 4.0 / 29.0);
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

	private static double clamp(double value) {
		return Math.clamp(value, 0.0, 1.0);
	}

	record Rgb(int red, int green, int blue) {
	}

	record Cmyk(double cyan, double magenta, double yellow, double black) {
	}

	record Lab(double lightness, double a, double b) {
	}
}
