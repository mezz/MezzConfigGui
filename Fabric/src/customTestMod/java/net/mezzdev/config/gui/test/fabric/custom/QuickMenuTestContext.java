package net.mezzdev.config.gui.test.fabric.custom;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.mezzdev.config.gui.test.fabric.custom.mixin.QuickMenuTestMouseAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Runs opt-in screen tests on 1.21.1 using the real Minecraft input handlers. */
final class QuickMenuTestContext {
	private final Minecraft client;
	private final AtomicReference<CompletableFuture<Void>> nextTick = new AtomicReference<>();
	private final Input input = new Input();

	private QuickMenuTestContext(Minecraft client) {
		this.client = client;
	}

	static void register() {
		// This run feeds Minecraft's input handlers directly, so it does not need OS focus.
		ClientTickEvents.START_CLIENT_TICK.register(client -> client.setWindowActive(true));
		ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
			private QuickMenuTestContext context;
			private int startupTicks;

			@Override
			public void onEndTick(Minecraft client) {
				if (context == null) {
					if (client.getOverlay() == null && client.screen instanceof TitleScreen) {
						context = new QuickMenuTestContext(client);
						Thread thread = new Thread(context::run, "MezzConfig quick-menu test");
						thread.setDaemon(true);
						thread.start();
					} else if (++startupTicks > 600) {
						throw new AssertionError("Timed out waiting for the client title screen: " + client.screen + " with overlay " + client.getOverlay());
					}
					return;
				}
				var tick = context.nextTick.getAndSet(null);
				if (tick != null) {
					tick.complete(null);
				}
			}
		});
	}

	private void run() {
		try {
			QuickMenuClientGameTest.runTest(this);
			Files.writeString(client.gameDirectory.toPath().resolve("quick-menu-test-passed"), "passed\n");
			LogManager.getLogger().info("MEZZ_CONFIG_GUI_QUICK_MENU_TEST_PASSED");
		} catch (Throwable failure) {
			LogManager.getLogger().error("MezzConfig GUI quick-menu test failed", failure);
		} finally {
			runOnClient(Minecraft::stop);
		}
	}

	<T> T computeOnClient(Function<Minecraft, T> operation) {
		return client.submit(() -> operation.apply(client)).orTimeout(30, TimeUnit.SECONDS).join();
	}

	void runOnClient(Consumer<Minecraft> operation) {
		computeOnClient(minecraft -> {
			operation.accept(minecraft);
			return null;
		});
	}

	void setScreen(Supplier<Screen> factory) {
		runOnClient(minecraft -> minecraft.setScreen(factory.get()));
		waitTick();
	}

	void waitTick() {
		var tick = new CompletableFuture<Void>();
		nextTick.set(tick);
		tick.orTimeout(30, TimeUnit.SECONDS).join();
	}

	void takeScreenshot(String name) {
		waitTick();
		waitTick();
		runOnClient(minecraft -> {
			var output = minecraft.gameDirectory.toPath().resolve("screenshots").resolve(name + ".png");
			try (var screenshot = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
				Files.createDirectories(output.getParent());
				screenshot.writeToFile(output);
			} catch (IOException e) {
				throw new AssertionError("Could not save test screenshot " + name, e);
			}
		});
	}

	Input getInput() {
		return input;
	}

	final class Input {
		void pressKey(int key) {
			holdKey(key);
			releaseKey(key);
		}

		void holdKey(int key) {
			key(key, GLFW.GLFW_PRESS);
		}

		void releaseKey(int key) {
			key(key, GLFW.GLFW_RELEASE);
		}

		private void key(int key, int action) {
			runOnClient(minecraft -> minecraft.keyboardHandler.keyPress(minecraft.getWindow().getWindow(), key, 0, action, 0));
		}

		void pressMouse(int button) {
			holdMouse(button);
			releaseMouse(button);
		}

		void holdMouse(int button) {
			mouse(button, GLFW.GLFW_PRESS);
		}

		void releaseMouse(int button) {
			mouse(button, GLFW.GLFW_RELEASE);
		}

		private void mouse(int button, int action) {
			runOnClient(minecraft -> ((QuickMenuTestMouseAccessor) minecraft.mouseHandler)
				.mezzConfigGui$press(minecraft.getWindow().getWindow(), button, action, 0));
		}

		void setCursorPos(double x, double y) {
			runOnClient(minecraft -> {
				((QuickMenuTestMouseAccessor) minecraft.mouseHandler).mezzConfigGui$move(minecraft.getWindow().getWindow(), x, y);
			});
		}
	}
}
