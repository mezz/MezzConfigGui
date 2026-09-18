package net.mezzdev.config.gui;

import net.mezzdev.config.gui.api.IConfigScreenValue;

/**
 * Internal access policy for native values, retained through identity-preserving GUI wrappers.
 */
public interface ConfigValueAccess {
	boolean isEditable();

	static boolean isEditable(IConfigScreenValue<?> value) {
		if (value.getIdentityKey() instanceof ConfigValueAccess access) {
			return access.isEditable();
		}
		return true;
	}
}
