package net.mezzdev.config.gui.quickmenu;

import net.mezzdev.config.api.schema.ConfigSchemaType;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.gui.ConfigScreenSchema;
import net.mezzdev.config.gui.ConfigContentPage;
import net.mezzdev.config.gui.ConfigValueAccess;
import net.mezzdev.config.gui.api.IQuickConfigMenuFactory;
import net.mezzdev.config.gui.api.IQuickConfigMenu;
import net.mezzdev.config.gui.api.IConfigScreenFactory;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IQuickConfigMenuBuilder;
import net.mezzdev.config.gui.api.ConfigValueEditorType;
import net.mezzdev.config.gui.api.IConfigValueEditorFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class QuickConfigMenuFactory implements IQuickConfigMenuFactory {
	private final String modId;
	private final IConfigScreenFactory screenFactory;
	private final Map<ConfigValueEditorType<?>, IConfigValueEditorFactory<?>> editors;
	private final Supplier<ConfigScreenSchema> screenSchema;
	private final Function<IConfigScreenValue<?>, Optional<IConfigSchema>> schemaResolver;

	public QuickConfigMenuFactory(String modId, Map<ConfigValueEditorType<?>, IConfigValueEditorFactory<?>> editors,
		Supplier<ConfigScreenSchema> screenSchema, Function<IConfigScreenValue<?>, Optional<IConfigSchema>> schemaResolver, IConfigScreenFactory screenFactory) {
		this.modId = modId;
		this.screenFactory = screenFactory;
		this.editors = editors;
		this.screenSchema = screenSchema;
		this.schemaResolver = schemaResolver;
	}

	@Override
	public IQuickConfigMenu create(Component title, List<String> defaults, List<String> additional) {
		return create(title, QuickMenuEntries.forPaths(screenSchema.get(), defaults, additional));
	}

	@Override
	public IQuickConfigMenuBuilder createBuilder(Component title) {
		return new QuickConfigMenuBuilder(screenSchema, entries -> create(title, entries));
	}

	private IQuickConfigMenu create(Component title, QuickMenuEntries entries) {
		Map<Object, IConfigSchema> schemas = new IdentityHashMap<>();
		entries.options().values().forEach(value -> validateValue(value, schemas));
		var selection = new QuickMenuPreferences(Minecraft.getInstance().gameDirectory.toPath().resolve("config/mezz_config_gui/quick-menus"),
			modId, entries.defaults(), entries.additional());
		List<QuickMenuModel.Row<String>> options = entries.options().entrySet().stream()
			.map(entry -> new QuickMenuModel.Row<>(entry.getKey(), entry.getValue()))
			.toList();
		ConfigContentPage page = new ConfigContentPage(editors, 3 + QuickMenuLayout.CONTROL_SIZE + 2 * 20,
			value -> Optional.ofNullable(schemas.get(value.getIdentityKey())));
		return new QuickConfigMenu<>(title, options, selection, page, screenFactory);
	}

	private void validateValue(IConfigScreenValue<?> value, Map<Object, IConfigSchema> schemas) {
		Object key = value.getIdentityKey();
		Optional<IConfigSchema> schema = schemaResolver.apply(value);
		if (schema.map(s -> s.getType() == ConfigSchemaType.SERVER).orElse(false) ||
			key instanceof ConfigValueAccess access && access.getServerAccess().isPresent()
		) {
			throw new IllegalArgumentException("Quick menus do not support server config values: " + value.getName());
		}
		schema.ifPresent(s -> schemas.put(key, s));
	}
}
