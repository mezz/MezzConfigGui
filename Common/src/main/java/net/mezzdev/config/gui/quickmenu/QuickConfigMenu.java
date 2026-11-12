package net.mezzdev.config.gui.quickmenu;

import net.mezzdev.config.gui.ConfigContentPage;
import net.mezzdev.config.gui.ConfigScreenLayout.ResizeHandle;
import net.mezzdev.config.gui.MezzConfigScreen;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IQuickConfigMenu;
import net.mezzdev.config.gui.quickmenu.QuickMenuTarget.Control;
import net.mezzdev.config.gui.quickmenu.QuickMenuTarget.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Coordinates the menu lifecycle, model, view, and input handler. */
final class QuickConfigMenu<T> implements IQuickConfigMenu {
	private final ConfigContentPage page;
	private final QuickMenuModel<T> model;
	private final QuickMenuLayout layout;
	private final QuickMenuView<T> view;
	private final QuickMenuInputHandler<T> input;
	private final IConfigScreenFactory screenFactory;
	private Supplier<Rect2i> anchor = () -> new Rect2i(0, 0, 0, 0);
	private BooleanSupplier available = () -> false;
	private Runnable onAnchorClick = () -> {};
	private Supplier<@Nullable ConfigInfo> anchorInfo = () -> null;
	private @Nullable Screen parent;

	QuickConfigMenu(Component title, List<QuickMenuModel.Row<T>> options,
		IConfigScreenValue<List<T>> selection, ConfigContentPage page, IConfigScreenFactory screenFactory) {
		this.screenFactory = screenFactory;
		this.page = page;
		this.model = new QuickMenuModel<>(options, selection, newValue -> page.applyChange(selection, newValue));
		this.layout = new QuickMenuLayout(page);
		this.view = new QuickMenuView<>(title, model, page, layout);
		this.input = new QuickMenuInputHandler<>(this, model, page, layout);
	}

	@Override
	public boolean open(Supplier<Rect2i> anchor, BooleanSupplier available, Runnable onAnchorClick, Supplier<@Nullable ConfigInfo> anchorInfo) {
		close();
		this.anchor = Objects.requireNonNull(anchor);
		this.available = Objects.requireNonNull(available);
		this.onAnchorClick = Objects.requireNonNull(onAnchorClick);
		this.anchorInfo = Objects.requireNonNull(anchorInfo);
		Screen screen = Minecraft.getInstance().screen;
		if (screen instanceof MezzConfigScreen) {
			return false;
		}
		parent = screen;
		if (parent != null) {
			page.open(parent);
		}
		refresh();
		if (isOpen()) {
			QuickMenuManager.open(this);
		}
		return isOpen();
	}

	@Override
	public void close() {
		QuickMenuManager.closed(this);
		input.cancelInput();
		if (parent != null) {
			if (page.applyPendingChanges(model.selection().getPendingChange().stream().toList())) {
				model.selection().saved();
			}
		}
		page.close();
		parent = null;
		model.close();
		layout.close();
	}

	@Override
	public boolean isOpen() {
		return parent != null;
	}

	void refresh() {
		if (parent != null && (parent != Minecraft.getInstance().screen || !available.getAsBoolean())) {
			close();
		}
		Screen host = parent;
		if (host == null) {
			return;
		}
		host.setFocused(null);
		host.setDragging(false);
		input.validateSelection();
		page.setValues(model.refreshRows());
		var window = Minecraft.getInstance().getWindow();
		if (!layout.update(anchor.get(), window.getGuiScaledWidth(), window.getGuiScaledHeight(), input::cancelInput)) {
			close();
		}
	}

	QuickMenuInputHandler<T> input() {
		return input;
	}

	void draw(GuiGraphics graphics, int mouseX, int mouseY) {
		if (!isOpen()) {
			return;
		}
		ResizeHandle handle = input.resizeHandleAt(mouseX, mouseY);
		input.stepScroll();
		view.draw(graphics, mouseX, mouseY, handle, input.reorderPreview(), isMouseOverAnchor(mouseX, mouseY), anchorInfo);
	}

	void activate(QuickMenuTarget<T> target) {
		if (target instanceof Control<T> control) {
			switch (control.action()) {
				case CLOSE -> close();
				case ANCHOR -> {
					Runnable action = onAnchorClick;
					close();
					action.run();
				}
				case FULL_CONFIG -> {
					Screen host = parent;
					close();
					Minecraft.getInstance().setScreen(screenFactory.create(host));
				}
				case EDIT -> {
					page.cancelInput();
					model.toggleEditing();
				}
				case PAGE, RESIZE, SCROLL -> {}
			}
		} else if (target instanceof Option<T> option) {
			switch (option.action()) {
				case MEMBERSHIP -> model.selection().toggleMembership(option.id());
				case UP -> model.selection().move(option.id(), -1);
				case DOWN -> model.selection().move(option.id(), 1);
				case REORDER -> {}
			}
		}
		if (target.playsClickSound()) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		}
		refresh();
	}

	boolean isMouseOverAnchor(double x, double y) {
		return !layout.area().contains(x, y) && !page.isMouseOverPopup(x, y) && anchor.get().contains((int) x, (int) y);
	}
}
