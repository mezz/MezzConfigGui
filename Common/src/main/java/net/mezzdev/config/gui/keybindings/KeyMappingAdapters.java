package net.mezzdev.config.gui.keybindings;

import net.mezzdev.config.gui.api.keybindings.IKeyMappingAdapter;
import net.minecraft.client.KeyMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Resolves a mapping's most specific registered adapter before using the platform fallback. */
public final class KeyMappingAdapters {
	private static final Map<Class<?>, Function<KeyMapping, IKeyMappingAdapter>> FACTORIES = new HashMap<>();

	private KeyMappingAdapters() {
	}

	public static void clearRegistrations() {
		FACTORIES.clear();
	}

	public static <T extends KeyMapping> void register(Class<T> type, Function<T, IKeyMappingAdapter> factory) {
		Objects.requireNonNull(type);
		Objects.requireNonNull(factory);
		if (FACTORIES.putIfAbsent(type, mapping -> factory.apply(type.cast(mapping))) != null) {
			throw new IllegalArgumentException("Duplicate key mapping adapter: " + type.getName());
		}
	}

	static IKeyMappingAdapter resolve(KeyMapping mapping) {
		for (Class<?> type = mapping.getClass(); type != null; type = type.getSuperclass()) {
			Function<KeyMapping, IKeyMappingAdapter> factory = FACTORIES.get(type);
			if (factory != null) {
				IKeyMappingAdapter adapter = Objects.requireNonNull(factory.apply(mapping));
				if (adapter.getKeyMapping() != mapping) {
					throw new IllegalArgumentException("Adapter must wrap the supplied key mapping");
				}
				return adapter;
			}
		}
		return ConfigKeyMappingPlatformServices.PLATFORM_HELPER.createAdapter(mapping);
	}

	public static IConfigKeyMapping create(KeyMapping mapping) {
		return new ConfigKeyMapping(mapping);
	}
}
