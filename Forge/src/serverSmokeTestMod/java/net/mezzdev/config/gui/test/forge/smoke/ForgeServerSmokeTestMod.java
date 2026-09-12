package net.mezzdev.config.gui.test.forge.smoke;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Mod(ForgeServerSmokeTestMod.MOD_ID)
public final class ForgeServerSmokeTestMod {
	public static final String MOD_ID = "mezz_config_gui_test_forge_smoke";
	private static final String SUCCESS_FILE_PROPERTY = "mezzConfigGui.loaderSmokeTest.successFile";

	public ForgeServerSmokeTestMod() {
		String successFile = System.getProperty(SUCCESS_FILE_PROPERTY);
		if (successFile != null) {
			MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
				MinecraftServer server = event.getServer();
				CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS)
					.execute(() -> server.execute(() -> runSmokeTest(server, Path.of(successFile))));
			});
		}
	}

	private static void runSmokeTest(MinecraftServer server, Path successFile) {
		try {
			ModList modList = ModList.get();
			if (!modList.isLoaded("mezz_config") || !modList.isLoaded("mezz_config_gui")) {
				throw new IllegalStateException("Forge did not load MezzConfig and MezzConfig GUI.");
			}
			Files.createDirectories(successFile.getParent());
			Files.writeString(successFile, "passed\n");
			System.out.println("MezzConfig GUI Forge loader smoke test passed.");
		} catch (IOException e) {
			throw new IllegalStateException("Failed to record the Forge loader smoke-test result.", e);
		} finally {
			server.halt(false);
		}
	}
}
