package net.mezzdev.config.gui.info;

import net.minecraft.network.chat.Component;

/**
 * The access currently available for a particular server config.
 */
public enum ServerConfigAccess {
	LOCAL("local", "These settings apply to your current world. You can edit them here."),
	READ_ONLY("readOnly", "These settings are controlled by the server and are read-only here. Ask a server administrator to change them."),
	CHECKING("checking", "Checking whether the server allows you to edit these settings. They are read-only until the check finishes."),
	OP_REQUIRED("opRequired", "This server lets operators change these settings from the client. You do not have permission to edit them."),
	EDITABLE("editable", "You can edit these server settings. Apply sends your changes to the server for all players."),
	UNAVAILABLE("unavailable", "These server settings are not currently available for editing.");

	private final String key;
	private final String fallback;

	ServerConfigAccess(String key, String fallback) {
		this.key = "mezz_config.config.server.access." + key;
		this.fallback = fallback;
	}

	public Component getDescription() {
		return net.mezzdev.config.gui.internal.LegacyTranslations.translatableWithFallback(key, fallback);
	}
}
