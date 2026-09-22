package net.mezzdev.config.gui;

import net.mezzdev.config.api.schema.update.IConfigBatchUpdater;
import net.mezzdev.config.api.schema.IConfigSchema;
import net.mezzdev.config.api.schema.ConfigSchemaType;
import net.mezzdev.config.api.value.editor.ConfigValueRestartRequirement;
import net.mezzdev.config.api.value.IConfigValue;
import net.mezzdev.config.gui.api.IConfigScreenValue;
import net.mezzdev.config.gui.model.AppliedConfigValueChange;
import net.mezzdev.config.gui.model.ConfigValueChange;
import net.mezzdev.config.gui.remote.RemoteConfigEditor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;

@FunctionalInterface
interface ConfigChangesHandler {
	CompletableFuture<ConfigChangesResult> applyChanges(List<ConfigValueChange<?>> changes);

	static CompletableFuture<ConfigChangesResult> applyBySchema(
		List<ConfigValueChange<?>> changes,
		Function<IConfigScreenValue<?>, Optional<IConfigSchema>> schemaResolver
	) {
		return applyBySchema(changes, schemaResolver, Runnable::run);
	}

	static CompletableFuture<ConfigChangesResult> applyBySchema(
		List<ConfigValueChange<?>> changes,
		Function<IConfigScreenValue<?>, Optional<IConfigSchema>> schemaResolver,
		Executor continuationExecutor
	) {
		return applyBySchema(
			changes,
			schemaResolver,
			continuationExecutor,
			RemoteConfigEditor.getInstance()::requestUpdate
		);
	}

	static CompletableFuture<ConfigChangesResult> applyBySchema(
		List<ConfigValueChange<?>> changes,
		Function<IConfigScreenValue<?>, Optional<IConfigSchema>> schemaResolver,
		Executor continuationExecutor,
		RemoteChangesHandler remoteChangesHandler
	) {
		Objects.requireNonNull(changes, "changes");
		Objects.requireNonNull(schemaResolver, "schemaResolver");
		Objects.requireNonNull(continuationExecutor, "continuationExecutor");
		Objects.requireNonNull(remoteChangesHandler, "remoteChangesHandler");
		List<ConfigChangeBatch> batches = createBatches(changes, schemaResolver);
		CompletableFuture<ConfigChangesResult> result = CompletableFuture.completedFuture(ConfigChangesResult.success());
		for (ConfigChangeBatch batch : batches) {
			Function<ConfigChangesResult, CompletableFuture<ConfigChangesResult>> applyBatch = previousResult -> {
				if (!previousResult.succeeded()) {
					return CompletableFuture.completedFuture(previousResult);
				}
				return batch.apply(remoteChangesHandler)
					.thenApply(previousResult::append);
			};
			if (result.isDone()) {
				result = result.thenCompose(applyBatch);
			} else {
				result = result.thenComposeAsync(applyBatch, continuationExecutor);
			}
		}
		return result;
	}

	private static List<ConfigChangeBatch> createBatches(
		List<ConfigValueChange<?>> changes,
		Function<IConfigScreenValue<?>, Optional<IConfigSchema>> schemaResolver
	) {
		List<ConfigChangeBatch> batches = new ArrayList<>();
		IdentityHashMap<IConfigSchema, ConfigChangeBatch> batchesBySchema = new IdentityHashMap<>();
		for (ConfigValueChange<?> change : changes) {
			IConfigSchema schema = schemaResolver.apply(change.configValue())
				.orElse(null);
			if (schema == null) {
				batches.add(new ConfigChangeBatch(null, List.of(change)));
				continue;
			}
			ConfigChangeBatch batch = batchesBySchema.get(schema);
			if (batch == null) {
				batch = new ConfigChangeBatch(schema, new ArrayList<>());
				batchesBySchema.put(schema, batch);
				batches.add(batch);
			}
			batch.changes().add(change);
		}
		return batches;
	}

	static ConfigChangesResult applySequentially(List<ConfigValueChange<?>> changes) {
		List<AppliedConfigValueChange<?>> appliedChanges = new ArrayList<>();
		ConfigValueRestartRequirement restartRequirement = ConfigValueRestartRequirement.NONE;
		for (ConfigValueChange<?> change : changes) {
			try {
				Optional<? extends AppliedConfigValueChange<?>> appliedChange = applyChange(change);
				if (appliedChange.isPresent()) {
					appliedChanges.add(appliedChange.get());
					restartRequirement = getLargerRestartRequirement(
						restartRequirement,
						change.configValue().getRestartRequirement()
					);
				}
			} catch (RuntimeException exception) {
				return new ConfigChangesResult(
					appliedChanges,
					restartRequirement,
					new ConfigChangeFailure(change, exception)
				);
			}
		}
		return new ConfigChangesResult(appliedChanges, restartRequirement, null);
	}

	private static CompletableFuture<ConfigChangesResult> applySchemaChanges(
		IConfigSchema schema,
		List<ConfigValueChange<?>> changes,
		RemoteChangesHandler remoteChangesHandler
	) {
		List<AppliedChangeCandidate<?>> candidates = new ArrayList<>();
		CompletableFuture<Void> request;
		try {
			for (ConfigValueChange<?> change : changes) {
				candidates.add(createCandidate(change));
			}
			if (!schema.isActive()) {
				throw new IllegalStateException("This config schema is not active.");
			}
			if (schema.getType() != ConfigSchemaType.SERVER || schema.getPath().isPresent()) {
				schema.batchUpdate(updater -> changes.forEach(change -> queueChange(updater, schema, change)));
				request = CompletableFuture.completedFuture(null);
			} else {
				request = Objects.requireNonNull(
					remoteChangesHandler.requestUpdate(schema, changes),
					"remote config request result"
				);
			}
		} catch (RuntimeException exception) {
			return CompletableFuture.completedFuture(ConfigChangesResult.failure(changes.getFirst(), exception));
		}
		return request.handle((ignored, throwable) -> {
			if (throwable != null) {
				return ConfigChangesResult.failure(changes.getFirst(), asRuntimeException(throwable));
			}
			try {
				List<AppliedConfigValueChange<?>> appliedChanges = new ArrayList<>();
				for (AppliedChangeCandidate<?> candidate : candidates) {
					candidate.createAppliedChange().ifPresent(appliedChanges::add);
				}
				return ConfigChangesResult.success(appliedChanges);
			} catch (RuntimeException exception) {
				return ConfigChangesResult.failure(changes.getFirst(), exception);
			}
		});
	}

	private static RuntimeException asRuntimeException(Throwable throwable) {
		Throwable cause = throwable;
		while (cause instanceof CompletionException && cause.getCause() != null) {
			cause = cause.getCause();
		}
		if (cause instanceof RuntimeException runtimeException) {
			return runtimeException;
		}
		return new RuntimeException(cause);
	}

	private static <T> AppliedChangeCandidate<T> createCandidate(ConfigValueChange<T> change) {
		return new AppliedChangeCandidate<>(change.configValue(), change.configValue().getValue());
	}

	private static <T> void queueChange(
		IConfigBatchUpdater updater,
		IConfigSchema schema,
		ConfigValueChange<T> change
	) {
		IConfigValue<T> configValue = findBackingConfigValue(schema, change.configValue());
		updater.set(configValue, change.value());
	}

	@SuppressWarnings("unchecked")
	private static <T> IConfigValue<T> findBackingConfigValue(
		IConfigSchema schema,
		IConfigScreenValue<T> screenValue
	) {
		Object identityKey = screenValue.getIdentityKey();
		return (IConfigValue<T>) schema.getCategories()
			.stream()
			.flatMap(category -> category.getConfigValues().stream())
			.filter(configValue -> configValue == identityKey)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
				"Config value does not have a MezzConfig backing value in this schema: " + screenValue.getName()
			));
	}

	private static <T> Optional<AppliedConfigValueChange<T>> applyChange(ConfigValueChange<T> change) {
		T oldValue = change.configValue().getValue();
		if (!change.apply()) {
			return Optional.empty();
		}
		return Optional.of(new AppliedConfigValueChange<>(change.configValue(), oldValue, change.value()));
	}

	private static ConfigValueRestartRequirement getLargerRestartRequirement(
		ConfigValueRestartRequirement first,
		ConfigValueRestartRequirement second
	) {
		if (first == ConfigValueRestartRequirement.GAME_RESTART || second == ConfigValueRestartRequirement.GAME_RESTART) {
			return ConfigValueRestartRequirement.GAME_RESTART;
		}
		if (first == ConfigValueRestartRequirement.WORLD_RESTART || second == ConfigValueRestartRequirement.WORLD_RESTART) {
			return ConfigValueRestartRequirement.WORLD_RESTART;
		}
		return ConfigValueRestartRequirement.NONE;
	}

	record ConfigChangeBatch(
		IConfigSchema schema,
		List<ConfigValueChange<?>> changes
	) {
		private CompletableFuture<ConfigChangesResult> apply(RemoteChangesHandler remoteChangesHandler) {
			if (schema == null) {
				return CompletableFuture.completedFuture(applySequentially(changes));
			}
			return applySchemaChanges(schema, changes, remoteChangesHandler);
		}
	}

	@FunctionalInterface
	interface RemoteChangesHandler {
		CompletableFuture<Void> requestUpdate(IConfigSchema schema, List<ConfigValueChange<?>> changes);
	}

	record AppliedChangeCandidate<T>(
		IConfigScreenValue<T> configValue,
		T oldValue
	) {
		private Optional<AppliedConfigValueChange<T>> createAppliedChange() {
			T newValue = configValue.getValue();
			if (Objects.equals(oldValue, newValue)) {
				return Optional.empty();
			}
			return Optional.of(new AppliedConfigValueChange<>(configValue, oldValue, newValue));
		}
	}
}

record ConfigChangesResult(
	List<AppliedConfigValueChange<?>> appliedChanges,
	ConfigValueRestartRequirement restartRequirement,
	@Nullable ConfigChangeFailure failure
) {
	ConfigChangesResult {
		appliedChanges = List.copyOf(appliedChanges);
		Objects.requireNonNull(restartRequirement, "restartRequirement");
	}

	boolean succeeded() {
		return failure == null;
	}

	static ConfigChangesResult success() {
		return new ConfigChangesResult(List.of(), ConfigValueRestartRequirement.NONE, null);
	}

	static ConfigChangesResult success(List<AppliedConfigValueChange<?>> appliedChanges) {
		ConfigValueRestartRequirement restartRequirement = ConfigValueRestartRequirement.NONE;
		for (AppliedConfigValueChange<?> change : appliedChanges) {
			restartRequirement = getLargerRestartRequirement(
				restartRequirement,
				change.configValue().getRestartRequirement()
			);
		}
		return new ConfigChangesResult(appliedChanges, restartRequirement, null);
	}

	static ConfigChangesResult failure(ConfigValueChange<?> change, RuntimeException exception) {
		return new ConfigChangesResult(
			List.of(),
			ConfigValueRestartRequirement.NONE,
			new ConfigChangeFailure(change, exception)
		);
	}

	ConfigChangesResult append(ConfigChangesResult other) {
		List<AppliedConfigValueChange<?>> combinedChanges = new ArrayList<>(appliedChanges);
		combinedChanges.addAll(other.appliedChanges);
		@Nullable
		ConfigChangeFailure combinedFailure = failure;
		if (other.failure != null) {
			combinedFailure = other.failure;
		}
		return new ConfigChangesResult(
			combinedChanges,
			getLargerRestartRequirement(restartRequirement, other.restartRequirement),
			combinedFailure
		);
	}

	private static ConfigValueRestartRequirement getLargerRestartRequirement(
		ConfigValueRestartRequirement first,
		ConfigValueRestartRequirement second
	) {
		if (first == ConfigValueRestartRequirement.GAME_RESTART || second == ConfigValueRestartRequirement.GAME_RESTART) {
			return ConfigValueRestartRequirement.GAME_RESTART;
		}
		if (first == ConfigValueRestartRequirement.WORLD_RESTART || second == ConfigValueRestartRequirement.WORLD_RESTART) {
			return ConfigValueRestartRequirement.WORLD_RESTART;
		}
		return ConfigValueRestartRequirement.NONE;
	}
}

record ConfigChangeFailure(ConfigValueChange<?> change, RuntimeException exception) {
	ConfigChangeFailure {
		Objects.requireNonNull(change, "change");
		Objects.requireNonNull(exception, "exception");
	}
}
