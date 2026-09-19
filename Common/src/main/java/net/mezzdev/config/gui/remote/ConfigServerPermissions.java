package net.mezzdev.config.gui.remote;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Uses the server's configured operator level for remote config edits. */
final class ConfigServerPermissions {
	private ConfigServerPermissions() {}
	static boolean canEdit(MinecraftServer server, ServerPlayer player) {
		return player.createCommandSourceStack().hasPermission(server.getOperatorUserPermissionLevel());
	}
}
