package net.mezzdev.config.gui.test.fabric.custom;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IQuickConfigMenu;
import net.mezzdev.config.gui.api.IQuickConfigMenuFactory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.SequencedMap;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Supplier;

/** Manual integration fixture enabled with -DmezzConfigGui.quickMenuTest=true. */
final class QuickMenuTestScreen extends Screen {
	static final List<String> TOP_DEFAULT_SELECTION = List.of(Option.PRIMARY, Option.MODE, Option.COLOR);
	static final List<String> BOTTOM_DEFAULT_SELECTION = List.of(Option.SECONDARY, Option.COLOR);
	private static @Nullable Supplier<QuickMenuTestScreen> factory;
	final IConfigScreenValue<List<String>> topSelection;
	final IConfigScreenValue<List<String>> bottomSelection;
	final IConfigScreenValue<?> color;
	final SequencedMap<String, IConfigScreenValue<?>> options;
	final IConfigSchema schema;
	final IQuickConfigMenu topMenu;
	final IQuickConfigMenu bottomMenu;
	int hostActions;
	int hostHookKeys;
	int hostHookClicks;
	boolean hostKeyHeld;
	int hostKeyPresses;
	int hostKeyReleases;

	static void install(IQuickConfigMenuFactory menuFactory, IConfigSchema schema) {
		QuickMenuTestScreen.factory = () -> new QuickMenuTestScreen(menuFactory, schema);
		if (!Boolean.getBoolean("mezzConfigGui.quickMenuTest")) {
			return;
		}
		ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
			private boolean opened;
			@Override
			public void onEndTick(net.minecraft.client.Minecraft client) {
				if (!opened && client.screen instanceof TitleScreen) {
					opened = true;
					client.setScreen(create());
				}
			}
		});
	}

	static QuickMenuTestScreen create() {
		return Objects.requireNonNull(factory, "quick menu test factory").get();
	}

	final Button topButton;
	final Button bottomButton;

	@SuppressWarnings("unchecked")
	private QuickMenuTestScreen(IQuickConfigMenuFactory factory, IConfigSchema schema) {
		super(Component.literal("Quick menu integration test"));
		this.schema = schema;
		topMenu = factory.create(Component.literal("Top menu"), TOP_DEFAULT_SELECTION, List.of(Option.SECONDARY));
		bottomMenu = factory.create(Component.literal("Bottom menu"), BOTTOM_DEFAULT_SELECTION, List.of(Option.PRIMARY, Option.MODE));
		topButton = Button.builder(Component.literal("Open top menu"), button -> topMenu.open(button)).bounds(0, 0, 110, 20).build();
		bottomButton = Button.builder(Component.literal("Open bottom menu"), button -> bottomMenu.open(button)).bounds(0, 0, 110, 20).build();
		// Inspect the models so test controls can change selections without opening the full config screen.
		var topModel = QuickMenuClientGameTest.field(topMenu, "model");
		var bottomModel = QuickMenuClientGameTest.field(bottomMenu, "model");
		this.topSelection = (IConfigScreenValue<List<String>>) QuickMenuClientGameTest.field(QuickMenuClientGameTest.field(topModel, "selection"), "value");
		this.bottomSelection = (IConfigScreenValue<List<String>>) QuickMenuClientGameTest.field(QuickMenuClientGameTest.field(bottomModel, "selection"), "value");
		SequencedMap<String, ?> availableRows = (SequencedMap<String, ?>) QuickMenuClientGameTest.field(topModel, "availableRows");
		this.options = new LinkedHashMap<>();
		availableRows.forEach((id, row) -> options.put(id, (IConfigScreenValue<?>) QuickMenuClientGameTest.field(row, "value")));
		this.color = Objects.requireNonNull(options.get(Option.COLOR));
		if (System.getProperty("fabric.client.gametest") != null) {
			QuickMenuClientGameTest.checkPathValidation(factory);
		}
	}

	@Override
	protected void init() {
		ScreenMouseEvents.allowMouseClick(this).register((screen, event) -> {
			hostHookClicks++;
			return true;
		});
		ScreenKeyboardEvents.allowKeyPress(this).register((screen, event) -> {
			if (event.key() == InputConstants.KEY_H) {
				hostHookKeys++;
			}
			return true;
		});
		topButton.setPosition(width / 2 - 55, 4);
		bottomButton.setPosition(width / 2 - 55, height - 24);
		addRenderableWidget(topButton);
		addRenderableWidget(bottomButton);
		addRenderableWidget(Button.builder(Component.literal("Host action (H)"), button -> hostActions++)
			.bounds(width - 144, height / 2, 140, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Clear top"), button -> topSelection.set(List.of()))
			.bounds(4, 4, 110, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Reset top"), button -> topSelection.set(TOP_DEFAULT_SELECTION))
			.bounds(width - 114, 4, 110, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Clear bottom"), button -> bottomSelection.set(List.of()))
			.bounds(4, height - 24, 110, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Reset bottom"), button -> bottomSelection.set(BOTTOM_DEFAULT_SELECTION))
			.bounds(width - 114, height - 24, 110, 20).build());
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_J) {
			hostKeyHeld = true;
			hostKeyPresses++;
			return true;
		}
		if (event.key() == InputConstants.KEY_H) {
			hostActions++;
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (event.key() == InputConstants.KEY_J) {
			hostKeyHeld = false;
			hostKeyReleases++;
			return true;
		}
		return super.keyReleased(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, width, height, 0xFF708090);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.text(font, "Host actions: " + hostActions, 8, 32, 0xFFFFFFFF, false);
		graphics.text(font, "Top selected: " + topSelection.getValue().size(), 8, 44, 0xFFFFFFFF, false);
		graphics.text(font, "Bottom selected: " + bottomSelection.getValue().size(), 8, 56, 0xFFFFFFFF, false);
		graphics.text(font, "Menu open: " + (topMenu.isOpen() || bottomMenu.isOpen()), 8, 68, 0xFFFFFFFF, false);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(new TitleScreen());
	}

	static final class Option {
		static final String PRIMARY = "controls.primaryEnabled";
		static final String SECONDARY = "controls.secondaryEnabled";
		static final String MODE = "controls.mode";
		static final String COLOR = "overview.accentColor";

		static String[] values() {
			return new String[]{PRIMARY, SECONDARY, MODE, COLOR};
		}
	}
}
