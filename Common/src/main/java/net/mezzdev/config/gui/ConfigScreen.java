package net.mezzdev.config.gui;

import net.mezzdev.config.api.schema.ConfigOwnership;
import net.mezzdev.config.api.schema.ConfigScope;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.gui.api.ConfigValueApplyMode;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IConfigValueEditorFactory;
import net.mezzdev.config.gui.config.ConfigGuiOptions;
import net.mezzdev.config.gui.entries.ConfigEntryWidget;
import net.mezzdev.config.gui.entries.ConfigEntryWidgetFactory;
import net.mezzdev.config.gui.input.InputType;
import net.mezzdev.config.gui.input.UserInput;
import net.mezzdev.config.gui.model.ConfigCategoryWidget;
import net.mezzdev.config.gui.model.ConfigNavItem;
import net.mezzdev.config.gui.model.ConfigScreenHistory;
import net.mezzdev.config.gui.model.ConfigScreenModel;
import net.mezzdev.config.gui.model.ConfigValueChange;
import net.mezzdev.config.gui.popup.ConfigPopupSelector;
import net.mezzdev.config.gui.popup.ConfigValueSelectorInputHandler;
import net.mezzdev.config.gui.screenlist.ConfigScreenListEntry;
import net.mezzdev.config.gui.textures.ConfigTextures;
import net.mezzdev.config.gui.util.ImmutableRect2i;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Main in-game config screen that wires the model, layout, view, and input routing together.
 */
public class ConfigScreen extends MezzConfigScreen {
	static Screen create(
		@Nullable Screen parent,
		String modId,
		Component title,
		ConfigScreenSchema clientSchema,
		ConfigChangesHandler changesHandler,
		Map<ConfigValueEditorType<?>, IConfigValueEditorFactory<?>> valueEditorFactories,
		ConfigScreenNavigation navigation
	) {
		if (isConfigScreenOpen(parent)) {
			return parent;
		}
		return new ConfigScreen(parent, modId, title, clientSchema, changesHandler, valueEditorFactories, navigation);
	}

	public static boolean isConfigScreenOpen(@Nullable Screen screen) {
		return screen instanceof ConfigScreen ||
			screen instanceof PendingChangesScreen;
	}

	public static boolean isCapturingKeyBinding(Screen screen) {
		return screen instanceof ConfigScreen configScreen && configScreen.isCapturingKeyBinding();
	}

	private final ConfigInputRouter inputHandler;
	private final EditBox searchBox;
	private final ConfigScreenLayout layout = new ConfigScreenLayout();
	private final ConfigScreenModel model;
	private final ConfigScreenController controller;
	private final ConfigScreenModTabs modTabs;
	private final ConfigScreenView view;
	private final ConfigScreenNavigation navigation;

	@Nullable
	private final Screen parent;

	@Nullable
	private ConfigPopupSelector valueSelector;
	private boolean changeRequestPending;

	private ConfigScreen(
		@Nullable Screen parent,
		String modId,
		Component title,
		ConfigScreenSchema clientSchema,
		ConfigChangesHandler changesHandler,
		Map<ConfigValueEditorType<?>, IConfigValueEditorFactory<?>> valueEditorFactories,
		ConfigScreenNavigation navigation
	) {
		super(title);
		this.parent = parent;
		this.navigation = navigation;
		ConfigTextures textures = ConfigTextures.get();

		Font font = Minecraft.getInstance().font;
		this.searchBox = new EditBox(font, 0, 0, 0, ConfigScreenLayout.SEARCH_HEIGHT, Component.translatable("mezz_config.config.screen.search"));
		this.searchBox.setMaxLength(64);
		this.searchBox.setBordered(false);
		this.searchBox.setHint(Component.translatable("mezz_config.config.screen.search"));
		updateSearchTextColor("");

		List<ConfigScreenCategory> categories = createCategories(clientSchema);
		this.model = new ConfigScreenModel(categories);
		this.model.setActiveCategoryIndex(ConfigScreenHistory.getInitialCategoryIndex(modId, categories));
		this.controller = new ConfigScreenController(changesHandler, model, layout, () -> {
			if (!searchBox.getValue().isEmpty()) {
				searchBox.setValue("");
			}
		}, index -> ConfigScreenHistory.rememberCategory(modId, model.getCategories().get(index)));
		this.modTabs = new ConfigScreenModTabs(modId, navigation.getScreenListEntries());
		this.view = new ConfigScreenView(title, searchBox, model, layout, controller, modTabs, textures);
		this.searchBox.setResponder(searchText -> {
			updateSearchTextColor(searchText);
			controller.setSearchText(searchText);
		});

		List<ConfigInputHandler> allInputHandlers = new ArrayList<>();
		ConfigEntryWidgetFactory entryWidgetFactory = new ConfigEntryWidgetFactory(
			this::openValueSelector,
			controller::updateContentLayout,
			textures,
			valueEditorFactories
		);
		Map<Object, ConfigEntryWidget<?>> entryWidgetsByValueKey = new IdentityHashMap<>();
		List<ConfigEntryWidget<?>> allEntryWidgets = new ArrayList<>();
		for (int i = 0; i < categories.size(); i++) {
			ConfigScreenCategory category = categories.get(i);
			List<ConfigEntryWidget<?>> entryWidgets = createEntryWidgets(
				category,
				clientSchema,
				entryWidgetsByValueKey,
				allEntryWidgets,
				entryWidgetFactory,
				controller
			);
			ConfigCategoryWidget widget = new ConfigCategoryWidget(
				category,
				entryWidgets
			);
			model.addCategoryWidget(widget);

			ConfigNavItem navItem = new ConfigNavItem(
				category.getLocalizedName(),
				i,
				widget,
				layout::getNavArea,
				controller::setActiveCategory
			);
			model.addNavItem(navItem);
		}
		allEntryWidgets
			.stream()
			.map(this::createEntryInputHandler)
			.forEach(allInputHandlers::add);
		allInputHandlers.addAll(model.getNavItems());

		allInputHandlers.addFirst(new ConfigValueSelectorInputHandler(
			() -> valueSelector,
			() -> ConfigScreenView.getValueSelectorClipArea(layout.getContentArea()),
			this::closeValueSelector,
			controller::updateContentLayout
		));
		this.inputHandler = new ConfigInputRouter(allInputHandlers);
	}

	private List<ConfigEntryWidget<?>> createEntryWidgets(
		ConfigScreenCategory category,
		ConfigScreenSchema schema,
		Map<Object, ConfigEntryWidget<?>> entryWidgetsByValueKey,
		List<ConfigEntryWidget<?>> allEntryWidgets,
		ConfigEntryWidgetFactory entryWidgetFactory,
		ConfigScreenController controller
	) {
		List<ConfigEntryWidget<?>> entryWidgets = new ArrayList<>();
		for (IConfigScreenValue<?> configValue : category.getConfigValues()) {
			entryWidgets.add(getOrCreateEntryWidget(schema, entryWidgetsByValueKey, allEntryWidgets, entryWidgetFactory, controller, configValue));
		}
		return entryWidgets;
	}

	private static List<ConfigScreenCategory> createCategories(ConfigScreenSchema schema) {
		return List.copyOf(schema.getCategories());
	}

	private ConfigEntryWidget<?> getOrCreateEntryWidget(
		ConfigScreenSchema schema,
		Map<Object, ConfigEntryWidget<?>> entryWidgetsByValueKey,
		List<ConfigEntryWidget<?>> allEntryWidgets,
		ConfigEntryWidgetFactory entryWidgetFactory,
		ConfigScreenController controller,
		IConfigScreenValue<?> configValue
	) {
		Optional<IConfigSchema> backingSchema = schema.findBackingSchema(configValue);
		configValue = useSupportedApplyMode(configValue, backingSchema);
		Object identityKey = configValue.getIdentityKey();
		ConfigEntryWidget<?> entryWidget = entryWidgetsByValueKey.get(identityKey);
		if (entryWidget == null) {
			entryWidget = entryWidgetFactory.create(configValue);
			entryWidget.setImmediateChangeHandler(this::applyImmediateChange);
			entryWidget.setEditableSupplier(() -> !changeRequestPending && backingSchema
				.map(IConfigSchema::canEdit)
				.orElse(true));
			entryWidgetsByValueKey.put(identityKey, entryWidget);
			allEntryWidgets.add(entryWidget);
		}
		return entryWidget;
	}

	private static IConfigScreenValue<?> useSupportedApplyMode(
		IConfigScreenValue<?> configValue,
		Optional<IConfigSchema> backingSchema
	) {
		if (backingSchema
			.filter(ConfigScreen::requiresOnApply)
			.isPresent()
		) {
			return withApplyMode(configValue, ConfigValueApplyMode.ON_APPLY);
		}
		return configValue;
	}

	private static boolean requiresOnApply(IConfigSchema schema) {
		return schema.getOwnership() == ConfigOwnership.SERVER && schema.getScope() == ConfigScope.WORLD;
	}

	@SuppressWarnings("unchecked")
	private static <T> IConfigScreenValue<T> withApplyMode(
		IConfigScreenValue<?> configValue,
		ConfigValueApplyMode applyMode
	) {
		return IConfigScreenValue.withApplyMode((IConfigScreenValue<T>) configValue, applyMode);
	}

	private boolean applyImmediateChange(ConfigValueChange<?> change) {
		boolean succeeded = controller.applyImmediateChange(change);
		if (succeeded) {
			refreshLayout();
		}
		return succeeded;
	}

	private ConfigInputHandler createEntryInputHandler(ConfigEntryWidget<?> entry) {
		ConfigInputHandler entryInputHandler = entry.createInputHandler();
		return new ConfigInputHandler() {
			@Override
			public Optional<ConfigInputHandler> handleUserInput(Screen screen, UserInput input) {
				if (!entry.isEditable()) {
					return Optional.empty();
				}
				ImmutableRect2i displayArea = layout.getContentArea();
				if (!entry.getArea().equals(ImmutableRect2i.EMPTY) &&
					displayArea.contains(input.getMouseX(), input.getMouseY()) &&
					entry.isMouseOver(input.getMouseX(), input.getMouseY())
				) {
					return entryInputHandler.handleUserInput(screen, input);
				}
				return Optional.empty();
			}

			@Override
			public void unfocus() {
				entryInputHandler.unfocus();
			}

			@Override
			public Optional<ConfigInputHandler> handleMouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
				if (entry.isEditable() && !entry.getArea().equals(ImmutableRect2i.EMPTY)) {
					return entryInputHandler.handleMouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
				}
				return Optional.empty();
			}
		};
	}

	private void updateSearchTextColor(String searchText) {
		this.searchBox.setTextColor(getSearchTextColor(searchText));
	}

	private static int getSearchTextColor(String searchText) {
		if (searchText.isEmpty()) {
			return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_SCREEN_SEARCH_HINT);
		}
		return ConfigGuiColors.getColor(ConfigGuiColors.GuiColor.CONFIG_SCREEN_SEARCH_TEXT);
	}

	private void openValueSelector(ConfigPopupSelector selector) {
		closeValueSelector();
		selector.updateBounds(ConfigScreenView.getValueSelectorClipArea(layout.getContentArea()));
		this.valueSelector = selector;
		selector.onOpened();
	}

	private void closeValueSelector() {
		ConfigPopupSelector valueSelector = this.valueSelector;
		this.valueSelector = null;
		if (valueSelector != null) {
			valueSelector.onClosed();
		}
	}

	private boolean isCapturingKeyBinding() {
		return controller.getVisibleEntryWidgets()
			.stream()
			.filter(ConfigEntryWidget::isEditable)
			.anyMatch(ConfigEntryWidget::isCapturingKeyboardInput);
	}

	private void flushPendingInput() {
		inputHandler.handleGuiChange();
		closeValueSelector();
	}

	@Nullable
	public Rect2i getValueSelectorArea() {
		ConfigPopupSelector valueSelector = this.valueSelector;
		if (valueSelector == null) {
			return null;
		}
		ImmutableRect2i clipArea = ConfigScreenView.getValueSelectorClipArea(layout.getContentArea());
		valueSelector.updateBounds(clipArea);
		@Nullable
		ImmutableRect2i intersection = getIntersection(valueSelector.getArea(), clipArea);
		if (intersection == null) {
			return null;
		}
		return new Rect2i(
			intersection.getX(),
			intersection.getY(),
			intersection.getWidth(),
			intersection.getHeight()
		);
	}

	@Nullable
	public Rect2i getModTabsArea() {
		ImmutableRect2i area = modTabs.getTabsArea();
		if (area.isEmpty()) {
			return null;
		}
		return new Rect2i(
			area.getX(),
			area.getY(),
			area.getWidth(),
			area.getHeight()
		);
	}

	@Nullable
	private static ImmutableRect2i getIntersection(ImmutableRect2i first, ImmutableRect2i second) {
		int x = Math.max(first.getX(), second.getX());
		int y = Math.max(first.getY(), second.getY());
		int right = Math.min(first.getX() + first.getWidth(), second.getX() + second.getWidth());
		int bottom = Math.min(first.getY() + first.getHeight(), second.getY() + second.getHeight());
		if (right <= x || bottom <= y) {
			return null;
		}
		return new ImmutableRect2i(x, y, right - x, bottom - y);
	}

	@Nullable
	@Override
	public Rect2i getScreenArea() {
		ImmutableRect2i area = layout.getArea();
		if (area.isEmpty()) {
			return null;
		}
		return new Rect2i(
			area.getX(),
			area.getY(),
			area.getWidth(),
			area.getHeight()
		);
	}

	@Override
	protected void init() {
		super.init();
		layout.updateScreenBounds(width, height, searchBox, canOpenScreenList());
		modTabs.updateLayout(layout.getArea());
		addWidget(searchBox);

		layout.resetNavScroll();
		controller.updateNavLayout();
		if (model.hasActiveCategory()) {
			controller.setActiveCategory(model.getActiveCategoryIndex());
		} else {
			controller.updateContentLayout();
		}
		if (ConfigGuiOptions.focusSearchOnOpen()) {
			searchBox.setFocused(true);
		}
		controller.startListening();
	}

	@Override
	public void removed() {
		controller.stopListening();
		closeValueSelector();
		super.removed();
	}

	@Override
	public void onClose() {
		requestClose();
	}

	private void requestClose() {
		requestLeave(this::closeWithoutPrompt);
	}

	private void requestOpenScreenList() {
		requestLeave(this::openScreenListWithoutPrompt);
	}

	private void requestOpenConfigScreen(ConfigScreenListEntry entry) {
		requestLeave(() -> openConfigScreenWithoutPrompt(entry));
	}

	private void requestLeave(Runnable leaveAction) {
		if (changeRequestPending) {
			return;
		}
		flushPendingInput();
		if (controller.hasPendingChanges()) {
			if (ConfigGuiOptions.confirmPendingChangesOnClose()) {
				openPendingChangesConfirmation(leaveAction);
				return;
			}
			applyPendingChanges(leaveAction, () -> {});
			return;
		}
		leaveAction.run();
	}

	private void closeWithoutPrompt() {
		if (minecraft != null) {
			minecraft.setScreen(parent);
		}
	}

	private void openScreenListWithoutPrompt() {
		if (minecraft != null) {
			minecraft.setScreen(navigation.createScreenList(parent));
		}
	}

	private void openConfigScreenWithoutPrompt(ConfigScreenListEntry entry) {
		if (minecraft != null) {
			minecraft.setScreen(entry.factory().create(parent));
		}
	}

	private void openPendingChangesConfirmation(Runnable leaveAction) {
		if (minecraft == null) {
			return;
		}
		PendingChangesScreen pendingChangesScreen = new PendingChangesScreen(
			applyChanges -> {
				if (applyChanges) {
					minecraft.setScreen(this);
					applyPendingChanges(leaveAction, () -> {});
					return;
				} else {
					controller.discardPendingChanges();
				}
				leaveAction.run();
			},
			() -> minecraft.setScreen(this),
			controller.getPendingChangesRestartRequirement(),
			controller.getPendingConfigChanges()
		);
		minecraft.setScreen(pendingChangesScreen);
	}

	private void applyPendingChanges() {
		applyPendingChanges(() -> {}, () -> {});
	}

	private void applyPendingChanges(Runnable successAction, Runnable failureAction) {
		if (changeRequestPending) {
			return;
		}
		startChangeRequest(controller.applyPendingChanges(), successAction, failureAction);
	}

	private void undoChanges() {
		if (changeRequestPending) {
			return;
		}
		startChangeRequest(controller.undoChanges(), () -> {}, () -> {});
	}

	private void startChangeRequest(
		CompletableFuture<ConfigChangesResult> request,
		Runnable successAction,
		Runnable failureAction
	) {
		if (changeRequestPending) {
			return;
		}
		changeRequestPending = true;
		request.whenComplete((result, throwable) -> runOnClientThread(() -> {
			changeRequestPending = false;
			refreshLayout();
			if (throwable == null && result.succeeded()) {
				successAction.run();
			} else {
				failureAction.run();
			}
		}));
	}

	private void runOnClientThread(Runnable task) {
		Minecraft minecraft = this.minecraft;
		if (minecraft == null || minecraft.isSameThread()) {
			task.run();
		} else {
			minecraft.execute(task);
		}
	}

	private void refreshLayout() {
		layout.updateScreenBounds(width, height, searchBox, canOpenScreenList());
		modTabs.updateLayout(layout.getArea());
		controller.updateNavLayout();
		controller.updateContentLayout();
		updateValueSelectorBounds();
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		ConfigPopupSelector valueSelector = this.valueSelector;
		if (valueSelector != null && valueSelector.charTyped(codePoint, modifiers)) {
			return true;
		}
		if (searchBox.isFocused() && searchBox.charTyped(codePoint, modifiers)) {
			return true;
		}
		if (forwardCharTypedToEntries(codePoint, modifiers)) {
			return true;
		}
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		ConfigPopupSelector valueSelector = this.valueSelector;
		if (valueSelector != null) {
			if (valueSelector.keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				closeValueSelector();
				return true;
			}
		}
		UserInput input = UserInput.fromVanilla(keyCode, scanCode, modifiers, InputType.IMMEDIATE);
		if (searchBox.isFocused()) {
			if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			}
			if (input.is(Minecraft.getInstance().options.keyInventory)) {
				return true;
			}
		}
		if (forwardKeyPressedToEntries(keyCode, scanCode, modifiers)) {
			return true;
		}
		if (input.is(Minecraft.getInstance().options.keyInventory)) {
			requestClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private boolean forwardCharTypedToEntries(char codePoint, int modifiers) {
		for (ConfigEntryWidget<?> entry : controller.getVisibleEntryWidgets()) {
			if (entry.isEditable() && entry.charTyped(codePoint, modifiers)) {
				return true;
			}
		}
		return false;
	}

	private boolean forwardKeyPressedToEntries(int keyCode, int scanCode, int modifiers) {
		for (ConfigEntryWidget<?> entry : controller.getVisibleEntryWidgets()) {
			if (entry.isEditable() && entry.keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (forwardKeyReleasedToEntries(keyCode, scanCode, modifiers)) {
			return true;
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	private boolean forwardKeyReleasedToEntries(int keyCode, int scanCode, int modifiers) {
		for (ConfigEntryWidget<?> entry : controller.getVisibleEntryWidgets()) {
			if (entry.isEditable() && entry.keyReleased(keyCode, scanCode, modifiers)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (modTabs.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		if (button == 0 && layout.startResizeDrag(mouseX, mouseY)) {
			flushPendingInput();
			return true;
		}
		if (button == 0 && isActionButton(mouseX, mouseY)) {
			return true;
		}
		if (button == 1 && searchBox.isMouseOver(mouseX, mouseY)) {
			if (!searchBox.getValue().isEmpty()) {
				searchBox.setValue("");
			}
			searchBox.setFocused(true);
			return true;
		}
		if (button == 0 && controller.startContentScrollDrag(mouseX, mouseY)) {
			return true;
		}
		if (button == 0 && controller.startNavScrollDrag(mouseX, mouseY)) {
			return true;
		}
		if (searchBox.isFocused() && !searchBox.isMouseOver(mouseX, mouseY)) {
			searchBox.setFocused(false);
		}
		boolean ret = UserInput.fromVanilla(mouseX, mouseY, button, InputType.SIMULATE)
			.map(this::handleInput)
			.orElse(false);
		return ret || super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		ConfigScreenModTabs.ClickResult modTabResult = modTabs.mouseReleased(mouseX, mouseY, button);
		if (modTabResult.handled()) {
			if (modTabResult.playSound()) {
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
			modTabResult.entry().ifPresent(this::requestOpenConfigScreen);
			return true;
		}
		if (button == 0 && layout.isResizing()) {
			layout.finishResizeDrag()
				.ifPresent(resizedArea -> ConfigGuiOptions.setWindowSize(resizedArea.getWidth(), resizedArea.getHeight()));
			return true;
		}
		if (button == 0 && (controller.stopContentScrollDrag() || controller.stopNavScrollDrag())) {
			return true;
		}
		if (button == 0) {
			if (handleActionButton(mouseX, mouseY)) {
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			}
			if (isActionButton(mouseX, mouseY)) {
				return true;
			}
		}
		boolean ret = UserInput.fromVanilla(mouseX, mouseY, button, InputType.EXECUTE)
			.map(this::handleInput)
			.orElse(false);
		if (ret) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		}
		return ret || super.mouseReleased(mouseX, mouseY, button);
	}

	private boolean isActionButton(double mouseX, double mouseY) {
		return layout.getScreenListButtonArea().contains(mouseX, mouseY) ||
			layout.getApplyPendingChangesButtonArea().contains(mouseX, mouseY) ||
			layout.getUndoChangesButtonArea().contains(mouseX, mouseY);
	}

	private boolean handleActionButton(double mouseX, double mouseY) {
		if (changeRequestPending) {
			return false;
		}
		if (layout.getScreenListButtonArea().contains(mouseX, mouseY)) {
			requestOpenScreenList();
			return true;
		}
		if (layout.getApplyPendingChangesButtonArea().contains(mouseX, mouseY)) {
			flushPendingInput();
			if (controller.hasPendingChanges()) {
				applyPendingChanges();
				return true;
			}
			return false;
		}
		if (layout.getUndoChangesButtonArea().contains(mouseX, mouseY)) {
			flushPendingInput();
			if (controller.hasUndoableChanges()) {
				undoChanges();
				return true;
			}
			return false;
		}
		return false;
	}

	private boolean canOpenScreenList() {
		return navigation.canOpenScreenList(parent);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (button == 0 && modTabs.isPressing()) {
			return true;
		}
		if (button == 0 && layout.isResizing()) {
			if (layout.dragResize(mouseX, mouseY, width, height)) {
				refreshLayout();
			}
			return true;
		}
		if (button == 0 && controller.dragContentScroll(mouseY)) {
			return true;
		}
		if (button == 0 && controller.dragNavScroll(mouseY)) {
			return true;
		}
		if (inputHandler.handleMouseDragged(this, mouseX, mouseY, button, dragX, dragY)) {
			if (controller.autoScrollContentForDrag(mouseY)) {
				inputHandler.handleMouseDragged(this, mouseX, mouseY, button, dragX, dragY);
			}
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	private boolean handleInput(UserInput input) {
		return this.inputHandler.handleUserInput(this, input);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (modTabs.mouseScrolled(mouseX, mouseY, scrollY)) {
			return true;
		}
		if (inputHandler.handleMouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
			return true;
		}
		if (controller.scroll(mouseX, mouseY, scrollY)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		if (minecraft == null) {
			return;
		}
		updateSearchTextColor(searchBox.getValue());
		renderTransparentBackground(guiGraphics);
		controller.stepScrollPositions();
		updateValueSelectorBounds();
		view.render(guiGraphics, mouseX, mouseY, partialTick, valueSelector);
	}

	private void updateValueSelectorBounds() {
		ConfigPopupSelector valueSelector = this.valueSelector;
		if (valueSelector != null) {
			valueSelector.updateBounds(ConfigScreenView.getValueSelectorClipArea(layout.getContentArea()));
		}
	}

}
