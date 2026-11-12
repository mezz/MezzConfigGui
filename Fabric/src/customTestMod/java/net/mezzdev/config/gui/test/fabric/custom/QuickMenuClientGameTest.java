package net.mezzdev.config.gui.test.fabric.custom;

import com.google.common.collect.Iterables;
import net.mezzdev.config.gui.ConfigContentPage;
import net.mezzdev.config.gui.ConfigScreen;
import net.mezzdev.config.gui.fabric.ConfigGuiModMenuPlugin;
import net.mezzdev.config.gui.model.ConfigScreenModel;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IQuickConfigMenu;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.IQuickConfigMenuFactory;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.mezzdev.config.gui.test.fabric.custom.mixin.QuickMenuTestMouseAccessor;
import net.mezzdev.config.gui.test.fabric.custom.mixin.QuickMenuTestKeyboardAccessor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.apache.logging.log4j.LogManager;

import java.util.List;
import java.util.ArrayList;

/** Exercises modal input through Minecraft's mouse and keyboard handlers and Fabric screen events. */
public final class QuickMenuClientGameTest {
	static void checkPathValidation(IQuickConfigMenuFactory factory) {
		try {
			factory.create(Component.literal("Server option"), List.of("server.enabled"), List.of());
			throw new AssertionError("Quick menus must reject server settings");
		} catch (IllegalArgumentException expected) {
			check(expected.getMessage().contains("server config"), "Server settings must be rejected for their schema type");
		}
		for (var paths : List.of(List.of("missing.setting"), List.of(QuickMenuTestScreen.Option.PRIMARY, QuickMenuTestScreen.Option.PRIMARY))) {
			try {
				factory.create(Component.literal("Invalid paths"), paths, List.of());
				throw new AssertionError("Quick menus must reject invalid paths: " + paths);
			} catch (IllegalArgumentException expected) {
				check(expected.getMessage().contains("path"), "Invalid setting paths must be identified");
			}
		}
	}

	static void runTest(QuickMenuTestContext context) {
		context.runOnClient(client -> client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0));
		context.setScreen(QuickMenuTestScreen::create);
		var screen = context.computeOnClient(client -> (QuickMenuTestScreen) client.screen);
		var restoreTop = context.computeOnClient(client -> restoreValue(screen.topSelection));
		var restoreBottom = context.computeOnClient(client -> restoreValue(screen.bottomSelection));
		var restoreColor = context.computeOnClient(client -> restoreValue(screen.color));
		try {
			keyOwnership(context, screen);
			saveRules(context, screen);
			independentMenus(context, screen);
			context.runOnClient(client -> {
				screen.topSelection.set(List.of(QuickMenuTestScreen.Option.values()));
				screen.bottomSelection.set(List.of(QuickMenuTestScreen.Option.values()));
			});
			context.takeScreenshot("quick-menu-closed");
			click(context, screen.width / 2.0, 14);
			context.runOnClient(client -> {
				check(screen.topMenu.isOpen(), "The top anchor must open its menu");
				check(screen.getFocused() == null, "An open menu must clear the host's focus");
				check(bounds(screen.topMenu).getY() > 24, "The menu must fit below the top anchor");
				check(((QuickMenuTestMouseAccessor) client.mouseHandler).mezzConfigGui$getActiveButton() == -1,
					"Opening the menu must not leave a mouse button pressed");
			});
			context.getInput().pressKey(GLFW.GLFW_KEY_H);
			context.runOnClient(client -> {
				check(screen.hostActions == 0, "An open menu must block host shortcuts");
				check(screen.hostHookKeys == 0, "An open menu must handle keys before other screen hooks");
			});
			context.takeScreenshot("quick-menu-below-anchor");
			var initialHeight = context.computeOnClient(client -> bounds(screen.topMenu).getHeight());
			colorPopup(context, screen, screen.topMenu, "below");
			resize(context, screen.topMenu, false, 30);
			scrollAcrossModes(context, screen.topMenu);
			rememberHeight(context, screen.topSelection, screen.topMenu);
			var topPreferred = context.computeOnClient(client -> bounds(screen.topMenu));
			click(context, screen.width / 2.0, 14);
			context.runOnClient(client -> check(!screen.topMenu.isOpen(), "Clicking the anchor must close the menu without reopening it"));
			context.getInput().pressKey(GLFW.GLFW_KEY_H);
			context.runOnClient(client -> {
				check(screen.hostActions == 1, "Host shortcuts must work after closing");
				check(screen.hostHookKeys == 1, "Other screen hooks must work after closing");
			});

			click(context, screen.width / 2.0, screen.height - 14);
			context.runOnClient(client -> {
				check(screen.bottomMenu.isOpen(), "The bottom anchor must open its menu");
				var area = bounds(screen.bottomMenu);
				check(area.getY() + area.getHeight() < screen.height - 24, "The menu must fit above the bottom anchor");
				check(area.getHeight() == initialHeight, "Resizing the top menu must not resize the bottom menu");
			});
			context.takeScreenshot("quick-menu-above-anchor");
			colorPopup(context, screen, screen.bottomMenu, "above");
			resize(context, screen.bottomMenu, true, 45);
			rememberHeight(context, screen.bottomSelection, screen.bottomMenu);
			context.runOnClient(client -> {
				var bottomPreferred = bounds(screen.bottomMenu);
				check(bottomPreferred.getHeight() != topPreferred.getHeight(), "The menus must have different preferred heights for this check");
				check(screen.topMenu.open(screen.topButton) && !screen.bottomMenu.isOpen(), "Opening the top menu must replace the bottom menu");
				check(bounds(screen.topMenu).equals(topPreferred), "Resizing the bottom menu must preserve the top menu's size");
				check(screen.bottomMenu.open(screen.bottomButton) && !screen.topMenu.isOpen(), "Opening the bottom menu must replace the top menu");
				check(bounds(screen.bottomMenu).equals(bottomPreferred), "Each menu must remember its own size");
			});
			var hostClicks = context.computeOnClient(client -> screen.hostHookClicks);
			click(context, screen.width - 74, screen.height / 2.0 + 10);
			context.runOnClient(client -> {
				check(!screen.bottomMenu.isOpen(), "An outside click must close the menu");
				check(screen.hostActions == 1, "An outside click must not activate the host button");
				check(screen.hostHookClicks == hostClicks, "An outside click must not reach other screen hooks");
			});
			click(context, screen.width - 74, screen.height / 2.0 + 10);
			context.runOnClient(client -> check(screen.hostActions == 2, "A subsequent host click must work"));

			context.runOnClient(client -> screen.topSelection.set(List.of(QuickMenuTestScreen.Option.COLOR)));
			click(context, screen.width / 2.0, 14);
			colorPopup(context, screen, screen.topMenu, "single-below");
			context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
			context.runOnClient(client -> screen.bottomSelection.set(List.of(QuickMenuTestScreen.Option.COLOR)));
			click(context, screen.width / 2.0, screen.height - 14);
			colorPopup(context, screen, screen.bottomMenu, "single-above");
			context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);

			context.runOnClient(client -> screen.topSelection.set(List.of()));
			click(context, screen.width / 2.0, 14);
			context.runOnClient(client -> check(screen.topMenu.isOpen(), "The same menu must reopen with an empty selection"));
			context.takeScreenshot("quick-menu-empty");
			var emptyArea = context.computeOnClient(client -> bounds(screen.topMenu));
			click(context, emptyArea.getX() + emptyArea.getWidth() - 26, emptyArea.getY() + 16);
			var addButton = context.computeOnClient(client -> membershipButton(screen.topMenu));
			click(context, addButton.getX() + addButton.getWidth() / 2.0, addButton.getY() + addButton.getHeight() / 2.0);
			context.runOnClient(client -> {
				check(screen.topSelection.getValue().isEmpty(), "Customize must defer saving a batch-mode selection");
				check(screen.bottomSelection.getValue().equals(List.of(QuickMenuTestScreen.Option.COLOR)), "Editing an empty top menu must preserve the bottom selection");
			});
			context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
			context.runOnClient(client -> {
				check(!screen.topMenu.isOpen(), "Escape must close the menu");
				check(screen.topSelection.getValue().size() == 1, "Closing must save the recovered selection");
			});
			click(context, screen.width / 2.0, 14);
			context.setScreen(TitleScreen::new);
			context.runOnClient(client -> check(!screen.topMenu.isOpen(), "Changing screens must close the menu"));
			regularMenuPopup(context, screen);
			LogManager.getLogger().info("Quick menu client checks passed");
		} finally {
			context.setScreen(TitleScreen::new);
			context.runOnClient(client -> restoreTop.run());
			context.runOnClient(client -> restoreBottom.run());
			context.runOnClient(client -> restoreColor.run());
		}
	}

	private static void keyOwnership(QuickMenuTestContext context, QuickMenuTestScreen screen) {
		try {
			context.getInput().holdKey(GLFW.GLFW_KEY_J);
			context.runOnClient(client -> {
				check(screen.hostKeyHeld && screen.hostKeyPresses == 1, "The host must receive the initial key press");
				check(screen.topMenu.open(screen.topButton), "The menu must open while a host key is held");
				client.keyboardHandler.keyPress(client.getWindow().getWindow(), GLFW.GLFW_KEY_J, 0, GLFW.GLFW_REPEAT, 0);
				check(screen.hostKeyPresses == 1, "Repeating a host key must not activate the host while the menu is open");
				((QuickMenuTestKeyboardAccessor) client.keyboardHandler).mezzConfigGui$type(client.getWindow().getWindow(), 'x', 0);
				check(screen.hostCharacters == 0, "An open menu must intercept character input before the host");
			});
			context.getInput().releaseKey(GLFW.GLFW_KEY_J);
			context.runOnClient(client -> check(!screen.hostKeyHeld && screen.hostKeyReleases == 1,
				"A key pressed before opening must release its host state"));

			context.getInput().holdKey(GLFW.GLFW_KEY_J);
			context.runOnClient(client -> {
				check(!screen.hostKeyHeld && screen.hostKeyPresses == 1, "A key pressed in the menu must not reach the host");
				screen.topMenu.close();
				((QuickMenuTestKeyboardAccessor) client.keyboardHandler).mezzConfigGui$type(client.getWindow().getWindow(), 'x', 0);
				check(screen.hostCharacters == 1, "The host must receive character input after the menu closes");
				client.keyboardHandler.keyPress(client.getWindow().getWindow(), GLFW.GLFW_KEY_J, 0, GLFW.GLFW_REPEAT, 0);
				check(screen.hostKeyPresses == 1, "Closing the menu must not leak a captured key's repeat");
			});
			context.getInput().releaseKey(GLFW.GLFW_KEY_J);
			context.runOnClient(client -> check(screen.hostKeyReleases == 1, "A captured key's release must not leak after closing"));
			context.getInput().pressKey(GLFW.GLFW_KEY_J);
			context.runOnClient(client -> check(screen.hostKeyPresses == 2 && screen.hostKeyReleases == 2 && !screen.hostKeyHeld,
				"A fresh press and release must work normally after closing"));
		} finally {
			context.runOnClient(client -> screen.topMenu.close());
			context.getInput().releaseKey(GLFW.GLFW_KEY_J);
		}
	}

	private static void regularMenuPopup(QuickMenuTestContext context, QuickMenuTestScreen parent) {
		context.setScreen(() -> {
			var screen = new ConfigGuiModMenuPlugin().getProvidedConfigScreenFactories()
				.get("mezz_config_gui_test_fabric_custom").create(parent);
			var model = (ConfigScreenModel) field(screen, "model");
			for (int i = 0; i < model.getCategories().size(); i++) {
				if (model.getCategories().get(i).getConfigValues().stream()
					.anyMatch(value -> value.getIdentityKey() == parent.color.getIdentityKey() && value.getApplyMode() == ConfigValueApplyMode.IMMEDIATE)
				) {
					model.setActiveCategoryIndex(i);
					return screen;
				}
			}
			throw new AssertionError("The regular menu must contain the immediate color editor");
		});
		var screen = context.computeOnClient(client -> (ConfigScreen) client.screen);
		context.waitTick();
		var control = context.computeOnClient(client -> {
			var model = (ConfigScreenModel) field(screen, "model");
			var entry = model.getVisibleEntryWidgets().stream()
				.filter(widget -> widget.getConfigValue().getIdentityKey() == parent.color.getIdentityKey()).findFirst().orElseThrow();
			return (ImmutableRect2i) field(entry, "valueArea");
		});
		click(context, control.getX() + control.getWidth() / 2.0, control.getY() + control.getHeight() / 2.0);
		context.runOnClient(client -> check(screen.getValueSelectorArea() != null, "The regular menu must open the color popup"));
		context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
		context.runOnClient(client -> check(client.screen == screen && screen.getValueSelectorArea() == null,
			"Escape must close only the regular menu's popup"));
		click(context, control.getX() + control.getWidth() / 2.0, control.getY() + control.getHeight() / 2.0);
		var popup = context.computeOnClient(client -> screen.getValueSelectorArea());
		check(popup != null, "The regular menu popup must reopen");
		click(context, popup.getX() + popup.getWidth() / 2.0, popup.getY() + 9);
		context.runOnClient(client -> {
			check(screen.getValueSelectorArea() == null, "Selecting a color must close the regular menu popup");
			check(parent.color.getValue().toString().equals("RED"), "The regular menu must keep its immediate save rule");
		});
		click(context, control.getX() + control.getWidth() / 2.0, control.getY() + control.getHeight() / 2.0);
		context.setScreen(() -> parent);
		context.runOnClient(client -> check(screen.getValueSelectorArea() == null, "Removing the regular screen must close its popup"));
	}

	private static void scrollAcrossModes(QuickMenuTestContext context, IQuickConfigMenu menu) {
		var page = context.computeOnClient(client -> (ConfigContentPage) field(menu, "page"));
		var marker = context.computeOnClient(client -> page.getScrollState().getMarkerArea());
		check(!marker.isEmpty(), "The resized menu must have a scrollbar");
		move(context, marker.getX() + marker.getWidth() / 2.0, marker.getY() + 2);
		context.getInput().holdMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.runOnClient(client -> check(page.getScrollState().getPosition() == 0, "Grabbing the scrollbar must not jump"));
		var track = context.computeOnClient(client -> page.getScrollState().getScrollbarArea());
		move(context, marker.getX() + marker.getWidth() / 2.0, track.getY() + track.getHeight() - marker.getHeight() + 2);
		context.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.waitTick();
		var bottom = context.computeOnClient(client -> page.getScrollState().getPosition());
		check(bottom > 0, "Dragging the scrollbar must scroll the normal menu");
		toggleCustomize(context, menu);
		context.runOnClient(client -> check(page.getScrollState().getPosition() == bottom, "Customize must retain the scroll position"));
		marker = context.computeOnClient(client -> page.getScrollState().getMarkerArea());
		move(context, marker.getX() + marker.getWidth() / 2.0, marker.getY() + 2);
		context.getInput().holdMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		move(context, marker.getX() + marker.getWidth() / 2.0, track.getY() + 2);
		context.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.waitTick();
		toggleCustomize(context, menu);
		context.runOnClient(client -> check(page.getScrollState().getPosition() == 0, "Normal mode must retain the Customize scroll position"));
	}

	private static void saveRules(QuickMenuTestContext context, QuickMenuTestScreen screen) {
		var primary = screen.options.get(QuickMenuTestScreen.Option.PRIMARY);
		var secondary = screen.options.get(QuickMenuTestScreen.Option.SECONDARY);
		var restorePrimary = context.computeOnClient(client -> restoreValue(primary));
		var restoreSecondary = context.computeOnClient(client -> restoreValue(secondary));
		try {
			for (String close : List.of("outside", "escape", "replace", "screen", "api", "full")) {
				context.runOnClient(client -> {
					screen.topSelection.set(List.of(QuickMenuTestScreen.Option.PRIMARY, QuickMenuTestScreen.Option.SECONDARY));
					screen.topMenu.open(screen.topButton);
				});
				boolean oldPrimary = context.computeOnClient(client -> (Boolean) primary.getValue());
				boolean oldSecondary = context.computeOnClient(client -> (Boolean) secondary.getValue());
				clickEntry(context, screen.topMenu, 0);
				clickEntry(context, screen.topMenu, 1);
				context.runOnClient(client -> {
					check(primary.getValue().equals(oldPrimary), "Batch-mode edits must stay pending while the menu is open");
					check(secondary.getValue().equals(!oldSecondary), "Immediate-mode edits must save while the menu is open");
				});
				toggleCustomize(context, screen.topMenu);
				var remove = context.computeOnClient(client -> membershipButton(screen.topMenu));
				click(context, remove.getX() + remove.getWidth() / 2.0, remove.getY() + remove.getHeight() / 2.0);
				toggleCustomize(context, screen.topMenu);
				context.runOnClient(client -> check(visibleOptionsMatch(screen, screen.topMenu, List.of(QuickMenuTestScreen.Option.SECONDARY)),
					"Pending customization must immediately update the visible rows"));
				List<Integer> batches = new ArrayList<>();
				Runnable unsubscribe = context.computeOnClient(client -> screen.schema.addPendingBatchListener(changes -> batches.add(changes.size())));
				try {
					switch (close) {
						case "outside" -> click(context, screen.width / 2.0, 14);
						case "escape" -> context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
						case "replace" -> context.runOnClient(client -> screen.bottomMenu.open(screen.bottomButton));
						case "screen" -> context.setScreen(TitleScreen::new);
						case "api" -> context.runOnClient(client -> screen.topMenu.close());
						case "full" -> {
							var button = context.computeOnClient(client -> fullConfigButton(screen.topMenu));
							click(context, button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0);
							context.runOnClient(client -> check(client.screen instanceof ConfigScreen, "The header button must open the full config screen"));
						}
						default -> throw new AssertionError(close);
					}
					context.runOnClient(client -> {
						check(!screen.topMenu.isOpen(), "The menu must close via " + close);
						check(primary.getValue().equals(!oldPrimary), "Closing must save edits to removed options via " + close);
						check(screen.topSelection.getValue().equals(List.of(QuickMenuTestScreen.Option.SECONDARY)), "Closing must save customization via " + close);
						check(batches.equals(List.of(1)), "The pending option must save in its schema batch via " + close + ": " + batches);
					});
				} finally {
					context.runOnClient(client -> unsubscribe.run());
					context.runOnClient(client -> screen.bottomMenu.close());
					context.setScreen(() -> screen);
				}
			}
		} finally {
			context.runOnClient(client -> {
				screen.topMenu.close();
				restorePrimary.run();
				restoreSecondary.run();
			});
		}
	}

	private static void clickEntry(QuickMenuTestContext context, IQuickConfigMenu menu, int index) {
		var control = context.computeOnClient(client -> {
			var page = (ConfigContentPage) field(menu, "page");
			return (ImmutableRect2i) field(page.getEntries().get(index), "valueArea");
		});
		click(context, control.getX() + control.getWidth() / 2.0, control.getY() + control.getHeight() / 2.0);
	}

	private static void independentMenus(QuickMenuTestContext context, QuickMenuTestScreen screen) {
		click(context, screen.width - 59, 14);
		click(context, screen.width - 59, screen.height - 14);
		click(context, screen.width / 2.0, 14);
		context.takeScreenshot("quick-menu-top-defaults");
		context.runOnClient(client -> check(visibleOptionsMatch(screen, screen.topMenu, QuickMenuTestScreen.TOP_DEFAULT_SELECTION), "The top menu must display its own saved selection"));
		toggleCustomize(context, screen.topMenu);
		var removeButton = context.computeOnClient(client -> membershipButton(screen.topMenu));
		click(context, removeButton.getX() + removeButton.getWidth() / 2.0, removeButton.getY() + removeButton.getHeight() / 2.0);
		context.runOnClient(client -> {
			check(screen.topSelection.getValue().equals(QuickMenuTestScreen.TOP_DEFAULT_SELECTION), "Customize must stage the top menu's selection");
			check(screen.bottomSelection.getValue().equals(QuickMenuTestScreen.BOTTOM_DEFAULT_SELECTION), "Customizing the top menu must not change the bottom selection");
		});
		context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
		context.runOnClient(client -> check(screen.topSelection.getValue().equals(List.of(QuickMenuTestScreen.Option.MODE, QuickMenuTestScreen.Option.COLOR)),
			"Closing must save the top menu's selection"));
		click(context, screen.width / 2.0, screen.height - 14);
		context.takeScreenshot("quick-menu-bottom-defaults");
		context.runOnClient(client -> check(visibleOptionsMatch(screen, screen.bottomMenu, QuickMenuTestScreen.BOTTOM_DEFAULT_SELECTION), "The bottom menu must display its own saved selection"));
		context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
		click(context, 59, 14);
		context.runOnClient(client -> {
			check(screen.topSelection.getValue().isEmpty(), "Clear top must clear the top selection");
			check(screen.bottomSelection.getValue().equals(QuickMenuTestScreen.BOTTOM_DEFAULT_SELECTION), "Clear top must preserve the bottom selection");
		});
		click(context, screen.width - 59, 14);
		click(context, 59, screen.height - 14);
		context.runOnClient(client -> {
			check(screen.topSelection.getValue().equals(QuickMenuTestScreen.TOP_DEFAULT_SELECTION), "Reset top must restore its defaults");
			check(screen.bottomSelection.getValue().isEmpty(), "Clear bottom must clear only the bottom selection");
		});
	}

	private static boolean visibleOptionsMatch(QuickMenuTestScreen screen, IQuickConfigMenu menu, List<String> paths) {
		var page = (ConfigContentPage) field(menu, "page");
		var actual = page.getEntries().stream().map(entry -> entry.getConfigValue().getIdentityKey()).toList();
		var expected = paths.stream().map(path -> screen.options.get(path).getIdentityKey()).toList();
		return actual.equals(expected);
	}

	private static void colorPopup(QuickMenuTestContext context, QuickMenuTestScreen screen, IQuickConfigMenu menu, String position) {
		var before = context.computeOnClient(client -> bounds(menu));
		var page = context.computeOnClient(client -> (ConfigContentPage) field(menu, "page"));
		var control = context.computeOnClient(client -> (ImmutableRect2i) field(Iterables.getLast(page.getEntries()), "valueArea"));
		click(context, control.getX() + control.getWidth() / 2.0, control.getY() + control.getHeight() / 2.0);
		context.takeScreenshot("quick-menu-color-" + position);
		context.runOnClient(client -> {
			check(page.hasPopup(), "The accent color control must open its popup");
			check(bounds(menu).equals(before), "Opening the color popup must preserve menu bounds: " + before + " -> " + bounds(menu));
		});
		context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
		context.runOnClient(client -> {
			check(!page.hasPopup() && menu.isOpen(), "Escape must close only the color popup");
			check(bounds(menu).equals(before), "Closing the color popup must preserve menu bounds");
		});
		click(context, control.getX() + control.getWidth() / 2.0, control.getY() + control.getHeight() / 2.0);
		var popup = context.computeOnClient(client -> page.getPopupArea());
		check(popup.getHeight() == 72, "All four accent colors must remain visible, even in a one-row menu");
		double x = popup.getX() + popup.getWidth() / 2.0;
		double y = popup.getY() + popup.getHeight() - 9;
		if (position.startsWith("single")) {
			// Pick the end away from the settings list to exercise popup input beyond the scrolling viewport.
			var viewport = context.computeOnClient(client -> (ImmutableRect2i) field(page, "area"));
			if (viewport.contains(x, y)) {
				y = popup.getY() + 9;
			}
			check(!viewport.contains(x, y), "The one-row fixture must exercise a color option outside the settings list");
		}
		String selectedColor;
		if (y < popup.getY() + 18) {
			selectedColor = "RED";
		} else {
			selectedColor = "GOLD";
		}
		click(context, x, y);
		context.runOnClient(client -> {
			check(menu.isOpen() && !page.hasPopup(), "Selecting a color must close only the color popup");
			var entry = Iterables.getLast(page.getEntries());
			Object displayedColor = entry.getPendingChange().map(change -> (Object) change.value()).orElseGet(screen.color::getValue);
			check(displayedColor.toString().equals(selectedColor), "Clicking a popup option must update the displayed color");
			check(screen.color.getValue().toString().equals(selectedColor), "The popup must inherit immediate saving from the full config screen");
			check(bounds(menu).equals(before), "Selecting a color must preserve menu bounds");
		});
		click(context, control.getX() + control.getWidth() / 2.0, control.getY() + control.getHeight() / 2.0);
		click(context, before.getX() + before.getWidth() - 26, before.getY() + 16);
		context.runOnClient(client -> {
			check(!page.hasPopup() && menu.isOpen(), "Clicking menu chrome must dismiss the popup");
			check(!(boolean) field(field(menu, "model"), "editing"), "Dismissing the popup must not click through to Customize");
		});
	}

	private static <T> Runnable restoreValue(IConfigScreenValue<T> value) {
		T original = value.getValue();
		return () -> value.set(original);
	}

	private static void rememberHeight(QuickMenuTestContext context, IConfigScreenValue<List<String>> selectionValue, IQuickConfigMenu menu) {
		var preferred = context.computeOnClient(client -> bounds(menu));
		for (var selection : List.of(List.of(QuickMenuTestScreen.Option.COLOR), List.<String>of())) {
			context.runOnClient(client -> selectionValue.set(selection));
			context.waitTick();
			var fitted = context.computeOnClient(client -> bounds(menu));
			check(fitted.getHeight() < preferred.getHeight(), "A short selection must fit to its content");
			toggleCustomize(context, menu);
			context.runOnClient(client -> check(bounds(menu).equals(preferred),
				"Customize must restore the user's preferred bounds: " + preferred + " -> " + bounds(menu)));
			toggleCustomize(context, menu);
			context.runOnClient(client -> check(bounds(menu).equals(fitted), "Leaving Customize must fit the short selection again"));
		}
		context.runOnClient(client -> {
			menu.close();
			var screen = (QuickMenuTestScreen) client.screen;
			check(screen != null, "The quick menu host screen must remain open");
			var button = screen.topButton;
			if (menu == screen.bottomMenu) {
				button = screen.bottomButton;
			}
			check(menu.open(button), "The same menu must reopen after resizing");
		});
		toggleCustomize(context, menu);
		context.runOnClient(client -> check(bounds(menu).equals(preferred), "Reopening the menu must retain the preferred height"));
		context.runOnClient(client -> selectionValue.set(List.of(QuickMenuTestScreen.Option.values())));
		toggleCustomize(context, menu);
		context.runOnClient(client -> check(bounds(menu).equals(preferred), "Restoring the full selection must reuse the preferred height"));
	}

	private static void toggleCustomize(QuickMenuTestContext context, IQuickConfigMenu menu) {
		var area = context.computeOnClient(client -> bounds(menu));
		click(context, area.getX() + area.getWidth() - 26, area.getY() + 16);
	}

	private static void resize(QuickMenuTestContext context, IQuickConfigMenu menu, boolean above, int amount) {
		var before = context.computeOnClient(client -> bounds(menu));
		double x = before.getX() + before.getWidth() / 2.0;
		double y = before.getY() + before.getHeight() - 1;
		double delta = -amount;
		if (above) {
			y = before.getY() + 1;
			delta = amount;
		}
		move(context, x, y);
		context.getInput().holdMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		move(context, x, y + delta);
		context.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.waitTick();
		context.runOnClient(client -> check(bounds(menu).getHeight() < before.getHeight(), "The edge away from the anchor must resize the menu"));
	}

	private static void click(QuickMenuTestContext context, double x, double y) {
		move(context, x, y);
		context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.waitTick();
	}

	private static void move(QuickMenuTestContext context, double x, double y) {
		double[] pixel = context.computeOnClient(client -> new double[]{
			x * client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth(),
			y * client.getWindow().getScreenHeight() / client.getWindow().getGuiScaledHeight()
		});
		context.getInput().setCursorPos(pixel[0], pixel[1]);
	}

	private static ImmutableRect2i bounds(IQuickConfigMenu menu) {
		return (ImmutableRect2i) field(field(menu, "layout"), "area");
	}

	private static ImmutableRect2i fullConfigButton(IQuickConfigMenu menu) {
		try {
			var layout = field(menu, "layout");
			var method = layout.getClass().getDeclaredMethod("fullConfigButtonArea");
			method.setAccessible(true);
			return (ImmutableRect2i) method.invoke(layout);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static ImmutableRect2i membershipButton(IQuickConfigMenu menu) {
		try {
			var layout = field(menu, "layout");
			var method = layout.getClass().getDeclaredMethod("controlArea", int.class, int.class);
			method.setAccessible(true);
			return (ImmutableRect2i) method.invoke(layout, 0, 0);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	static Object field(Object object, String name) {
		try {
			var field = object.getClass().getDeclaredField(name);
			field.setAccessible(true);
			return field.get(object);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
