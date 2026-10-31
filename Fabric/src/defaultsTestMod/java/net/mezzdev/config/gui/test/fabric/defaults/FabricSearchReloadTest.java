package net.mezzdev.config.gui.test.fabric.defaults;

import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.mezzdev.config.gui.ConfigGui;
import net.mezzdev.config.gui.ConfigScreen;
import net.mezzdev.config.gui.api.IConfigGuiPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.apache.logging.log4j.LogManager;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Opt-in client regression for search input across screen and resource reload lifecycles.
 */
final class FabricSearchReloadTest {
	private static final String FILTER = "enabled";
	private static final int CURSOR = 3;
	private final IConfigGuiPlugin plugin;
	@Nullable
	private ConfigScreen screen;
	@Nullable
	private Screen parent;
	@Nullable
	private EditBox search;
	@Nullable
	private CompletableFuture<Void> reload;
	private int ticks;
	private boolean finished;

	private FabricSearchReloadTest(IConfigGuiPlugin plugin) {
		this.plugin = plugin;
	}

	static void register(IConfigGuiPlugin plugin) {
		FabricSearchReloadTest test = new FabricSearchReloadTest(plugin);
		ClientTickEvents.END_CLIENT_TICK.register(test::tick);
	}

	private void tick(Minecraft minecraft) {
		if (finished) {
			return;
		}
		check(++ticks < 6000, "Timed out waiting for the search reload test");
		if (screen == null) {
			if (minecraft.getOverlay() != null || !(minecraft.screen instanceof TitleScreen)) {
				return;
			}
			start(minecraft);
			return;
		}
		CompletableFuture<Void> reload = Objects.requireNonNull(this.reload);
		if (!reload.isDone() || minecraft.getOverlay() != null) {
			return;
		}
		reload.join();
		check(minecraft.screen == screen, "Resource reload replaced the config screen");
		verifySearch("resource reload");
		verifyTyping();

		minecraft.setScreen(parent);
		minecraft.setScreen(screen);
		EditBox search = Objects.requireNonNull(this.search);
		check(FILTER.equals(search.getValue()), "Search text changed when reopening the same screen");
		check(screen.mouseClicked(search.getX() + 2, search.getY() + 2, GLFW.GLFW_MOUSE_BUTTON_LEFT), "Reopened search click was not handled");
		positionCursor();
		verifySearch("reopening the same screen");
		verifyTyping();
		finished = true;
		LogManager.getLogger().info("MEZZ_CONFIG_GUI_SEARCH_RELOAD_TEST_PASSED");
		minecraft.stop();
	}

	private void start(Minecraft minecraft) {
		if (Boolean.getBoolean("mezzConfigGui.testJeiKeyMapping")) {
			FabricJeiKeyMappingTest.run();
		}
		parent = minecraft.screen;
		Screen created = ConfigGui.createScreenFactories(List.of(plugin)).get(plugin.getModId()).create(parent);
		check(created instanceof ConfigScreen, "Test plugin did not create a config screen");
		screen = (ConfigScreen) created;
		minecraft.setScreen(screen);
		search = screen.children().stream()
			.filter(EditBox.class::isInstance)
			.map(EditBox.class::cast)
			.findFirst()
			.orElseThrow(() -> new AssertionError("Search box is not registered as a screen child"));
		check(screen.mouseClicked(search.getX() + 2, search.getY() + 2, GLFW.GLFW_MOUSE_BUTTON_LEFT), "Search click was not handled");
		for (char character : FILTER.toCharArray()) {
			check(screen.charTyped(character, 0), "Search character was not handled");
		}
		positionCursor();
		verifySearch("initial input");

		int width = screen.width;
		int height = screen.height;
		screen.resize(minecraft, width - 20, height - 20);
		verifySearch("resize");
		verifyTyping();
		screen.init(minecraft, width, height);
		verifySearch("reinitialization");
		verifyTyping();
		reload = minecraft.reloadResourcePacks();
	}

	private void verifySearch(String stage) {
		ConfigScreen screen = Objects.requireNonNull(this.screen);
		EditBox search = Objects.requireNonNull(this.search);
		check(screen.children().stream().filter(child -> child == search).count() == 1, "Search child registration lost after " + stage);
		check(FILTER.equals(search.getValue()), "Search text changed after " + stage + ": " + search.getValue());
		check(search.getCursorPosition() == CURSOR, "Search cursor moved after " + stage);
		check(search.isFocused(), "Search focus lost after " + stage);
	}

	private void verifyTyping() {
		ConfigScreen screen = Objects.requireNonNull(this.screen);
		EditBox search = Objects.requireNonNull(this.search);
		check(screen.charTyped('x', 0), "Search stopped accepting characters");
		String edited = FILTER.substring(0, CURSOR) + "x" + FILTER.substring(CURSOR);
		check(edited.equals(search.getValue()), "Typed character did not reach the search box at its cursor");
		check(screen.keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, 0), "Search stopped accepting keyboard events");
		check(FILTER.equals(search.getValue()), "Backspace did not reach the search box");
	}

	private void positionCursor() {
		ConfigScreen screen = Objects.requireNonNull(this.screen);
		check(screen.keyPressed(GLFW.GLFW_KEY_HOME, 0, 0), "Search home key was not handled");
		for (int i = 0; i < CURSOR; i++) {
			check(screen.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0), "Search cursor key was not handled");
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
