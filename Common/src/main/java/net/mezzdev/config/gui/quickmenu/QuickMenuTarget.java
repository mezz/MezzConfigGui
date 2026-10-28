package net.mezzdev.config.gui.quickmenu;

import org.jspecify.annotations.Nullable;

/** A menu action identified by hit testing. */
record QuickMenuTarget<T>(@Nullable T id, Action action) {
	enum Action { CLOSE, ANCHOR, FULL_CONFIG, EDIT, SCROLL, MEMBERSHIP, UP, DOWN, PAGE, RESIZE, REORDER }
}
