package net.mezzdev.config.gui.remote;

import net.mezzdev.config.api.schema.IConfigSchema;

import java.util.Objects;

record RemoteSchemaKey(String modId, String schemaId) {
	RemoteSchemaKey {
		Objects.requireNonNull(modId, "modId");
		Objects.requireNonNull(schemaId, "schemaId");
	}

	static RemoteSchemaKey from(IConfigSchema schema) {
		Objects.requireNonNull(schema, "schema");
		return new RemoteSchemaKey(schema.getModId(), schema.getId());
	}
}
