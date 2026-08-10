package net.mezzdev.config.gui.popup;

/**
 * Internal extension for popup content that has a smaller layout for constrained screens.
 */
interface ResponsiveConfigValuePopup {
	Size getPreferredSize(int availableWidth, int availableHeight);

	record Size(int width, int height) {

	}
}
