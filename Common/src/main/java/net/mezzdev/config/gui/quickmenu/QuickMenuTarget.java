package net.mezzdev.config.gui.quickmenu;

/** A menu action identified by hit testing. */
sealed interface QuickMenuTarget<T> {
	boolean playsClickSound();

	record Control<T>(ControlAction action) implements QuickMenuTarget<T> {
		@Override
		public boolean playsClickSound() {
			return action != ControlAction.CLOSE && action != ControlAction.SCROLL;
		}
	}

	record Option<T>(T id, OptionAction action) implements QuickMenuTarget<T> {
		@Override
		public boolean playsClickSound() {
			return action != OptionAction.REORDER;
		}
	}

	enum ControlAction { CLOSE, ANCHOR, FULL_CONFIG, EDIT, SCROLL, PAGE, RESIZE }
	enum OptionAction { MEMBERSHIP, UP, DOWN, REORDER }
}
