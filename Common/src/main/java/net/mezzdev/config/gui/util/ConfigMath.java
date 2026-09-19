package net.mezzdev.config.gui.util;

/** Java 17 equivalents of the range clamps used by the GUI. */
public final class ConfigMath {
	private ConfigMath() {
	}

	public static int clamp(long value, int min, int max) {
		if (min > max) {
			throw new IllegalArgumentException("Minimum exceeds maximum");
		}
		return (int) Math.min(max, Math.max(min, value));
	}

	public static float clamp(float value, float min, float max) {
		if (!(min <= max)) {
			throw new IllegalArgumentException("Invalid clamp bounds");
		}
		return Math.min(max, Math.max(min, value));
	}

	public static double clamp(double value, double min, double max) {
		if (!(min <= max)) {
			throw new IllegalArgumentException("Invalid clamp bounds");
		}
		return Math.min(max, Math.max(min, value));
	}
}
