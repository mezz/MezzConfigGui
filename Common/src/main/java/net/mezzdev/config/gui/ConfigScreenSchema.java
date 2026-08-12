package net.mezzdev.config.gui;

import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
	 * Find the MezzConfig schema that owns a screen value, when it has one.
	 */
	default Optional<IConfigSchema> findBackingSchema(IConfigScreenValue<?> value) {
		Objects.requireNonNull(value, "value");
		return Optional.empty();
	}

	/**
	 * Create an internal screen schema view of a MezzConfig schema.
	 */
	static ConfigScreenSchema from(IConfigSchema schema) {
		Objects.requireNonNull(schema, "schema");
		return new MezzConfigScreenSchema(schema);
	}
}
