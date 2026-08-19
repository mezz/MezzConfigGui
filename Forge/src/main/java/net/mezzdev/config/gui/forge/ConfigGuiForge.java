package net.mezzdev.config.gui.forge;

import net.mezzdev.config.gui.remote.RemoteConfigEditorServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge entry point for the config GUI mod.
 */
@Mod(ConfigGuiForge.MOD_ID)
public final class ConfigGuiForge {
	public static final String MOD_ID = "mezz_config_gui";

	public ConfigGuiForge(FMLJavaModLoadingContext context) {
		ConfigGuiForgeNetwork network = new ConfigGuiForgeNetwork();
		MinecraftForge.EVENT_BUS.addListener(
			(ServerStartedEvent event) -> RemoteConfigEditorServer.onServerStarted(event.getServer())
		);
		MinecraftForge.EVENT_BUS.addListener(
			(ServerStoppedEvent event) -> RemoteConfigEditorServer.onServerStopped(event.getServer())
		);
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				RemoteConfigEditorServer.onServerTick(event.getServer());
			}
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				RemoteConfigEditorServer.onPlayerDisconnect(player);
			}
		});
		ConfigGuiForgeClientSafeRunner clientSafeRunner = new ConfigGuiForgeClientSafeRunner(context.getModEventBus(), network);
		DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> clientSafeRunner::registerClient);
	}
}
