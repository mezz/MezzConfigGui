package net.mezzdev.config.gui.api;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Identifies the kind of editor a config screen should use for a config value.
 * <p>
 * Editor types are reference-identity tokens. Create one token for each UID and share that exact instance between the
 * serializer and editor registration. Registering a different token with the same UID is rejected, preventing callers
 * from associating incompatible generic value types with one logical editor type.
 *
 * @since 0.1.0
 */
public final class ConfigValueEditorType<T> {
	/**
	 * Create a config value editor type.
	 *
	 * @param namespace the namespace for this editor type
	 * @param path      the path for this editor type
	 *
	 * @since 0.1.0
	 */
	public static <T> ConfigValueEditorType<T> create(String namespace, String path) {
		ResourceLocation uid = new ResourceLocation(namespace, path);
		return create(uid);
	}

	/**
	 * Create a config value editor type.
	 *
	 * @param uid the unique id for this editor type
	 *
	 * @since 0.1.0
	 */
	public static <T> ConfigValueEditorType<T> create(ResourceLocation uid) {
		return new ConfigValueEditorType<>(uid);
	}

	private final ResourceLocation uid;

	private ConfigValueEditorType(ResourceLocation uid) {
		this.uid = Objects.requireNonNull(uid, "uid");
	}

	/**
	 * The unique id of this editor type.
	 *
	 * @since 0.1.0
	 */
	public ResourceLocation getUid() {
		return uid;
	}

	@Override
	public String toString() {
		return "ConfigValueEditorType[" +
			"uid=" + uid + ']';
	}
}
