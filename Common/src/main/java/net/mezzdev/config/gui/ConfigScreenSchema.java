package net.mezzdev.config.gui;

import net.mezzdev.config.api.schema.IConfigSchema;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/**
 * Internal config schema view used by config screens.
 */
@FunctionalInterface
public interface ConfigScreenSchema {
	/**
	 * Get the categories shown by this config screen.
	 */
	@Unmodifiable
	List<? extends ConfigScreenCategory> getCategories();

	/**
	 * Create an internal screen schema view of a MezzConfig schema.
	 */
	static ConfigScreenSchema from(IConfigSchema schema) {
		Objects.requireNonNull(schema, "schema");
		return new MezzConfigScreenSchema(schema);
	}
}
