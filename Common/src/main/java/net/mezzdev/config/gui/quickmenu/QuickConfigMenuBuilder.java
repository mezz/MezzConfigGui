package net.mezzdev.config.gui.quickmenu;

import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.gui.ConfigScreenSchema;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.api.IQuickConfigMenu;
import net.mezzdev.config.gui.api.IQuickConfigMenuBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

final class QuickConfigMenuBuilder implements IQuickConfigMenuBuilder {
	private final Supplier<ConfigScreenSchema> schema;
	private final Function<QuickMenuEntries, IQuickConfigMenu> createMenu;
	private final List<Function<QuickMenuEntries.Resolver, String>> defaults = new ArrayList<>();
	private final List<Function<QuickMenuEntries.Resolver, String>> additional = new ArrayList<>();

	QuickConfigMenuBuilder(Supplier<ConfigScreenSchema> schema, Function<QuickMenuEntries, IQuickConfigMenu> createMenu) {
		this.schema = schema;
		this.createMenu = createMenu;
	}

	@Override
	public IQuickConfigMenuBuilder addDefault(String path) {
		Objects.requireNonNull(path, "path");
		defaults.add(resolver -> path);
		return this;
	}

	@Override
	public IQuickConfigMenuBuilder addDefault(IConfigValue<?> value) {
		Objects.requireNonNull(value, "value");
		defaults.add(resolver -> resolver.path(value));
		return this;
	}

	@Override
	public IQuickConfigMenuBuilder addDefault(IConfigScreenValue<?> value) {
		Objects.requireNonNull(value, "value");
		defaults.add(resolver -> resolver.path(value));
		return this;
	}

	@Override
	public IQuickConfigMenuBuilder addAdditional(String path) {
		Objects.requireNonNull(path, "path");
		additional.add(resolver -> path);
		return this;
	}

	@Override
	public IQuickConfigMenuBuilder addAdditional(IConfigValue<?> value) {
		Objects.requireNonNull(value, "value");
		additional.add(resolver -> resolver.path(value));
		return this;
	}

	@Override
	public IQuickConfigMenuBuilder addAdditional(IConfigScreenValue<?> value) {
		Objects.requireNonNull(value, "value");
		additional.add(resolver -> resolver.path(value));
		return this;
	}

	@Override
	public IQuickConfigMenu build() {
		return createMenu.apply(resolve());
	}

	QuickMenuEntries resolve() {
		var resolver = new QuickMenuEntries.Resolver(schema.get());
		return resolver.resolve(
			defaults.stream().map(setting -> setting.apply(resolver)).toList(),
			additional.stream().map(setting -> setting.apply(resolver)).toList());
	}
}
