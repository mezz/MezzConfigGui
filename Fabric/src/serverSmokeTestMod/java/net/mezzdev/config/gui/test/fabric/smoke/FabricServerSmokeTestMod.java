package net.mezzdev.config.gui.test.fabric.smoke;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class FabricServerSmokeTestMod implements ModInitializer {
	private static final String SUCCESS_FILE_PROPERTY = "mezzConfigGui.loaderSmokeTest.successFile";

	@Override
	public void onInitialize() {
		String successFile = System.getProperty(SUCCESS_FILE_PROPERTY);
		if (successFile != null) {
			ServerLifecycleEvents.SERVER_STARTED.register(server -> CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS)
				.execute(() -> server.execute(() -> runSmokeTest(server, Path.of(successFile)))));
		}
	}

	private static void runSmokeTest(MinecraftServer server, Path successFile) {
		try {
			FabricLoader loader = FabricLoader.getInstance();
			if (!loader.isModLoaded("mezz_config") || !loader.isModLoaded("mezz_config_gui")) {
				throw new IllegalStateException("Fabric did not load MezzConfig and MezzConfig GUI.");
			}
			Files.createDirectories(successFile.getParent());
			Files.writeString(successFile, "passed\n");
			System.out.println("MezzConfig GUI Fabric loader smoke test passed.");
		} catch (IOException e) {
			throw new IllegalStateException("Failed to record the Fabric loader smoke-test result.", e);
		} finally {
			server.halt(false);
		}
	}
}
