package net.mezzdev.config.gui.test.forge.nativeconfig;

import com.electronwill.nightconfig.toml.TomlParser;
import net.mezzdev.config.gui.ConfigGui;
import net.mezzdev.config.gui.ConfigValueAccess;
import net.mezzdev.config.gui.MezzConfigScreen;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.forge.config.ForgeConfigScreenConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.gui.LoadingErrorScreen;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.versions.forge.ForgeVersion;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Mod(ForgeNativeConfigTestMod.MOD_ID)
public final class ForgeNativeConfigTestMod {
	public static final String MOD_ID = "mezz_config_gui_test_forge_native";
	private static final String CLIENT_FILE = MOD_ID + "-client.toml";
	private static final ConfigScreenHandler.ConfigScreenFactory CUSTOM_SCREEN = new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new TitleScreen());
	private int renderedTicks;
	private boolean started;

	public ForgeNativeConfigTestMod() {
		ModLoadingContext context = ModLoadingContext.get();
		ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
		client.comment("Animal settings").push("animals");
		client.defineInRange("limit", 4, 0, 32);
		client.defineListAllowEmpty(List.of("names"), () -> List.of("cow", "pig"), value -> value instanceof String);
		client.pop();
		context.registerConfig(ModConfig.Type.CLIENT, client.build(), CLIENT_FILE);
		for (String section : List.of("animals", "items")) {
			ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();
			common.define(section + ".enabled", true);
			context.registerConfig(ModConfig.Type.COMMON, common.build(), MOD_ID + "-" + section + ".toml");
		}
		ForgeConfigSpec.Builder server = new ForgeConfigSpec.Builder();
		server.worldRestart().define("enabled", true);
		context.registerConfig(ModConfig.Type.SERVER, server.build());
		context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> CUSTOM_SCREEN);
		MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
	}

	private void onClientTick(TickEvent.ClientTickEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (event.phase != TickEvent.Phase.END || minecraft.getOverlay() != null) {
			return;
		}
		try {
			check(!(minecraft.screen instanceof LoadingErrorScreen), "The loader reported startup warnings or errors; see the client log.");
			if (!started && minecraft.screen instanceof TitleScreen) {
				started = true;
				testDiscoveryAndEditing(minecraft);
			} else if (started && ++renderedTicks == 10) {
				check(minecraft.screen instanceof MezzConfigScreen, "The generated config screen must remain open while rendering.");
				Path result = Path.of(System.getProperty("mezzConfigGui.nativeTest.successFile"));
				Files.createDirectories(result.getParent());
				String loader = ForgeVersion.getGroup() + ":" + ForgeVersion.getVersion();
				Files.writeString(result, "passed " + loader + "\n");
				System.out.println("MezzConfig GUI native config client test passed on " + loader + ".");
				minecraft.stop();
			}
		} catch (Exception | AssertionError exception) {
			exception.printStackTrace();
			minecraft.stop();
		}
	}

	@SuppressWarnings("unchecked")
	private static void testDiscoveryAndEditing(Minecraft minecraft) throws Exception {
		check(ForgeVersion.getGroup().equals(System.getProperty("mezzConfigGui.nativeTest.expectedGroup")),
			"The test must run on the requested loader, not a fallback Forge runtime.");
		check(ForgeVersion.getVersion().equals(System.getProperty("mezzConfigGui.nativeTest.expectedVersion")),
			"The test must run on the pinned loader version.");
		var container = ModList.get().getModContainerById(MOD_ID).orElseThrow();
		check(container.getCustomExtension(ConfigScreenHandler.ConfigScreenFactory.class).orElseThrow() == CUSTOM_SCREEN,
			"Automatic discovery must preserve an existing screen factory.");
		var screens = ForgeConfigScreenConfigs.getConfigScreens();
		var nativeScreen = screens.stream().filter(screen -> screen.getModId().equals(MOD_ID)).findFirst().orElseThrow();
		var categories = nativeScreen.getSchema().getCategories();
		check(categories.size() == 3, "Discovery must include both Common files and omit the unloaded Server config.");
		check(categories.get(0).getName().equals(CLIENT_FILE), "Client files must precede Common files.");
		check(categories.get(1).getNavigationGroup().name().equals(categories.get(2).getNavigationGroup().name()),
			"Both Common files must share one navigation group.");
		var value = (IConfigScreenValue<Integer>) categories.get(0).getConfigValues().stream()
			.filter(option -> option.getName().equals("animals.limit")).findFirst().orElseThrow();
		AtomicInteger notifications = new AtomicInteger();
		Runnable removeListener = value.addListener(ignored -> notifications.incrementAndGet());
		check(value.set(12), "A valid native value must be editable.");
		check(!value.set(12), "Writing an unchanged value must not trigger another update.");
		check(notifications.get() == 1, "One saved edit must notify listeners once.");
		ModConfig client = ConfigTracker.INSTANCE.fileMap().get(CLIENT_FILE);
		try (var reader = Files.newBufferedReader(client.getFullPath())) {
			check(new TomlParser().parse(reader).<Integer>get("animals.limit") == 12, "Native edits must persist to TOML.");
		}
		try {
			value.set(100);
			throw new AssertionError("Out-of-range values must be rejected.");
		} catch (IllegalArgumentException expected) {
			check(value.getValue() == 12, "Rejected edits must leave the current value intact.");
		}
		String loadedToml = "[animals]\nlimit = 7\nnames = [\"cow\", \"pig\"]\n";
		client.acceptSyncedConfig(loadedToml.getBytes(StandardCharsets.UTF_8));
		check(value.getValue() == 7 && notifications.get() == 2, "Native reload events must update values and notify listeners.");
		removeListener.run();
		client.acceptSyncedConfig(loadedToml.replace("7", "8").getBytes(StandardCharsets.UTF_8));
		check(notifications.get() == 2, "Removed listeners must not receive reload events.");

		ModConfig server = ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.SERVER).stream()
			.filter(config -> config.getModId().equals(MOD_ID)).findFirst().orElseThrow();
		server.acceptSyncedConfig("enabled = true".getBytes(StandardCharsets.UTF_8));
		check(nativeScreen.getSchema().getCategories().size() == 4, "Opening a screen must pick up newly loaded Server configs.");
		var serverValue = (IConfigScreenValue<Boolean>) nativeScreen.getSchema().getCategories().get(3).getConfigValues().iterator().next();
		check(!ConfigValueAccess.isEditable(serverValue), "A native Server config must be read-only without a singleplayer server.");
		try {
			serverValue.set(false);
			throw new AssertionError("Client writes to native Server configs must be rejected.");
		} catch (IllegalStateException expected) {
			check(serverValue.getValue(), "Rejected Server edits must leave the server value intact.");
		}

		var registry = ConfigGui.createScreenFactoryRegistryFromInternalConfigs(screens, List.of());
		var factory = registry.getFactories().get(MOD_ID);
		check(factory != null, "Native configs must produce a screen without a GUI plugin.");
		minecraft.setScreen(factory.create(minecraft.screen));
		check(!minecraft.screen.children().isEmpty(), "The generated config screen must create its controls.");
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
