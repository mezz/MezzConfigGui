package net.mezzdev.config.gui;

import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.gui.api.ConfigInfo;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditorFactory;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.entries.ConfigEntryWidgetFactory;
import net.mezzdev.config.gui.model.ConfigValueChange;
import net.mezzdev.config.gui.popup.ConfigPopupSelector;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** A scrollable group of standard config editors without screen navigation. */
public final class ConfigContentPage {
	private static final int SCROLLBAR_WIDTH = 6;
	private final Map<ConfigValueEditorType<?>, IConfigValueEditorFactory<?>> editors;
	private final Function<IConfigScreenValue<?>, Optional<IConfigSchema>> schemaResolver;
	private @Nullable ConfigEntryWidgetFactory factory;
	private final int nameRightReserve;
	private final Map<IConfigScreenValue<?>, ConfigEntryWidget<?>> entries = new IdentityHashMap<>();
	private final List<ConfigEntryWidget<?>> allEntries = new ArrayList<>();
	private List<ConfigEntryWidget<?>> visible = List.of();
	private @Nullable Screen parent;
	private ImmutableRect2i area = ImmutableRect2i.EMPTY;
	private ImmutableRect2i popupClipArea = ImmutableRect2i.EMPTY;
	private final ConfigScrollState scroll = new ConfigScrollState(3, 0);
	private final ConfigEditorInteraction interaction = new ConfigEditorInteraction(() -> visible, () -> popupClipArea);
	private final ConfigContentInputHandler input = new ConfigContentInputHandler(scroll, interaction, () -> parent, () -> area, this::layout);
	private int contentHeight;
	private List<Component> failure = List.of();

	public ConfigContentPage(Map<ConfigValueEditorType<?>, IConfigValueEditorFactory<?>> editors, int nameRightReserve,
		Function<IConfigScreenValue<?>, Optional<IConfigSchema>> schemaResolver) {
		this.editors = editors;
		this.nameRightReserve = nameRightReserve;
		this.schemaResolver = schemaResolver;
	}

	public void open(Screen parent) {
		this.parent = parent;
		allEntries.forEach(ConfigEntryWidget::subscribeToConfigValue);
	}

	public void close() {
		cancelInput();
		allEntries.forEach(ConfigEntryWidget::unsubscribeFromConfigValue);
		interaction.close();
		parent = null;
		scroll.reset();
	}

	public void setValues(List<IConfigScreenValue<?>> values) {
		List<ConfigEntryWidget<?>> next = values.stream().<ConfigEntryWidget<?>>map(value -> entries.computeIfAbsent(value, this::createEntry)).toList();
		if (visible.equals(next)) {
			return;
		}
		cancelInput();
		visible = next;
		input.setEntries(visible);
		layout();
	}

	private ConfigEntryWidget<?> createEntry(IConfigScreenValue<?> value) {
		if (factory == null) {
			factory = new ConfigEntryWidgetFactory(interaction::openPopup, this::layout, ConfigTextures.get(), editors);
		}
		ConfigEntryWidget<?> entry = factory.create(value);
		allEntries.add(entry);
		entry.setShowResetButton(false);
		entry.setMinimumNameRightReserve(nameRightReserve);
		entry.setImmediateChangeHandler(change -> apply(List.of(change)));
		if (parent != null) {
			entry.subscribeToConfigValue();
		}
		return entry;
	}

	public int preferredHeight(int width) {
		int height = 0;
		for (ConfigEntryWidget<?> entry : visible) {
			entry.updateBounds(new ImmutableRect2i(0, 0, width - SCROLLBAR_WIDTH, entry.getHeight()));
			height += entry.getHeight();
		}
		return Math.max(ConfigEntryWidget.getMinimumHeight(), height);
	}

	public void updateBounds(ImmutableRect2i area, ImmutableRect2i popupClipArea) {
		this.area = area;
		this.popupClipArea = popupClipArea;
		layout();
	}

	public boolean hasPopup() {
		return interaction.getPopup() != null;
	}

	public boolean isMouseOverPopup(double x, double y) {
		ConfigPopupSelector popup = interaction.getPopup();
		return popup != null && popup.isMouseOver(x, y);
	}

	public ImmutableRect2i getPopupArea() {
		ConfigPopupSelector popup = interaction.getPopup();
		if (popup == null) {
			return ImmutableRect2i.EMPTY;
		}
		return popup.getArea();
	}

	public List<ConfigEntryWidget<?>> getEntries() {
		return visible;
	}

	public int getContentHeight() {
		return contentHeight;
	}

	public ConfigScrollState getScrollState() {
		return scroll;
	}

	public void stepScroll() {
		scroll.step();
		layout();
	}

	private void layout() {
		if (area.isEmpty()) {
			return;
		}
		contentHeight = preferredHeight(area.getWidth());
		scroll.updateBounds(area, contentHeight, area.keepRight(SCROLLBAR_WIDTH));
		int y = area.getY() - (int) Math.round(scroll.getPosition());
		// Keep labels from reflowing when the scrollbar appears.
		int width = area.getWidth() - SCROLLBAR_WIDTH;
		for (ConfigEntryWidget<?> entry : visible) {
			int height = entry.getHeight();
			entry.updateBounds(new ImmutableRect2i(area.getX(), y, width, height));
			y += height;
		}
		interaction.updatePopupBounds();
	}

	public void draw(GuiGraphics graphics, int x, int y) {
		graphics.enableScissor(area.getX(), area.getY(), area.getX() + area.getWidth(), area.getY() + area.getHeight());
		for (int i = 0; i < visible.size(); i++) {
			visible.get(i).draw(graphics, x, y, !hasPopup() && area.contains(x, y), i, area);
		}
		graphics.disableScissor();
		drawScrollbar(graphics);
	}

	public void drawScrollbar(GuiGraphics graphics) {
		ImmutableRect2i track = scroll.getScrollbarArea();
		if (!track.isEmpty()) {
			ConfigTextures.get().getScrollbarBackground().draw(graphics, track);
			ConfigTextures.get().getScrollbarMarker().draw(graphics, scroll.getMarkerArea());
		}
	}

	public void drawPopup(GuiGraphics graphics, int x, int y) {
		ConfigPopupSelector popup = interaction.getPopup();
		if (popup != null) {
			graphics.pose().pushPose();
			graphics.pose().translate(0, 0, 50);
			graphics.enableScissor(popupClipArea.getX(), popupClipArea.getY(), popupClipArea.getX() + popupClipArea.getWidth(), popupClipArea.getY() + popupClipArea.getHeight());
			popup.draw(graphics, x, y);
			graphics.disableScissor();
			graphics.pose().popPose();
		}
	}

	public @Nullable ConfigInfo getInfo(int x, int y) {
		if (!failure.isEmpty()) {
			return new ConfigInfo(Component.empty(), failure);
		}
		ConfigInfo info = null;
		ConfigPopupSelector popup = interaction.getPopup();
		if (popup != null) {
			info = popup.getTooltipInfo(x, y);
			if (info == null) {
				info = popup.getInfo();
			}
		} else if (area.contains(x, y)) {
			for (ConfigEntryWidget<?> entry : visible) {
				if (entry.isMouseOver(x, y)) {
					info = entry.getInfoWithAccess(x, y);
					break;
				}
			}
		}
		return info;
	}

	public boolean mousePressed(double x, double y, int button, int modifiers) {
		return input.mousePressed(x, y, button, modifiers);
	}

	public void mouseReleased(double x, double y, int button, int modifiers) {
		input.mouseReleased(x, y, button, modifiers);
	}

	public void mouseDragged(double x, double y, int button, double dragX, double dragY) {
		input.mouseDragged(x, y, button, dragX, dragY);
	}

	public void mouseScrolled(double x, double y, double scrollX, double scrollY) {
		input.mouseScrolled(x, y, scrollX, scrollY);
	}

	public boolean keyPressed(int key, int scanCode, int modifiers) {
		return input.keyPressed(key, scanCode, modifiers);
	}

	public void keyReleased(int key, int scanCode, int modifiers) {
		input.keyReleased(key, scanCode, modifiers);
	}

	public void charTyped(char character, int modifiers) {
		input.charTyped(character, modifiers);
	}

	public void cancelInput() {
		input.cancelInput();
	}

	public List<Component> getFailure() {
		return failure;
	}

	public <T> boolean applyChange(IConfigScreenValue<T> value, T newValue) {
		return apply(List.of(new ConfigValueChange<>(value, newValue)));
	}

	public boolean applyPendingChanges(List<ConfigValueChange<?>> additionalChanges) {
		List<ConfigValueChange<?>> changes = new ArrayList<>(additionalChanges);
		allEntries.forEach(entry -> entry.getPendingChange().ifPresent(changes::add));
		if (!apply(changes)) {
			LogManager.getLogger().error("Unable to save quick menu changes: {}", failure);
			return false;
		}
		allEntries.forEach(ConfigEntryWidget::discardPendingChange);
		return true;
	}

	private boolean apply(List<ConfigValueChange<?>> changes) {
		ConfigChangesResult result = ConfigChangesHandler.applyBySchema(changes, schemaResolver).join();
		failure = List.of();
		if (result.failure() != null) {
			failure = List.of(Component.literal(result.failure().exception().toString()));
		}
		layout();
		return result.succeeded();
	}
}
